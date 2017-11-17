/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android.binary;

public interface TorServiceConstants {

    String TAG = "TorBinary";
	//name of the tor C binary
	String TOR_ASSET_KEY = "tor";	
	
	//torrc (tor config file)
	String TORRC_ASSET_KEY = "torrc";

	String COMMON_ASSET_KEY = "common/";

	//privoxy
	String POLIPO_ASSET_KEY = "polipo";
	
	//privoxy.config
	String POLIPOCONFIG_ASSET_KEY = "torpolipo.conf";
	
	//geoip data file asset key
	String GEOIP_ASSET_KEY = "geoip";
	String GEOIP6_ASSET_KEY = "geoip6";

	int FILE_WRITE_BUFFER_SIZE = 1024;



}
