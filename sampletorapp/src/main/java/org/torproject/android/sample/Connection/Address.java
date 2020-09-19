package org.torproject.android.sample.Connection;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Random;

import static org.torproject.android.sample.SampleTorServiceConstants.TOR_TRANSPROXY_PORT_DEFAULT;

public class Address {
    private String _Host;
    private int _Port;

    private ServerSocket ss;
    private Proxy p;
    private SocketAddress hiddenSA;
    private Socket hiddenSocket;
    private Socket innerSocket;

    HandlerThread i2hThread;
    HandlerThread h2iThread;
    Handler i2hhandler;
    Handler h2ihandler;
    Looper i2hlooper;
    Looper h2ilooper;

    HandlerThread serverThread;
    Handler serverHandler;
    Looper serverLooper;

    public Address(String Host, int Port) {
        this._Host = Host;
        this._Port = Port;

        if (getType() == AddressHelper.AddressType.HiddenService) {
            createInnerHandler();
        }
    }

    private void createInnerHandler() {
        String hiddenHost = getHost();
        int hiddenPort = getPort();

        //Connect to hiddenService
        hiddenSA = InetSocketAddress.createUnresolved(hiddenHost, hiddenPort);
        p = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", TOR_TRANSPROXY_PORT_DEFAULT));
        hiddenSocket = new Socket(p);

        this._Host = "127.0.0.1";
        Random r = new Random();
        while (ss == null) {
            try {
                _Port = r.nextInt(5000) + 10000;
                ss = new ServerSocket(_Port);
            } catch (Exception ex) {
                continue;
            }
        }


        serverThread = new HandlerThread("server");
        serverThread.start();
        serverLooper = serverThread.getLooper();
        serverHandler = new Handler(serverLooper);
        Runnable serverRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    innerSocket = ss.accept();
                    hiddenSocket.connect(hiddenSA);
                    prepareAndStartTunnelThreads();
                } catch (IOException ex) {
                }
            }
        };
        serverHandler.post(serverRunnable);
    }

    private void prepareAndStartTunnelThreads() {
        i2hThread = new HandlerThread("inner2hidden");
        h2iThread = new HandlerThread("hidden2inner");

        i2hThread.start();
        h2iThread.start();

        i2hlooper = i2hThread.getLooper();
        h2ilooper = h2iThread.getLooper();

        i2hhandler = new Handler(i2hlooper);
        h2ihandler = new Handler(h2ilooper);

        Runnable i2h = new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream is = innerSocket.getInputStream();
                    OutputStream os = hiddenSocket.getOutputStream();
                    while (!innerSocket.isInputShutdown() && !hiddenSocket.isOutputShutdown()) {
                        int i = is.read();
                        if (i != -1) {
                            os.write(i);
                        } else {
                            break;
                        }
                    }
                    //for (int i; (i = is.read()) != -1; i++) {
                    //    os.write(i);
                    //}
                } catch (IOException ex) {
                    //Do nothing
                }
            }
        };

        Runnable h2i = new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream is = hiddenSocket.getInputStream();
                    OutputStream os = innerSocket.getOutputStream();
                    while (!hiddenSocket.isInputShutdown() && !innerSocket.isOutputShutdown()) {
                        int i = is.read();
                        if (i != -1) {
                            os.write(i);
                        } else {
                            break;
                        }
                    }
                } catch (IOException ex) {
                    //Do nothing
                }
            }
        };

        i2hhandler.post(i2h);
        h2ihandler.post(h2i);
    }

    public int getPort() {
        return _Port;
    }

    public String getHost() {
        return _Host;
    }

    public AddressHelper.AddressType getType() {
        return AddressHelper.getType(getHost());
    }
}
