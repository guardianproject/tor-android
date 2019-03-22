package org.torproject.android.sample;

import android.app.Application;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.jrummyapps.android.shell.CommandResult;
import com.jrummyapps.android.shell.Shell;

import org.torproject.android.binary.TorResourceInstaller;
import org.torproject.android.binary.TorServiceConstants;

import java.io.File;
import java.io.IOException;

public class sampleTorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample_tor);

        try {
            TorResourceInstaller torResourceInstaller = new TorResourceInstaller(this,getFilesDir());

            File fileTorBin = torResourceInstaller.installResources();
            File fileTorRc = torResourceInstaller.getTorrcFile();

            boolean success = fileTorBin != null && fileTorBin.canExecute();

            String message = "Tor install success? " + success;

            ((TextView)findViewById(R.id.lblStatus)).setText(message);

            if (success)
            {
                runTorShellCmd (fileTorBin, fileTorRc);
            }


        } catch (IOException e) {
            e.printStackTrace();
            logNotice(e.getMessage());

        } catch (Exception e) {
            e.printStackTrace();
            logNotice(e.getMessage());
        }
    }

    public void logNotice (String notice)
    {
        ((TextView)findViewById(R.id.lblStatus)).setText(notice);
    }

    public void logNotice (String notice, Exception e)
    {
        ((TextView)findViewById(R.id.lblStatus)).setText(notice);
        Log.e("SampleTor","error occurred",e);
    }

    public void doTorThings ()
    {
        //please see this project: https://github.com/thaliproject/Tor_Onion_Proxy_Library/
    }

    private boolean runTorShellCmd(File fileTor, File fileTorrc) throws Exception
    {
        File appCacheHome = getDir(SampleTorServiceConstants.DIRECTORY_TOR_DATA,Application.MODE_PRIVATE);

        boolean result = true;
        if (!fileTorrc.exists())
        {
            logNotice("torrc not installed: " + fileTorrc.getCanonicalPath());
            return false;
        }

        String torCmdString = fileTor.getCanonicalPath()
                + " DataDirectory " + appCacheHome.getCanonicalPath()
                + " --defaults-torrc " + fileTorrc;

        int exitCode = -1;

        try {
            exitCode = exec(torCmdString + " --verify-config", true);
        }
        catch (Exception e)
        {
            logNotice("Tor configuration did not verify: " + e.getMessage(),e);
            return false;
        }

        try {
            exitCode = exec(torCmdString, true);
        }
        catch (Exception e)
        {
            logNotice("Tor was unable to start: " + e.getMessage(),e);
            return false;
        }

        if (exitCode != 0)
        {
            logNotice("Tor did not start. Exit:" + exitCode);
            return false;
        }


        return result;
    }


    private int exec (String cmd, boolean wait) throws Exception
    {
        CommandResult shellResult = Shell.run(cmd);


      //  debug("CMD: " + cmd + "; SUCCESS=" + shellResult.isSuccessful());

        if (!shellResult.isSuccessful()) {
            throw new Exception("Error: " + shellResult.exitCode + " ERR=" + shellResult.getStderr() + " OUT=" + shellResult.getStdout());
        }

        return shellResult.exitCode;
    }
}
