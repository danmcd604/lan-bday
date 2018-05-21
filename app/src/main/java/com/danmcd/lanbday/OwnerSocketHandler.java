package com.danmcd.lanbday;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by dan on 5/19/18.
 */

public class OwnerSocketHandler extends Thread implements Closeable {

    private static final String TAG = "OwnerSocketHandler";

    // Connection //
    private static final int THREAD_COUNT = 10;

    private ServerSocket socket = null;
    private Handler handler;
    private CommunicationManager manager;

    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(
            THREAD_COUNT, THREAD_COUNT, 10, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());

    public OwnerSocketHandler(Handler handler) throws IOException {
        try {
            // Define server socket w/ port:
            socket = new ServerSocket(MainActivity.SERVER_PORT);
            // Set handler:
            this.handler = handler;
            Log.d(TAG, "Socket Started");
        } catch (IOException e) {
            e.printStackTrace();
            // Send connection error:
            sendConnectionError();
            // Shutdown background executor:
            pool.shutdownNow();
            throw e;
        }

    }

    @Override
    public void run() {
        while (true) {
            try {
                // Initialize communication manager:
                manager = new CommunicationManager(socket.accept(), handler);
                pool.execute(manager);
                Log.d(TAG, "Launching the I/O handler");
            } catch (IOException e) {
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException ioe) {
                    e.printStackTrace();
                }
                e.printStackTrace();
                pool.shutdownNow();
                break;
            }
        }
    }

    @Override
    public void close() throws IOException {
        socket.close();
        socket = null;
    }

    public CommunicationManager getManager() {
        return manager;
    }

    private void sendConnectionError() {
        // Define message:
        Message message = new Message();
        message.what = CommunicationManager.CONNECTION_ERROR;

        // Send message to handler:
        handler.dispatchMessage(message);
    }
}
