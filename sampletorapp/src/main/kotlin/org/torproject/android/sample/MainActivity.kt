/*
 * Copyright (c) 2018 Michael Pöhn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.torproject.android.sample

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.webkit.WebView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import org.torproject.jni.TorService

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val webView = findViewById<WebView>(R.id.webview)
        val statusTextView = findViewById<TextView>(R.id.status)

        val webViewClient = GenericWebViewClient()
        webViewClient.setRequestCounterListener(object :
            GenericWebViewClient.RequestCounterListener {
            @SuppressLint("SetTextI18n")
            override fun countChanged(requestCount: Int) {
                runOnUiThread { statusTextView.text = "Request Count: $requestCount" }
            }

        })
        webView.webViewClient = webViewClient

        val torServiceReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val status = intent.getStringExtra(TorService.EXTRA_STATUS)
                Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                webView.loadUrl("https://check.torproject.org/")
            }
        }

        ContextCompat.registerReceiver(
            this,
            torServiceReceiver,
            IntentFilter(TorService.ACTION_STATUS),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        bindService(Intent(this, TorService::class.java), object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder) {
                val torService = (service as TorService.LocalBinder).service

                while (torService.torControlConnection == null) {
                    try {
                        Thread.sleep(500)
                    } catch (e: InterruptedException) {
                        Log.e("SampleTorApp", e.toString())
                    }
                }

                Toast.makeText(this@MainActivity, "Got Tor control connection", Toast.LENGTH_LONG)
                    .show()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.d("SampleTorApp", "service disconnected")
            }
        }, BIND_AUTO_CREATE)
    }
}
