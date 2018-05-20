package com.danmcd.lanbday;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Fragment which displays results from local connection. Specifically this fragment
 * will display the name, birthday, and age of the other connected client.
 */
public class ReceiverFragment extends BaseFragment {

    // General //
    public static final String TAG = "ReceiverFragment";

    // Birthday Data //
    private static final int DAYS_IN_YEAR = 365;
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
        View root = inflater.inflate(R.layout.fragment_receiver, container, false);

        // Setup content views:
        mNameTextView = (TextView) root.findViewById(R.id.tv_name);
        mBirthdayTextView = (TextView) root.findViewById(R.id.tv_birthdate);
        mAgeTextView = (TextView) root.findViewById(R.id.tv_age);

        return root;
    }

    public void setBirthdayInfo(String name, Date birthDate) {
        if(TextUtils.isEmpty(name) || birthDate == null) {
            Log.w(TAG, "Receievd bad birthday info");
            return;
        }
        // Set name & birth date:
        setName(name);
        setBirthDate(birthDate);
        // Calculate age:
        setAge(calculateAge(birthDate));
    }

    private void setName(String name) {
        // Set member name:
        mName = name;

        // Set name text:
        if(mNameTextView != null) {
            mNameTextView.setText(name);
        }
    }

    private void setBirthDate(Date birthDate) {
        // Set member date:
        mBirthDate = birthDate;

        // Set birth date text:
        if(mBirthdayTextView != null) {
            mBirthdayTextView.setText(SenderFragment.BIRTHDAY_FORMAT.format(birthDate));
        }
    }

    private void setAge(int age) {
        // Set member age:
        mAge = age;

        // Set age text:
        if(mAgeTextView != null) {
            mAgeTextView.setText(String.format("%d", age));
        }
    }

    public String getName() {
        return mName;
    }

    public  Date getBirthDate() {
        return mBirthDate;
    }

    public int getAge() {
        return mAge;
    }

    private int calculateAge(Date birthDate) {
        // Initialize age:
        int age = -1;

        // Get current date:
        long current = Calendar.getInstance().getTimeInMillis();
        long birth = birthDate.getTime();

        // Calculate differene:
        long difference = current - birth;

        // Calculate age of user in years:
        if(difference > 0) {
            long ageInDays = TimeUnit.DAYS.convert(difference, TimeUnit.MILLISECONDS);
            age = (int) ageInDays/DAYS_IN_YEAR;
        }

        return age;
    }


}
