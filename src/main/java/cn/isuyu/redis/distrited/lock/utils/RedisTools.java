package cn.isuyu.redis.distrited.lock.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @Author NieZhiLiang
 * @Email nzlsgg@163.com
 * @GitHub https://github.com/niezhiliang
 * @Date 2020/3/11 下午2:24
 */
@Component
public class RedisTools<T> {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 永久存储数据
     * @param key
     * @param value
     */
    public void save(String key, String value) {
        stringRedisTemplate.opsForValue().set(key,value);
    }

    /**
     * 给定过期时间(分钟)存储数据
     * @param key
     * @param value
     * @param times
     */
    public void save(String key, String value, int times) {
        if (null != value) {
            stringRedisTemplate.opsForValue().set(key,value,times,TimeUnit.MINUTES);
        }
    }

    /**
     * 给定过期时间存储数据
     * @param key
     * @param value
     * @param times
     * @param timeUnit
     */
    public void save(String key, String value, int times,TimeUnit timeUnit) {
        if (null != value) {
            stringRedisTemplate.opsForValue().set(key,value,times,timeUnit);
        }
    }

    /**
     * 通过key获取值
     * @param key
     * @return
     */
    public String get(String key){
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 根据指定key删除
     * @param key
     */
    public boolean delete(String key) {
        return stringRedisTemplate.delete(key);
    }

    /**
     * 判断是否包含key
     * @param key
     * @return
     */
    public boolean hasKey(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    /**
     * 计数器自减
     * @param key
     * @return
     */
    public Long decrementCount(String key) {
        RedisAtomicLong entityIdCounter = new RedisAtomicLong(key, redisTemplate.getConnectionFactory());
        Long increment = entityIdCounter.getAndDecrement();
        return increment;
    }

    /**
     * 初始化计数器的值
     * @param key
     * @return
     */
    public Long initCount(String key, Long start) {
        RedisAtomicLong entityIdCounter = new RedisAtomicLong(key, redisTemplate.getConnectionFactory());
        entityIdCounter.set(start);
        Long increment = entityIdCounter.get();
        return increment;
    }
}
