package iit.cnr.wifidirectsocial.sqlite.model;

import java.io.Serializable;

/**
 * Created by sorbeppe84 on 14/07/2015.
 */

public class Group implements Serializable {

    int id;
    String mac_address_go;
    String group_name;
    String created_at;
    String destroyed_at;
    String last_time_seen;

    // Costruttore vuoto
    public Group() {
    }

    // Costruttore completo
    public Group(int id, String mac_address_go,String group_name,String created_at,String destroyed_at,String last_time_seen) {
        this.id = id;
        this.mac_address_go = mac_address_go;
        this.group_name = group_name;
        this.created_at = created_at;
        this.destroyed_at = destroyed_at;
        this.last_time_seen = last_time_seen;
    }

    public Group( String mac_address_go,String group_name,String created_at,String destroyed_at, String last_time_seen) {

        this.mac_address_go = mac_address_go;
        this.group_name = group_name;
        this.created_at = created_at;
        this.destroyed_at = destroyed_at;
        this.last_time_seen = last_time_seen;
    }


    // setters
    public void setId(int id) {

        this.id = id;
    }

    public void setMacAddressGo(String mac_address_go) {

        this.mac_address_go = mac_address_go;
    }

    public void setGroupName(String group_name) {

        this.group_name = group_name;
    }

    public void setCreatedAt(String created_at){

        this.created_at = created_at;
    }

    public void setDestroyedAt(String destroyed_at){

        this.destroyed_at = destroyed_at;
    }

    public void setLastTimeSeen(String last_time_seen){

        this.last_time_seen = last_time_seen;
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

    public String getCreatedAt(){

        return this.created_at;
    }
    public String getDestroyedAt(){

        return this.destroyed_at;
    }
    public String getLastTimeSeen(){

        return this.last_time_seen;
    }


}