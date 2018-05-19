package com.danmcd.lanbday;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by dan on 5/19/18.
 */

public class SenderSocketHandler extends Thread {
    private static final String TAG = "SenderSocketHandler";
    private Handler handler;
    private CommunicationManager manager;
    private InetAddress mAddress;

    public SenderSocketHandler(Handler handler, InetAddress groupOwnerAddress) {
        this.handler = handler;
        mAddress = groupOwnerAddress;

    }

    @Override
    public void run() {
        Socket socket = new Socket();
        try {
            socket.bind(null);
            Log.d(TAG, ""+socket.getLocalPort());
            socket.connect(new InetSocketAddress(mAddress.getHostAddress(),
                    MainActivity.SERVER_PORT), 5000);
            Log.d(TAG, "Launching the I/O handler");
            manager = new CommunicationManager(socket, handler);
            new Thread(manager).start();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return;
        }
    }

    public CommunicationManager getManager() {
        return manager;
    }

}
