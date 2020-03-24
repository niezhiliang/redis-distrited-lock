package cn.isuyu.redis.distrited.lock.controller;

import cn.isuyu.redis.distrited.lock.utils.RedisTools;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
    private RedisTools redisTools;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 初始化库存的数量
     * @return
     */
    @GetMapping(value = "init")
    public String initStock() {
        redisTools.initCount(MY_STOCK,STOCK_NUM);
        return "库存初始化成功";
    }

    /**
     * 并发扣库存
     * @return
     */
    @GetMapping(value = "reduck_stock")
    public String killProduct() throws InterruptedException {

        RLock rLock = redissonClient.getLock("myLock");

        rLock.lock();
        try {
            Thread.sleep(5000);
            Long stock = redisTools.decrementCount(MY_STOCK);
            System.out.println("剩余库存:"+stock);
        } finally {
            rLock.unlock();
        }
        return "success";

    }
}
