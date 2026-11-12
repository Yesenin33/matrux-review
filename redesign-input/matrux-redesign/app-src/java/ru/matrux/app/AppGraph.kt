package ru.matrux.app

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.StatFs
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.matrux.app.account.AccountInfo
import ru.matrux.app.account.AuthGateway
import ru.matrux.app.account.Onboarding
import ru.matrux.app.account.Pkce
import ru.matrux.app.billing.BillingGateway
import ru.matrux.app.billing.Checkout
import ru.matrux.app.billing.Offers
import ru.matrux.app.billing.Plan
import ru.matrux.app.billing.PlanChoice
import ru.matrux.app.billing.PlanOffer
import ru.matrux.app.billing.PurchaseGate
import ru.matrux.app.data.Catalog
import ru.matrux.app.data.Failure
import ru.matrux.app.data.Failures
import ru.matrux.app.data.KeystoreSecretStore
import ru.matrux.app.data.MatruxApi
import ru.matrux.app.data.MatruxRepository
import ru.matrux.app.data.PrefsLocalState
import ru.matrux.app.data.ResumableDownloader
import ru.matrux.app.data.SubscriptionStatus
import ru.matrux.app.install.InstallController
import ru.matrux.app.learn.CodeRunner
import ru.matrux.app.learn.CourseLibrary
import ru.matrux.app.learn.FileProgressStore
import ru.matrux.app.learn.GuestRunner
import ru.matrux.app.learn.ProgressStore
import ru.matrux.app.learn.RunOutcome
import ru.matrux.app.learn.Runtime
import ru.matrux.app.learn.Task
import ru.matrux.app.ui.learn.WebChecker
import ru.matrux.app.session.SessionController
import ru.matrux.app.shell.NativeX11Server
import ru.matrux.app.shell.ProotEngine
import ru.matrux.app.store.StoreSdk
import ru.matrux.app.ui.state.Load
import java.io.File

class AppGraph(context: Context) {
    private val app = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val local = PrefsLocalState(app)
    private val themeState = MutableStateFlow(local.theme)
    val theme: StateFlow<String?> = themeState.asStateFlow()

    fun setTheme(value: String?) {
        local.theme = value
        themeState.value = value
    }
    private val api = MatruxApi(BuildConfig.API_BASE)
    val billing: BillingGateway = StoreSdk.billing(app as Application)
    val auth: AuthGateway = StoreSdk.auth(app as Application)
    val repository = MatruxRepository(
        api = api,
        local = local,
        secrets = KeystoreSecretStore(app),
        pinnedKey = BuildConfig.MANIFEST_PUBKEY,
        deviceName = Build.MODEL,
        storePurchases = billing::activePurchases,
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
        engine = ProotEngine(
            nativeLibraryDir = File(app.applicationInfo.nativeLibraryDir),
            tmpDir = File(app.cacheDir, "proot"),
            sessionScript = { app.assets.open("matrux-session").use { it.readBytes() } },
            homeDir = File(app.filesDir, "home"),
            dnsServers = ::dns,
        ),
        x11 = NativeX11Server(File(app.applicationInfo.nativeLibraryDir)),
        tmpDir = File(app.cacheDir, "x11"),
        guestTmp = File(app.cacheDir, "proot"),
        scope = scope,
        geometry = ::geometry,
    )
    @Volatile
    var portraitLock: Boolean? = null

    private fun dns(): String = runCatching {
        val link = connectivity?.activeNetwork?.let { connectivity.getLinkProperties(it) }
        link?.dnsServers
            ?.mapNotNull { it.hostAddress }
            ?.filter { it.contains('.') }
            ?.distinct()
            ?.joinToString(",")
            .orEmpty()
    }.getOrDefault("")

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

    val plans = PlanChoice(Offers.fromBuildConfig())
    val onboarding = Onboarding(local, ru.matrux.app.BuildConfig.VERSION_CODE)

