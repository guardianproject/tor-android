// Copyright 2005 Nick Mathewson, Roger Dingledine
// See LICENSE file for copying information
package net.freehaven.tor.control;

/**
 * Abstract interface whose methods are invoked when Tor sends us an event.
 *
 * @see TorControlConnection#setEventHandler
 * @see TorControlConnection#setEvents
 */
@Deprecated
public interface EventHandler {
    /**
     * Invoked when a circuit's status has changed.
     * Possible values for <b>status</b> are:
     * <ul>
     * <li>{@link TorControlCommands#CIRC_EVENT_LAUNCHED} :  circuit ID assigned to new circuit</li>
     * <li>{@link TorControlCommands#CIRC_EVENT_BUILT}    :  all hops finished, can now accept streams</li>
     * <li>{@link TorControlCommands#CIRC_EVENT_EXTENDED} :  one more hop has been completed</li>
     * <li>{@link TorControlCommands#CIRC_EVENT_FAILED}   :  circuit closed (was not built)</li>
     * <li>{@link TorControlCommands#CIRC_EVENT_CLOSED}   :  circuit closed (was built)</li>
     * </ul>
     *
     * <b>circID</b> is the alphanumeric identifier of the affected circuit,
     * and <b>path</b> is a comma-separated list of alphanumeric ServerIDs.
     */
    public void circuitStatus(String status, String circID, String path);

    /**
     * Invoked when a stream's status has changed.
     * Possible values for <b>status</b> are:
     * <ul>
     * <li>{@link TorControlCommands#STREAM_EVENT_SENT_CONNECT}: Sent a connect cell along a circuit</li>
     * <li>{@link TorControlCommands#STREAM_EVENT_SENT_RESOLVE}: Sent a resolve cell along a circuit</li>
     * <li>{@link TorControlCommands#STREAM_EVENT_SUCCEEDED}: Received a reply; stream established</li>
     * <li>{@link TorControlCommands#STREAM_EVENT_FAILED}: Stream failed and not retriable</li>
     * <li>{@link TorControlCommands#STREAM_EVENT_CLOSED}: Stream closed</li>
     * <li>{@link TorControlCommands#STREAM_EVENT_NEW}: New request to connect</li>
     * <li>{@link TorControlCommands#STREAM_EVENT_NEW_RESOLVE}: New request to resolve an address</li>
     * <li>{@link TorControlCommands#STREAM_EVENT_FAILED_RETRIABLE}: </li>
     * <li>{@link TorControlCommands#STREAM_EVENT_REMAP}: </li>
     * </ul>
     *
     * <b>streamID</b> is the alphanumeric identifier of the affected stream,
     * and its <b>target</b> is specified as address:port.
     */
    public void streamStatus(String status, String streamID, String target);

    /**
     * Invoked when the status of a connection to an OR has changed.
     * Possible values for <b>status</b> are:
     * <ul>
     * <li>{@link TorControlCommands#OR_CONN_EVENT_LAUNCHED}</li>
     * <li>{@link TorControlCommands#OR_CONN_EVENT_CONNECTED}</li>
     * <li>{@link TorControlCommands#OR_CONN_EVENT_FAILED}</li>
     * <li>{@link TorControlCommands#OR_CONN_EVENT_CLOSED}</li>
     * <li>{@link TorControlCommands#OR_CONN_EVENT_NEW}</li>
     * </ul>
     * <b>orName</b> is the alphanumeric identifier of the OR affected.
     */
    public void orConnStatus(String status, String orName);

    /**
     * Invoked once per second. <b>read</b> and <b>written</b> are
     * the number of bytes read and written, respectively, in
     * the last second.
     */
    public void bandwidthUsed(long read, long written);

    /**
     * Invoked whenever Tor learns about new ORs.  The <b>orList</b> object
     * contains the alphanumeric ServerIDs associated with the new ORs.
     */
    public void newDescriptors(java.util.List<String> orList);

    /**
     * Invoked when Tor logs a message.
     * <b>severity</b> is one of:
     * <ul>
     * <li>{@link TorControlCommands#EVENT_DEBUG_MSG}</li>
     * <li>{@link TorControlCommands#EVENT_INFO_MSG}</li>
     * <li>{@link TorControlCommands#EVENT_NOTICE_MSG}</li>
     * <li>{@link TorControlCommands#EVENT_WARN_MSG}</li>
     * <li>{@link TorControlCommands#EVENT_ERR_MSG}</li>
     * </ul>
     * and <b>msg</b> is the message string.
     */
    public void message(String severity, String msg);

    /**
     * Invoked when an unspecified message is received.
     *
     * @param type the message type
     * @param msg  the message string
     */
    public void unrecognized(String type, String msg);

}

