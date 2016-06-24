package com.fangstar.forum;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.List;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

/**
 * Created at 2016/5/19.
 *
 * @author YinLanShan
 */
public class Network {
    public static OkHttpClient sClient = new OkHttpClient();
    public static Handler sHandler = new Handler();

    private static final String SITE = "http://symx.fangstar.net";

    public static boolean login(String name, String password, Callback cb) {
        String md5 = getMd5(password);
        if(md5 == null)
            return false;
        final String body = String.format(
                "{\"nameOrEmail\":\"%s\",\"userPassword\":\"%s\"}",
                name, md5);
        RequestBody rb = new RequestBody() {
            @Override
            public long contentLength() throws IOException {
                return body.length();
            }

            @Override
            public MediaType contentType() {
                return MediaType.parse("application/x-www-form-urlencoded");
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                sink.writeString(body, Charset.defaultCharset());
            }
        };

        Request request = new Request.Builder()
                .url(SITE+ "/login")
                .post(rb)
                .build();
        sClient.newCall(request).enqueue(new NetworkProcessor(cb));
        return true;
    }

    public static void getCredit(String name, String cookie, Callback cb) {
        String url = String.format("%s/member/%s/points", SITE, name);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .header("Cookie", cookie)
                .build();
        sClient.newCall(request).enqueue(new NetworkProcessor(cb));
    }

    public static void loadImage(final String url, String cookie, final ImageView imageView) {
        String previousUrl = (String)imageView.getTag();
        if(url.equals(previousUrl))
            return;;
        String cacheUrl = getCacheUrl();
        if(url.equals(cacheUrl)) {
            Bitmap bitmap = getCache(imageView.getWidth(), imageView.getHeight());
            imageView.setImageBitmap(bitmap);
            return;
        }

        Request request = new Request.Builder()
                .url(url)
                .get()
                .header("Cookie", cookie)
                .build();
        sClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.code() == 200) {
                    byte[] bytes = response.body().bytes();
                    saveCache(url, bytes);

                    final Bitmap bitmap = getCache(imageView.getWidth(), imageView.getHeight());
                    imageView.post(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setImageBitmap(bitmap);
                            imageView.setTag(url);
                        }
                    });
                }
            }
        });
    }

    public static void attend(String name, String cookie, Callback cb) {
        String url = String.format("%s/activity/daily-checkin", SITE);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .header("Cookie", cookie)
                .build();
        sClient.newCall(request).enqueue(new NetworkProcessor(cb));
    }

    public static void getDiary(String id, final String cookie, Callback cb){
        String url = String.format("%s/update?id=%s", SITE, id);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .header("Cookie", cookie)
                .build();
        sClient.newCall(request).enqueue(new NetworkProcessor(cb));
    }

    public static void diary(final String content, final String cookie, final Callback cb) {
        final String origin = SITE + "/post?type=4&tags=" +
                "%E8%88%AA%E6%B5%B7%E6%97%A5%E8%AE%B0,%E6%AE%B5%E8%90%BD";
        Request request = new Request.Builder()
                .url(origin)
                .get()
                .header("Cookie", cookie)
                .build();
        sClient.newCall(request).enqueue(new NetworkProcessor(new Callback() {
            @Override
            public void onResponse(Response response, String data) {
                if(data == null) {
                    cb.onResponse(null, null);
                    return;
                }

                String csrf = HtmlUtils.getCsrf(data);
                if(csrf == null) {
                    cb.onResponse(null, null);
                    return;
                }

                List<String> cookies = response.headers("Set-Cookie");
                StringBuilder sb = new StringBuilder();
                for(String c : cookies) {
                    int index = c.indexOf(';');
                    String item = index == -1 ? c : c.substring(0, index);
                    sb.append(item).append(";");
                }
                String ck = cookie;
                if(sb.length() > 0) {
                    ck = sb.substring(0, sb.length() - 1);
                    Account.saveCookie(ck);
                }

                String url = String.format("%s/article", SITE);
                String body = String.format("{\"articleTitle\":\"%tF\"," +
                        "\"articleContent\":\"%s\"," +
                        "\"articleTags\":\"航海日记,段落\"," +
                        "\"articleCommentable\":true," +
                        "\"articleType\":\"4\"," +
                        "\"articleRewardContent\":\"\"," +
                        "\"articleRewardPoint\":\"\"}\n", Calendar.getInstance(), content);
                MediaType type = MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8");
                RequestBody requestBody = RequestBody.create(type, body);
                Request.Builder builder = new Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .header("Cookie", ck)
                        .header("Origin", "http://symx.fangstar.net")
                        .header("Referer", origin)
                        .header("csrfToken", csrf);

                Request request = builder.build();
                sClient.newCall(request).enqueue(new NetworkProcessor(cb));
            }
        }));
    }

    private static String getMd5(String input) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            // 输入的字符串转换成字节数组
            byte[] inputByteArray = input.getBytes();


            // inputByteArray是输入字符串转换得到的字节数组

            messageDigest.update(inputByteArray);


            // 转换并返回结果，也是字节数组，包含16个元素

            byte[] resultByteArray = messageDigest.digest();

            StringBuilder sb = new StringBuilder(resultByteArray.length*2);
            // 字符数组转换成字符串返回
            for(byte b : resultByteArray) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
        catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private static String getCacheUrl() {
        SharedPreferences sp = App.sSp;
        return sp.getString("avatar", null);
    }

    private static Bitmap getCache(int width, int height) {
        File cache = new File(App.sApp.getCacheDir(), "avatar.jpg");
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(cache.getPath(), options);
        int sample = 1;
        if(width > 0 && height > 0) {
            double rw = (float)options.outWidth/width;
            double rh = (float)options.outHeight/height;
            double r = Math.min(rw, rh);
            if(r > 2) {
                int p = (int) (Math.log(r)/Math.log(2));
                sample = (int) Math.pow(2, p);
            }
        }

        options.inJustDecodeBounds = false;
        options.inSampleSize = sample;
        return BitmapFactory.decodeFile(cache.getPath(), options);
    }

    private static void saveCache(String url, byte[] data) {
        File cache = new File(App.sApp.getCacheDir(), "avatar.jpg");
        try {
            FileOutputStream fos = new FileOutputStream(cache);
            fos.write(data);
            fos.close();
        } catch (IOException e) {
            Log.e("Forum", "save cache fail", e);
            SharedPreferences sp = App.sSp;
            SharedPreferences.Editor editor = sp.edit();
            editor.remove("avatar");
            editor.commit();
            return;
        }

        SharedPreferences sp = App.sSp;
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("avatar", url);
        editor.commit();
    }

    static class NetworkProcessor implements okhttp3.Callback, Runnable {
        private Callback mCallback;
        private Response mResponse;
        private String mData;

        public NetworkProcessor(Callback callback) {
            mCallback = callback;
        }

        @Override
        public void onFailure(Call call, IOException e) {
            sHandler.post(this);
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            mResponse = response;
            if(response != null && response.code() == 200)
                mData = mResponse.body().string();
            sHandler.post(this);
        }

        @Override
        public void run() {
            mCallback.onResponse(mResponse, mData);
        }
    }

    public interface Callback {
        void onResponse(Response response, String data);
    }
}
