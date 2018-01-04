/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android.binary;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringBufferInputStream;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.Context;
import android.os.Build;
import android.util.Log;


public class TorResourceInstaller implements TorServiceConstants {

    
    File installFolder;
    Context context;
    
    public TorResourceInstaller (Context context, File installFolder)
    {
        this.installFolder = installFolder;
        
        this.context = context;
    }
    
    public void deleteDirectory(File file) {
        if( file.exists() ) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for(int i=0; i<files.length; i++) {
                    if(files[i].isDirectory()) {
                        deleteDirectory(files[i]);
                    }
                    else {
                        files[i].delete();
                    }
                }
            }
                
            file.delete();
        }
    }
    
    private final static String MP3_EXT = ".mp3";
    //        
    /*
     * Extract the Tor resources from the APK file using ZIP
     */
    public boolean installResources () throws IOException, TimeoutException
    {
        
        InputStream is;
        File outFile;

        String cpuPath = "armeabi";

        if (Build.CPU_ABI.contains("x86"))
        	cpuPath = "x86";

        deleteDirectory(installFolder);
        
        installFolder.mkdirs();

        is = context.getAssets().open(COMMON_ASSET_KEY + TORRC_ASSET_KEY);
        outFile = new File(installFolder, TORRC_ASSET_KEY);
        streamToFile(is,outFile, false, false);

        is = context.getAssets().open(COMMON_ASSET_KEY + POLIPOCONFIG_ASSET_KEY);
        outFile = new File(installFolder, POLIPOCONFIG_ASSET_KEY);
        streamToFile(is,outFile, false, false);

        is = context.getAssets().open(cpuPath + '/' + TOR_ASSET_KEY + MP3_EXT);
        outFile = new File(installFolder, TOR_ASSET_KEY);
        streamToFile(is,outFile, false, true);
        setExecutable(outFile);
    
        is = context.getAssets().open(cpuPath + '/' + POLIPO_ASSET_KEY + MP3_EXT);
        outFile = new File(installFolder, POLIPO_ASSET_KEY);
        streamToFile(is,outFile, false, true);
        setExecutable(outFile);

        installGeoIP();
    
        return true;
    }
    
    public boolean updateTorConfigCustom (File fileTorRcCustom, String extraLines) throws IOException, FileNotFoundException, TimeoutException
    {
    	if (fileTorRcCustom.exists())
    	{
    		fileTorRcCustom.delete();
    		Log.d("torResources","deleting existing torrc.custom");
    	}
    	else
    		fileTorRcCustom.createNewFile();
    	
    	FileOutputStream fos = new FileOutputStream(fileTorRcCustom, false);
    	PrintStream ps = new PrintStream(fos);
    	ps.print(extraLines);
    	ps.close();
    	
        return true;
    }
    
    public boolean updatePolipoConfig (File filePolipo, String extraLines) throws IOException, FileNotFoundException, TimeoutException
    {
        
        InputStream is;

        is = context.getAssets().open(COMMON_ASSET_KEY + POLIPOCONFIG_ASSET_KEY);
        streamToFile(is,filePolipo, false, false);

        if (extraLines != null && extraLines.length() > 0)
        {
            StringBufferInputStream sbis = new StringBufferInputStream('\n' + extraLines + '\n');
            streamToFile(sbis,filePolipo,true,false);
        }
        

        return true;
    }
    
    public boolean installPolipoConf () throws IOException, FileNotFoundException, TimeoutException
    {
        
        InputStream is;
        File outFile;

        is = context.getAssets().open(COMMON_ASSET_KEY + POLIPOCONFIG_ASSET_KEY);
        outFile = new File(installFolder, POLIPOCONFIG_ASSET_KEY);
        streamToFile(is,outFile, false, false);
        
        return true;
    }
    
    /*
     * Extract the Tor binary from the APK file using ZIP
     */
    
    private boolean installGeoIP () throws IOException
    {
        
        InputStream is;
        File outFile;
        
        outFile = new File(installFolder, GEOIP_ASSET_KEY);
        is = context.getAssets().open(COMMON_ASSET_KEY + GEOIP_ASSET_KEY);
        streamToFile(is, outFile, false, false);

        is = context.getAssets().open(COMMON_ASSET_KEY + GEOIP6_ASSET_KEY);
        outFile = new File(installFolder, GEOIP6_ASSET_KEY);
        streamToFile(is, outFile, false, false);
    
        return true;
    }
    

    /*
     * Write the inputstream contents to the file
     */
    public static boolean streamToFile(InputStream stm, File outFile, boolean append, boolean zip) throws IOException

    {
        byte[] buffer = new byte[FILE_WRITE_BUFFER_SIZE];

        int bytecount;

        OutputStream stmOut = new FileOutputStream(outFile.getAbsolutePath(), append);
        ZipInputStream zis = null;
        
        if (zip)
        {
            zis = new ZipInputStream(stm);            
            ZipEntry ze = zis.getNextEntry();
            stm = zis;
            
        }
        
        while ((bytecount = stm.read(buffer)) > 0)
        {

            stmOut.write(buffer, 0, bytecount);

        }

        stmOut.close();
        stm.close();
        
        if (zis != null)
            zis.close();
        
        
        return true;

    }



    private void setExecutable(File fileBin) {
        fileBin.setReadable(true);
        fileBin.setExecutable(true);
        fileBin.setWritable(false);
        fileBin.setWritable(true, true);
    }

}
