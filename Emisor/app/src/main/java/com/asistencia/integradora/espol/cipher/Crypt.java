package com.asistencia.integradora.espol.cipher;

/**
 * Created by HouSe on 27/12/2017.
 */

import android.util.Base64;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypt {

    private static final String tag = Crypt.class.getSimpleName();

    private static final String characterEncoding = "UTF-8";
    private static final String cipherTransformation = "AES/CBC/PKCS5Padding";
    private static final String aesEncryptionAlgorithm = "AES";
    private static final String key = "51e1f539-b614-4df1-8005-96eb4b4e4b07"; //@@UUID.randomUUID().toString();
    private static byte[] ivBytes = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    private static byte[] keyBytes;

    private static Crypt instance = null;

    Crypt() {
        SecureRandom random = new SecureRandom();
        Crypt.ivBytes = new byte[16];
        random.nextBytes(Crypt.ivBytes);
    }

    public static Crypt getInstance() {
        if (instance == null) {
            instance = new Crypt();
        }

        return instance;
    }

    public String encrypt_string(final String plain) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, IOException {
        return Base64.encodeToString(encrypt(plain.getBytes()), Base64.NO_WRAP); //Base64.DEFAULT
    }

    public String decrypt_string(final String plain){
        byte[] encryptedBytes = new byte[0];
        try {
            encryptedBytes = decrypt(Base64.decode(plain, 0));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        //return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        return new String(encryptedBytes);
    }


    public byte[] encrypt(byte[] mes)
            throws NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException, IOException {

        keyBytes = key.getBytes(characterEncoding);
        //Log.d(tag, "Long KEY: " + keyBytes.length);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(keyBytes);
        keyBytes = md.digest();

        //Log.d(tag, "Long KEY: " + keyBytes.length);

        AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        SecretKeySpec newKey = new SecretKeySpec(keyBytes, aesEncryptionAlgorithm);
        Cipher cipher = null;
        cipher = Cipher.getInstance(cipherTransformation);

        SecureRandom random = new SecureRandom();
        Crypt.ivBytes = new byte[16];
        random.nextBytes(Crypt.ivBytes);

        cipher.init(Cipher.ENCRYPT_MODE, newKey, random);
        //cipher.init(Cipher.ENCRYPT_MODE, newKey, ivSpec);
        byte[] destination = new byte[ivBytes.length + mes.length];
        System.arraycopy(ivBytes, 0, destination, 0, ivBytes.length);
        System.arraycopy(mes, 0, destination, ivBytes.length, mes.length);
        return cipher.doFinal(destination);

    }

    public byte[] decrypt(byte[] bytes)
            throws NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException, IOException, ClassNotFoundException {

        keyBytes = key.getBytes(characterEncoding);
        //Log.d(tag, "Long KEY: " + keyBytes.length);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(keyBytes);
        keyBytes = md.digest();
        //Log.d(tag, "Long KEY: " + keyBytes.length);

        byte[] ivB = Arrays.copyOfRange(bytes, 0, 16);
        //Log.d(tag, "IV: " + new String(ivB));
        byte[] codB = Arrays.copyOfRange(bytes, 16, bytes.length);


        AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivB);
        SecretKeySpec newKey = new SecretKeySpec(keyBytes, aesEncryptionAlgorithm);
        Cipher cipher = Cipher.getInstance(cipherTransformation);
        cipher.init(Cipher.DECRYPT_MODE, newKey, ivSpec);
        byte[] res = cipher.doFinal(codB);
        return res;
    }
}