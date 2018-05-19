package com.danmcd.lanbday;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by dan on 5/19/18.
 */

public class Utils {

    public static Toast showToast(Context context, String text) {
        return showToast(context, text, Toast.LENGTH_LONG);
    }

    /**
     * Shows toast w/ given text to the user for the given duration.
     * @param context
     * @param text
     * @param duration
     * @return
     */
    public static Toast showToast(Context context, String text, int duration) {
        // Define toast:
        Toast toast = Toast.makeText(context, text, duration);
        // Show toast:
        toast.show();

        return toast;
    }
}
