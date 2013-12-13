package com.github.redisscheduler;

import com.github.redisscheduler.impl.RedisTaskSchedulerImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeast;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.class)
public class RedisTaskSchedulerTest {

    private static final int MAX_RETRIES = 3;

    @InjectMocks
    private RedisTaskSchedulerImpl scheduler = new RedisTaskSchedulerImpl();
    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Before
    public void setUp() {
        scheduler.setPollingDelayMillis(50);
        scheduler.setMaxRetriesOnConnectionFailure(MAX_RETRIES);
    }

    @After
    public void tearDown() {
        scheduler.destroy();
    }

    @Test
    public void can_retry_after_redis_connection_error() throws InterruptedException {

        // given
        doThrow(RedisConnectionFailureException.class).when(redisTemplate).execute(any(SessionCallback.class));

        // execute
        scheduler.initialize();
        Thread.sleep(500);

        // assert
        verify(redisTemplate, times(MAX_RETRIES)).execute(any(SessionCallback.class));
    }

    @Test
    public void can_recover_after_single_connection_error() throws InterruptedException {

        // given
        when(redisTemplate.execute(any(SessionCallback.class)))
                .thenThrow(RedisConnectionFailureException.class)
                .thenReturn(true);

        // execute
        scheduler.initialize();
        Thread.sleep(500);

        // assert
        verify(redisTemplate, atLeast(MAX_RETRIES + 1)).execute(any(SessionCallback.class));
    }
}
