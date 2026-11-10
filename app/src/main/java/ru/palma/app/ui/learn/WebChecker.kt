package ru.palma.app.ui.learn

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import ru.palma.app.learn.RunOutcome
import kotlin.coroutines.resume

class WebChecker(private val context: Context) {

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun check(html: String, expression: String): RunOutcome = withContext(Dispatchers.Main) {
        var view: WebView? = null
        val outcome = withTimeoutOrNull(TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val web = WebView(context).also { view = it }
                web.settings.javaScriptEnabled = true
                web.measure(
                    View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(HEIGHT, View.MeasureSpec.EXACTLY),
                )
                web.layout(0, 0, WIDTH, HEIGHT)
                web.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(page: WebView, url: String?) {
                        page.evaluateJavascript(script(expression)) { result ->
                            if (continuation.isActive) continuation.resume(RunOutcome.Checked(result == "true"))
                        }
                    }
                }
                web.loadDataWithBaseURL(BASE, html, "text/html", "utf-8", null)
            }
        }
        view?.destroy()
        outcome ?: RunOutcome.Timeout
    }

    companion object {
        const val BASE = "https://lesson.palma.local/"
        private const val TIMEOUT_MS = 8_000L
        private const val WIDTH = 1080
        private const val HEIGHT = 1920

        fun script(expression: String): String =
            "(function(){try{return !!(eval(" + JSONObject.quote(expression) + "))}catch(e){return false}})()"
    }
}
