package com.github.redisscheduler.impl;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

public class StubbedClock implements Clock {

    private static final DateFormat DATETIME_PARSER = new SimpleDateFormat("yyyyMMdd HH:mm");

    private Calendar now = new GregorianCalendar();

    @Override
    public Calendar now() {
        return now;
    }

    public void is(String dateTimeStr) throws ParseException {
        Calendar stubbedTime = Calendar.getInstance();
        stubbedTime.setTime(DATETIME_PARSER.parse(dateTimeStr));

        stubTime(stubbedTime);
    }

    public void fastForward(int period, TimeUnit unit) {
        stubTime(in(period, unit));
    }

    public Calendar in(int period, TimeUnit unit) {
        long currentTime = now.getTimeInMillis();
        long newTime = currentTime + (unit.toMillis(period));

        return fromMillis(newTime);
    }

    public static Calendar fromMillis(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        return cal;
    }

    private void stubTime(Calendar stubbedTime) {
        now = stubbedTime;
    }
}
