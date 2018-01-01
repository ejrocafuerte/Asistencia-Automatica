package retrofit;

import models.Token;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * Created by HouSe on 13/08/2017.
 */

public interface RestClient {
    @Headers("Content-Type: application/json; charset=utf-8")
    @POST("gestionar/")
    Call<String> sendMessage(@Body Token message);
}