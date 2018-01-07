package models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Created by Desarrollo on 03/01/2018.
 */

public class Asistencia {
    @SerializedName("mac")
    @Expose
    private String mac = "";

    @SerializedName("imei")
    @Expose
    private String imei = "";

    @SerializedName("estudiante")
    @Expose
    private Estudiante estudiante;

    @SerializedName("fecha")
    @Expose
    private String fecha = "";

    @SerializedName("materia")
    @Expose
    private String materia = "CCG01INTEGRADORA";

    @SerializedName("codigo")
    @Expose
    private String codigo = "0110101";

    @SerializedName("distanciaX")
    @Expose
    private String distanciaX = "0.0";

    @SerializedName("distanciaY")
    @Expose
    private String distanciaY = "0.0";

    @SerializedName("senales")
    @Expose
    private ArrayList<Senal> senales = new ArrayList<Senal>();

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getMateria() {
        return materia;
    }

    public void setMateria(String materia) {
        this.materia = materia;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDistanciaX() {
        return distanciaX;
    }

    public void setDistanciaX(String distanciaX) {
        this.distanciaX = distanciaX;
    }

    public String getDistanciaY() {
        return distanciaY;
    }

    public void setDistanciaY(String distanciaY) {
        this.distanciaY = distanciaY;
    }

    public ArrayList<Senal> getSenales() {
        return senales;
    }

    public void setSenales(ArrayList<Senal> senales) {
        this.senales = senales;
    }

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public void setEstudiante(Estudiante estudiante) {
        this.estudiante = estudiante;
    }

    @Override
    public String toString() {
        StringBuilder sbs = new StringBuilder();
        for(int i = 0; i < senales.size(); i++){
            sbs.append(senales.get(i).toString());
            if(i < senales.size()-1){
                sbs.append("&&");
            }
        }
        return  mac + ";;" +
                imei + ";;" +
                estudiante.toString() + ";;" +
                fecha + ";;" +
                materia + ";;" +
                codigo + ";;" +
                distanciaX + ";;" +
                distanciaY + ";;" +
                sbs.toString();
    }
}
