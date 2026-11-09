package ru.palma.app

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.content.pm.PackageManager
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.palma.app.data.Catalog
import ru.palma.app.data.ImageInfo
import ru.palma.app.install.InstallState
import ru.palma.app.session.SessionState
import ru.palma.app.service.InstallService
import ru.palma.app.service.SessionService
import ru.palma.app.shell.LaunchState
import ru.palma.app.ui.Keys
import ru.palma.app.ui.components.Tab
import ru.palma.app.ui.screens.CatalogScreen
import ru.palma.app.ui.screens.DesktopActions
import ru.palma.app.ui.screens.DesktopScreen
import ru.palma.app.ui.screens.DesktopUi
import ru.palma.app.ui.screens.InputMode
import ru.palma.app.ui.screens.HomeActions
import ru.palma.app.ui.screens.HomeScreen
import ru.palma.app.ui.screens.InstallActions
import ru.palma.app.ui.screens.InstallScreen
import ru.palma.app.ui.screens.LaunchScreen
import ru.palma.app.ui.screens.SubscriptionActions
import ru.palma.app.ui.screens.SubscriptionScreen
import ru.palma.app.ui.state.CatalogAction
import ru.palma.app.ui.state.Load
import ru.palma.app.ui.state.SubscriptionUi
import ru.palma.app.ui.state.action
import ru.palma.app.ui.state.catalogImageSha
import ru.palma.app.ui.state.catalogUi
import ru.palma.app.ui.state.homeUi
import ru.palma.app.ui.state.subLine
import ru.palma.app.ui.theme.Palma
import ru.palma.app.ui.theme.PalmaTheme
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Route { HOME, CATALOG, INSTALL, SUBSCRIPTION, LAUNCH, DESKTOP }

