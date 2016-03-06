package iit.cnr.wifidirectsocial.Model;

/**
 * Created by sorbeppe84 on 12/10/2015.
 */
import java.net.InetAddress;

/**
 * @author Valerio Arnaboldi (valerio.arnaboldi@gmail.com)
 * version 2.0
 *
 */
public class ReliableMessageNotSentException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = -2881264626564456715L;
    private Object messageObject;
    private InetAddress dest_ip;

    public ReliableMessageNotSentException(Object obj, InetAddress dest_ip){
        super();
        messageObject = obj;
        this.dest_ip = dest_ip;
    }

    /**
     * @return the messageObject
     */
    public Object getMessageObject() {
        return messageObject;
    }

    /**
     * @return the dest_ip
     */
    public InetAddress getDest_ip() {
        return dest_ip;
    }

}

