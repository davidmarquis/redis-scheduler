package com.github.davidmarquis.redisscheduler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.ConnectException;
import java.util.function.Function;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.atLeast;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class TaskSchedulerTest {

    private static final int MAX_RETRIES = 3;

    @Mock
    private RedisDriver driver;

    private RedisTaskScheduler scheduler;


    @Before
    public void setUp() {
        scheduler = new RedisTaskScheduler(driver, taskId -> {});
        scheduler.setPollingDelayMillis(50);
        scheduler.setMaxRetriesOnConnectionFailure(MAX_RETRIES);
    }

    @After
    public void tearDown() {
        scheduler.close();
    }

    @Test
    public void canRetryAfterRedisConnectionError() throws InterruptedException {
        doThrow(RedisConnectException.class).when(driver).fetch(any(Function.class));

        scheduler.initialize();
        Thread.sleep(500);

        verify(driver, times(MAX_RETRIES)).fetch(any(Function.class));
    }

    @Test
    public void canRecoverAfterSingleConnectionError() throws InterruptedException {
        when(driver.fetch(any(Function.class)))
                .thenThrow(RedisConnectException.class)
                .thenReturn(true);

        scheduler.initialize();
        Thread.sleep(500);

        verify(driver, atLeast(MAX_RETRIES + 1)).fetch( any(Function.class));
    }
}
