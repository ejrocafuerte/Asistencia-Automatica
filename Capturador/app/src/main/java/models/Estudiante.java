package models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Desarrollo on 03/01/2018.
 */

public class Estudiante {
    @SerializedName("nombres")
    @Expose
    private String nombres = "";

    @SerializedName("apellidos")
    @Expose
    private String apellidos = "";

    @SerializedName("matricula")
    @Expose
    private String matricula = "";

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }
}
