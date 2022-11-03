package torcompose.com

import android.annotation.SuppressLint
import android.content.*
import android.content.Context.BIND_AUTO_CREATE
import android.os.Bundle
import android.os.IBinder
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import torcompose.com.ui.theme.TorComposeTheme
import org.torproject.jni.TorService
import org.torproject.jni.TorService.LocalBinder
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.MutableState
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction

class MainActivity : ComponentActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TorComposeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    TorComposeApp(
                        onConnectClick = {
                            onConnectClicked(this, it)
                        },
                        this
                    )
                }
            }
        }
    }
}

@Composable
fun TorComposeApp(
    onConnectClick: (status: MutableState<String>) -> Unit = {},
    ctx: MainActivity
) {
    val url = remember { mutableStateOf("") }
    val tempUrl = remember { mutableStateOf("") }
    val status = remember { mutableStateOf("Not connected") }
    val reqCount = remember { mutableStateOf(0) }
    val focusManager = LocalFocusManager.current

    Scaffold {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Button(
                onClick = { onConnectClick(status) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(8.dp)
            ) {
                Text(
                    text = "Connect to Tor Network",
                    style = MaterialTheme.typography.button
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextField(
                    value = tempUrl.value,
                    onValueChange = { tempUrl.value = it },
                    modifier = Modifier
                        .width(240.dp)
                        .fillMaxHeight()
                        .border(1.dp, MaterialTheme.colors.onSurface),
                    placeholder = { Text("Enter URL") },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = {
                        if (status.value == "Not connected") {
                            Toast.makeText(
                                ctx,
                                "Please connect to Tor network first",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            url.value = tempUrl.value
                        }
                        focusManager.clearFocus()
                    })
                )
                Button(
                    onClick = {
                        if (status.value == "Not connected") {
                            Toast.makeText(
                                ctx,
                                "Please connect to Tor network first",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            url.value = tempUrl.value
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Reload",
                        style = MaterialTheme.typography.button
                    )
                }
            }
            Text(
                text = "Tor Status: ${status.value}\nRequest count: ${reqCount.value}",
                style = MaterialTheme.typography.body2,
                modifier = Modifier
                    .padding(8.dp)
                    .height(48.dp)
                    .fillMaxWidth(),
            )
            AndroidView(
                factory = {
                    WebView(it).apply {
                        val webView = this
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        val genWebViewClient = GenericWebViewClient()
                        genWebViewClient.setRequestCounterListener(
                            object : GenericWebViewClient.RequestCounterListener {
                                override fun countChanged(requestCount: Int) {
                                    reqCount.value = requestCount
                                }
                            }
                        )
                        webViewClient = genWebViewClient
                    }
                },
                update = { view ->
                    view.loadUrl(url.value.toHttpsPrefix() ?: "")
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .border(1.dp, MaterialTheme.colors.onSurface)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    TorComposeTheme {
        TorComposeApp(
            onConnectClick = {},
            ctx = MainActivity()
        )
    }
}

fun onConnectClicked(ctx: Context, status: MutableState<String>) {
    ctx.registerReceiver(object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            status.value = intent.getStringExtra(TorService.EXTRA_STATUS)
                ?: "Not connected"
        }
    }, IntentFilter(TorService.ACTION_STATUS))

    ctx.bindService(
        Intent(ctx, TorService::class.java),
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName,
                service: IBinder
            ) {

                //moved torService to a local variable, since we only need it once
                val torService = (service as LocalBinder).service
                while (torService.torControlConnection == null) {
                    try {
                        Thread.sleep(500)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
                status.value = "Tor Control Connection established"
            }

            override fun onServiceDisconnected(name: ComponentName) {}
        },
        BIND_AUTO_CREATE
    )
}

fun String.toHttpsPrefix(): String? =
    if (isNotEmpty() && !startsWith("https://") && !startsWith("http://")) {
        "https://$this"
    } else if (startsWith("http://")) {
        replace("http://", "https://")
    } else this