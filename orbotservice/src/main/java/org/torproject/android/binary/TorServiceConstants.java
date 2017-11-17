/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android.binary;

import android.content.Intent;

public interface TorServiceConstants {

    String TAG = "TorBinary";
	String DIRECTORY_TOR_BINARY = "bin";
	String DIRECTORY_TOR_DATA = "data";
	
	//name of the tor C binary
	String TOR_ASSET_KEY = "tor";	
	
	//torrc (tor config file)
	String TORRC_ASSET_KEY = "torrc";
	String TORRCDIAG_ASSET_KEY = "torrcdiag";
	String TORRC_TETHER_KEY = "torrctether";
	
	String TOR_CONTROL_COOKIE = "control_auth_cookie";
	
	//privoxy
	String POLIPO_ASSET_KEY = "polipo";
	
	//privoxy.config
	String POLIPOCONFIG_ASSET_KEY = "torpolipo.conf";
	
	//geoip data file asset key
	String GEOIP_ASSET_KEY = "geoip";
	String GEOIP6_ASSET_KEY = "geoip6";

	String SHELL_CMD_PS = "toolbox ps";

	int FILE_WRITE_BUFFER_SIZE = 1024;

    //obfsproxy 
     String OBFSCLIENT_ASSET_KEY = "obfs4proxy";

     //DNS daemon for TCP DNS over TOr
	String PDNSD_ASSET_KEY = "pdnsd";




}