    val courses = CourseLibrary { name ->
        app.assets.open(name).bufferedReader().use { it.readText() }
    }
    val progress: ProgressStore = FileProgressStore(File(app.filesDir, "learning.json"))
    private val runner: CodeRunner = GuestRunner(
        nativeLibraryDir = File(app.applicationInfo.nativeLibraryDir),
        tmpDir = File(app.cacheDir, "lesson"),
        rootfs = { local.installed?.imageId?.let(installs::rootfs) },
    )
    private val web = WebChecker(app)

    init {
        scope.launch(Dispatchers.IO) {
            courses.courses
            courses.sheets
        }
    }

    suspend fun check(task: Task.Run, code: String): RunOutcome =
        if (task.runtime == Runtime.WEB) web.check(code, task.check.orEmpty()) else runner.run(task.runtime, code, task.stdin)

    private val catalogState = MutableStateFlow<Load<Catalog>>(Load.Loading)
    val catalog: StateFlow<Load<Catalog>> = catalogState.asStateFlow()
    private val statusState = MutableStateFlow(repository.cachedStatus)
    val status: StateFlow<SubscriptionStatus?> = statusState.asStateFlow()
    private val statusFailureState = MutableStateFlow<Failure?>(null)
    val statusFailure: StateFlow<Failure?> = statusFailureState.asStateFlow()
    private val purchaseState = MutableStateFlow<Pair<Boolean, Failure?>>(false to null)
    val purchase: StateFlow<Pair<Boolean, Failure?>> = purchaseState.asStateFlow()
    private val accountState = MutableStateFlow(repository.account)
    val account: StateFlow<AccountInfo?> = accountState.asStateFlow()
    private val signInState = MutableStateFlow<Pair<Boolean, Failure?>>(false to null)
    val signIn: StateFlow<Pair<Boolean, Failure?>> = signInState.asStateFlow()

    fun freeBytes(): Long = storage.freeBytes()
    fun metered(): Boolean = storage.metered()

    fun refresh() {
        installs.refreshInstalled()
        scope.launch {
            if (catalogState.value !is Load.Ready) catalogState.value = Load.Loading
            catalogState.value = try {
                Load.Ready(repository.catalog())
            } catch (error: Throwable) {
                Load.Failed(Failures.fromThrowable(error))
            }
        }
        scope.launch { refreshStatus() }
        scope.launch { runCatching { repository.plans() }.onSuccess(plans::applyServer) }
    }

    private suspend fun refreshStatus() {
        try {
            statusState.value = repository.refreshStatus()
            statusFailureState.value = null
        } catch (error: Throwable) {
            statusFailureState.value = Failures.fromThrowable(error)
        }
        accountState.value = repository.account
    }

    fun signIn(owner: LifecycleOwner) {
        if (signInState.value.first) return
        signInState.value = true to null
        scope.launch {
            signInState.value = try {
                authorize(owner)
                gate.onSignedIn()
                false to null
            } catch (error: Throwable) {
                val failure = Failures.fromThrowable(error)
                false to failure.takeIf { it != Failure.Cancelled }
            }
        }
    }

    private suspend fun authorize(owner: LifecycleOwner) {
        val request = Pkce.request()
        val code = auth.authorize(owner, request)
        statusState.value = repository.signIn(code, request)
        statusFailureState.value = null
        accountState.value = repository.account
    }

    fun signOut() {
        scope.launch {
            repository.signOut()
            accountState.value = null
            signInState.value = false to null
            refreshStatus()
        }
    }

    fun reRegister() {
        repository.forgetDevice()
        statusState.value = null
        statusFailureState.value = null
        refresh()
    }

    fun select(plan: Plan) {
        plans.select(plan)
        purchaseState.value = false to null
    }

    val gate = PurchaseGate({ !repository.account?.id.isNullOrBlank() }, ::purchase)

    fun buy(offer: PlanOffer) {
        if (purchaseState.value.first) return
        gate.buy(offer)
    }

    private fun purchase(offer: PlanOffer) {
        if (purchaseState.value.first) return
        purchaseState.value = true to null
        val checkout = Checkout(billing, { repository.account?.id }, {}, repository::verifyPurchase)
        scope.launch {
            val failure = checkout.attempt(offer) { statusState.value = it.status }
            purchaseState.value = false to failure
        }
    }
}

private const val MAX_WIDTH = 900
