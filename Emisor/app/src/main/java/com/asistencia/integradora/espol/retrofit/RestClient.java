package com.asistencia.integradora.espol.retrofit;

import java.util.ArrayList;

import com.asistencia.integradora.espol.models.Asistencia;
import com.asistencia.integradora.espol.models.CodigosServer;
import com.asistencia.integradora.espol.models.ResponseServer;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * Created by HouSe on 13/08/2017.
 */

public interface RestClient {

    @POST("gestionarestudiante/")
    Call<ResponseServer> sendMessage(@Body ArrayList<Asistencia> asistencias);

    @GET("codigosasistencias/")
    Call<CodigosServer> getCodigosServer();
}
