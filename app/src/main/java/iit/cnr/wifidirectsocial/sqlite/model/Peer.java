package iit.cnr.wifidirectsocial.sqlite.model;

import java.io.Serializable;

/**
 * Created by sorbeppe84 on 14/07/2015.
 */
public class Peer implements Serializable {

    int id;
    String mac_address;
    String added_at;

    // constructors
    public Peer() {

    }

    public Peer( String mac_address, String added_at) {
        this.mac_address = mac_address;
        this.added_at = added_at;
    }


    public Peer(int id, String mac_address, String added_at) {
        this.id = id;
        this.mac_address = mac_address;
        this.added_at = added_at;
    }

    // setter
    public void setId(int id) {

        this.id = id;
    }

    public void setMacAddress(String mac_address) {

        this.mac_address = mac_address;
    }

    public void setAddedAt(String added_at) {

        this.added_at = added_at;
    }

    // getter
    public int getId() {
        return this.id;
    }

    public String getMacAddress() {

        return this.mac_address;
    }
    public String getAddedAt() {

        return this.added_at;
    }


}
