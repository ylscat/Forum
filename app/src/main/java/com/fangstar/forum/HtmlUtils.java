package com.fangstar.forum;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created at 2016/5/23.
 *
 * @author YinLanShan
 */
public class HtmlUtils {

    public static int[] getFragment(String text, int index) {
        String sub = text.substring(0, index);
        int start = sub.lastIndexOf("<tr");
        int i = sub.lastIndexOf("</tr");
        if(start == - 1 || i > start)
            return null;

        int end = text.indexOf("</tr", index);
        end = text.indexOf('>', end) + 1;
        return new int[]{start, end};
    }

    public static Calendar parseDate(String text) {
        String[] frags = new String[5];
        frags[0] = text.substring(0, 4);
        frags[1] = text.substring(5, 7);
        frags[2] = text.substring(8, 10);
        frags[3] = text.substring(11, 13);
        frags[4] = text.substring(14, 16);


        try {
            int year = Integer.parseInt(frags[0]);
            int month = Integer.parseInt(frags[1]) - 1;
            int day = Integer.parseInt(frags[2]);
            int hour = Integer.parseInt(frags[3]);
            int minute = Integer.parseInt(frags[4]);
            return new GregorianCalendar(year, month, day, hour, minute);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String getDateString(String text) {
        Matcher matcher = Pattern.compile(
                ".*?(\\d\\d\\d\\d-\\d\\d-\\d\\d)",
                Pattern.DOTALL)
                .matcher(text);
        String date;
        if(matcher.find()) {
            date = matcher.group(1);
        }
        else
            return null;

        int index = matcher.end() + 1;
        String time = "00:00";
        matcher.reset();
        matcher.usePattern(Pattern.compile("\\d\\d:\\d\\d"));
        if(matcher.find(index)) {
            time = matcher.group();
        }

        return date + " " + time;
    }

    public static String getHref(String text) {
        Matcher matcher = Pattern.compile(
                "(?<=(<a href=\\\")).*(?=\\\">)")
                .matcher(text);
        if(matcher.find()) {
            return matcher.group();
        }
        else
            return null;
    }

    public static String getNumber(String text) {
        Matcher matcher = Pattern.compile("\\d+").matcher(text);
        if(matcher.find())
            return matcher.group();
        else
            return null;
    }

    public static String[] getInfo(String text) {
        int index = text.indexOf("每日签到");
        int[] att = null;
        String attend = null;
        String article = null;
        if(index != -1) {
            att = getFragment(text, index);
            if(att != null) {
                String frag = text.substring(att[0], att[1]);
                attend = getDateString(frag);
            }
        }

        index = text.indexOf("记录了航海经历");
        int[] dr = null;
        String diary = null;
        if(index != -1) {
            dr = getFragment(text, index);
            if(dr != null) {
                String frag = text.substring(dr[0], dr[1]);
                diary = getDateString(frag);
                String ref = getHref(frag);
                if(ref != null) {
                    article = getNumber(ref);
                }
            }
        }

        int[] frag = null;
        if(att != null) {
            if(dr != null) {
                frag = att[0] < dr[0] ? att : dr;
            }
            else
                frag = att;
        }
        else if(dr != null)
            frag = dr;

        String points = null;
        if(frag != null) {
            String sub = text.substring(frag[0], frag[1]);
            int start = sub.indexOf("balance");
            if(start > 0) {
                int end = sub.indexOf('\n', start);
                if(end > 0)
                    sub = sub.substring(start, end);
                else
                    sub = sub.substring(start);
                points = getNumber(sub);
            }
        }

        index = text.indexOf("avatarURLDom");
        String avatar = null;
        if(index != -1) {
            int end = text.indexOf('>', index);
            String sub = text.substring(index, end);
            final String tag = "url('";
            int start = sub.indexOf(tag);
            if(start != -1) {
                start = start + tag.length();
                end = sub.indexOf('\'', start);
                avatar = sub.substring(start, end);
            }
        }

        return new String[]{attend, diary, points, article, avatar};
    }

    public static String getCsrf(String text) {
        int index = text.indexOf("AddArticle.add");
        if(index == -1)
            return null;
        int end = text.indexOf('\n', index);
        String fun;
        if(end != -1)
            fun = text.substring(index, end);
        else
            fun = text.substring(index);

        end = fun.lastIndexOf('\'');
        if(end == -1)
            return null;
        String sub = fun.substring(0, end);
        int start = sub.lastIndexOf('\'');
        if(start == -1)
            return null;
        start++;
        return sub.substring(start);
    }

    public static String getContent(String text) {
        int index = text.indexOf("id=\"articleContent\"");
        if(index == -1)
            return null;

        int start = text.indexOf('>', index) + 1;
        int end = text.indexOf("<", start);
        return text.substring(start, end);
    }

    public static boolean isBeforeToday(Calendar c) {
        Calendar now = Calendar.getInstance();
        int cur = now.get(Calendar.YEAR);
        int val = c.get(Calendar.YEAR);
        if(cur > val)
            return true;

        cur = now.get(Calendar.DAY_OF_YEAR);
        val = c.get(Calendar.DAY_OF_YEAR);
        return cur > val;
    }
}
