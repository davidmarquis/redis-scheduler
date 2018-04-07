package com.github.davidmarquis.redisscheduler;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class StubbedClock extends Clock {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm");

    private Instant now = Instant.now();

    public Instant instant() {
        return now;
    }

    public void is(String dateTimeStr) {
        LocalDateTime time = LocalDateTime.parse(dateTimeStr, FORMATTER);
        stubTime(time.atZone(ZoneId.systemDefault()).toInstant());
    }

    public void fastForward(int period, TimeUnit unit) {
        stubTime(in(period, unit));
    }

    public Instant in(int period, TimeUnit unit) {
        long currentTime = now.toEpochMilli();
        long newTime = currentTime + (unit.toMillis(period));
        return Instant.ofEpochMilli(newTime);
    }

    private void stubTime(Instant stubbedTime) {
        now = stubbedTime;
    }

    public ZoneId getZone() {
        return null;  // not used
    }

    public Clock withZone(ZoneId zone) {
        return null;  // not used
    }
}
