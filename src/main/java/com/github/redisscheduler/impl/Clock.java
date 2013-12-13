package com.github.redisscheduler.impl;

import java.util.Calendar;

interface Clock {

    Calendar now();
}
