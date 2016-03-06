package iit.cnr.wifidirectsocial.Model;

import java.net.InetAddress;

/**
 * Created by sorbeppe84 on 15/10/2015.
 */
public class MTentry {


    ClientInfo client_info;
    InetAddress ip_address;


    // Costruttore vuoto
    public MTentry() {

    }

    // Costruttore
    public MTentry(ClientInfo client_info, InetAddress ip_address)  {

        this.client_info = client_info;
        this.ip_address = ip_address;


    }

    public void setClientInfo(ClientInfo client_info) {
        this.client_info = client_info;
    }

    public void setInetAddress(InetAddress ip_address) {
        this.ip_address = ip_address;
    }


    public ClientInfo getClientInfo() {

        return this.client_info;
    }

    public InetAddress getInetAddress() {

        return this.ip_address;
    }


}
