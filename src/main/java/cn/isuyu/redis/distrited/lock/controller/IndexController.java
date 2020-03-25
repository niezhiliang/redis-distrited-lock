package cn.isuyu.redis.distrited.lock.controller;

import org.redisson.api.RLock;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.redisson.codec.SerializationCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.Topic;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @Author NieZhiLiang
 * @Email nzlsgg@163.com
 * @GitHub https://github.com/niezhiliang
 * @Date 2020/3/24 下午6:57
 */
@RestController
public class IndexController {

    public final static String  MY_STOCK = "myStock";

    public final static Long STOCK_NUM = 100l;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 初始化库存的数量
     * @return
     */
    @GetMapping(value = "init")
    public String initStock() {
        redisTemplate.opsForValue().set(MY_STOCK,String.valueOf(STOCK_NUM));
        return "库存初始化成功";
    }

    /**
     * 并发扣库存
     * @return
     */
    @GetMapping(value = "reduck_stock")
    public String killProduct() throws InterruptedException {

        RLock rLock = redissonClient.getLock("myLock");
        rLock.lock(1000000, TimeUnit.SECONDS);
//        synchronized (this) {
            try {
                Integer stock = Integer.parseInt(redisTemplate.opsForValue().get(MY_STOCK));
                if (stock < 1) {
                    System.out.println("秒杀失败，商品已被全部秒杀,");
                } else {
                    stock = stock - 1;
                    redisTemplate.opsForValue().set(MY_STOCK,String.valueOf(stock));
                    System.out.println("剩余库存:"+stock);
                }
            } finally {
                rLock.unlock();
            }
//        }
        return "success";

    }

    /**
     * 订阅监听redis消息
     * @return
     */
    @GetMapping(value = "listen")
    public String listen() {
       RTopic rTopic = redissonClient.getTopic("myTopic",new SerializationCodec());
       rTopic.addListener(String.class,new MessageListener() {
           @Override
           public void onMessage(CharSequence channel, Object msg) {
               System.out.println("onMessage:"+channel+"; Thread: "+Thread.currentThread().toString());
               System.out.println(msg +"  "+ new Date());
           }
       });
       return "success";
    }

    /**
     * redis发布消息
     * @return
     */
    @GetMapping(value = "pub")
    public String publish() {
        RTopic rTopic = redissonClient.getTopic("myTopic",new SerializationCodec());
        rTopic.publish("hello");
        rTopic.publish("world");
        return "success";
    }

}
