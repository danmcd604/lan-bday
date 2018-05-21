package com.danmcd.lanbday;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Build;

/**
 * Base fragment class that contains basic support methods usable by all fragments, as well as,
 * SDK compatibility code.
 */
public class BaseFragment extends Fragment {

    @Override
    @TargetApi(23)
    public void onAttach(Context context) {
        super.onAttach(context);
        onAttachToContext(context);
    }


    @Override
    @SuppressWarnings("deprecation")
    public void onAttach(Activity activity){
        super.onAttach(activity);
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            onAttachToContext(activity);
        }
    }

    public void onAttachToContext(Context context){
    }

    /**
     * States if the fragment is attached to its host/context. If
     * true, then the fragment is capable of accessing the application's
     * resource files.
     * @return
     */
    public boolean isAttached(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            return getHost() != null;
        }else{
            return getActivity() != null;
        }
    }
}
