package com.danmcd.lanbday;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


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

    // Birthday //
    public static final String DATE_REGEX = "\\d{2}/\\d{2}/\\d{4}";
    public static final DateFormat BIRTHDAY_FORMAT = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

    // Views //
    private Button mSendButton;
    private EditText mNameTextInput;
    private EditText mDateTextInput;

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
        mNameTextInput = (EditText) root.findViewById(R.id.input_name);
        mDateTextInput = (EditText) root.findViewById(R.id.input_date);

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
                /*
                    At this point we should check the current
                    fields to see if the content is formatted
                    correctly. If it is, send the data. If not,
                    toast the appropriate error.
                 */
                // Get name & birthday:
                String name = getName();
                Date birthday = getBirthDate();
                if(isValidContent()) {
                    mListener.sendBirthday(name, birthday);
                } else {
                    // Handle possible errors:
                    if(TextUtils.isEmpty(name)) {
                        Utils.showToast(
                                getActivity(),
                                "Please enter your name");
                    } else if(!getBirthDateText().matches(DATE_REGEX)) {
                        Utils.showToast(
                                getActivity(),
                                "Please enter a valid birth date (mm/dd/yyyy)");
                    }
                }
                break;
        }
    }

    private String getName() {
        if(mNameTextInput == null || mNameTextInput.getText() == null) {
            return null;
        }

        return mNameTextInput.getText().toString();
    }

    private String getBirthDateText() {
        if(mDateTextInput == null || mDateTextInput.getText() == null) {
            return "";
        }
        return mDateTextInput.getText().toString();
    }

    /**
     * Retrieves the birth date from the date text input field, if the fields text is
     * formatted correctly. Otherwise, return null.
     * @return
     */
    private Date getBirthDate() {
        // Get date text:
        String dateString = getBirthDateText();

        // Parse date from string:
        Date date = null;
        try {
            date = BIRTHDAY_FORMAT.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return date;
    }

    /**
     * Checks if the current content fields are formatted correctly.
     * Only the data field is restricted to a specific format.
     * @return
     */
    private boolean isValidContent() {
        return getBirthDateText().matches(DATE_REGEX) && !TextUtils.isEmpty(getName());
    }
}
