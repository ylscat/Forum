package com.fangstar.forum;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Calendar;

public class Automatic {
    public long time;
    public String article;
    public static final String DEFAULT_CONTENT = "占坑日志";

    public Automatic(Context context) {
        SharedPreferences sp = App.sSp;
        time = sp.getLong("time", 7L*60*60*1000);
        article = sp.getString("article", DEFAULT_CONTENT);
    }

    public Automatic(long attend, String article) {
        this.time = attend;
        this.article = article;
    }

    public void save(Context context) {
        SharedPreferences sp = App.sSp;
        long t = sp.getLong("time", 7L*60*60*1000);
        String a = sp.getString("article", DEFAULT_CONTENT);

        SharedPreferences.Editor editor = null;
        if(t != time) {
            editor = sp.edit();
            editor.putLong("time", time);
        }

        if(!(a == article || a.equals(article))) {
            if(editor == null)
                editor = sp.edit();
            editor.putString("article", article);
        }

        if(editor != null)
            editor.apply();

        PendingIntent pi = PendingIntent.getBroadcast(context, 0,
                new Intent(context, AlarmReceiver.class), 0);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if(time == -1)
            am.cancel(pi);
        else {
            Calendar c = Calendar.getInstance();
            c.clear(Calendar.SECOND);
            c.clear(Calendar.MILLISECOND);
            int hour = (int)(time/(3600L*1000));
            int min = (int)(time%(3600L*1000)/1000/60);
            c.set(Calendar.HOUR_OF_DAY, hour);
            c.set(Calendar.MINUTE, min);
            am.setRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, pi);
        }
    }
}