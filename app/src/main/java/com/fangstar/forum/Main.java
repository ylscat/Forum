package com.fangstar.forum;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.List;

import okhttp3.Response;

/**
 * Created at 2016/5/19.
 *
 * @author YinLanShan
 */
public class Main extends Activity implements View.OnClickListener, TextView.OnEditorActionListener {
    private Dialog mWaitingDialog;
    private PopupWindow mPopupWindow;
    private Calendar mAttendTime, mDiaryTime;
    private String mArticleId, mCsrf;
    private EditText mPost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        dialog.setContentView(R.layout.waiting);
        dialog.setCancelable(false);
        mWaitingDialog = dialog;

        findViewById(R.id.attend).setOnClickListener(this);
        findViewById(R.id.diary).setOnClickListener(this);
        findViewById(R.id.more).setOnClickListener(this);
        mPost = (EditText) findViewById(R.id.post);
        mPost.setOnEditorActionListener(this);

        setup();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.attend:
                attend();
                break;
            case R.id.more:
                if(mPopupWindow == null) {
                    PopupWindow pw = new PopupWindow(this);
                    View view = getLayoutInflater().inflate(R.layout.more, null);
                    view.findViewById(R.id.item_account).setOnClickListener(this);
                    view.findViewById(R.id.item_setting).setOnClickListener(this);
                    pw.setContentView(view);
                    pw.setBackgroundDrawable(new ColorDrawable(0x99000000));
                    pw.setOutsideTouchable(true);
                    pw.setFocusable(true);
                    pw.showAsDropDown(v);
                    mPopupWindow = pw;
                }
                mPopupWindow.showAsDropDown(v);
                break;
            case R.id.diary:
                if(mPost.getVisibility() == View.VISIBLE)
                    mPost.setVisibility(View.GONE);
                else {
                    if(mDiaryTime != null && !HtmlUtils.isBeforeToday(mDiaryTime)) {
                        if(mArticleId == null)
                            return;
                        mWaitingDialog.show();
                        Network.getDiary(mArticleId, Account.getCookie(), new Network.Callback() {
                            @Override
                            public void onResponse(Response response, String data) {
                                mWaitingDialog.dismiss();
                                if(data != null) {
                                    mCsrf = HtmlUtils.getCsrf(data);
                                    mPost.setText(HtmlUtils.getContent(data));
                                }
                                else {
                                    mPost.setVisibility(View.GONE);
                                }
                            }
                        });
                    }

                    mPost.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.item_account:
                mPopupWindow.dismiss();
                promptLogin();
                break;
            case R.id.item_setting:
                mPopupWindow.dismiss();
                promptSettings();
                break;
        }
    }

    private void promptLogin() {
        final Dialog dialog = new Dialog(this,
                android.R.style.Theme_Holo_Light_Dialog_NoActionBar_MinWidth);

        dialog.setContentView(R.layout.login);
        dialog.findViewById(R.id.login).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        login(dialog);
                    }
                }
        );
        EditText et = (EditText)dialog.findViewById(R.id.password);
        et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                login(dialog);
                return true;
            }
        });
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void login(final Dialog dialog) {
        EditText et = (EditText) dialog.findViewById(R.id.name);
        final String name = et.getText().toString().trim();
        et = (EditText) dialog.findViewById(R.id.password);
        final String password = et.getText().toString().trim();

        if(name.length() == 0) {
            Toast.makeText(this, "账户不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        if(password.length() == 0) {
            Toast.makeText(this, "密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        mWaitingDialog.show();
        Network.login(name, password, new Network.Callback() {
            @Override
            public void onResponse(Response response, String data) {
                mWaitingDialog.dismiss();
                if(response.code() != 200) {
                    Toast.makeText(Main.this, "登录失败", Toast.LENGTH_SHORT).show();
                    return;
                }

                String msg = null;
                try {
                    if(data.length() > 1) {
                        JSONObject json = new JSONObject(data);
                        if(!json.optBoolean("sc"))
                            msg = json.optString("msg");
                    }
                }
                catch (JSONException e) {
                    msg = e.getMessage();
                }

                if(msg != null) {
                    Toast.makeText(Main.this, msg, Toast.LENGTH_SHORT).show();
                    return;
                }

                Account.saveAccount(name, password);

                List<String> cookies = response.headers("Set-Cookie");
                StringBuilder cookie = new StringBuilder();
                for(String c : cookies) {
                    int index = c.indexOf(';');
                    String item = index == -1 ? c : c.substring(0, index);
                    cookie.append(item).append(";");
                }
                if(cookie.length() > 0) {
                    Account.saveCookie(cookie.substring(0, cookie.length() - 1));
                }
                setup();
                dialog.dismiss();
            }
        });
    }

    private void setup() {
        String[] acc = Account.getAccount();
        if(acc[0] == null) {
            promptLogin();
            return;
        }
        final String cookie = Account.getCookie();
        if(cookie == null) {
            promptLogin();
            return;
        }
        mWaitingDialog.show();
        TextView tv = (TextView)findViewById(R.id.name);
        tv.setText(acc[0]);
        Network.getCredit(acc[0], cookie, new Network.Callback() {
            @Override
            public void onResponse(Response response, String data) {
                mWaitingDialog.dismiss();
                if(data == null) {
                    if(response.code() == 413)
                        promptLogin();
                    return;
                }

                String[] info = HtmlUtils.getInfo(data);

                TextView tv = (TextView) findViewById(R.id.attend_date);
                if(info[0] != null) {
                    tv.setText(info[0]);
                    findViewById(R.id.attend).setEnabled(true);
                    mAttendTime = HtmlUtils.parseDate(info[0]);
                }
                else {
                    tv.setText("--");
                    findViewById(R.id.attend).setEnabled(false);
                }

                tv = (TextView) findViewById(R.id.diary_date);
                if(info[1] != null) {
                    tv.setText(info[1]);
                    findViewById(R.id.diary).setEnabled(true);
                    mDiaryTime = HtmlUtils.parseDate(info[1]);
                }
                else {
                    tv.setText("--");
                    findViewById(R.id.diary).setEnabled(false);
                }

                tv = (TextView) findViewById(R.id.points);
                if(info[2] != null)
                    tv.setText(info[2]);
                else
                    tv.setText("--");

                mArticleId = info[3];

                String avatar = info[4];
                if(avatar != null) {
                    ImageView iv = (ImageView)findViewById(R.id.avatar);
                    Network.loadImage(avatar, cookie, iv);
                }
            }
        });
    }

    private void attend() {
        final String[] acc = Account.getAccount();
        if(acc[0] == null) {
            promptLogin();
            return;
        }
        String cookie = Account.getCookie();
        if(cookie == null) {
            promptLogin();
            return;
        }

        mWaitingDialog.show();
        Network.attend(acc[0], cookie, new Network.Callback() {
            @Override
            public void onResponse(Response response, String data) {
                mWaitingDialog.dismiss();
                setup();
            }
        });
    }

    private void promptSettings() {
        final Dialog d = new Dialog(this);
        d.setTitle("自动设置");
        d.setContentView(R.layout.settings);
        Automatic auto = new Automatic(this);
        CheckBox cb = (CheckBox) d.findViewById(R.id.en);
        TextView tv = (TextView) d.findViewById(R.id.time);
        final SettingController sc = new SettingController(cb, tv, auto.time);

        EditText et = (EditText) d.findViewById(R.id.diary_content);
        et.setText(auto.article);

        d.findViewById(R.id.commit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = "";
                if(sc.getTime() >= 0) {
                    EditText et = (EditText) d.findViewById(R.id.diary_content);
                    content = et.getText().toString().trim();
                    if(content.length() < 4) {
                        Toast.makeText(Main.this,
                                "默认内容不能小于4", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                Automatic auto = new Automatic(sc.getTime(), content);
                auto.save(Main.this);
                d.dismiss();
            }
        });
        d.show();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        return false;
    }

    class SettingController implements View.OnClickListener,
            TimePickerDialog.OnTimeSetListener,
            CompoundButton.OnCheckedChangeListener {
        private CheckBox mEn;
        private TextView mText;
        private long mTime;

        public SettingController(CheckBox cb, TextView tv, long time) {
            mEn = cb;
            mText = tv;
            mTime = time;
            setup();
            mText.setOnClickListener(this);
            mEn.setOnCheckedChangeListener(this);
        }

        private void setup() {
            if(mTime >= 0) {
                mEn.setChecked(true);
                int[] t = getFmtTime(mTime);
                mText.setText(String.format("%02d:%02d", t[0], t[1]));
            }
            else {
                mEn.setChecked(false);
                mText.setText(null);
            }
        }

        @Override
        public void onClick(View v) {
            if(!mEn.isChecked())
                return;
            int h = 0, m = 0;
            if(mTime >= 0) {
                int[] t = getFmtTime(mTime);
                h = t[0];
                m = t[1];
            }
            TimePickerDialog dialog = new TimePickerDialog(Main.this,
                    this, h, m, true);
            dialog.show();
        }

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            mTime = hourOfDay*3600L*1000 + minute*60*1000;
            mText.setText(String.format("%02d:%02d", hourOfDay, minute));
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked) {
                if(mTime < 0)
                    mTime = 0;

                int[] t = getFmtTime(mTime);
                mText.setText(String.format("%02d:%02d", t[0], t[1]));

            }
            else {
                mText.setText(null);
            }
        }

        public long getTime() {
            if(mEn.isChecked())
                return mTime;
            else
                return -1;
        }

        private int[] getFmtTime(long t) {
            int hour = (int)(t/(3600L*1000));
            int min = (int)(t%(3600L*1000)/1000/60);
            return new int[]{hour, min};
        }
    }
}
