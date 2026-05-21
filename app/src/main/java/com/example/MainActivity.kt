package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    // WebChromeClient support for file chooser inputs (<input type="file">)
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val results = WebChromeClient.FileChooserParams.parseResult(result.resultCode, data)
            filePathCallback?.onReceiveValue(results)
        } else {
            filePathCallback?.onReceiveValue(null)
        }
        filePathCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) { innerPadding ->
                    WebViewScreen(
                        url = "file:///android_asset/index.html",
                        onShowFileChooser = { callback, params ->
                            filePathCallback?.onReceiveValue(null)
                            filePathCallback = callback
                            try {
                                val intent = params.createIntent()
                                fileChooserResultLauncher.launch(intent)
                                true
                            } catch (e: Exception) {
                                callback.onReceiveValue(null)
                                filePathCallback = null
                                false
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WebViewScreen(
    url: String,
    onShowFileChooser: (ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams) -> Boolean,
    modifier: Modifier = Modifier
) {
    var webViewKey by remember { mutableStateOf(0) }

    key(webViewKey) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    // Adjust layout to fill parent perfectly
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    // Configure WebView settings
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        supportZoom()
                    }

                    // Attach custom clients
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                            // Keep navigation internal rather than launching system browser
                            return false
                        }

                        override fun onRenderProcessGone(
                            view: WebView?,
                            detail: android.webkit.RenderProcessGoneDetail?
                        ): Boolean {
                            // Complete renewal of the WebView instance by changing the key
                            try {
                                webViewKey++
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            return true // Return true to confirm we handled the crash ourselves
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onShowFileChooser(
                            webView: WebView?,
                            filePathCallback: ValueCallback<Array<Uri>>?,
                            fileChooserParams: FileChooserParams?
                        ): Boolean {
                            if (filePathCallback != null && fileChooserParams != null) {
                                return onShowFileChooser(filePathCallback, fileChooserParams)
                            }
                            return super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
                        }
                    }

                    loadUrl(url)
                }
            },
            modifier = modifier.fillMaxSize()
        )
    }
}
