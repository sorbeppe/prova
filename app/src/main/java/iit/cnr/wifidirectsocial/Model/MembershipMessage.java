package iit.cnr.wifidirectsocial.Model;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Hashtable;

/**
 * Created by sorbeppe84 on 14/10/2015.
 */
public class MembershipMessage implements Serializable {


    // type = 1 Se il payload  e' un oggetto di tipo ClientInfo
    // type = 0

    int type;
    Object payload;
    //Hashtable payload;

    // Costruttore vuoto
    public MembershipMessage() {

    }

    // Costruttore 1
    public MembershipMessage(int type, Object payload)  {

        this.type = type;
        this.payload = payload;

    }



    public void setType(int type) {
        this.type = type;
    }

    public void setPayload(Object payload) {

        this.payload = payload;
    }

    public int getType() {

        return this.type;
    }

    public Object getPayload() {

        return this.payload;
    }



}
