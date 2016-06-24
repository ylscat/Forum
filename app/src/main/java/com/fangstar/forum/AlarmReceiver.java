package com.fangstar.forum;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import java.util.Calendar;

import okhttp3.Response;

/**
 * Created at 2016/5/25.
 *
 * @author YinLanShan
 */
public class AlarmReceiver extends BroadcastReceiver implements Network.Callback{
    @Override
    public void onReceive(Context context, Intent intent) {
        String[] acc = Account.getAccount();
        if(acc[0] == null) {
            notifyFail(context, "未登录");
            return;
        }
        String cookie = Account.getCookie();
        if(cookie == null) {
            notifyFail(context, "未登录");
            return;
        }

        Network.getCredit(acc[0], cookie, this);
    }

    private void notifyFail(Context context, String reason) {
        Notification.Builder builder = new Notification.Builder(context);
        builder.setAutoCancel(true);
        builder.setSmallIcon(android.R.drawable.sym_def_app_icon);
        builder.setLights(Color.BLUE, 300, 1500);
        builder.setContentTitle("自动签到失败");
        builder.setContentText(reason);
        Intent intent = new Intent(context, Main.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        builder.setContentIntent(PendingIntent.getActivity(context, 0, intent, 0));
        NotificationManager nm = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        Notification n;
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
            n = builder.getNotification();
        else
            n = builder.build();
        n.defaults |= Notification.DEFAULT_VIBRATE;
        n.defaults |= Notification.DEFAULT_SOUND;
        nm.notify(1, n);
    }

    @Override
    public void onResponse(Response response, String data) {
        if(data == null) {
            if(response.code() == 413)
                notifyFail(App.sApp, "获取签到日志事件失败");
            return;
        }

        String[] info = HtmlUtils.getInfo(data);
        String[] acc = Account.getAccount();
        String cookie = Account.getCookie();
        if(acc[0] == null || cookie == null)
            return;

        Network.attend(acc[0], cookie, new Network.Callback() {
            @Override
            public void onResponse(Response response, String data) {
                if (response.code() != 200) {
                    notifyFail(App.sApp, "签到失败");
                }
            }
        });


        Automatic auto = new Automatic(App.sApp);
        if(auto.article == null)
            return;

        if(info[1] != null) {
            Calendar c = HtmlUtils.parseDate(info[1]);
            if(c != null && !HtmlUtils.isBeforeToday(c)) {
                notifyFail(App.sApp, "日志已存在");
                return;
            }

            Network.diary(auto.article, cookie, new Network.Callback() {
                @Override
                public void onResponse(Response response, String data) {
                    if(response == null)
                        notifyFail(App.sApp, "日志失败");
                    else {
                        if (response.code() != 200)
                            notifyFail(App.sApp, "日志失败 " + response.code());
                    }
                }
            });
        }
    }
}
