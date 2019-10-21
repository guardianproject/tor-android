/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android.sample;

public interface SampleTorServiceConstants {

	String TOR_APP_USERNAME = "org.torproject.android";
	
	String DIRECTORY_TOR_BINARY = "bin";
	String DIRECTORY_TOR_DATA = "data";
	
	//name of the tor C binary
	String TOR_ASSET_KEY = "libtor";	
	
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

	//various console cmds
	String SHELL_CMD_CHMOD = "chmod";
	String SHELL_CMD_KILL = "kill -9";
	String SHELL_CMD_RM = "rm";
	String SHELL_CMD_PS = "toolbox ps";
	String SHELL_CMD_PS_ALT = "ps";
    
    
	//String SHELL_CMD_PIDOF = "pidof";
	String SHELL_CMD_LINK = "ln -s";
	String SHELL_CMD_CP = "cp";
	

	String CHMOD_EXE_VALUE = "770";

	int FILE_WRITE_BUFFER_SIZE = 1024;

	String IP_LOCALHOST = "127.0.0.1";
	int UPDATE_TIMEOUT = 1000;
	int TOR_TRANSPROXY_PORT_DEFAULT = 9040;
	
	int STANDARD_DNS_PORT = 53;
	int TOR_DNS_PORT_DEFAULT = 5400;
	String TOR_VPN_DNS_LISTEN_ADDRESS = "127.0.0.1";
	
	int CONTROL_PORT_DEFAULT = 9051;
    int HTTP_PROXY_PORT_DEFAULT = 8118; // like Privoxy!
    int SOCKS_PROXY_PORT_DEFAULT = 9050;

    
	//path to check Tor against
	String URL_TOR_CHECK = "https://check.torproject.org";

    //control port 
    String TOR_CONTROL_PORT_MSG_BOOTSTRAP_DONE = "Bootstrapped 100%";
    String LOG_NOTICE_HEADER = "NOTICE";
    String LOG_NOTICE_BOOTSTRAPPED = "Bootstrapped";
    




}
