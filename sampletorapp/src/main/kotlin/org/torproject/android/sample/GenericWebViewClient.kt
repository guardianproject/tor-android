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

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import kotlin.concurrent.Volatile

class GenericWebViewClient : WebViewClient() {
    private var requestCounter = 0

    interface RequestCounterListener {
        fun countChanged(requestCount: Int)
    }

    @Volatile
    private var requestCounterListener: RequestCounterListener? = null

    fun setRequestCounterListener(requestCounterListener: RequestCounterListener) {
        this.requestCounterListener = requestCounterListener
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest
    ): WebResourceResponse {
        requestCounter++
        if (requestCounterListener != null) {
            requestCounterListener!!.countChanged(requestCounter)
        }

        val urlString: String =
            request.url.toString().split("#".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()[0]

        try {
            val connection: HttpURLConnection
            val proxied = true
            if (proxied) {
                val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("localhost", 9050))
                connection = URL(urlString).openConnection(proxy) as HttpURLConnection
            } else {
                connection = URL(urlString).openConnection() as HttpURLConnection
            }

            connection.requestMethod = request.method
            for (requestHeader in request.requestHeaders.entries) {
                connection.setRequestProperty(requestHeader.key, requestHeader.value)
            }

            // transform response to required format for WebResourceResponse parameters
            val `in` = BufferedInputStream(connection.getInputStream())
            val encoding = connection.contentEncoding
            connection.headerFields
            val responseHeaders = HashMap<String?, String?>()
            for (key in connection.headerFields.keys) {
                responseHeaders[key] = connection.getHeaderField(key)
            }

            var mimeType = "text/plain"
            if (connection.contentType != null && !connection.contentType.isEmpty()) {
                mimeType =
                    connection.contentType.split("; ".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[0]
            }

            return WebResourceResponse(
                mimeType,
                encoding,
                connection.getResponseCode(),
                connection.getResponseMessage(),
                responseHeaders,
                `in`
            )
        } catch (_: IOException) {
        }
        // failed doing proxied http request: return empty response
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            204,
            "No Content",
            HashMap<String?, String?>(),
            ByteArrayInputStream(
                byteArrayOf()
            )
        )
    }
}