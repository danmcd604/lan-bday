package com.danmcd.lanbday;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by dan on 5/19/18.
 */

public class MemberSocketHandler extends Thread implements Closeable {


    private static final String TAG = "MemberSocketHandler";

    // Connections //
    private Handler handler;
    private CommunicationManager manager;
    private InetAddress address;
    private Socket socket;

    public MemberSocketHandler(Handler handler, InetAddress groupAddress) {
        this.handler = handler;
        address = groupAddress;

    }

    @Override
    public void run() {
        // Initialize socket:
        socket = new Socket();
        try {
            socket.bind(null);
            socket.connect(new InetSocketAddress(address.getHostAddress(),
                    MainActivity.SERVER_PORT), 5000);

            // Define communication manager:
            manager = new CommunicationManager(socket, handler);
            new Thread(manager).start();
        } catch (IOException e) {
            e.printStackTrace();
            Log.w(TAG, "Connection was refused!");
            Message message = new Message();
            message.what = CommunicationManager.CONNECTION_ERROR;
            handler.dispatchMessage(message);
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (NullPointerException e){
            Log.w(TAG, "Connection was refused!");
            Message message = new Message();
            message.what = CommunicationManager.CONNECTION_ERROR;
            handler.dispatchMessage(message);
            e.printStackTrace();
        }
    }

    public CommunicationManager getManager() {
        return manager;
    }

    @Override
    public void close() throws IOException {
        socket.close();
        socket = null;
    }
}
