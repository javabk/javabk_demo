package cn.javabk.redis.demo;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.TimeUnit;

/**
 * @author LiSheng
 * @date 2023/2/9
 */
public class RedisLockWithRedisson {

    RedissonClient redissonClient = null;

    public RedisLockWithRedisson(String host, int port) {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + host + ":" + port);
        config.setLockWatchdogTimeout(10 * 1000);//10s. 覆盖 watch log 默认30s 超时的配置
        redissonClient = Redisson.create(config);
    }


    public void apiDemonForLook() throws InterruptedException {
        RLock lock = redissonClient.getLock("javabk.cn");
        // 拿锁失败时会不停的重试
        // 具有Watch Dog 自动延期机制 默认续30s 每隔30/3=10 秒续到30s
        lock.lock();

        // 尝试拿锁20s后停止重试,返回false
        // 具有Watch Dog 自动延期机制 默认续30s
        boolean res1 = lock.tryLock(20, TimeUnit.SECONDS);

        // 拿锁失败时会不停的重试
        // 没有Watch Dog ，20s后自动释放
        lock.lock(20, TimeUnit.SECONDS);

        // 尝试拿锁20s后停止重试,返回false
        // 没有Watch Dog ，10s后自动释放
        boolean res2 = lock.tryLock(20, 10, TimeUnit.SECONDS);
    }


    public boolean lockWithWatchDog(String key,  int waitSecond) throws Exception {

        RLock lock = redissonClient.getLock(key);
        //尝试加锁，第一个参数表示最多等待多少秒。启动 watch log 续期
        boolean locked = lock.tryLock(waitSecond, TimeUnit.SECONDS);
        if (locked) {
            System.out.println("成功获取锁.key:" + key);
            System.out.println("业务处理...");
            //业务处理
            for (int i = 0; i < 10; i++) {
                System.out.println("业务处理中，剩余时间：" + lock.remainTimeToLive());
                Thread.sleep(1500);
            }
            System.out.println("处理业务完成后，锁是否存在:" + lock.isLocked());
            lock.unlock();
            System.out.println("解锁后，锁是否存在:" + lock.isLocked());
            return true;
        } else {
            System.out.println("获取锁失败在等待: " + waitSecond+ "秒,key:" + key);
            return false;
        }
    }


    public void lockAndManualUnLockDemon(String key, int expiredSecond) throws Exception {

        RLock lock = redissonClient.getLock(key);
        //尝试加锁，第一个参数表示最多等待多少秒，上第二个参数表示多少秒自动解锁
        boolean locked = lock.tryLock(1L, expiredSecond, TimeUnit.SECONDS);
        if (locked) {
            System.out.println("成功获取锁.key:" + key);
            System.out.println("业务处理...");
            //业务处理
            Thread.sleep(5 * 1000);

            System.out.println("处理业务完成后，锁是否存在:" + lock.isLocked());
            lock.unlock();
            System.out.println("手工解锁后，锁是否存在:" + lock.isLocked());
        } else {
            System.out.println("获取锁失败,key:" + key);
        }
    }

}
