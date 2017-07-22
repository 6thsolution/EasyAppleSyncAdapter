package com.sixthsolution.lpisyncadapter.authenticator.crypto;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by mehdok on 4/10/2016.
 */
public class KeyManager {
    private static final String file1 = "id_value";
    private static final String file2 = "iv_value";

    public KeyManager() {
    }

    public void setId(byte[] data, Context ctx) {
        writer(data, file1, ctx);
    }

    public void setIv(byte[] data, Context ctx) {
        writer(data, file2, ctx);
    }

    public byte[] getId(Context ctx) {
        return reader(file1, ctx);
    }

    public byte[] getIv(Context ctx) {
        return reader(file2, ctx);
    }

    private byte[] reader(String file, Context ctx) {
        byte[] data = null;
        try {
            int bytesRead = 0;
            FileInputStream fis = ctx.openFileInput(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            while ((bytesRead = fis.read(b)) != -1) {
                bos.write(b, 0, bytesRead);
            }
            data = bos.toByteArray();
        } catch (FileNotFoundException e) {
            Log.e("KeyManager", "File not found in getId()");
        } catch (IOException e) {
            Log.e("KeyManager", "IOException in setId(): " + e.getMessage());
        }
        return data;
    }

    private void writer(byte[] data, String file, Context ctx) {
        try {
            FileOutputStream fos = ctx.openFileOutput(file,
                                                      Context.MODE_PRIVATE);
            fos.write(data);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e("KeyManager", "File not found in setId()");
        } catch (IOException e) {
            Log.e("KeyManager", "IOException in setId(): " + e.getMessage());
        }
    }
}
