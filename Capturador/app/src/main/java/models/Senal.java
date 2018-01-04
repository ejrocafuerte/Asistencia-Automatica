package models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Desarrollo on 03/01/2018.
 */

public class Senal {

    @SerializedName("bssid")
    @Expose
    private String bssid = "";

    @SerializedName("level")
    @Expose
    private int level = 0;

    @SerializedName("level2")
    @Expose
    private int level2 = 0;

    public String getBssid() {
        return bssid;
    }

    public void setBssid(String bssid) {
        this.bssid = bssid;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getLevel2() {
        return level2;
    }

    public void setLevel2(int level2) {
        this.level2 = level2;
    }

    @Override
    public String toString() {
        return bssid + "^^" +
               level + "^^" +
               level2;
    }
}
