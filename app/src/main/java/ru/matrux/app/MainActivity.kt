package ru.matrux.app

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
import ru.matrux.app.data.Catalog
import ru.matrux.app.data.ImageInfo
import ru.matrux.app.data.InstalledSystem
import ru.matrux.app.install.InstallState
import ru.matrux.app.session.SessionState
import ru.matrux.app.service.InstallService
import ru.matrux.app.service.SessionService
import ru.matrux.app.store.StoreSdk
import ru.matrux.app.shell.LaunchState
import ru.matrux.app.ui.Keys
import ru.matrux.app.ui.components.Tab
import ru.matrux.app.ui.screens.CatalogScreen
import ru.matrux.app.ui.screens.DesktopActions
import ru.matrux.app.ui.screens.DesktopScreen
import ru.matrux.app.ui.screens.DesktopUi
import ru.matrux.app.ui.screens.InputMode
import ru.matrux.app.ui.screens.HomeActions
import ru.matrux.app.ui.screens.HomeScreen
import ru.matrux.app.ui.screens.InstallActions
import ru.matrux.app.ui.screens.InstallScreen
import ru.matrux.app.ui.screens.LaunchScreen
import ru.matrux.app.ui.screens.KnowledgeScreen
import ru.matrux.app.ui.screens.KnowledgeActions
import ru.matrux.app.ui.screens.CourseActions
import ru.matrux.app.ui.screens.CourseScreen
import ru.matrux.app.ui.screens.LessonActions
import ru.matrux.app.ui.screens.LessonScreen
import ru.matrux.app.ui.screens.SourcesActions
import ru.matrux.app.ui.screens.SourcesScreen
import ru.matrux.app.learn.Answer
import ru.matrux.app.learn.Course
import ru.matrux.app.learn.Grader
import ru.matrux.app.learn.Lesson
import ru.matrux.app.learn.Task
import ru.matrux.app.learn.Verdict
import ru.matrux.app.ui.state.Attempt
import ru.matrux.app.ui.state.courseUi
import ru.matrux.app.ui.state.lessonUi
import ru.matrux.app.ui.screens.OnboardingOverlay
import ru.matrux.app.ui.screens.ProOfferOverlay
import ru.matrux.app.ui.screens.SectionActions
import ru.matrux.app.ui.screens.SectionScreen
import ru.matrux.app.ui.screens.SubscriptionActions
import ru.matrux.app.ui.screens.SubscriptionScreen
import ru.matrux.app.ui.state.SectionRow
import ru.matrux.app.ui.state.SystemAction
import ru.matrux.app.ui.state.Load
import ru.matrux.app.ui.state.SubscriptionUi
import ru.matrux.app.ui.state.AccountUi
import ru.matrux.app.ui.state.knowledgeUi
import ru.matrux.app.ui.state.onboardUi
import ru.matrux.app.ui.state.proOffer
import ru.matrux.app.ui.state.catalogUi
import ru.matrux.app.ui.state.homeSections
import ru.matrux.app.ui.state.paidSub
import ru.matrux.app.ui.state.subLine
import ru.matrux.app.ui.theme.Matrux
import ru.matrux.app.ui.theme.MatruxTheme
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Route { HOME, CATALOG, INSTALL, KNOWLEDGE, SUBSCRIPTION, LAUNCH, DESKTOP, SECTION, COURSE, LESSON, SOURCES }

