package retrofit;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Created by HouSe on 13/08/2017.
 */

public interface RestClient {
    @POST("gestionar/")
    Call<String> sendMessage(@Body String message);
}
