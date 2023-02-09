package cn.javabk.test.cache.redis;

import cn.javabk.redis.demo.RedisLockWithJedis;
import org.junit.Test;

import java.util.UUID;

/**
 * @author LiSheng
 * @date 2023/2/2
 */
public class DistributedLockDemoTest {

    RedisLockWithJedis jedisLock = new RedisLockWithJedis("127.0.0.1", 6379);

    @Test
    public void testReentrantLock() {
        String value = UUID.randomUUID().toString();
        String key = "javabk.cn";
        for (int i = 1; i < 3; i++) {
            jedisLock.lockReentrant(key, value, 30);
        }
        for (int i = 1; i < 3; i++) {
            jedisLock.unLockReentrant(key, value);
        }
    }


    @Test
    public void setnxAndExpire() {
        String myId = UUID.randomUUID().toString();
        String key = "javabk.cn";
        //1. 加锁
        boolean success = jedisLock.lockWithSetNx(key, myId);
        //2. 结果判断
        if (success) {
            try {
                //3. 设置过期，避免死锁
                jedisLock.expireKey(key, 30);//30 seconds expired
                //4. 业务处理....
            } finally {
                //5. 释放锁
                jedisLock.unLockAfterCompare(key, myId);
            }

        } else {//获取锁失败
            //...
        }
    }

    @Test
    public void setExtendCommand() {
        String myId = UUID.randomUUID().toString();
        String key = "javabk.cn";
        // 加锁
        boolean success = jedisLock.lockWithSetExtend(key, myId, 30);
        // 结果判断
        if (success) {
            try {
                //业务处理....
            } finally {
                // 释放锁
                jedisLock.unLockAfterCompare(key, myId);
            }

        } else {//获取锁失败
            //...
        }
    }


    @Test
    public void lockWithLua() {
        String myId = UUID.randomUUID().toString();
        String key = "javabk.cn";
        jedisLock.lockWithLua(key, myId, 30);
    }

    @Test
    public void testLuaLockAndUnLock() throws InterruptedException {
        String value = UUID.randomUUID().toString();
        String key = "javabk.cn";
        int expiredSecond = 30;
        boolean locked = jedisLock.lockWithLua(key, value, expiredSecond);
        System.out.println("加锁结果:" + locked);
        Thread.sleep(1000);
        System.out.println("业务处理.....");
        boolean result = jedisLock.unLockAfterCompareWithLua(key, value);
        System.out.println("解锁结果:" + result);
    }

}