private const val STUCK_MS = 25_000L
private const val AUTOTEST_EXTRA = "palma_autotest"
private const val AUTOTEST_TAG = "palma-autotest"
private const val AUTOTEST_IMAGE = "alpine-xfce"
private const val AUTOTEST_IMAGE_EXTRA = "palma_image"
private const val AUTOTEST_ROTATION_EXTRA = "palma_rotation"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        graph.refresh()
        applyRotationExtra(intent)
        setContent { PalmaTheme { PalmaApp() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        applyRotationExtra(intent)
    }

    private fun applyRotationExtra(intent: Intent) {
        if (!BuildConfig.DEBUG) return
        android.util.Log.i("palma-geo", "extra=" + intent.getStringExtra(AUTOTEST_ROTATION_EXTRA) + " debug=" + BuildConfig.DEBUG)
        when (intent.getStringExtra(AUTOTEST_ROTATION_EXTRA)) {
            "land" -> { graph.portraitLock = false; requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            "port" -> { graph.portraitLock = true; requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
        }
    }

    private fun rotate() {
        val toLandscape = resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
        graph.portraitLock = !toLandscape
        requestedOrientation =
            if (toLandscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    private fun askNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
    }

    private fun askBattery() {
        val pm = getSystemService(POWER_SERVICE) as? PowerManager ?: return
        if (pm.isIgnoringBatteryOptimizations(packageName)) return
        runCatching {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(Uri.parse("package:$packageName")))
        }
    }

    private fun install(image: ImageInfo, allowMetered: Boolean = false) {
        askNotifications()
        askBattery()
        graph.installs.request(image, allowMetered)
        if (graph.installs.state.value !is InstallState.ConfirmMetered) InstallService.start(this)
    }

    private fun launch() {
        graph.session.start()
        SessionService.start(this)
    }

    private fun manageSubscription() {
        val opened = listOf("rustore://profile/subscriptions").any { link ->
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                true
            } catch (error: ActivityNotFoundException) {
                false
            }
        }
        if (!opened) Toast.makeText(this, R.string.subs_manage_fallback, Toast.LENGTH_LONG).show()
    }

    @Composable
    private fun PalmaApp() {
        val graph = graph
        val zone = remember { ZoneId.systemDefault() }
        var route by rememberSaveable { mutableStateOf(Route.HOME) }
        val catalog by graph.catalog.collectAsStateWithLifecycle()
        val status by graph.status.collectAsStateWithLifecycle()
        val statusFailure by graph.statusFailure.collectAsStateWithLifecycle()
        val install by graph.installs.state.collectAsStateWithLifecycle()
        val session by graph.session.state.collectAsStateWithLifecycle()
        val frame by graph.session.frame.collectAsStateWithLifecycle()
        val purchase by graph.purchase.collectAsStateWithLifecycle()
        val blank by graph.session.blank.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()
        val plan by graph.selectedPlan.collectAsStateWithLifecycle()

        if (BuildConfig.DEBUG && intent.getBooleanExtra(AUTOTEST_EXTRA, false)) {
            var stage by remember { mutableStateOf(0) }
            val autoImages = (catalog as? Load.Ready<Catalog>)?.value?.manifest?.images.orEmpty()
            val autoInstalled = graph.local.installed
            LaunchedEffect(stage, autoImages.size, install, session) {
                if (stage == 0 && autoImages.isNotEmpty()) {
                    val wanted = intent.getStringExtra(AUTOTEST_IMAGE_EXTRA) ?: AUTOTEST_IMAGE
                    val image = autoImages.firstOrNull { it.id == wanted } ?: autoImages.first()
                    if (autoInstalled?.sha256.equals(image.sha256, ignoreCase = true)) {
                        Log.i(AUTOTEST_TAG, "автотест: ${image.id} уже стоит, запускаю")
                        launch()
                        route = Route.LAUNCH
                        stage = 2
                    } else {
                        Log.i(AUTOTEST_TAG, "автотест: ставлю ${image.id} ${image.sha256.take(8)}")
                        install(image, allowMetered = true)
                        route = Route.INSTALL
                        stage = 1
                    }
                } else if (stage == 1 && install is InstallState.Done) {
                    Log.i(AUTOTEST_TAG, "автотест: установка завершена, запускаю")
                    launch()
                    route = Route.LAUNCH
                    stage = 2
                }
                if (stage >= 2) {
                    Log.i(AUTOTEST_TAG, "состояние: $session")
                    if (session is SessionState.Lost) {
                        val dump = withContext(Dispatchers.IO) { graph.session.diagnostics() }
                        Log.i(AUTOTEST_TAG, "диагностика: ${dump.replace('\n', '|')}")
                    }
                }
            }
        }

        LaunchedEffect(session) {
            when (session) {
                is SessionState.Live -> route = Route.DESKTOP
                is SessionState.Lost -> if (route == Route.LAUNCH) route = Route.DESKTOP
                else -> Unit
            }
        }

        LaunchedEffect(route) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (route == Route.DESKTOP) {
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }

        val stuck by produceState(false, blank, session) {
            value = false
            if (blank && session is SessionState.Live) {
                delay(STUCK_MS)
                value = true
            }
        }
        var report by remember { mutableStateOf("") }
        var input by remember { mutableStateOf(InputMode.Touch) }
        var pad by remember { mutableStateOf(false) }
        var keyboard by remember { mutableStateOf(false) }
        val russian by graph.session.russian.collectAsStateWithLifecycle()
        LaunchedEffect(stuck) {
            if (stuck) report = withContext(Dispatchers.IO) { graph.session.diagnostics() }
        }

        val onTab: (Tab) -> Unit = { tab ->
            route = when (tab) {
                Tab.HOME -> Route.HOME
                Tab.CATALOG -> if (install is InstallState.Running || install is InstallState.Failed) Route.INSTALL else Route.CATALOG
                Tab.SUBSCRIPTION -> Route.SUBSCRIPTION
            }
        }

        BackHandler(route != Route.HOME) {
            route = when (route) {
                Route.INSTALL -> Route.CATALOG
                else -> Route.HOME
            }
        }

        val motion = Palma.motion
        AnimatedContent(
            targetState = route,
            transitionSpec = {
                fadeIn(tween(motion.duration(motion.medium), easing = motion.easing)) togetherWith
                    fadeOut(tween(motion.duration(motion.short), easing = motion.easing))
            },
            label = "route",
        ) { current ->
            when (current) {
                Route.HOME -> {
                    val installed = graph.local.installed
                    val ui = homeUi(
                        installed = installed,
                        installedReady = installed != null && graph.installs.isCurrent(installed.imageId, catalogImageSha(catalog, installed.imageId)),
                        catalog = catalog,
                        install = install,
                        session = session,
                        status = status,
                        statusFailure = statusFailure,
                        freeBytes = graph.freeBytes(),
                        zone = zone,
                    )
                    HomeScreen(
                        ui,
                        HomeActions(
                            onLaunch = {
                                launch()
                                route = Route.LAUNCH
                            },
                            onReturn = { route = if (session is SessionState.Live) Route.DESKTOP else Route.LAUNCH },
                            onStop = graph.session::stop,
                            onInstall = { image ->
                                install(image)
                                route = Route.INSTALL
                            },
                            onOpenInstall = { route = Route.INSTALL },
                            onCatalog = { route = Route.CATALOG },
                            onRetry = graph::refresh,
                            onReRegister = graph::reRegister,
                            onTab = onTab,
                        ),
                    )
                }
                Route.CATALOG -> CatalogScreen(
                    catalogUi(catalog, status, statusFailure, graph.local.installed?.imageId, graph.local.installed?.sha256, graph.local.bytesPerSecond),
                    onPick = { row ->
                        when (row.action()) {
                            CatalogAction.INSTALL, CatalogAction.REINSTALL -> {
                                install(row.image)
                                route = Route.INSTALL
                            }
                            CatalogAction.SUBSCRIBE -> route = Route.SUBSCRIPTION
                        }
                    },
                    onRetry = graph::refresh,
                    onTab = onTab,
                )
                Route.INSTALL -> InstallScreen(
                    install,
                    graph.metered(),
                    InstallActions(
                        onCancel = {
                            graph.installs.cancel()
                            route = Route.CATALOG
                        },
                        onRetry = {
                            val image = (install as? InstallState.Failed)?.image
                            if (image != null) {
                                graph.installs.dismiss()
                                install(image, allowMetered = true)
                            }
                        },
                        onConfirmMetered = {
                            val image = (install as? InstallState.ConfirmMetered)?.image
                            if (image != null) install(image, allowMetered = true)
                        },
                        onWaitWifi = {
                            graph.installs.dismiss()
                            route = Route.CATALOG
                        },
                        onLaunch = {
                            graph.installs.dismiss()
                            launch()
                            route = Route.LAUNCH
                        },
                        onHome = {
                            graph.installs.dismiss()
                            route = Route.HOME
                        },
                        onCatalog = { route = Route.CATALOG },
                        onTab = onTab,
                    ),
                )
                Route.SUBSCRIPTION -> SubscriptionScreen(
                    SubscriptionUi(
                        status = status,
                        sub = subLine(status, statusFailure, zone),
                        offers = graph.offers,
                        selected = plan,
                        busy = purchase.first,
                        failure = purchase.second,
                    ),
                    SubscriptionActions(
                        onSelect = graph::select,
                        onBuy = graph::buy,
                        onManage = ::manageSubscription,
                        onTab = onTab,
                    ),
                )
                Route.LAUNCH -> {
                    val launching = session as? SessionState.Launching
                    LaunchScreen(
                        state = launching?.launch ?: LaunchState.Running(ru.palma.app.shell.LaunchStep.SUBSCRIPTION),
                        onRetry = ::launch,
                        onBack = { route = Route.HOME },
                    )
                }
                Route.DESKTOP -> {
                    val live = session as? SessionState.Live
                    val lost = session as? SessionState.Lost
                    val orientation = LocalConfiguration.current.orientation
                    LaunchedEffect(orientation) { graph.session.syncGeometry() }
                    val bitmap = frame?.first?.frameBitmap()
                    DesktopScreen(
                        DesktopUi(
                            title = listOfNotNull(
                                live?.name ?: lost?.name ?: graph.local.installed?.name,
                                live?.desktop?.takeIf { !it.equals("none", true) }?.uppercase(),
                            ).joinToString(" · "),
                            mode = live?.mode ?: ru.palma.app.shell.DisplayMode.VNC,
                            offline = live?.offline == true,
                            frame = bitmap,
                            frameStamp = frame?.second ?: 0L,
                            lost = lost?.failure,
                            stuck = stuck,
                            report = report,
                            input = input,
                            pad = pad,
                            keyboard = keyboard,
                            russian = russian,
                        ),
                        DesktopActions(
                            onClose = {
                                graph.session.stop()
                                route = Route.HOME
                            },
                            onMinimize = { moveTaskToBack(true) },
                            onRestart = {
                                report = ""
                                launch()
                                route = Route.LAUNCH
                            },
                            onReport = {
                                scope.launch { report = withContext(Dispatchers.IO) { graph.session.diagnostics() } }
                            },
                            onPointer = { x, y, buttons -> graph.session.pointer(x, y, buttons) },
                            onMoveCursor = { dx, dy -> graph.session.moveCursor(dx, dy) },
                            onClick = { button -> graph.session.click(button) },
                            onScroll = { steps -> graph.session.scroll(steps) },
                            onKey = { keysym -> graph.session.key(keysym) },
                            onChar = { letter -> graph.session.typeChar(letter) },
                            onToggleInput = {
                                input = if (input == InputMode.Touch) InputMode.Mouse else InputMode.Touch
                                pad = input == InputMode.Mouse
                            },
                            onTogglePad = { pad = !pad },
                            onToggleKeyboard = { keyboard = !keyboard },
                            onToggleLayout = { graph.session.toggleLayout() },
                            onConsole = { graph.session.tap(139, 20) },
                            onSettings = { graph.session.combo(Keys.SUPER, 's'.code) },
                            onRotate = ::rotate,
                        ),
                    )
                }
            }
        }
    }
}
