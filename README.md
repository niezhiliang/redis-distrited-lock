```java
private void lock(long leaseTime, TimeUnit unit, boolean interruptibly) throws InterruptedException {
        //得到当前线程的id
        long threadId = Thread.currentThread().getId();
        //尝试获取锁 如果获取成功 返回null  如果获取失败，则获取当前持有锁的过期时间
        Long ttl = tryAcquire(leaseTime, unit, threadId);
        // lock acquired  获取锁成功  直接返回
        if (ttl == null) {
            return;
        }
        
        RFuture<RedissonLockEntry> future = subscribe(threadId);
        if (interruptibly) {
            commandExecutor.syncSubscriptionInterrupted(future);
        } else {
            commandExecutor.syncSubscription(future);
        }
        
        try {
            while (true) {
                ttl = tryAcquire(leaseTime, unit, threadId);
                // lock acquired
                if (ttl == null) {
                    break;
                }

                // waiting for message
                if (ttl >= 0) {
                    try {
                        future.getNow().getLatch().tryAcquire(ttl, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        if (interruptibly) {
                            throw e;
                        }
                        future.getNow().getLatch().tryAcquire(ttl, TimeUnit.MILLISECONDS);
                    }
                } else {
                    if (interruptibly) {
                        future.getNow().getLatch().acquire();
                    } else {
                        future.getNow().getLatch().acquireUninterruptibly();
                    }
                }
            }
        } finally {
            unsubscribe(future, threadId);
        }
//        get(lockAsync(leaseTime, unit));
    }
```

尝试获取锁
```java
    private <T> RFuture<Long> tryAcquireAsync(long leaseTime, TimeUnit unit, long threadId) {
        if (leaseTime != -1) {
            return tryLockInnerAsync(leaseTime, unit, threadId, RedisCommands.EVAL_LONG);
        }
        RFuture<Long> ttlRemainingFuture = tryLockInnerAsync(commandExecutor.getConnectionManager().getCfg().getLockWatchdogTimeout(), TimeUnit.MILLISECONDS, threadId, RedisCommands.EVAL_LONG);
        ttlRemainingFuture.onComplete((ttlRemaining, e) -> {
            if (e != null) {
                return;
            }

            // lock acquired
            if (ttlRemaining == null) {
                scheduleExpirationRenewal(threadId);
            }
        });
        return ttlRemainingFuture;
    }
```

```java
    /**
     * 尝试获取锁，支持重入锁 如果获取锁成功返回null
     * 获取失败则返会有效期
     * @return
     */
    <T> RFuture<T> tryLockInnerAsync(long leaseTime, TimeUnit unit, long threadId, RedisStrictCommand<T> command) {
        //获取设置锁的过期时间 默认是30s
        internalLockLeaseTime = unit.toMillis(leaseTime);
        //执行加锁脚本
        return commandExecutor.evalWriteAsync(getName(), LongCodec.INSTANCE, command,
                  "if (redis.call('exists', KEYS[1]) == 0) then " +
                      "redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
                      "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                      "return nil; " +
                  "end; " +
                  "if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then " +
                      "redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
                      "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                      "return nil; " +
                  "end; " +
                  "return redis.call('pttl', KEYS[1]);",
                    Collections.<Object>singletonList(getName()), internalLockLeaseTime, getLockName(threadId));
    }
```
这里执行勒一个`lua`脚本，下面我们一行一行分析这个lua脚本

- `if (redis.call('exists', KEYS[1]) == 0) then `：判断KEYS[1]这个key是否不存在`redis`中，KEYS[1]就是我们的锁的名称`myLock`
    - `redis.call('hincrby', KEYS[1], ARGV[2], 1)`：以`myLock`为key,存储hash格式的数据结构，解析为redis命令就是  `hset myLock fce75ff5-efe5-4b94-a094-64fc63708fdb:66 1`
    - `redis.call('pexpire', KEYS[1], ARGV[1])`：设置`myLock`的过期时间，过期时间为锁的过期时间 10s
    - `return nil; `：获取锁成功 返回null
    - `end; ` 结束该if判断

- `if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then `：判断KEYS[1]这个key是否已存在`redis`中，KEYS[1]就是我们的锁的名称`myLock`
    - `redis.call('hincrby', KEYS[1], ARGV[2], 1); `：对`redis`中，key为`myLock`的值进行+1，用来标识锁的重入次数
    - `redis.call('pexpire', KEYS[1], ARGV[1]); `：设置`myLock`的有效期
    - `return nil; `：获取重入锁成功 返回null
    - `end; ` 结束该if判断   

- `return redis.call('pttl', KEYS[1]);`：返回持有锁的过期时间（获取锁失败）