package retrofit;

import android.text.TextUtils;
import android.util.Log;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by HouSe on 15/08/2017.
 */

public class Connection {
    private static final String TAG = "Connection";
    private static final String BASE_URL_DESA = "http://192.168.0.7:8000/";
    private static final String BASE_URL_PROD = "http://kalafitness.pythonanywhere.com/api/";
    private static Retrofit.Builder builder = null;
    private static Retrofit retrofit = null;
    private static OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

    public Connection() {
    }

    public static void init(){
        if(builder == null) {

            builder = new Retrofit.Builder()
                    .baseUrl(BASE_URL_DESA)
                    .addConverterFactory(GsonConverterFactory.create());
        }
    }

    public static <S> S createService(Class<S> serviceClass) {
        return createService(serviceClass, ".", ".");
    }

    public static <S> S createService( Class<S> serviceClass, String username, String password) {
        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
            String auth = Credentials.basic(username, password);
            return createService(serviceClass, auth, true);
        }
        return null;
    }

    public static <S> S createService( Class<S> serviceClass, final String auth, boolean withCredentials) {
        if (!TextUtils.isEmpty(auth)) {
            AuthenticationInterceptor interceptor_auth = new AuthenticationInterceptor(auth, withCredentials);
            EncryptionInterceptor interceptor_encryptor = new EncryptionInterceptor();
            HttpLoggingInterceptor interceptor_log = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);

            if (!httpClient.interceptors().contains(interceptor_auth)) {
                httpClient.addInterceptor(interceptor_auth);
                httpClient.addInterceptor(interceptor_log);
                httpClient.addInterceptor(interceptor_encryptor);

                builder.client(httpClient.build());
                retrofit = builder.build();
            }
        }
        return retrofit.create(serviceClass);
    }
}
