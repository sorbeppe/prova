package iit.cnr.wifidirectsocial.Model;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Created by sorbeppe84 on 26/10/2015.
 */
public class DeviceWithParamGreatestGroup extends WifiP2pDevice {

    String device_address;
    int nr_peers_tot_greatest_group;
    double ind_variabilita_greatest_group;
    long recency_greatest_group;

    public DeviceWithParamGreatestGroup (){

    }

    public DeviceWithParamGreatestGroup (String device_address,int nr_peers_tot_greatest_group, double ind_variabilita_greatest_group, long recency_greatest_group  ){

        this.device_address = device_address;
        this.nr_peers_tot_greatest_group = nr_peers_tot_greatest_group;
        this.ind_variabilita_greatest_group = ind_variabilita_greatest_group;
        this.recency_greatest_group = recency_greatest_group;

    }

    public String getDeviceAddress() {

        return this.device_address;
    }

    public int getNrPeersTotGreatestGroup() {

        return this.nr_peers_tot_greatest_group;
    }

    public double getIndVariabilitaGreatestGroup() {

        return this.ind_variabilita_greatest_group;
    }

    public long getRecencyGreatestGroup() {

        return this.recency_greatest_group;
    }


}
