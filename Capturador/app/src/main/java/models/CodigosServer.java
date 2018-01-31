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

    @SerializedName("codigosasistencias")
    @Expose
    private ArrayList<String> asistencias;

    public ArrayList<String> getAsistencias() {
        return asistencias;
    }

    public void setAsistencias(ArrayList<String> asistencias) {
        this.asistencias = asistencias;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
