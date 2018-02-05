package com.asistencia.integradora.espol.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by erick on 4/2/2018.
 */

public class Usuario {
    @SerializedName("user")
    @Expose
    private String user = "";

    @SerializedName("pass")
    @Expose
    private String pass = "";

    public Usuario(){}

    public Usuario(String user, String pass) {
        this.user = user;
        this.pass = pass;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    @Override
    public String toString() {
        return user + ";;" +
                pass;
    }
}