private const val STUCK_MS = 25_000L
private const val AUTOTEST_EXTRA = "matrux_autotest"
private const val AUTOTEST_TAG = "matrux-autotest"
private const val AUTOTEST_IMAGE = "alpine-xfce"
private const val AUTOTEST_IMAGE_EXTRA = "matrux_image"
private const val AUTOTEST_ROTATION_EXTRA = "matrux_rotation"
private const val AUTOTEST_SHADE_EXTRA = "matrux_shade"
private const val AUTOTEST_ACTION_EXTRA = "matrux_action"
private const val AUTOTEST_LOGIN_EXTRA = "matrux_login"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        graph.refresh()
        StoreSdk.handleIntent(this, intent, savedInstanceState == null)
        applyRotationExtra(intent)
        setContent { MatruxTheme { MatruxApp() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        StoreSdk.handleIntent(this, intent, firstLaunch = false)
        applyRotationExtra(intent)
    }

    private fun applyRotationExtra(intent: Intent) {
        if (!BuildConfig.DEBUG) return
        android.util.Log.i("matrux-geo", "extra=" + intent.getStringExtra(AUTOTEST_ROTATION_EXTRA) + " debug=" + BuildConfig.DEBUG)
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

    private fun switchTo(image: ImageInfo) {
        val sha = graph.installs.installedShas.value[image.id] ?: image.sha256
        graph.local.installed = InstalledSystem(
            imageId = image.id,
            name = image.name,
            distro = image.distro,
            desktop = image.desktop,
            sizeBytes = image.sizeBytes,
            sha256 = sha,
            lastLaunchMillis = null,
        )
        launch()
    }

    private fun launch() {
        graph.session.start()
        SessionService.start(this)
    }

    private fun openLink(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (error: ActivityNotFoundException) {
            Log.w("matrux-learn", "no browser for $url")
        }
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
    private fun MatruxApp() {
        val graph = graph
        val zone = remember { ZoneId.systemDefault() }
        var route by rememberSaveable { mutableStateOf(Route.HOME) }
        var sectionId by rememberSaveable { mutableStateOf("") }
        var courseId by rememberSaveable { mutableStateOf("") }
        var lessonId by rememberSaveable { mutableStateOf("") }
        var attempt by remember { mutableStateOf<Pair<String, Attempt>>("" to Attempt.Idle) }
        val progress by graph.progress.progress.collectAsStateWithLifecycle()
        val courses = graph.courses.courses
        val sheets = graph.courses.sheets
        val catalog by graph.catalog.collectAsStateWithLifecycle()
        val status by graph.status.collectAsStateWithLifecycle()
        val statusFailure by graph.statusFailure.collectAsStateWithLifecycle()
        val install by graph.installs.state.collectAsStateWithLifecycle()
        val installedShas by graph.installs.installedShas.collectAsStateWithLifecycle()
        val session by graph.session.state.collectAsStateWithLifecycle()
        val hasFrame by remember(graph) {
            graph.session.frame.map { it != null }.distinctUntilChanged()
        }.collectAsStateWithLifecycle(false)
        val purchase by graph.purchase.collectAsStateWithLifecycle()
        val account by graph.account.collectAsStateWithLifecycle()
        val signedIn = !account?.id.isNullOrBlank()
        val signIn by graph.signIn.collectAsStateWithLifecycle()
        val blank by graph.session.blank.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()
        val terms by graph.plans.terms.collectAsStateWithLifecycle()
        var offerOpen by rememberSaveable { mutableStateOf(false) }
        val onboardFlags by graph.onboarding.flags.collectAsStateWithLifecycle()

        if (BuildConfig.DEBUG && intent.getBooleanExtra(AUTOTEST_EXTRA, false)) {
            var stage by remember { mutableStateOf(0) }
            val autoImages = (catalog as? Load.Ready<Catalog>)?.value?.manifest?.images.orEmpty()
            val autoInstalled = graph.local.installed
            LaunchedEffect(stage, autoImages.size, install, session) {
                if (stage == 0 && autoImages.isNotEmpty()) {
                    val wanted = intent.getStringExtra(AUTOTEST_IMAGE_EXTRA) ?: AUTOTEST_IMAGE
                    val image = autoImages.firstOrNull { it.id == wanted } ?: autoImages.first()
                    if (autoInstalled?.sha256.equals(image.sha256, ignoreCase = true)) {
                        Log.i(AUTOTEST_TAG, "autotest: ${image.id} installed, launching")
                        launch()
                        route = Route.LAUNCH
                        stage = 2
                    } else {
                        Log.i(AUTOTEST_TAG, "autotest: installing ${image.id} ${image.sha256.take(8)}")
                        install(image, allowMetered = true)
                        route = Route.INSTALL
                        stage = 1
                    }
                } else if (stage == 1 && install is InstallState.Done) {
                    Log.i(AUTOTEST_TAG, "autotest: install done, launching")
                    launch()
                    route = Route.LAUNCH
                    stage = 2
                }
                if (stage >= 2) {
                    Log.i(AUTOTEST_TAG, "state: $session")
                    if (session is SessionState.Lost) {
                        val dump = withContext(Dispatchers.IO) { graph.session.diagnostics() }
                        Log.i(AUTOTEST_TAG, "diagnostics: ${dump.replace('\n', '|')}")
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
                Tab.KNOWLEDGE -> Route.KNOWLEDGE
                Tab.SUBSCRIPTION -> Route.SUBSCRIPTION
            }
        }

        BackHandler(route != Route.HOME) {
            route = when (route) {
                Route.INSTALL -> Route.CATALOG
                Route.SECTION, Route.COURSE, Route.SOURCES -> Route.HOME
                Route.LESSON -> Route.COURSE
                else -> Route.HOME
            }
        }

        val paid = subLine(status, statusFailure, zone).paidSub()
        val openSection: (SectionRow) -> Unit = { row ->
            if (row.locked) {
                offerOpen = true
            } else {
                sectionId = row.section.id
                route = Route.SECTION
            }
        }
        val openLesson: (String, String) -> Unit = { course, lesson ->
            courseId = course
            lessonId = lesson
            attempt = "" to Attempt.Idle
            route = Route.LESSON
        }
        val check: (Course, Lesson, Answer) -> Unit = { course, lesson, answer ->
            val key = course.id + "/" + lesson.id
            val finish: (Verdict) -> Unit = { verdict ->
                attempt = key to Attempt.Result(verdict)
                if (verdict is Verdict.Pass) graph.progress.mark(course.id, lesson.id)
            }
            when (val task = lesson.task) {
                is Task.Choice -> finish(Grader.choice(task, (answer as Answer.Pick).index))
                is Task.Output -> finish(Grader.output(task, (answer as Answer.Text).text))
                is Task.Run -> {
                    attempt = key to Attempt.Running
                    scope.launch { finish(Grader.run(task, graph.check(task, (answer as Answer.Code).text))) }
                }
            }
        }
        val images = (catalog as? Load.Ready<Catalog>)?.value?.manifest?.images.orEmpty()
        val running = session is SessionState.Live || session is SessionState.Launching
        val subscription = SubscriptionUi(
            status = status,
            sub = subLine(status, statusFailure, zone),
            terms = terms,
            busy = purchase.first,
            failure = purchase.second,
            billingReady = graph.billing.available,
            account = AccountUi(account, graph.auth.available, signIn.first, signIn.second),
        )

        val motion = Matrux.motion
        val installedSystem = graph.local.installed
        AnimatedContent(
            targetState = route,
            transitionSpec = {
                fadeIn(tween(motion.duration(motion.medium), easing = motion.easing)) togetherWith
                    fadeOut(tween(motion.duration(motion.short), easing = motion.easing))
            },
            label = "route",
        ) { current ->
            when (current) {
                Route.HOME -> HomeScreen(
                    homeSections(sheets, paid),
                    HomeActions(
                        onSection = openSection,
                        onSubscribe = { route = Route.SUBSCRIPTION },
                        onTab = onTab,
                    ),
                )
                Route.CATALOG -> CatalogScreen(
                    catalogUi(catalog, status, statusFailure, graph.local.installed?.imageId, graph.local.bytesPerSecond, installedShas, running),
                    onAction = { row, action ->
                        when (action) {
                            SystemAction.OPEN -> route = if (session is SessionState.Live) Route.DESKTOP else Route.LAUNCH
                            SystemAction.STOP -> graph.session.stop()
                            SystemAction.LAUNCH -> {
                                launch()
                                route = Route.LAUNCH
                            }
                            SystemAction.SWITCH -> {
                                switchTo(row.image)
                                route = Route.LAUNCH
                            }
                            SystemAction.INSTALL, SystemAction.REINSTALL -> {
                                install(row.image)
                                route = Route.INSTALL
                            }
                            SystemAction.SUBSCRIBE -> route = Route.SUBSCRIPTION
                        }
                    },
                    onRetry = graph::refresh,
                    onTab = onTab,
                )
                Route.KNOWLEDGE -> KnowledgeScreen(KnowledgeActions(onTab = onTab))
                Route.COURSE -> {
                    val course = graph.courses.course(courseId) ?: courses.first()
                    CourseScreen(
                        courseUi(course, paid, progress),
                        CourseActions(
                            onBack = { route = Route.KNOWLEDGE },
                            onLesson = { row -> if (row.locked) offerOpen = true else openLesson(course.id, row.lesson.id) },
                            onPro = { offerOpen = true },
                        ),
                    )
                }
                Route.LESSON -> {
                    val course = graph.courses.course(courseId) ?: courses.first()
                    val lesson = course.lesson(lessonId) ?: course.lessons.first()
                    val key = course.id + "/" + lesson.id
                    LessonScreen(
                        lessonUi(course, lesson, paid, progress, if (attempt.first == key) attempt.second else Attempt.Idle),
                        LessonActions(
                            onBack = { route = Route.COURSE },
                            onCheck = { answer -> check(course, lesson, answer) },
                            onNext = { course.next(lesson.id)?.let { openLesson(course.id, it.id) } },
                            onPro = { offerOpen = true },
                        ),
                    )
                }
                Route.SOURCES -> SourcesScreen(
                    courses,
                    graph.courses.sources,
                    SourcesActions(onBack = { route = Route.KNOWLEDGE }, onOpen = ::openLink),
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
                    subscription,
                    SubscriptionActions(
                        onSelect = graph::select,
                        onBuy = graph::buy,
                        onManage = ::manageSubscription,
                        onTab = onTab,
                        onSignIn = graph.gate::show,
                        onSignOut = graph::signOut,
                    ),
                )
                Route.SECTION -> {
                    val section = graph.courses.sheet(sectionId) ?: sheets.first()
                    SectionScreen(
                        section = section,
                        actions = SectionActions(onBack = { route = Route.HOME }),
                    )
                }
                Route.LAUNCH -> {
                    val launching = session as? SessionState.Launching
                    LaunchScreen(
                        state = launching?.launch ?: LaunchState.Running(ru.matrux.app.shell.LaunchStep.SUBSCRIPTION),
                        onRetry = ::launch,
                        onBack = { route = Route.CATALOG },
                    )
                }
                Route.DESKTOP -> {
                    val live = session as? SessionState.Live
                    val lost = session as? SessionState.Lost
                    val orientation = LocalConfiguration.current.orientation
                    LaunchedEffect(orientation) { graph.session.syncGeometry() }
                    val actions = remember(graph) {
                        DesktopActions(
                            onClose = {
                                graph.session.stop()
                                stopService(Intent(this@MainActivity, SessionService::class.java))
                                finishAndRemoveTask()
                            },
                            onMinimize = {
                                graph.session.stop()
                                route = Route.CATALOG
                            },
                            onRestart = {
                                report = ""
                                launch()
                                route = Route.LAUNCH
                            },
                            onReport = {
                                scope.launch { report = withContext(Dispatchers.IO) { graph.session.diagnostics() } }
                            },
                            onPointer = { x, y, buttons -> graph.session.pointer(x, y, buttons) },
                            onTap = { x, y -> graph.session.tap(x, y) },
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
                        )
                    }
                    if (BuildConfig.DEBUG) {
                        LaunchedEffect(Unit) {
                            val act = intent.getStringExtra(AUTOTEST_ACTION_EXTRA) ?: return@LaunchedEffect
                            delay(5_000)
                            android.util.Log.i(AUTOTEST_TAG, "auto action=$act")
                            if (act == "close") actions.onClose() else if (act == "collapse") actions.onMinimize()
                        }
                    }
                    DesktopScreen(
                        DesktopUi(
                            title = listOfNotNull(
                                live?.name ?: lost?.name ?: graph.local.installed?.name,
                                live?.desktop?.takeIf { !it.equals("none", true) }?.uppercase(),
                            ).joinToString(" · "),
                            mode = live?.mode ?: ru.matrux.app.shell.DisplayMode.VNC,
                            offline = live?.offline == true,
                            frame = graph.session.frame,
                            lost = lost?.failure,
                            hasFrame = hasFrame,
                            stuck = stuck,
                            report = report,
                            input = input,
                            pad = pad,
                            keyboard = keyboard,
                            russian = russian,
                        ),
                        actions,
                        openShade = BuildConfig.DEBUG && intent.getBooleanExtra(AUTOTEST_SHADE_EXTRA, false),
                    )
                }
            }
        }

        BackHandler(enabled = offerOpen) { offerOpen = false }
        ProOfferOverlay(
            visible = offerOpen,
            ui = proOffer(images, courses, sheets),
            onSubscribe = {
                offerOpen = false
                route = Route.SUBSCRIPTION
            },
            onClose = { offerOpen = false },
        )
        OnboardingOverlay(
            visible = onboardFlags.visible,
            ui = onboardUi(),
            onFinish = {
                route = Route.HOME
                offerOpen = false
                graph.onboarding.finish()
            },
            onSubscribe = {
                route = Route.SUBSCRIPTION
                offerOpen = false
                graph.onboarding.finish()
            },
        )
        if (BuildConfig.DEBUG) {
            LaunchedEffect(Unit) {
                if (intent.getBooleanExtra(AUTOTEST_LOGIN_EXTRA, false)) {
                    delay(1_500)
                    android.util.Log.i(AUTOTEST_TAG, "autotest: sign-in window")
                    graph.gate.show()
                }
            }
        }
    }
}
