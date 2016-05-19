/*===============================================================================
Copyright (c) 2016 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.omeragca.artest.VuforiaSamples.ui.Splash;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.omeragca.artest.VuforiaSamples.R;
import com.omeragca.artest.VuforiaSamples.app.ImageTargets.ImageTargetActivity;

/**
 * Created by omer on 19.03.2016.
 */
public class ActivitySplashScreen extends Activity {

    private static long SPLASH_MILLIS = 500;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.splash_screen);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                startActivity(new Intent(ActivitySplashScreen.this, ImageTargetActivity.class));
            }

        }, SPLASH_MILLIS);
        // ImageTargets activity'sini 500 ms gecikmeli çağırdık.
    }

}
