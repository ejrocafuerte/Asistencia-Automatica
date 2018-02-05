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
    private ArrayList<Codigo> codigos;

    @SerializedName("msg")
    @Expose
    private String message;

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public ArrayList<Codigo> getCodigos() {
        return codigos;
    }

    public void setCodigos(ArrayList<Codigo> codigos) {
        this.codigos = codigos;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
