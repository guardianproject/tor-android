package org.torproject.android.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import org.torproject.android.binary.TorResourceInstaller;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;


public class sampleTorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample_tor);

        try {
            File fileTorBin = new TorResourceInstaller(this,getFilesDir()).installResources();

            boolean success = fileTorBin != null && fileTorBin.canExecute();

            String message = "Tor install success? " + success;

            ((TextView)findViewById(R.id.lblStatus)).setText(message);

        } catch (IOException e) {
            e.printStackTrace();
            ((TextView)findViewById(R.id.lblStatus)).setText(e.getMessage());

        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    public void doTorThings ()
    {
        //please see this project: https://github.com/thaliproject/Tor_Onion_Proxy_Library/
    }
}
