package com.danmcd.lanbday;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by dan on 5/19/18.
 */

public class CommunicationManager implements Runnable {

    public static final int START = 1;
    public static final int RECEIVED = 2;
    public static final int CONNECTION_REFUSED = 3;

    private Socket socket = null;
    private Handler handler;

    public CommunicationManager(Socket socket, Handler handler) {
        this.socket = socket;
        this.handler = handler;
    }

    private InputStream iStream;
    private OutputStream oStream;
    private static final String TAG = "CommunicationManager";

    @Override
    public void run() {
        try {

            iStream = socket.getInputStream();
            oStream = socket.getOutputStream();
            byte[] buffer = new byte[1024];
            int bytes;
            handler.obtainMessage(START, this)
                    .sendToTarget();

            while (true) {
                try {
                    // Read from the InputStream

                    bytes = iStream.read(buffer);
                    if (bytes == -1) {
                        break;
                    }

                    // Send the obtained bytes to the UI Activity
//                    Log.d(TAG, "Rec:" + String.valueOf(buffer));
                    handler.obtainMessage(RECEIVED,
                            bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void write(byte[] buffer) {
        try {
            oStream.flush();
            oStream.write(buffer);
        } catch (IOException e) {
            Log.e(TAG, "Exception during write", e);
        }
    }

    public Socket getSocket() {
        return socket;
    }
}
