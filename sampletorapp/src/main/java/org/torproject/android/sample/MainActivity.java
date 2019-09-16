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
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;
import org.torproject.jni.TorService;

public class MainActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        final TextView statusTextView = findViewById(R.id.status);

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(context, intent.getStringExtra(TorService.EXTRA_STATUS), Toast.LENGTH_SHORT).show();
            }
        }, new IntentFilter(TorService.ACTION_STATUS));
        startService(new Intent(this, TorService.class));

        GenericWebViewClient webViewClient = new GenericWebViewClient(this);
        webViewClient.setRequestCounterListener(new GenericWebViewClient.RequestCounterListener() {
            @Override
            public void countChanged(final int requestCount) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        statusTextView.setText("request count: " + requestCount + " - ");
                    }
                });
            }
        });

        webView.setWebViewClient(webViewClient);
        webView.loadUrl("https://check.torproject.org/");
    }

}
