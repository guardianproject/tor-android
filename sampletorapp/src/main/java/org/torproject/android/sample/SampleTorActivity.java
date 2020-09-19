package org.torproject.android.sample;

import android.app.Application;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.jrummyapps.android.shell.CommandResult;
import com.jrummyapps.android.shell.Shell;

import org.torproject.android.binary.TorResourceInstaller;
import org.torproject.android.sample.Connection.Address;
import org.torproject.android.sample.Connection.ConnectionManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

import static org.torproject.android.sample.SampleTorServiceConstants.TOR_TRANSPROXY_PORT_DEFAULT;

public class SampleTorActivity extends AppCompatActivity {

    private TextView tvNotice;
    TorProxy tb;

    public void callback() {
        try {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    Log.d("MSTTOR", "Hidden service created(?) at: " + tb.getOnionAddress());
                    normalSocketExample();
                    transparentSocketExample();
                }

                private void transparentSocketExample() {
                    try {
                        Log.d("MSTTOR", "Starting FACEBOOK using transparent socket!");
                        Address destination = ConnectionManager.getInstance().getDestination("facebookcorewwwi.onion", 80);
                        Socket s = new Socket(destination.getHost(), destination.getPort());
                        s.getOutputStream().write("GET / HTTP/1.0\r\nHOST: facebookcorewwwi.onion\r\n\r\n".getBytes());
                        s.getOutputStream().flush();
                        BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
                        String tmp = br.readLine();
                        String html = "";

                        while (tmp != null && !tmp.equalsIgnoreCase("")) {
                            html += tmp + "\r\n";
                            tmp = br.readLine();
                        }

                        Log.d("MSTTOR", "Response from facebook:\r\n" + html);

                    } catch (Exception ex) {
                        Log.e("MSTTOR", ex.getMessage());
                    }
                }

                private void normalSocketExample() {
                    try {
                        Log.d("MSTTOR", "Starting FACEBOOK using normal socket!");
                        Socket s = new Socket(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", TOR_TRANSPROXY_PORT_DEFAULT)));
                        InetSocketAddress facebook = InetSocketAddress.createUnresolved("facebookcorewwwi.onion", 80);
                        s.connect(facebook, 60 * 1000);
                        s.getOutputStream().write("GET / HTTP/1.0\r\nHOST: facebookcorewwwi.onion\r\n\r\n".getBytes());
                        s.getOutputStream().flush();
                        BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
                        String tmp = br.readLine();
                        String html = "";

                        while (tmp != null && !tmp.equalsIgnoreCase("")) {
                            html += tmp + "\r\n";
                            tmp = br.readLine();
                        }

                        Log.d("MSTTOR", "Response from facebook:\r\n" + html);
                    } catch (Exception ex) {
                        Log.e("MSTTOR", ex.getMessage());
                    }
                }
            };

            Thread t = new Thread(r);
            t.start();
        } catch (Exception ex) {
            Log.e("MSTTOR", ex.getMessage());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample_tor);

        tb = new TorProxy.TorBuilder(getApplicationContext())
                .setSOCKsPort(TOR_TRANSPROXY_PORT_DEFAULT)
                .setUseBrideges(false)
                .setExternalHiddenServicePort(80)
                .setInternalHiddenServicePort(80)
                .build();
        tb.init();
        try {
            tb.start(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
        tvNotice = findViewById(R.id.lblStatus);

        try {
            TorResourceInstaller torResourceInstaller = new TorResourceInstaller(this, getFilesDir());

            File fileTorBin = torResourceInstaller.installResources();
            File fileTorRc = torResourceInstaller.getTorrcFile();

            boolean success = fileTorBin != null && fileTorBin.canExecute();

            String message = "Tor install success? " + success;
            logNotice(message);

            if (success) {
                runTorShellCmd(fileTorBin, fileTorRc);
            }

        } catch (Exception e) {
            e.printStackTrace();
            logNotice(e.getMessage());
        }
         */
    }

    private void logNotice(String notice) {
        tvNotice.setText(notice);
    }

    private void logNotice(String notice, Exception e) {
        logNotice(notice);
        Log.e("SampleTor", "error occurred", e);
    }

    private boolean runTorShellCmd(File fileTor, File fileTorrc) throws Exception {
        File appCacheHome = getDir(SampleTorServiceConstants.DIRECTORY_TOR_DATA, Application.MODE_PRIVATE);

        if (!fileTorrc.exists()) {
            logNotice("torrc not installed: " + fileTorrc.getCanonicalPath());
            return false;
        }

        String torCmdString = fileTor.getCanonicalPath()
                + " DataDirectory " + appCacheHome.getCanonicalPath()
                + " --defaults-torrc " + fileTorrc;

        int exitCode = -1;

        try {
            exitCode = exec(torCmdString + " --verify-config", true);
        } catch (Exception e) {
            logNotice("Tor configuration did not verify: " + e.getMessage(), e);
            return false;
        }

        try {
            exitCode = exec(torCmdString, true);
        } catch (Exception e) {
            logNotice("Tor was unable to start: " + e.getMessage(), e);
            return false;
        }

        if (exitCode != 0) {
            logNotice("Tor did not start. Exit:" + exitCode);
            return false;
        }

        return true;
    }


    private int exec(String cmd, boolean wait) throws Exception {
        CommandResult shellResult = Shell.run(cmd);

        //  debug("CMD: " + cmd + "; SUCCESS=" + shellResult.isSuccessful());

        if (!shellResult.isSuccessful()) {
            throw new Exception("Error: " + shellResult.exitCode + " ERR=" + shellResult.getStderr() + " OUT=" + shellResult.getStdout());
        }

        return shellResult.exitCode;
    }
}
