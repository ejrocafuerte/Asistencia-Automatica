package com.asistencia.integradora.espol.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by HouSe on 06/01/2018.
 */

public class ResponseServer {
    @SerializedName("response")
    @Expose
    private String response;
    @SerializedName("msg")
    @Expose
    private String message;
    @SerializedName("Profesor")
    @Expose
    private Profesor profesor;


    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Profesor getProfesor() {
        return profesor;
    }

    public void setProfesor(Profesor profesor) {
        this.profesor = profesor;
    }
}