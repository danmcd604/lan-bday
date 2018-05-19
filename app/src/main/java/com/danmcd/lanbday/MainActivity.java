package com.danmcd.lanbday;

import android.app.Fragment;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.Date;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, SenderFragment.SenderInteractions {

    private static final String TAG = "MainActivity";

    // Views //
    private Button mSenderButton;
    private Button mReceiverButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup chooser buttons:
        mSenderButton = (Button) findViewById(R.id.btn_sender);
        mSenderButton.setOnClickListener(this);

        mReceiverButton = (Button) findViewById(R.id.btn_receiver);
        mReceiverButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        // Handle view clicks:
        switch (view.getId()) {
            case R.id.btn_sender:
                Log.i(TAG, "Clicked sender button");
                // Launch sender fragment:
                launchSenderFragment();
                //TODO: initiate communication
                break;
            case R.id.btn_receiver:
                Log.i(TAG, "Clicked receiver button");
                //TODO: launch ReceiverFragment
                break;
        }
    }

    /**
     * Switches UI visibility to either show the fragment content or 'chooser' content.
     *      show = true -> Shows 'chooser' content
     *      show = false -> Shows fragment content
     * @param show
     */
    private void showContentChooserArea(boolean show) {
        // Transition to appropriate content:
        findViewById(R.id.content_chooser).setVisibility(show ? View.VISIBLE : View.GONE);
        findViewById(R.id.content_frame).setVisibility(show ? View.GONE : View.VISIBLE);
    }

    /////////////////////////////////////////////////
    ////////////////// NAVIGATION ///////////////////
    /////////////////////////////////////////////////

    private void launchSenderFragment() {
        // Launch sender fragment:
        launchFragment(SenderFragment.newInstance(), SenderFragment.TAG);
        // Transition to fragment content:
        showContentChooserArea(false);
    }

    private void launchReceiverFragment() {
        //TODO: implement launch receiver
    }

    /**
     * Launches fragment with a given tag adding it to the backstack.
     * @param fragment
     * @param tag
     */
    private void launchFragment(Fragment fragment, String tag) {
        if(fragment == null) {
            return;
        }

        // Launch fragment w/ tag:
        getFragmentManager().beginTransaction()
                .replace(R.id.content_frame, fragment, tag)
                .addToBackStack(tag)
                .commit();
    }

    ///////////////////////////////////////////////////
    ////////////////// CONNECTIONS ////////////////////
    ///////////////////////////////////////////////////

    ///////////////////////////////////////////////////
    /////////////// SEND COMMUNICATION ////////////////
    ///////////////////////////////////////////////////


    @Override
    public void sendBirthday(String name, Date birthDay) {
        //TODO: implement sending birthday
    }
}
