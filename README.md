redis-scheduler
===============

Distributed Scheduler using Redis (Java)

What is this?
-------------

redis-scheduler is a Java implementation of a distributed scheduler using Redis. It has the following features:

 - **Useable in a distributed environment**: Uses Redis transactions for effectively preventing a task to be run on
 multiple instances of the same application.
 - **Lightweight**: Uses a single thread.
 - **Configurable polling**: Polling delay can be configured to tweak execution precision (at the cost of performance)
 - **Multiple schedulers support**: You can create multiple schedulers in the same application if you need to.

High level concepts
-------------------

#### Scheduled Task

A scheduled task is a job that you need to execute in the future at a particular time.
In `redis-scheduler`, a task is represented solely by an arbitrary string identifier that has no particular meaning to the library.
It's your application that has to make sense of this identifier.

#### Scheduler

`RedisTaskScheduler`: This interface is where you submit your tasks for future execution.  Once submitted, a task will only be
executed at or after the trigger time you provide.

#### `TaskTriggerListener` interface

This is the main interface you must implement to actually run the tasks once they are due for execution. The library will
call the `taskTriggered` method for each task that is due for execution.


Maven dependency
----------------

``` xml
    <dependency>
        <groupId>com.github</groupId>
        <artifactId>redis-scheduler</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
```

Usage with Spring
-----------------

Note: the examples below assume you're using Spring's autowiring features.

First declare the base beans for Redis connectivity (if not already done in your project). This part can be different
for your project. `redis-scheduler` only needs a functional RedisTemplate instance to work correctly.

``` xml
    <bean id="jedisConnectionFactory" class="org.springframework.data.redis.connection.jedis.JedisConnectionFactory">
        <property name="hostName" value="localhost"/>
        <property name="port" value="6379"/>
    </bean>

    <bean id="redisTemplate" class="org.springframework.data.redis.core.RedisTemplate">
        <property name="connectionFactory" ref="jedisConnectionFactory"/>
        <property name="keySerializer">
            <bean class="org.springframework.data.redis.serializer.StringRedisSerializer"/>
        </property>
    </bean>
```

Then declare a scheduler:

``` xml
    <bean id="scheduler" class="com.github.redisscheduler.impl.RedisTaskSchedulerImpl" />
```

`RedisTaskSchedulerImpl` expects an implementation of the `TaskTriggerListener` interface to be available in your Spring
context. You must implement this interface and instantiate it in your own Spring context.

See the the test Spring context in `test/resources/application-context-test.xml` for an example of the setup.


Scheduling a task in the future
-------------------------------

``` java
    @Autowired
    private RedisTaskScheduler scheduler;

    ...

    scheduler.schedule("mytask", new GregorianCalendar(2013, 1, 1));
```

Be notified once a task is due for execution
--------------------------------------------

``` java
public class MyTaskTriggerListener implements TaskTriggerListener {
    public void taskTriggered(String taskId) {
        System.out.printf("Task %s is due for execution.", taskId);
    }
}
```

Then instantiate this class in your Spring context and it'll be used by the scheduler.

Customizing polling delay
----------------------------------

By default, polling delay is set to a few seconds (see implementation `RedisTaskSchedulerImpl` for actual value). If
you need your tasks to be triggered with more precision, decrease the polling delay using the `pollingDelayMillis` attribute of `RedisTaskSchedulerImpl`:

``` xml
    <bean id="scheduler" class="com.github.redisscheduler.impl.RedisTaskSchedulerImpl">
        <property name="pollingDelayMillis" value="500"/>
    </bean>
```

Increasing polling delay comes with a cost: higher load on Redis and your connection. Try to find the best balance for your needs.

Retry polling when a Redis connection error happens
---------------------------------------------------

Retries can be configured using the `maxRetriesOnConnectionFailure` property on `RedisTaskSchedulerImpl`:

``` xml
    <bean id="scheduler" class="com.github.redisscheduler.impl.RedisTaskSchedulerImpl">
        <property name="maxRetriesOnConnectionFailure" value="5"/>
    </bean>
```

After the specified number of retries, the polling thread will stop and log an error.
