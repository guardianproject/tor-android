
package org.torproject.android.binary;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class NativeLoader {

    private final static int LIB_VERSION = 1;
    private final static String LIB_NAME = "tor";
    private final static String LIB_SO_NAME = "tor.so";
    private final static String LOCALE_LIB_SO_NAME = "tor.so";

    private static volatile boolean nativeLoaded = false;

    private final static String TAG = "TorNativeLoader";
    
    private static File getNativeLibraryDir(Context context) {
        File f = null;
        if (context != null) {
            try {
                f = new File((String)ApplicationInfo.class.getField("nativeLibraryDir").get(context.getApplicationInfo()));
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
        if (f == null) {
            f = new File(context.getApplicationInfo().dataDir, "lib");
        }
        if (f != null && f.isDirectory()) {
            return f;
        }
        return null;
    }

    private static boolean loadFromZip(Context context, File destLocalFile, String folder) {


        ZipFile zipFile = null;
        InputStream stream = null;
        try {
            zipFile = new ZipFile(context.getApplicationInfo().sourceDir);
            ZipEntry entry = zipFile.getEntry("lib/" + folder + "/" + LIB_SO_NAME);
            if (entry == null) {
                throw new Exception("Unable to find file in apk:" + "lib/" + folder + "/" + LIB_NAME);
            }
            stream = zipFile.getInputStream(entry);

            OutputStream out = new FileOutputStream(destLocalFile);
            byte[] buf = new byte[4096];
            int len;
            while ((len = stream.read(buf)) > 0) {
                Thread.yield();
                out.write(buf, 0, len);
            }
            out.close();

            if (Build.VERSION.SDK_INT >= 9) {
                destLocalFile.setReadable(true, false);
                destLocalFile.setExecutable(true, false);
                destLocalFile.setWritable(true);
            }

            try {
              //  System.load(destLocalFile.getAbsolutePath());
                nativeLoaded = true;
            } catch (Error e) {
                Log.e(TAG, e.getMessage());
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
        return false;
    }

    public static synchronized boolean initNativeLibs(Context context, File destLocalFile) {
        if (nativeLoaded) {
            return nativeLoaded;
        }

        try {
            String folder = null;

            try {
                /**
                if (Build.CPU_ABI.equalsIgnoreCase("armeabi-v7a")) {
                    folder = "armeabi-v7a";
                } else
                **/
                if (Build.CPU_ABI.startsWith("armeabi")) {
                    folder = "armeabi";
                } else if (Build.CPU_ABI.equalsIgnoreCase("x86")) {
                    folder = "x86";
                } else if (Build.CPU_ABI.equalsIgnoreCase("mips")) {
                    folder = "mips";
                } else {
                    folder = "armeabi";
                    //FileLog.e("tmessages", "Unsupported arch: " + Build.CPU_ABI);
                }
            } catch (Exception e) {
                //  FileLog.e("tmessages", e);
                Log.e(TAG, e.getMessage());
                folder = "armeabi";
            }


            String javaArch = System.getProperty("os.arch");
            if (javaArch != null && javaArch.contains("686")) {
                folder = "x86";
            }

            if (destLocalFile != null && destLocalFile.exists()) {
                try {
          //          System.load(destLocalFile.getAbsolutePath());
                    nativeLoaded = true;
                    return nativeLoaded;
                } catch (Error e) {
                    Log.e(TAG, e.getMessage());
                }
                destLocalFile.delete();
            }


            if (loadFromZip(context, destLocalFile, folder)) {
                return true;
            }

            /*
            folder = "x86";
                destLocalFile = new File(context.getFilesDir().getAbsolutePath() + "/libtmessages86.so");
                if (!loadFromZip(context, destLocalFile, folder)) {
                    destLocalFile = new File(context.getFilesDir().getAbsolutePath() + "/libtmessagesarm.so");
                    folder = "armeabi";
                    loadFromZip(context, destLocalFile, folder);
                }
             */
        } catch (Throwable e) {
            e.printStackTrace();
        }

        try {
       //     System.loadLibrary(LIB_NAME);
            nativeLoaded = true;
        } catch (Error e) {
            Log.e(TAG, e.getMessage());
        }

        return nativeLoaded;
    }
}
