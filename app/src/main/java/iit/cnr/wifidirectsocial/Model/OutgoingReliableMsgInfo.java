package iit.cnr.wifidirectsocial.Model;

/**
 * Created by sorbeppe84 on 12/10/2015.
 */
import android.net.wifi.p2p.WifiP2pDevice;

import java.net.InetAddress;

/**
 * This class represents a message ready to be stored as an outgoing message
 * inside the reliable sender queue
 *
 * @author Valerio Arnaboldi (valerio.arnaboldi@gmail.com)
 * @author Massimiliano Matozzo (massimiliano.matozzo@gmail.com)
 * @version 2.0
 *
 */

public class OutgoingReliableMsgInfo {

    private Object obj;
    private InetAddress dest_ip;
    private int port;

    /**
     * Creates a new OutgoingReliableMsgInfo object
     *
     * @param obj The message received from the upper level
     * @param dest_ip The destionation IP address
     * @param port The destination port
     */

    public OutgoingReliableMsgInfo(Object obj, InetAddress dest_ip, int port) {
        this.obj = obj;
        this.dest_ip = dest_ip;
        this.port = port;
    }

    /**
     * Retrieves the message object
     *
     * @return An Object containing the message
     */

    public Object getObj(){

        return obj;
    }

    /**
     * Retrieves the destination IP address
     *
     * @return An InetAddress containing the destination IP address
     */

    public InetAddress getDestIp(){

        return dest_ip;
    }

    /**
     * Gets the destination port
     *
     * @return An int representing the destionation port
     */

    public int getPort(){

        return port;
    }
}
