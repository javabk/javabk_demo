package cn.javabk.test.cache.redis;

import com.google.common.collect.Lists;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.util.*;

/**
 * @author LiSheng
 * @date 2023/2/2
 */
public class DistributedLockDemoTest {

    Jedis jedis = new Jedis("127.0.0.1", 6379);

    private ThreadLocal<Map<String, Integer>> threadLocks = new ThreadLocal<>();

    private Map<String, Integer> getCurrentThreadLocks() {
        Map<String, Integer> refs = threadLocks.get();
        if (refs != null) {
            return refs;
        }
        threadLocks.set(new HashMap<>());
        return threadLocks.get();
    }

    private boolean lock(String key, String value, int second) {
        String result = jedis.set(key, value, SetParams.setParams().ex(second).nx());
        if ("OK".equals(result)) {//成功获取锁
            return true;
        }
        return false;
    }

    private void unLock(String key, String value) {
        //5. 释放锁
        if (value != null && value.equals(jedis.get(key))) {//判断value是自身设置才删除
            jedis.del(key);
        }
    }

    //锁-可重入性
    public boolean lockReentrant(String key, String value, int expireSecond) {
        Map<String, Integer> currentThreadLocks = getCurrentThreadLocks();
        Integer thisLockCount = currentThreadLocks.get(key);
        if (thisLockCount != null) {//如果本地有锁，说明已经在redis上加过锁，本地内存进行累计即可
            currentThreadLocks.put(key, thisLockCount + 1);
            System.out.println("unLockReentrant local cache lock");
            return true;
        }
        boolean ok = this.lock(key, value, expireSecond);
        if (!ok) {
            return false;
        }
        System.out.println("unLockReentrant local redis real lock");
        currentThreadLocks.put(key, 1);
        return true;
    }


    //解锁-可重入性
    public boolean unLockReentrant(String key, String value) {
        Map<String, Integer> currentThreadLocks = getCurrentThreadLocks();
        Integer thisLockCount = currentThreadLocks.get(key);
        if (thisLockCount == null) {
            return true;
        }
        thisLockCount = thisLockCount - 1;
        if (thisLockCount > 0) {
            System.out.println("unLockReentrant local cache del");
            currentThreadLocks.put(key, thisLockCount);
        } else {//如果没有锁，需要对redis的锁进行删除
            currentThreadLocks.remove(key);
            System.out.println("unLockReentrant real del");
            unLock(key, value);
        }
        return true;
    }

    @Test
    public void testReentrantLock() {
        String value = UUID.randomUUID().toString();
        String key = "javabk.cn";
        for (int i = 1; i < 3; i++) {
            lockReentrant(key, value, 30);
        }
        for (int i = 1; i < 3; i++) {
            unLockReentrant(key, value);
        }
    }


    @Test
    public void setnxAndExpire() {
        String myId = UUID.randomUUID().toString();
        String key = "javabk.cn";
        //1. 通过 setnx 抢锁
        long result = jedis.setnx(key, myId);
        //2. 结果判断
        if (result == 1) {//成功获取锁
            try {
                //3. 设置过期，避免死锁
                jedis.expire(key, 30);//30 seconds expired
                //4. 业务处理....
            } finally {
                //5. 释放锁
                if (myId.equals(jedis.get(key))) {//判断value是自身设置才删除
                    jedis.del(key);
                }
            }
        } else {  //获取锁失败
            // ....
        }
    }

    @Test
    public void setExtendCommand() {
        String myId = UUID.randomUUID().toString();
        String key = "javabk.cn";
        //1. 通过 setnx 抢锁
        String result = jedis.set(key, myId, SetParams.setParams().ex(30).nx());
        System.out.println("result is:" + result);
        //2. 结果判断
        if ("OK".equals(result)) {//成功获取锁
            try {
                //3. 业务处理....
            } finally {
                //4. 释放锁
                if (myId.equals(jedis.get(key))) {//判断value是自身设置才删除
                    jedis.del(key);
                }
            }
        } else {  //获取锁失败
            // ....
        }
    }


    @Test
    public void setNxAndExpireWithLua() {
        String myId = UUID.randomUUID().toString();
        String key = "javabk.cn";
        //放到服务端执行的lua脚本
        String luaSrcipt = "if redis.call('setnx',KEYS[1],ARGV[1]) == 1 then\n" +
                "redis.call('expire',KEYS[1],ARGV[2])\n" +
                "return 1 \n" +
                "else\n" +
                "return 0\n" +
                "end";

        //1. 通过 setnx 抢锁
        List<String> placeHolderKeys = Lists.newArrayList(key);
        List<String> placeHolderValues = Lists.newArrayList(myId, "30");
        Object result = jedis.eval(luaSrcipt, placeHolderKeys, placeHolderValues);//核心改动
        System.out.println("result is:" + result);
        //2. 结果判断
        if ("1".equals(result)) {//成功获取锁（跟lua脚本的返回1对应）
            try {
                //3. 业务处理....
            } finally {
                //4. 释放锁
                if (myId.equals(jedis.get(key))) {//判断value是自身设置才删除
                    jedis.del(key);
                }
            }
        } else {  //获取锁失败
            // ....
        }
    }
}
