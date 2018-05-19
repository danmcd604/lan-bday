package com.danmcd.lanbday;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import java.util.Date;


/**
 * Fragment which initializes a Wifi-P2P Service, and is responsible for sending 
 */
public class SenderFragment extends BaseFragment implements View.OnClickListener {

    // General //
    public static final String TAG = "SenderFragment";

    // Interactions //
    public interface SenderInteractions {
        void sendBirthday(String name, Date birthDay);
    }
    private SenderInteractions mListener;

    // Views //
    private Button mSendButton;
    private EditText mNameTextInput;

    public SenderFragment() {
        // Empty constructor, could be utilized later
    }

    public static SenderFragment newInstance() {
        return new SenderFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate root view:
        View root = inflater.inflate(R.layout.fragment_sender, container, false);

        // Setup input buttons:
        //TODO: setup input buttons

        // Setup 'send' button:
        mSendButton = (Button) root.findViewById(R.id.btn_send);
        mSendButton.setOnClickListener(this);


        return root;
    }

    @Override
    public void onAttachToContext(Context context) {
        super.onAttachToContext(context);
        // Handle attachment:
        if (context instanceof SenderInteractions) {
            mListener = (SenderInteractions) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement SenderInteractions");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View view) {
        // Handle view click:
        switch (view.getId()) {
            case R.id.btn_send:
                //TODO: handle send
                Log.i(TAG, "Clicked send button");
                break;
        }
    }
}
