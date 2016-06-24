package com.fangstar.forum;

import android.app.Application;
import android.content.SharedPreferences;

/**
 * Created at 2016/5/20.
 *
 * @author YinLanShan
 */
public class App extends Application {
    public static final String SP_NAME = "sp";

    public static App sApp;
    public static SharedPreferences sSp;

    @Override
    public void onCreate() {
        super.onCreate();
        sApp = this;
        sSp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
    }
}
