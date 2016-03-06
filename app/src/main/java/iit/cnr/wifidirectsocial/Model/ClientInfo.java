package iit.cnr.wifidirectsocial.Model;

import java.io.Serializable;
import java.net.InetAddress;

import iit.cnr.wifidirectsocial.sqlite.model.Group;

/**
 * Created by sorbeppe84 on 14/10/2015.
 */
public class ClientInfo implements Serializable{

    String mac_addrees;
    String device_name;
    InetAddress inetAddress;

    // Costruttore vuoto
    public ClientInfo() {

    }

    // Costruttore completo
    public ClientInfo(String mac_address, String device_name,InetAddress inetAddress)  {

        this.mac_addrees = mac_address;
        this.device_name = device_name;
        this.inetAddress = inetAddress;
    }



    public void setMacAdrress(String mac_addrees) {
        this.mac_addrees = mac_addrees;
    }

    public void setDeviceName(String device_name) {
        this.device_name = device_name;
    }

    public void setInetAddress(InetAddress inetAddress) {

        this.inetAddress= inetAddress;
    }



    public String getMacAddress() {

        return this.mac_addrees;
    }

    public String getDeviceName() {

        return this.device_name;
    }

    public InetAddress getInetAddress() {

        return this.inetAddress;
    }


}
