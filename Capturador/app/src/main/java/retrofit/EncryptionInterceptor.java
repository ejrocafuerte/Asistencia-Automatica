package retrofit;

import android.util.Log;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import cipher.Crypt;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

/**
 * Created by Desarrollo on 05/01/2018.
 */

public class EncryptionInterceptor implements Interceptor {

    private static final String TAG = EncryptionInterceptor.class.getSimpleName();
    protected static Crypt crypto;

    @Override
    public Response intercept(Chain chain) throws IOException {

        crypto = Crypt.getInstance();
        Request request = chain.request();
        RequestBody oldBody = request.body();
        Buffer buffer = new Buffer();
        oldBody.writeTo(buffer);
        String strOldBody = buffer.readUtf8();
        MediaType mediaType = MediaType.parse("text/plain; charset=utf-8");
        String strNewBody = encrypt(strOldBody);
        RequestBody body = RequestBody.create(mediaType, strNewBody);
        request = request.newBuilder().header("Content-Type", body.contentType().toString()).header("Content-Length", String.valueOf(body.contentLength())).method(request.method(), body).build();
        return chain.proceed(request);
    }

    private static String encrypt(final String message) {
        try {
            String encryptMessage = crypto.encrypt_string(message);
            Log.e("Encrypted Message", encryptMessage);
            return encryptMessage;
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}

