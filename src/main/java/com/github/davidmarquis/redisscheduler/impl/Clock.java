package com.github.davidmarquis.redisscheduler.impl;

import java.util.Calendar;

interface Clock {

    Calendar now();
}
