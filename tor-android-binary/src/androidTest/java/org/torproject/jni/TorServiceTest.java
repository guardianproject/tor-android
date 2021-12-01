package org.torproject.jni;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.JsonReader;
import android.util.Log;
import androidx.test.InstrumentationRegistry;
import androidx.test.rule.ServiceTestRule;
import androidx.test.runner.AndroidJUnit4;
import info.guardianproject.netcipher.NetCipher;
import net.freehaven.tor.control.ConfigEntry;
import net.freehaven.tor.control.TorControlConnection;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class TorServiceTest {

    public static final String TAG = "TorServiceTest";

    @Rule
    public final ServiceTestRule serviceRule = ServiceTestRule.withTimeout(120L, TimeUnit.SECONDS);

    private Context context;
    private TorService torService;
    private File defaultsTorrc;
    private File torrc;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getTargetContext();
        defaultsTorrc = TorService.getDefaultsTorrc(context);
        defaultsTorrc.deleteOnExit();
        defaultsTorrc.delete();
        torrc = TorService.getTorrc(context);
        torrc.deleteOnExit();
        torrc.delete();
    }

    @After
    public void tearDown() {
        defaultsTorrc.delete();
        torrc.delete();
    }

    /**
     * Test using {@link ServiceTestRule#bindService(Intent, ServiceConnection, int)}
     * for reliable start/stop when testing.
     */
    @Test
    public void testBindService() throws TimeoutException, InterruptedException, IOException {
        final CountDownLatch startingLatch = new CountDownLatch(1);
        final CountDownLatch startedLatch = new CountDownLatch(1);
        final CountDownLatch stoppingLatch = new CountDownLatch(1);
        final CountDownLatch stoppedLatch = new CountDownLatch(1);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!TorService.ACTION_STATUS.equals(intent.getAction())) {
                    Log.d(TAG, "!TorService.ACTION_STATUS.equals(intent.getAction())");
                    return;
                }
                String status = intent.getStringExtra(TorService.EXTRA_STATUS);
                Log.i(TAG, "receiver.onReceive: " + status + " " + intent);
                if (TorService.STATUS_STARTING.equals(status)) {
                    startingLatch.countDown();
                } else if (TorService.STATUS_ON.equals(status)) {
                    startedLatch.countDown();
                } else if (TorService.STATUS_STOPPING.equals(status)) {
                    stoppingLatch.countDown();
                } else if (TorService.STATUS_OFF.equals(status)) {
                    stoppedLatch.countDown();
                } else {
                    throw new IllegalStateException("UNKNOWN STATUS FROM INTENT: " + intent);
                }
            }
        };
        // run the BroadcastReceiver in its own thread
        HandlerThread handlerThread = new HandlerThread(receiver.getClass().getSimpleName());
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        Handler handler = new Handler(looper);
        context.registerReceiver(receiver, new IntentFilter(TorService.ACTION_STATUS), null, handler);

        Intent serviceIntent = new Intent(context, TorService.class);
        IBinder binder = serviceRule.bindService(serviceIntent);
        TorService torService = ((TorService.LocalBinder) binder).getService();
        startingLatch.await();
        startedLatch.await();

        int socksPort = torService.getSocksPort();
        assertTrue(isServerSocketInUse(socksPort));
        if (socksPort != 9050) {
            assertFalse("Something else is providing port 9050!", isServerSocketInUse(9050));
        }

        int httpTunnelPort = torService.getHttpTunnelPort();
        assertTrue(isServerSocketInUse(httpTunnelPort));
        if (httpTunnelPort != 8118) {
            assertFalse("Something else is providing port 8118!", isServerSocketInUse(8118));
        }

        assertTrue(canConnectToSocket("localhost", socksPort));
        assertTrue(canConnectToSocket("localhost", httpTunnelPort));

        assertNotEquals(InetAddress.getByName(null), InetAddress.getByName("check.torproject.org"));
        assertFalse("URLConnection should not use Tor by default", NetCipher.isURLConnectionUsingTor());
        Log.i(TAG, "NetCipher.setProxy()");
        NetCipher.setProxy("localhost", 5);
        try {
            checkIsTor(NetCipher.getHttpURLConnection("https://check.torproject.org/api/ip"));
            fail();
        } catch (Exception e) {
            // success!
        }
        NetCipher.setProxy("localhost", httpTunnelPort);
        assertTrue("NetCipher.getHttpURLConnection should use Tor",
                NetCipher.isNetCipherGetHttpURLConnectionUsingTor());

        URLConnection c = NetCipher.getHttpsURLConnection("https://www.nytimesn7cgmftshazwhfgzm37qxb44r64ytbb2dj3x62d2lljsciiyd.onion/");
        Log.i(TAG, "Content-Length: " + c.getContentLength());
        Log.i(TAG, "CONTENTS: " + new String(IOUtils.readFully(c.getInputStream(), 100)));

        assertTrue(checkIsTor(NetCipher.getHttpURLConnection("https://check.torproject.org/api/ip")));

        serviceRule.unbindService();
        stoppedLatch.await();
    }


    @Test
    public void testOverridingDefaultsTorrc() throws TimeoutException, InterruptedException, IOException {
        final String dnsPort = "DNSPort";
        final String testValue = "auto";
        FileUtils.write(defaultsTorrc, dnsPort + " 53\n");
        FileUtils.write(torrc, dnsPort + " " + testValue + "\n");

        final CountDownLatch startedLatch = new CountDownLatch(1);
        final CountDownLatch stoppedLatch = new CountDownLatch(1);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!TorService.ACTION_STATUS.equals(intent.getAction())) {
                    Log.d(TAG, "!TorService.ACTION_STATUS.equals(intent.getAction())");
                    return;
                }
                String status = intent.getStringExtra(TorService.EXTRA_STATUS);
                Log.i(TAG, "receiver.onReceive: " + status + " " + intent);
                if (TorService.STATUS_ON.equals(status)) {
                    startedLatch.countDown();
                } else if (TorService.STATUS_OFF.equals(status)) {
                    stoppedLatch.countDown();
                }
            }
        };
        // run the BroadcastReceiver in its own thread
        HandlerThread handlerThread = new HandlerThread(receiver.getClass().getSimpleName());
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        Handler handler = new Handler(looper);
        context.registerReceiver(receiver, new IntentFilter(TorService.ACTION_STATUS), null, handler);

        Intent serviceIntent = new Intent(context, TorService.class);
        IBinder binder = serviceRule.bindService(serviceIntent);
        torService = ((TorService.LocalBinder) binder).getService();
        startedLatch.await();

        assertEquals(testValue, getConf(torService.getTorControlConnection(), dnsPort));

        serviceRule.unbindService();
        stoppedLatch.await();
    }

    @Test
    public void testDownloadingLargeFile() throws TimeoutException, InterruptedException, IOException {
        Assume.assumeTrue("Only works on Android 7.1.2 or higher", Build.VERSION.SDK_INT >= 24);
        final CountDownLatch startedLatch = new CountDownLatch(1);
        final CountDownLatch stoppedLatch = new CountDownLatch(1);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!TorService.ACTION_STATUS.equals(intent.getAction())) {
                    Log.d(TAG, "!TorService.ACTION_STATUS.equals(intent.getAction())");
                    return;
                }
                String status = intent.getStringExtra(TorService.EXTRA_STATUS);
                Log.i(TAG, "receiver.onReceive: " + status + " " + intent);
                if (TorService.STATUS_ON.equals(status)) {
                    startedLatch.countDown();
                } else if (TorService.STATUS_OFF.equals(status)) {
                    stoppedLatch.countDown();
                }
            }
        };
        // run the BroadcastReceiver in its own thread
        HandlerThread handlerThread = new HandlerThread(receiver.getClass().getSimpleName());
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        Handler handler = new Handler(looper);
        context.registerReceiver(receiver, new IntentFilter(TorService.ACTION_STATUS), null, handler);

        Intent serviceIntent = new Intent(context, TorService.class);
        IBinder binder = serviceRule.bindService(serviceIntent);
        torService = ((TorService.LocalBinder) binder).getService();
        startedLatch.await();

        NetCipher.setProxy(
                new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("localhost", torService.getSocksPort())));
        assertTrue("NetCipher.getHttpURLConnection should use Tor",
                NetCipher.isNetCipherGetHttpURLConnectionUsingTor());
        // ~350MB
        //URL url = new URL("http://dl.google.com/android/ndk/android-ndk-r9b-linux-x86_64.tar.bz2");
        // ~3MB
        URL url = new URL("https://dl.google.com/android/repository/platform-tools_r24-linux.zip");
        // 55KB
        //URL url = new URL("https://jcenter.bintray.com/com/android/tools/build/gradle/2.2.3/gradle-2.2.3.jar");
        HttpURLConnection connection = NetCipher.getHttpURLConnection(url);
        connection.setConnectTimeout(0); // blocking connect with TCP timeout
        connection.setReadTimeout(0);
        assertEquals(200, connection.getResponseCode());
        IOUtils.copy(connection.getInputStream(), new FileWriter(new File("/dev/null")));

        serviceRule.unbindService();
        stoppedLatch.await();
    }

    private static boolean canConnectToSocket(String host, int port) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 120);
            socket.close();
            return true;
        } catch (IOException e) {
            // Could not connect.
            return false;
        }
    }

    private static boolean isServerSocketInUse(int port) {
        Log.i(TAG, "isServerSocketInUse: " + port);
        try {
            (new ServerSocket(port)).close();
            return false;
        } catch (IOException e) {
            // Could not connect.
            return true;
        }
    }

    /**
     * Return the value of the first match as a {@link String}, with the quotes
     * stripped off.
     */
    private static String getConf(TorControlConnection torControlConnection, String key) {
        try {
            List<ConfigEntry> configEntries = torControlConnection.getConf(key);
            return configEntries.get(0).value;
            //return value.substring(1, value.length() - 1);
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean checkIsTor(URLConnection connection) throws IOException {
        boolean isTor = false;
        Log.i("NetCipher", "content length: " + connection.getContentLength());
        JsonReader jsonReader = new JsonReader(new InputStreamReader(connection.getInputStream()));
        jsonReader.beginObject();

        while (jsonReader.hasNext()) {
            String name = jsonReader.nextName();
            if ("IsTor".equals(name)) {
                isTor = jsonReader.nextBoolean();
                break;
            }

            jsonReader.skipValue();
        }

        return isTor;
    }
}
