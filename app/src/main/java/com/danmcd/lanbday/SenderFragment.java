package com.danmcd.lanbday;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * Fragment which initializes a Wifi-P2P Service, and is responsible for sending 
 */
public class SenderFragment extends BaseFragment implements View.OnClickListener {

    // General //
    private static final String TAG = SenderFragment.class.getName();

    // Interactions //
    public interface SenderInteractions {
        void onFragmentInteraction(Uri uri);
    }
    private SenderInteractions mListener;

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
                break;
        }
    }
}
