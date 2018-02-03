package com.asistencia.integradora.espol.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created by erick on 19/01/2018.
 */
public class Profesor implements Serializable{
    @SerializedName("nombres")
    @Expose
    private String nombres = "";

    @SerializedName("apellidos")
    @Expose
    private String apellidos = "";

    @SerializedName("matricula")
    @Expose
    private String matricula = "";

    public Profesor(){}

    public Profesor(String nombres, String apellidos, String matricula) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.matricula = matricula;
    }

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

    @Override
    public String toString() {
        return nombres + ";;" +
                apellidos + ";;" +
                matricula;
    }
}
