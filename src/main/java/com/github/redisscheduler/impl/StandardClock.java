package com.github.redisscheduler.impl;

import java.util.Calendar;

class StandardClock implements Clock {

    @Override
    public Calendar now() {
        return Calendar.getInstance();
    }
}
