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

import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

class GenericWebViewClient extends WebViewClient {

    private int requestCounter;

    private MainActivity mainActivity;

    public GenericWebViewClient(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        requestCounter = 0;
    }

    interface RequestCounterListener {
        void countChanged(int requestCount);
    }

    private volatile RequestCounterListener requestCounterListener = null;

    public void setRequestCounterListener(RequestCounterListener requestCounterListener) {
        this.requestCounterListener = requestCounterListener;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {

        requestCounter++;
        if (requestCounterListener != null) {
            requestCounterListener.countChanged(requestCounter);
        }

        String urlString = request.getUrl().toString().split("#")[0];

        try {
            HttpURLConnection connection = null;
            boolean proxied = true;
            if (proxied) {
                Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("localhost", 9050));
                connection = (HttpURLConnection) new URL(urlString).openConnection(proxy);
            } else {
                connection = (HttpURLConnection) new URL(urlString).openConnection();
            }

            connection.setRequestMethod(request.getMethod());
            for (Map.Entry<String, String> requestHeader : request.getRequestHeaders().entrySet()) {
                connection.setRequestProperty(requestHeader.getKey(), requestHeader.getValue());
            }

            // transform response to required format for WebResourceResponse parameters
            InputStream in = new BufferedInputStream(connection.getInputStream());
            String encoding = connection.getContentEncoding();
            connection.getHeaderFields();
            Map<String, String> responseHeaders = new HashMap<>();
            for (String key : connection.getHeaderFields().keySet()) {
                responseHeaders.put(key, connection.getHeaderField(key));
            }

            String mimeType = "text/plain";
            if (connection.getContentType() != null && !connection.getContentType().isEmpty()) {
                mimeType = connection.getContentType().split("; ")[0];
            }

            return new WebResourceResponse(mimeType, encoding, connection.getResponseCode(), connection.getResponseMessage(), responseHeaders, in);
            //return new WebResourceResponse(mimeType, "binary", in);
        } catch (UnsupportedEncodingException e) {
        } catch (IOException e) {
        }
        // failed doing proxied http request: return empty response
        return new WebResourceResponse("text/plain", "UTF-8", 204, "No Content", new HashMap<String, String>(), new ByteArrayInputStream(new byte[]{}));
    }
}