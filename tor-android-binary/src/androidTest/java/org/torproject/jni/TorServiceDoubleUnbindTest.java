package org.torproject.jni;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.torproject.jni.TorService.ACTION_STATUS;
import static org.torproject.jni.TorService.EXTRA_STATUS;
import static org.torproject.jni.TorService.STATUS_OFF;
import static org.torproject.jni.TorService.STATUS_ON;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ServiceTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class TorServiceDoubleUnbindTest {

    public static final String TAG = "TorServiceTest";

    @Rule
    public final ServiceTestRule serviceRule = ServiceTestRule.withTimeout(120L, TimeUnit.SECONDS);

    private Context context;

    @Before
    public void setUp() {
        context = getInstrumentation().getTargetContext();
    }

    /**
     * Test using {@link ServiceTestRule#bindService(Intent, ServiceConnection, int)}
     * for reliable start/stop when testing.
     */
    @Test
    public void testBindService() throws Exception {
        startAndUnbind();
        startAndUnbind();
    }

    private void startAndUnbind() throws Exception {
        final CountDownLatch startedLatch = new CountDownLatch(1);
        final CountDownLatch stoppedLatch = new CountDownLatch(1);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String status = intent.getStringExtra(EXTRA_STATUS);
                Log.i(TAG, "receiver.onReceive: " + status + " " + intent);
                if (STATUS_ON.equals(status)) {
                    startedLatch.countDown();
                } else if (STATUS_OFF.equals(status)) {
                    stoppedLatch.countDown();
                }
            }
        };
        // run the BroadcastReceiver in its own thread
        HandlerThread handlerThread = new HandlerThread(receiver.getClass().getSimpleName());
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        Handler handler = new Handler(looper);
        context.registerReceiver(receiver, new IntentFilter(ACTION_STATUS), null, handler);

        Intent serviceIntent = new Intent(context, TorService.class);
        IBinder binder = serviceRule.bindService(serviceIntent);
        TorService torService = ((TorService.LocalBinder) binder).getService();
        startedLatch.await();

        serviceRule.unbindService();
        stoppedLatch.await();

        context.unregisterReceiver(receiver);
    }

}
