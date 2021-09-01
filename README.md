redis-scheduler
===============

Distributed Scheduler using Redis for Java applications.
[fork redis-scheduler 3.0.0 by David Marquis](https://github.com/davidmarquis/redis-scheduler)

What is this?
-------------

`redis-scheduler` is a Java implementation of a distributed scheduler using Redis. It has the following features:

 - **Useable in a distributed environment**: Uses Redis transactions for effectively preventing a task to be run on
 multiple instances of the same application.
 - **Lightweight**: Uses a single thread.
 - **Configurable polling**: Polling delay can be configured to tweak execution precision (at the cost of performance)
 - **Multiple schedulers support**: You can create multiple schedulers in the same logical application if you need to.
 - **Support for multiple client libraries**: Drivers exist for [Jedis](https://github.com/xetorthio/jedis), [Lettuce](https://lettuce.io/) and [Spring Data's RedisTemplate](https://projects.spring.io/spring-data-redis/)

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


Building the project
--------------------

``` bash
mvn package
```

Maven dependency
----------------

This artifact is published on Maven Central:

``` xml
<dependency>
    <groupId>com.github.davidmarquis</groupId>
    <artifactId>redis-scheduler</artifactId>
    <version>3.0.0</version>
</dependency>
```

You'll need to add one of the specific dependencies to use the different available drivers:

To use with Lettuce:

``` xml
<dependency>
    <groupId>io.lettuce</groupId>
    <artifactId>lettuce-core</artifactId>
    <version>5.0.3.RELEASE</version>
</dependency>
```

To use with Jedis:

``` xml
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>2.9.0</version>
</dependency>
```

To use with Spring Data Redis:

``` xml
<dependency>
    <groupId>org.springframework.data</groupId>
    <artifactId>spring-data-redis</artifactId>
    <version>1.8.11.RELEASE</version>
</dependency>
```


Usage with Lettuce
------------------

The scheduler must be instantiated with the `LettuceDriver`:

``` java
RedisClient client = RedisClient.create(RedisURI.create("localhost", 6379));
RedisTaskScheduler scheduler = new RedisTaskScheduler(new LettuceDriver(client), new YourTaskTriggerListener());

scheduler.start();
```

Usage with Jedis
----------------

The scheduler must be instantiated with the `JedisDriver`:

``` java
JedisPool pool = new JedisPool("localhost", 6379);
RedisTaskScheduler scheduler = new RedisTaskScheduler(new JedisDriver(pool), new YourTaskTriggerListener());

scheduler.start();
```

Usage with Spring
-----------------

First declare the base beans for Redis connectivity (if not already done in your project). This part can be different
for your project.

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

<bean id="springTemplateDriver" class="com.github.davidmarquis.redisscheduler.drivers.spring.RedisTemplateDriver">
    <constructor-arg name="redisTemplate" ref="redisTemplate"/>
</bean>
```

Finally, declare the scheduler instance:

``` xml
<bean id="scheduler" class="com.github.davidmarquis.redisscheduler.RedisTaskScheduler">
    <constructor-arg name="driver" ref="springTemplateDriver"/>
    <constructor-arg name="listener">
        <bean class="your.own.implementation.of.TaskTriggerListener"/>
    </constructor-arg>
</bean>
```

As noted above, `RedisTaskScheduler` expects an implementation of the `TaskTriggerListener` interface which it will notify when a task is due for execution. You must implement this interface yourself and provide it to the scheduler as a constructor argument.

See the the test Spring context in `test/resources/application-context-test.xml` for a complete working example of the setup.


Scheduling a task in the future
-------------------------------

``` java
scheduler.schedule("mytask", new GregorianCalendar(2015, Calendar.JANUARY, 1, 4, 45, 0));
```

This would schedule a task with ID "mytask" to be run at 4:45AM on January 1st 2015.

Be notified once a task is due for execution
--------------------------------------------

``` java
public class MyTaskTriggerListener implements TaskTriggerListener {
    public void taskTriggered(String taskId) {
        System.out.printf("Task %s is due for execution.", taskId);
    }
}
```

Customizing polling delay
----------------------------------

By default, polling delay is set to a few seconds (see implementation `RedisTaskScheduler` for actual value). If
you need your tasks to be triggered with more precision, decrease the polling delay using the `pollingDelayMillis` attribute of `RedisTaskScheduler`:

In Java:

``` java
scheduler.setPollingDelayMillis(500);
```

With Spring:

``` xml
<bean id="scheduler" class="com.github.davidmarquis.redisscheduler.RedisTaskScheduler">
    <property name="pollingDelayMillis" value="500"/>
</bean>
```

Increasing polling delay comes with a cost: higher load on Redis and your connection.
Try to find the best balance for your needs.

Retry polling when a Redis connection error happens
---------------------------------------------------

Retries can be configured using the `maxRetriesOnConnectionFailure` property on `RedisTaskScheduler`:

In Java:

``` java
scheduler.setMaxRetriesOnConnectionFailure(5);
```

With Spring:

``` xml
<bean id="scheduler" class="com.github.davidmarquis.redisscheduler.RedisTaskScheduler">
    <property name="maxRetriesOnConnectionFailure" value="5"/>
</bean>
```

After the specified number of retries, the polling thread will stop and log an error.
