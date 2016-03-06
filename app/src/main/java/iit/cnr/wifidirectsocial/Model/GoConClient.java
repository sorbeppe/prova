package iit.cnr.wifidirectsocial.Model;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Created by sorbeppe84 on 01/10/2015.
 */
public class GoConClient extends WifiP2pDevice {

    double NrClient;
    // prova cambio
    // Costruttore vuoto

    public GoConClient() {

    }

    // Costruttore
    public GoConClient(double NrClient)  {

        this.NrClient = NrClient;

    }
    public void setNrClient(double NrClient) {

        this.NrClient = NrClient;
    }

    public Double getNrClient() {

        return this.NrClient;
    }

}
