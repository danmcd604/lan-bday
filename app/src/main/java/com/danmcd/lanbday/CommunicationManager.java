package com.danmcd.lanbday;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by dan on 5/19/18.
 */

public class CommunicationManager implements Runnable {
    private static final String TAG = "CommunicationManager";

    // Messages //
    public static final int START = 1; // States communcation has started.
    public static final int RECEIVED = 2; // State message was received.
    public static final int CONNECTION_ERROR = 3; // States connection error occured.

    // General //
    private Socket socket = null;
    private Handler handler;

    // Communication //
    private InputStream iStream;
    private OutputStream oStream;

    public CommunicationManager(Socket socket, Handler handler) {
        this.socket = socket;
        this.handler = handler;
    }

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

                    // Send bytes to handler:
                    handler.obtainMessage(RECEIVED,
                            bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Send connection error:
            sendConnectionError();
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

    private void sendConnectionError() {
        // Define message:
        Message message = new Message();
        message.what = CommunicationManager.CONNECTION_ERROR;

        // Send message to handler:
        handler.dispatchMessage(message);
    }
}
