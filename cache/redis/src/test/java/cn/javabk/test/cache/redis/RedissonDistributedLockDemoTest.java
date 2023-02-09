package cn.javabk.test.cache.redis;

import cn.javabk.redis.demo.RedisLockWithRedisson;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author LiSheng
 * @date 2023/2/7
 */
public class RedissonDistributedLockDemoTest {

    RedisLockWithRedisson lockWithRedisson = new RedisLockWithRedisson("127.0.0.1", 6379);

    ThreadPoolExecutor executor = null;

    @Before
    public void init() {
        executor = new ThreadPoolExecutor(10, 10, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10));
    }


    @Test
    public void testDemon() throws Exception {
//        lockWithRedisson.lockAndAutoUnLockDemon("javabk.cn", 10);
        for (int i = 0; i < 3; i++) {
            Runnable runnable = () -> {
                try {
                    lockWithRedisson.lockAndManualUnLockDemon("javabk.cn", 10);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
            executor.submit(runnable);
        }
        Thread.currentThread().join();
    }

    @Test
    public void testLockWithWhatDogAndWait() throws Exception {
        String key = "javabk.cn";
        for (int i = 0; i < 2; i++) {
            Runnable runnable = () -> {
                try {
                    lockWithRedisson.lockWithWatchDog(key, 10);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
            executor.submit(runnable);
        }
        Thread.currentThread().join();
    }


}
