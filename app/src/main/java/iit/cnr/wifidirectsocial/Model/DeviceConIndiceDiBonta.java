package iit.cnr.wifidirectsocial.Model;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Created by sorbeppe84 on 07/10/2015.
 */
public class DeviceConIndiceDiBonta extends WifiP2pDevice {

    String device_address;
    double indice_bonta;

    // Costruttore vuoto
    public DeviceConIndiceDiBonta()  {

    }

    // Costruttore completo
    public DeviceConIndiceDiBonta(String device_address, double indice_bonta)  {

        this.device_address = device_address;
        this.indice_bonta = indice_bonta;

    }

    public void setDeviceAddress(String device_address) {
        this.device_address = device_address;
    }

    public void setIndiceBonta(double indice_bonta) {
        this.indice_bonta = indice_bonta;
    }

    public Double getIndiceBonta() {

        return this.indice_bonta;
    }

    public String getDeviceAddress() {

        return this.device_address;
    }


}
