package retrofit;

import com.google.gson.JsonObject;

import java.util.ArrayList;

import models.Asistencia;
import models.CodigosServer;
import models.ResponseServer;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * Created by HouSe on 13/08/2017.
 */

public interface RestClient {

    @POST("gestionarestudiante/")
    Call<ResponseServer> sendServer(@Body ArrayList<Asistencia> asistencias);

    @GET("codigosasistencias/")
    Call<CodigosServer> getCodigosServer();
}
