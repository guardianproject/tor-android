package org.torproject.android.sample;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import org.torproject.android.binary.TorResourceInstaller;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.LogOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.concurrent.TimeoutException;

import static org.torproject.android.sample.SampleTorServiceConstants.TOR_CONTROL_PORT_MSG_BOOTSTRAP_DONE;


public class TorProxy {

    private Context cntx;
    private TorResourceInstaller torResourceInstaller;
    private TorStatus ts;
    private File fileTorBin = null;
    private File fileTorRc = null;
    private ProcessExecutor ps;

    public enum TorStatus {NOT_INIT, OK, CONNECTING, DIED, FAILED_INSTLLATION, INSTALLED, TORRC_NOT_FOUND}

    private int SOCKS_PORT = 9050;
    private int externalHiddenServicePort = -1;
    private int internalHiddenServicePort = -1;
    private boolean useBrideges = false;

    public TorProxy(TorBuilder builder) {
        this.cntx = builder.cntx;
        torResourceInstaller = new TorResourceInstaller(getContext(), getContext().getFilesDir());
        ts = TorStatus.NOT_INIT;

        if (builder != null) {
            this.SOCKS_PORT = builder.SOCKS_PORT;
            this.externalHiddenServicePort = builder.externalHiddenServicePort;
            this.internalHiddenServicePort = builder.internalHiddenServicePort;
            this.useBrideges = builder.useBrideges;
        }
    }


    private Context getContext() {
        return cntx;
    }

    public boolean init() {
        try {
            fileTorBin = torResourceInstaller.installResources();
            fileTorRc = torResourceInstaller.getTorrcFile();

            boolean success = fileTorBin != null && fileTorBin.canExecute();

            if (success) {
                ts = TorStatus.INSTALLED;
                return true;
            } else {
                ts = TorStatus.FAILED_INSTLLATION;
                return false;
            }
        } catch (Exception e) {
            ts = TorStatus.DIED;
            return false;
        }
    }

    public String getOnionAddress() {
        if (ts != TorStatus.OK) {
            Log.e("MSTTOR", "Tor is not OK. Run it first or check other errors.");
            return "";
        }

        File hostnameFolder = cntx.getDir("HiddenService", Context.MODE_PRIVATE);
        File hostname = new File(hostnameFolder, "hostname");

        if (!hostname.exists()) {
            Log.e("MSTTOR", "HiddenService did not created. Check for errors.");
            return "";
        }

        try {
            FileInputStream fis = new FileInputStream(hostname);
            byte[] data = new byte[(int) hostname.length()];
            fis.read(data);
            fis.close();

            String hiddenName = new String(data, "UTF-8");

            return hiddenName;
        } catch (Exception ex) {
            Log.e("MSTTOR", "Error in reading file. " + ex.getMessage());
            return "";
        }

    }

    public boolean start(final SampleTorActivity callback) throws IOException {
        File appCacheHome = cntx.getDir(SampleTorServiceConstants.DIRECTORY_TOR_DATA, Application.MODE_PRIVATE);
        File HiddenServiceDir = null;

        Vector<String> command = new Vector<>();
        command.add("DataDirectory " + appCacheHome.getCanonicalPath());
        command.add("SocksPort " + SOCKS_PORT);

        if (this.internalHiddenServicePort != -1 && this.externalHiddenServicePort != -1) {
            HiddenServiceDir = cntx.getDir("HiddenService", Context.MODE_PRIVATE);
            command.add("HiddenServiceDir " + HiddenServiceDir.getCanonicalPath());
            command.add("HiddenServicePort " + externalHiddenServicePort + " 127.0.0.1:" + internalHiddenServicePort);

            //Folder is too permissive
            try {
                new ProcessExecutor("chmod" , "700", HiddenServiceDir.getCanonicalPath()).execute();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        }

        if (!fileTorRc.exists()) {
            ts = TorStatus.TORRC_NOT_FOUND;
            return false;
        }

        //Start Tor
        ps = new ProcessExecutor();
        File torrc = createTorrc(command);
        try {
            ps.command(fileTorBin.getCanonicalPath(), "-f", torrc.getCanonicalPath())
                    .redirectOutput(new LogOutputStream() {
                        @Override
                        protected void processLine(String line) {
                            Log.e("MSTTOR", line);
                            if (line.contains(TOR_CONTROL_PORT_MSG_BOOTSTRAP_DONE)) {
                                ts = TorStatus.OK;
                                callback.callback();
                                return;
                            }
                        }
                    })
                    .execute();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        } catch (TimeoutException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private File createTorrc(Vector<String> options) {
        try {

            File torrcDir = cntx.getDir("torconfig", Context.MODE_PRIVATE);
            File fileWithinMyDir = new File(torrcDir, "torrc");
            FileOutputStream out = new FileOutputStream(fileWithinMyDir);
            for (String line : options) {
                out.write((line + "\r\n").getBytes());
            }
            out.flush();
            out.close();

            return fileWithinMyDir;
        } catch (IOException ex) {
            return null;
        }
    }


    public TorStatus getStatus() {
        return ts;
    }

    public static class TorBuilder {
        // optional parameters
        private int SOCKS_PORT = -1;
        private int externalHiddenServicePort = -1;
        private int internalHiddenServicePort = -1;
        private String extraLines = "";
        private boolean useBrideges = false;
        private Context cntx;

        public TorBuilder(Context cntx) {
            this.cntx = cntx;
        }

        public TorBuilder setSOCKsPort(int SOCKS_PORT) {
            this.SOCKS_PORT = SOCKS_PORT;
            return this;
        }

        public TorBuilder setExternalHiddenServicePort(int externalHiddenServicePort) {
            this.externalHiddenServicePort = externalHiddenServicePort;
            return this;
        }

        public TorBuilder setInternalHiddenServicePort(int internalHiddenServicePort) {
            this.internalHiddenServicePort = internalHiddenServicePort;
            return this;
        }

        public TorBuilder setUseBrideges(boolean useBrideges) {
            this.useBrideges = useBrideges;
            return this;
        }

        public TorBuilder setExtra(String extraLines) {
            this.extraLines = extraLines;
            return this;
        }

        public TorProxy build() {
            return new TorProxy(this);
        }

    }
}