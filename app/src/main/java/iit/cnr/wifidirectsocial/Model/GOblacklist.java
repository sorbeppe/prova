package iit.cnr.wifidirectsocial.Model;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Created by sorbeppe84 on 17/09/2015.
 */
public class GOblacklist extends WifiP2pDevice {

    String addedAt;


    // Costruttore vuoto
    public GOblacklist() {

    }

    // Costruttore
    public GOblacklist(String addedAt)  {

        this.addedAt = addedAt;

    }
    public void setAddedAt(String addedAt) {
        this.addedAt = addedAt;
    }

    public String getAddedAt() {

        return this.addedAt;
    }



}
