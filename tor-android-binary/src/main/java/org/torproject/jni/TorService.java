package org.torproject.jni;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.FileObserver;
import android.os.IBinder;
import android.os.Process;

import net.freehaven.tor.control.RawEventListener;
import net.freehaven.tor.control.TorControlCommands;
import net.freehaven.tor.control.TorControlConnection;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * A {@link Service} that runs Tor.  To control Tor via the {@code ControlPort},
 * first bind to this using {@link #bindService(Intent, android.content.ServiceConnection, int)},
 * then use {@link #getTorControlConnection()} to get an instance of
 * {@link TorControlConnection} from {@code jtorctl}.  If
 * {@link TorControlCommands#EVENT_CIRCUIT_STATUS} is not included in
 * {@link TorControlConnection#setEvents(java.util.List)}, then this service
 * will not be able to function properly since it relies on those events to
 * detect the state of Tor.
 */
public class TorService extends Service {

    // TODO orbotserver and/or tor-android-server are the borders to use, strip out Prefs and VPN
    // grep -rF org.torproject.android.service app/src/main/java/ ~/code/guardianproject/orbot/app/src/main/java

    public static final String TAG = "TorService";

    public static final String VERSION_NAME = org.torproject.jni.BuildConfig.VERSION_NAME;

    /**
     * {@link Intent} send by Orbot with {@code ON/OFF/STARTING/STOPPING} status
     * included as an {@link #EXTRA_STATUS} {@code String}.  Your app should
     * always receive {@code ACTION_STATUS Intent}s since any other app could
     * start Orbot.  Also, user-triggered starts and stops will also cause
     * {@code ACTION_STATUS Intent}s to be broadcast.
     */
    public static final String ACTION_STATUS = "org.torproject.android.intent.action.STATUS";

    /**
     * {@code String} that contains a status constant: {@link #STATUS_ON},
     * {@link #STATUS_OFF}, {@link #STATUS_STARTING}, or
     * {@link #STATUS_STOPPING}
     */
    public static final String EXTRA_STATUS = "org.torproject.android.intent.extra.STATUS";

    /**
     * All tor-related services and daemons are stopped
     */
    public static final String STATUS_OFF = "OFF";
    /**
     * All tor-related services and daemons have completed starting
     */
    public static final String STATUS_ON = "ON";
    public static final String STATUS_STARTING = "STARTING";
    public static final String STATUS_STOPPING = "STOPPING";

    /**
     * @return a {@link File} pointing to the location of the optional
     * {@code torrc} file.
     * @see <a href="https://www.torproject.org/docs/tor-manual.html#_the_configuration_file_format">Tor configuration file format</a>
     */
    public static File getTorrc(Context context) {
        return new File(getAppTorServiceDir(context), "torrc");
    }

    /**
     * @return a {@link File} pointing to the location of the optional
     * {@code torrc-defaults} file.
     * @see <a href="https://www.torproject.org/docs/tor-manual.html#_the_configuration_file_format">Tor configuration file format</a>
     */
    public static File getDefaultsTorrc(Context context) {
        return new File(getAppTorServiceDir(context), "torrc-defaults");
    }

    /**
     * Tor writes the {@code ControlPort} connection info out to this file,
     * based on {@code ControlPortWriteToFile}.
     */
    public static File getControlPortFile(Context context) {
        if (controlPortFile == null) {
            controlPortFile = new File(getAppTorServiceDataDir(context), CONTROL_PORT_FILE_NAME);
        }
        return controlPortFile;
    }

    public static File getControlAuthCookieFile(Context context) {
        return new File(getAppTorServiceDataDir(context), CONTROL_AUTH_COOKIE_FILE_NAME);
    }

    /**
     * Get the directory that {@link TorService} uses for:
     * <ul>
     * <li>writing {@code ControlPort.txt} // TODO
     * <li>reading {@code torrc} and {@code torrc-defaults}
     * <li>{@code DataDirectory} and {@code CacheDirectory}
     * <li>the debug log file
     * </ul>
     */
    private static File getAppTorServiceDir(Context context) {
        if (appTorServiceDir == null) {
            appTorServiceDir = context.getDir(TorService.class.getSimpleName(), MODE_PRIVATE);
        }
        return appTorServiceDir;
    }

    /**
     * Tor stores private, internal data in this directory.
     */
    private static File getAppTorServiceDataDir(Context context) {
        File dir = new File(getAppTorServiceDir(context), "data");
        dir.mkdir();
        if (!(dir.setReadable(true, true) && dir.setWritable(true, true) && dir.setExecutable(true, true))) {
            throw new IllegalStateException("Cannot create " + dir);
        }
        return dir;
    }

    static {
        System.loadLibrary("tor");
    }

    static String currentStatus = STATUS_OFF;

    private static File appTorServiceDir = null;
    private static File controlPortFile = null;
    private static final String CONTROL_PORT_FILE_NAME = "ControlPort.txt";
    private static final String CONTROL_AUTH_COOKIE_FILE_NAME = "control_auth_cookie";

    // Store the opaque reference as a long (pointer) for the native code
    private long torConfiguration = -1;
    private int torControlFd = -1;

    private TorControlConnection torControlConnection;

    private native String apiGetProviderVersion();

    private native boolean createTorConfiguration();

    private native void mainConfigurationFree();

    private native boolean mainConfigurationSetCommandLine(String[] args);

    private native boolean mainConfigurationSetupControlSocket();

    private native int runMain();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        broadcastStatus(this, STATUS_STARTING);
        startTorServiceThread();
    }

    /**
     * Announce Tor is available for connections once the first circuit is complete
     */
    private final RawEventListener startedEventListener = new RawEventListener() {
        @Override
        public void onEvent(String keyword, String data) {
            if (TorService.STATUS_STARTING.equals(TorService.currentStatus)
                    && TorControlCommands.EVENT_CIRCUIT_STATUS.equals(keyword)
                    && data != null && data.length() > 0) {
                String[] tokenArray = data.split(" ");
                if (tokenArray.length > 1 && TorControlCommands.CIRC_EVENT_BUILT.equals(tokenArray[1])) {
                    TorService.broadcastStatus(TorService.this, TorService.STATUS_ON);
                }
            }
        }
    };

    /**
     * This waits for {@link #CONTROL_PORT_FILE_NAME} and the
     * {@link #CONTROL_AUTH_COOKIE_FILE_NAME} to be created by {@code tor},
     * then continues on to connect to the {@code ControlPort} as described in
     * that file.
     */
    private Thread controlPortThread = new Thread("ControlPort") {
        @Override
        public void run() {
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            Socket socket = new Socket(Proxy.NO_PROXY);
            try {
                final CountDownLatch countDownLatch = new CountDownLatch(2);
                final File controlPortFile = getControlPortFile(TorService.this);
                FileObserver controlPortFileObserver = new FileObserver(controlPortFile.getParent()) {
                    @Override
                    public void onEvent(int event, @Nullable String path) {
                        if ((event & FileObserver.MOVED_TO) == 0) {
                            return;
                        }
                        if (CONTROL_PORT_FILE_NAME.equals(path) || CONTROL_AUTH_COOKIE_FILE_NAME.equals(path)) {
                            countDownLatch.countDown();
                        }
                    }
                };
                controlPortFileObserver.startWatching();
                countDownLatch.await(10, TimeUnit.SECONDS);
                controlPortFileObserver.stopWatching();
                File controlSocket = new File(observeDir, CONTROL_SOCKET_NAME);
                if (!controlSocket.canRead()) {
                    throw new IllegalStateException("cannot read " + controlSocket);
                }

                File controlAuthCookieFile = getControlAuthCookieFile(TorService.this);
                byte[] controlAuthCookie = new byte[(int) controlAuthCookieFile.length()];
                FileInputStream fis = new FileInputStream(controlAuthCookieFile);
                fis.read(controlAuthCookie);
                fis.close();

                socket.connect(new InetSocketAddress("localhost", 9051));
                TorControlConnection torControlConnection = new TorControlConnection(socket);
                torControlConnection.launchThread(true);
                torControlConnection.authenticate(controlAuthCookie);
                torControlConnection.addRawEventListener(startedEventListener);
                torControlConnection.setEvents(Arrays.asList(TorControlCommands.EVENT_CIRCUIT_STATUS));

            } catch (IOException | ArrayIndexOutOfBoundsException | InterruptedException e) {
                e.printStackTrace();
                broadcastStatus(TorService.this, STATUS_STOPPING);
                TorService.this.stopSelf();
            }
        }
    };

    /**
     * Start tor in a {@link Thread} with the minimum required config.  The
     * rest of the config should happen via the ControlPort.  This first runs
     * tor to verify the command line flags.  If they are correct, then the
     * {@link #controlPortThread} is started, and then the {@code tor Thread}.
     *
     * @see <a href="https://github.com/torproject/tor/blob/40be20d542a83359ea480bbaa28380b4137c88b2/src/app/config/config.c#L4730">options that must be on the command line</a>
     */
    private void startTorServiceThread() {
        final Context context = this.getApplicationContext();
        new Thread("tor") {
            @Override
            public void run() {
                try {
                    createTorConfiguration();
                    ArrayList<String> lines = new ArrayList<>(Arrays.asList("tor", "--verify-config", // must always be here
                            "--RunAsDaemon", "0",
                            "-f", getTorrc(context).getAbsolutePath(),
                            "--defaults-torrc", getDefaultsTorrc(context).getAbsolutePath(),
                            "--ignore-missing-torrc",
                            "--SyslogIdentityTag", TAG,
                            "--CacheDirectory", new File(getCacheDir(), TAG).getAbsolutePath(),
                            "--DataDirectory", getAppTorServiceDataDir(context).getAbsolutePath(),
                            "--ControlPort", "9051",
                            "--ControlPortWriteToFile", getControlPortFile(context).getAbsolutePath(),
                            "--CookieAuthentication", "1",
                            //"--ControlSocket", "unix:" + getControlSocketPath(context),

                            // can be moved to ControlPort messages
                            "--SOCKSPort", "9050",
                            "--HTTPTunnelPort", "8118",
                            "--LogMessageDomains", "1",
                            "--Log", "debug file " + new File(getCacheDir(), TAG + "-debug.log").getAbsolutePath(),
                            "--TruncateLogFile", "1"
                    ));
                    String[] verifyLines = lines.toArray(new String[0]);
                    if (!mainConfigurationSetCommandLine(verifyLines)) {
                        throw new IllegalArgumentException("Setting command line failed: " + Arrays.toString(verifyLines));
                    }
                    int result = runMain(); // run verify
                    if (result != 0) {
                        throw new IllegalArgumentException("Bad command flags: " + Arrays.toString(verifyLines));
                    }

                    controlPortThread.start();

                    String[] runLines = new String[lines.size() - 1];
                    runLines[0] = "tor";
                    for (int i = 2; i < lines.size(); i++) {
                        runLines[i - 1] = lines.get(i);
                    }
                    if (!mainConfigurationSetCommandLine(runLines)) {
                        throw new IllegalArgumentException("Setting command line failed: " + Arrays.toString(runLines));
                    }
                    if (!mainConfigurationSetupControlSocket()) {
                        throw new IllegalStateException("Setting up ControlPort failed!");
                    }
                    if (runMain() != 0) {
                        throw new IllegalStateException("Tor could not start!");
                    }

                } catch (IllegalStateException | IllegalArgumentException e) {
                    e.printStackTrace();
                    broadcastStatus(context, STATUS_STOPPING);
                    TorService.this.stopSelf();
                }
            }
        }.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mainConfigurationFree();
        broadcastStatus(TorService.this, STATUS_OFF);
    }

    /**
     * Get an instance of {@link TorControlConnection} to control this instance
     * of Tor, and configure it to set events.
     *
     * @see TorControlConnection#setEvents(java.util.List)
     */
    public TorControlConnection getTorControlConnection() {
        return torControlConnection;
    }

    /**
     * Sends the current status both internally and for any apps that need to
     * follow the status of TorService.
     */
    static void broadcastStatus(Context context, String currentStatus) {
        TorService.currentStatus = currentStatus;
        Intent intent = getBroadcastIntent(ACTION_STATUS, currentStatus);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        context.sendBroadcast(intent);
    }

    private static Intent getBroadcastIntent(String action, String currentStatus) {
        Intent intent = new Intent(action);
        intent.putExtra(EXTRA_STATUS, currentStatus);
        return intent;
    }
}
