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

package org.torproject.android.sample;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import net.freehaven.tor.control.TorControlConnection;

import org.torproject.jni.TorService;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WebView webView = findViewById(R.id.webview);
        TextView statusTextView = findViewById(R.id.status);

        GenericWebViewClient webViewClient = new GenericWebViewClient();
        webViewClient.setRequestCounterListener(requestCount ->
                runOnUiThread(() -> statusTextView.setText("Request Count: " + requestCount)));
        webView.setWebViewClient(webViewClient);

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String status = intent.getStringExtra(TorService.EXTRA_STATUS);
                Toast.makeText(context, status, Toast.LENGTH_SHORT).show();
                webView.loadUrl("https://check.torproject.org/");

            }
        }, new IntentFilter(TorService.ACTION_STATUS));


        bindService(new Intent(this, TorService.class), new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {

                //moved torService to a local variable, since we only need it once
                TorService torService = ((TorService.LocalBinder) service).getService();
                TorControlConnection conn = torService.getTorControlConnection();

                while ((conn = torService.getTorControlConnection())==null)
                {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (conn != null)
                {
                    Toast.makeText(MainActivity.this, "Got Tor control connection", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        },BIND_AUTO_CREATE);

    }
}
