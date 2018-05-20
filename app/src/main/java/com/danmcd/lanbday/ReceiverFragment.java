package com.danmcd.lanbday;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Date;

/**
 * Fragment which displays results from local connection. Specifically this fragment
 * will display the name, birthday, and age of the other connected client.
 */
public class ReceiverFragment extends BaseFragment {

    // General //
    public static final String TAG = "ReceiverFragment";

    // Birthday Data //
    private String mName;
    private Date mBirthDate;
    private int mAge = -1;

    // Views //
    private TextView mNameTextView; // Displays the name of the connected client
    private TextView mBirthdayTextView; // Displays the birthday of the connected client
    private TextView mAgeTextView; // Displays the age of the connected client

    public ReceiverFragment(){}

    public static ReceiverFragment newInstance() {
        return new ReceiverFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        // Inflate root view:
        View root = inflater.inflate(R.layout.fragment_sender, container, false);

        //TODO: setup views

        return root;
    }

    private void setBirthdayInfo(String name, Date birthDate) {
        // Set name & birth date:
        mName = name;
        mBirthDate = birthDate;
        // Calculate age:
        mAge = calculateAge(birthDate);

        //TODO: update ui
    }

    private String getName() {
        return mName;
    }

    private Date getBirthDate() {
        return mBirthDate;
    }

    private int getAge() {
        return mAge;
    }

    private int calculateAge(Date birthDate) {
        //TODO: calculate age given the birthdate
        return -1;
    }


}
