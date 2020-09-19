package org.torproject.android.sample.Connection;

public class AddressHelper {
    public enum AddressType {
        IP,
        Domain,
        HiddenService
    }

    public static AddressType getType(String destination) {
        if (destination.matches("^.[0-9]{1,3}/..[0-9]{1,3}/..[0-9]{1,3}/..[0-9]{1,3}") == true) { //IPv4
            return AddressType.IP;
        } else if (destination.contains(":") && !destination.contains(".")) { //IPv6
            return AddressType.IP;
        } else if (destination.endsWith(".onion")) {
            return AddressType.HiddenService;
        } else {
            return AddressType.Domain;
        }
    }
}
