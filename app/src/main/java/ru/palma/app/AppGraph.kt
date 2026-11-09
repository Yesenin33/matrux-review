package ru.palma.app

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.StatFs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.palma.app.billing.BillingGateway
import ru.palma.app.billing.Offers
import ru.palma.app.billing.Plan
import ru.palma.app.billing.PlanOffer
import ru.palma.app.billing.StoreNotConnected
import ru.palma.app.data.Catalog
import ru.palma.app.data.Failure
import ru.palma.app.data.FailureException
import ru.palma.app.data.Failures
import ru.palma.app.data.KeystoreSecretStore
import ru.palma.app.data.PalmaApi
import ru.palma.app.data.PalmaRepository
import ru.palma.app.data.PrefsLocalState
import ru.palma.app.data.ResumableDownloader
import ru.palma.app.data.SubscriptionStatus
import ru.palma.app.install.InstallController
import ru.palma.app.session.SessionController
import ru.palma.app.shell.NativeX11Server
import ru.palma.app.shell.ProotEngine
import ru.palma.app.ui.state.Load
import java.io.File

class AppGraph(context: Context) {
    private val app = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val local = PrefsLocalState(app)
    private val api = PalmaApi(BuildConfig.API_BASE)
    val repository = PalmaRepository(
        api = api,
        local = local,
        secrets = KeystoreSecretStore(app),
        pinnedKey = BuildConfig.MANIFEST_PUBKEY,
        deviceName = Build.MODEL,
    )
    private val connectivity = app.getSystemService(ConnectivityManager::class.java)
    private val storage = object : InstallController.Storage {
        override val downloads = File(app.filesDir, "downloads")
        override val systems = File(app.filesDir, "systems")
        override fun freeBytes() = StatFs(app.filesDir.path).availableBytes
        override fun metered() = connectivity?.isActiveNetworkMetered == true
    }
    val installs = InstallController(repository, local, ResumableDownloader(repository.http), storage, scope)
    val session = SessionController(
        repository = repository,
        local = local,
        installs = installs,
        engine = ProotEngine(File(app.applicationInfo.nativeLibraryDir), File(app.cacheDir, "proot")),
        x11 = NativeX11Server(File(app.applicationInfo.nativeLibraryDir)),
        tmpDir = File(app.cacheDir, "x11"),
        scope = scope,
        geometry = ::geometry,
    )
    @Volatile
    var portraitLock: Boolean? = null

    private fun geometry(): String {
        val point = android.graphics.Point()
        val wm = app.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealSize(point)
        var width = point.x.coerceAtLeast(320)
        var height = point.y.coerceAtLeast(320)
        val portrait = portraitLock ?: (width <= height)
        if (portrait && width > height) { val t = width; width = height; height = t }
        if (!portrait && width < height) { val t = width; width = height; height = t }
        return if (width <= height) {
            if (width <= MAX_WIDTH) "${width}x$height" else "${MAX_WIDTH}x${height * MAX_WIDTH / width}"
        } else {
            if (height <= MAX_WIDTH) "${width}x$height" else "${width * MAX_WIDTH / height}x$MAX_WIDTH"
        }
    }

    val billing: BillingGateway = StoreNotConnected()
    val offers: List<PlanOffer> = Offers.fromBuildConfig()

    private val catalogState = MutableStateFlow<Load<Catalog>>(Load.Loading)
    val catalog: StateFlow<Load<Catalog>> = catalogState.asStateFlow()
    private val statusState = MutableStateFlow(repository.cachedStatus)
    val status: StateFlow<SubscriptionStatus?> = statusState.asStateFlow()
    private val statusFailureState = MutableStateFlow<Failure?>(null)
    val statusFailure: StateFlow<Failure?> = statusFailureState.asStateFlow()
    private val purchaseState = MutableStateFlow<Pair<Boolean, Failure?>>(false to null)
    val purchase: StateFlow<Pair<Boolean, Failure?>> = purchaseState.asStateFlow()
    private val selectedPlanState = MutableStateFlow(Plan.PRO_MONTH)
    val selectedPlan: StateFlow<Plan> = selectedPlanState.asStateFlow()

    fun freeBytes(): Long = storage.freeBytes()
    fun metered(): Boolean = storage.metered()

    fun refresh() {
        scope.launch {
            if (catalogState.value !is Load.Ready) catalogState.value = Load.Loading
            catalogState.value = try {
                Load.Ready(repository.catalog())
            } catch (error: Throwable) {
                Load.Failed(Failures.fromThrowable(error))
            }
        }
        scope.launch { refreshStatus() }
    }

    private suspend fun refreshStatus() {
        try {
            statusState.value = repository.refreshStatus()
            statusFailureState.value = null
        } catch (error: Throwable) {
            statusFailureState.value = Failures.fromThrowable(error)
        }
    }

    fun reRegister() {
        repository.forgetDevice()
        statusState.value = null
        statusFailureState.value = null
        refresh()
    }

    fun select(plan: Plan) {
        selectedPlanState.value = plan
        purchaseState.value = false to null
    }

    fun buy(offer: PlanOffer) {
        if (purchaseState.value.first) return
        purchaseState.value = true to null
        scope.launch {
            purchaseState.value = try {
                val purchaseId = billing.purchase(offer)
                statusState.value = repository.verifyPurchase(purchaseId, offer.productId).status
                false to null
            } catch (error: FailureException) {
                false to error.failure
            } catch (error: Throwable) {
                false to Failures.fromThrowable(error)
            }
        }
    }
}

private const val MAX_WIDTH = 900
