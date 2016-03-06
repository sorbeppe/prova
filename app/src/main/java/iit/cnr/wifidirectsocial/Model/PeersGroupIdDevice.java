package iit.cnr.wifidirectsocial.Model;

import android.net.wifi.p2p.WifiP2pDevice;

import java.util.List;

import iit.cnr.wifidirectsocial.sqlite.model.Peer;

/**
 * Created by sorbeppe84 on 06/10/2015.
 */
public class PeersGroupIdDevice extends WifiP2pDevice {

    String deviceAddress;
    String mac_address_go;
    String group_name;
    List <Peer> listaPeers;

    //costruttore vuoto
    public PeersGroupIdDevice () {

    }

    //costruttore
    public PeersGroupIdDevice (String deviceAddress, String mac_address_go,String group_name,List <Peer> listaPeers ) {

        this.deviceAddress = deviceAddress;
        this.mac_address_go = mac_address_go;
        this.group_name = group_name;
        this.listaPeers = listaPeers;

    }

    public void setDeviceAddress(String deviceAddress) {

        this.deviceAddress = deviceAddress;
    }

    public void setMacAddressGo(String  mac_address_go) {

        this.mac_address_go = mac_address_go;
    }

    public void setGroupName(String  group_name) {

        this.group_name = group_name;
    }
    
    public void setListaPeers(List listaPeers) {

        this.listaPeers = listaPeers;
    }


    public String getDeviceAddress() {

        return this.deviceAddress;
    }

    public String getMacAddressGo() {

        return this.mac_address_go;
    }

    public String getGroupName() {

        return this.group_name;
    }

    public List getListaPeers() {

        return this.listaPeers;
    }



}
