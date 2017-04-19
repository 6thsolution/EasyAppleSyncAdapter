package com.sixthsolution.lpisyncadapter.authenticator.crypto;

import android.content.Context;
import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by mehdok on 4/10/2016.
 */
public class Crypto {
    private static final String engine = "AES";
    private static final String crypto = "AES/CBC/PKCS5Padding";

    private static byte[] cipher(byte[] data, int mode, Context ctx)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        KeyManager km = new KeyManager();
        SecretKeySpec sks = new SecretKeySpec(km.getId(ctx), engine);
        IvParameterSpec iv = new IvParameterSpec(km.getIv(ctx));
        Cipher c = Cipher.getInstance(crypto);
        c.init(mode, sks, iv);
        return c.doFinal(data);
    }

    private static byte[] encrypt(byte[] data, Context ctx) throws InvalidKeyException,
            NoSuchAlgorithmException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException,
            InvalidAlgorithmParameterException {
        return cipher(data, Cipher.ENCRYPT_MODE, ctx);
    }

    private static byte[] decrypt(byte[] data, Context ctx) throws InvalidKeyException,
            NoSuchAlgorithmException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException,
            InvalidAlgorithmParameterException {
        return cipher(data, Cipher.DECRYPT_MODE, ctx);
    }

    private static String getMD5BASE64(String str) {
        try {
            String base64 = Base64.encodeToString(str.getBytes("UTF-8"), Base64.NO_WRAP);
            return (getMD5EncryptedString(base64));
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    public static String armorEncrypt(byte[] data, Context ctx)
            throws InvalidKeyException, NoSuchAlgorithmException,
            NoSuchPaddingException, IllegalBlockSizeException,
            BadPaddingException, InvalidAlgorithmParameterException {
        return Base64.encodeToString(encrypt(data, ctx), Base64.DEFAULT);
    }

    public static String armorDecrypt(String data, Context ctx)
            throws InvalidKeyException, NoSuchAlgorithmException,
            NoSuchPaddingException, IllegalBlockSizeException,
            BadPaddingException, InvalidAlgorithmParameterException {
        return new String(decrypt(Base64.decode(data, Base64.DEFAULT), ctx));
    }

    public static String getMD5EncryptedString(String encTarget) {
        MessageDigest mdEnc = null;
        try {
            mdEnc = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Exception while encrypting to md5");
            e.printStackTrace();
        }
        // Encryption algorithm
        mdEnc.update(encTarget.getBytes(), 0, encTarget.length());
        String md5 = new BigInteger(1, mdEnc.digest()).toString(16);
        while (md5.length() < 32) {
            md5 = "0" + md5;
        }
        return md5;
    }
}
