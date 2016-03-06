package iit.cnr.wifidirectsocial.sqlite.model;

/**
 * Created by sorbeppe84 on 16/07/2015.
 */
public class Group_peer {

    int id;
    String mac_address_go;
    String group_name;
    int peer_id;
    String last_time_stamp;

    // constructors
    public Group_peer() {
    }

    public Group_peer(int id,String mac_address_go,String group_name, int peer_id, String last_time_stamp) {
        this.id = id;
        this.mac_address_go = mac_address_go;
        this.group_name= group_name;
        this.peer_id = peer_id;
        this.last_time_stamp = last_time_stamp;
    }

    public Group_peer(String mac_address_go,String group_name, int peer_id, String last_time_stamp) {

        this.mac_address_go = mac_address_go;
        this.group_name = group_name;
        this.peer_id = peer_id;
        this.last_time_stamp = last_time_stamp;
    }

    // setters
    public void setId(int id) {

        this.id = id;
    }

    public void setMacAddressGo(String mac_address_go) {

        this.mac_address_go= mac_address_go;
    }

    public void setGroupName(String group_name) {

        this.group_name= group_name;
    }

    public void setPeerId(int peer_id) {

        this.peer_id = peer_id;
    }

    public void setLastTimeStamp(String last_time_stamp) {

        this.id = id;
    }
    // getters
    public long getId() {

        return this.id;
    }

    public String getMacAddressGo() {

        return this.mac_address_go;
    }

    public String getGroupName() {

        return this.group_name;
    }


    public long getPeerId() {

        return this.peer_id;
    }

    public String getLastTimeStamp() {

        return this.last_time_stamp;
    }

}
