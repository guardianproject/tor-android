// Copyright 2005 Nick Mathewson, Roger Dingledine
// See LICENSE file for copying information
package net.freehaven.tor.control;

/**
 * A single key-value pair from Tor's configuration.
 */
public record ConfigEntry(String key, String value) {
    public ConfigEntry(String k) {
        this(k, "");
    }
}
