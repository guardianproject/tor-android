/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android.binary;

public interface TorServiceConstants {

    String TAG = "TorBinary";
	//name of the tor C binary
	String TOR_ASSET_KEY = "libtor";
	String TOR_BINARY_KEY = "tor";

	//torrc (tor config file)
	String TORRC_ASSET_KEY = "torrc";

	String COMMON_ASSET_KEY = "common/";

	//geoip data file asset key
	String GEOIP_ASSET_KEY = "geoip";
	String GEOIP6_ASSET_KEY = "geoip6";

	int FILE_WRITE_BUFFER_SIZE = 1024;

    String BINARY_TOR_VERSION = "0.4.2.7-openssl1.1.1g";

}
