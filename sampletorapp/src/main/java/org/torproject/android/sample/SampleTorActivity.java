package org.torproject.android.sample;

import android.app.Application;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.jrummyapps.android.shell.CommandResult;
import com.jrummyapps.android.shell.Shell;

import org.torproject.android.binary.TorResourceInstaller;

import java.io.File;

public class SampleTorActivity extends AppCompatActivity {

    private TextView tvNotice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample_tor);
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
