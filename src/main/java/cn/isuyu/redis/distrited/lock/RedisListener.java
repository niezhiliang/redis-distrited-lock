package cn.isuyu.redis.distrited.lock;

import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.redisson.codec.SerializationCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @Author NieZhiLiang
 * @Email nzlsgg@163.com
 * @GitHub https://github.com/niezhiliang
 * @Date 2020/3/25 下午7:46
 */
@Component
public class RedisListener implements CommandLineRunner {

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 订阅监听redis消息
     * @return
     */
    @Override
    public void run(String... args) throws Exception {
        RTopic rTopic = redissonClient.getTopic("myTopic",new SerializationCodec());
        rTopic.addListener(String.class,new MessageListener<String>() {
            @Override
            public void onMessage(CharSequence channel, String msg) {
                System.out.println("onMessage:"+channel+"; Thread: "+Thread.currentThread().toString());
                System.out.println(msg +"  "+ new Date());
            }
        });
    }
}
