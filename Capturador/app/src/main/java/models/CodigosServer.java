package models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Created by Desarrollo on 03/01/2018.
 */

public class CodigosServer {
    @SerializedName("response")
    @Expose
    private String response;

    @SerializedName("codigos")
    @Expose
    private ArrayList<String> codigos;

    @SerializedName("msg")
    @Expose
    private String message;

    public ArrayList<String> getCodigos() {
        return codigos;
    }

    public void setCodigos(ArrayList<String> asistencias) {
        this.codigos = asistencias;
    }

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
}
