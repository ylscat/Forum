package com.fangstar.forum;

import android.content.SharedPreferences;

/**
 * Created at 2016/5/20.
 *
 * @author YinLanShan
 */
public class Account {

    private static final String NAME = "name";
    private static final String PASSWORD = "password";
    private static final String COOKIE = "cookie";

    private static String[] sAccount;
    private static String sCookie;

    public static String[] getAccount(){
        if(sAccount != null)
            return sAccount;
        sAccount = new String[2];
        String[] account = sAccount;
        SharedPreferences sp = App.sSp;
        account[0] = sp.getString(NAME, null);
        account[1] = sp.getString(PASSWORD, null);
        return account;
    }

    public static void saveAccount(String name, String password) {
        if(sAccount == null)
            sAccount = new String[2];
        int index = name.indexOf('@');
        if(index != -1) {
            name = name.substring(0, index);
        }
        sAccount[0] = name;
        sAccount[1] = password;
        SharedPreferences sp = App.sSp;
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(NAME, name);
        editor.putString(PASSWORD, password);
        editor.apply();
    }

    public static void saveCookie(String cookie) {
        sCookie = cookie;
        SharedPreferences sp = App.sSp;
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(COOKIE, cookie);
        editor.apply();
    }

    public static String getCookie() {
        if(sCookie == null) {
            SharedPreferences sp = App.sSp;
            sCookie = sp.getString(COOKIE, null);
        }
        return sCookie;
    }


}
