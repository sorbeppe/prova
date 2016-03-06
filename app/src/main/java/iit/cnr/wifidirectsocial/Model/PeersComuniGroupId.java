package iit.cnr.wifidirectsocial.Model;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Created by sorbeppe84 on 06/10/2015.
 */
public class PeersComuniGroupId extends WifiP2pDevice {

    String mac_address_go;
    String group_name;
    int nrPeersComuni;



    // Costruttore vuoto
    public PeersComuniGroupId () {

    }

    //Costruttore completo
    public PeersComuniGroupId (String mac_address_go,String group_name, int nrPeersComuni ) {

        this.mac_address_go = mac_address_go;
        this.group_name = group_name;
        this.nrPeersComuni = nrPeersComuni;

    }

    public void setMacAddressGo(String mac_address_go) {

        this.mac_address_go = mac_address_go;
    }

    public void setGroupName(String group_name) {

        this.group_name = group_name;
    }


    public void setNrPeersComuni(int nrPeersComuni) {

        this.nrPeersComuni = nrPeersComuni;
    }



    public String getMacAddressGo() {

        return this.mac_address_go;
    }

    public String getGroupName() {

        return this.group_name;
    }


    public Integer getNrPeersComuni() {

        return this.nrPeersComuni;
    }




}
