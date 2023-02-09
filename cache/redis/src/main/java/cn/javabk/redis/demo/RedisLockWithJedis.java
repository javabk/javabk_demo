package cn.javabk.redis.demo;

import com.google.common.collect.Lists;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author LiSheng
 * @date 2023/2/8
 */
public class RedisLockWithJedis {

    Jedis jedis = null;

    public RedisLockWithJedis(String host, int port) {
        this.jedis =  new Jedis(host, port);
    }

    public Jedis getJedis() {
        return jedis;
    }
    public boolean expireKey(String key, int expiredSecond) {
        long expire = jedis.expire(key, expiredSecond);
        if (expire == 1) {
            return true;
        }
        return false;
    }

    private ThreadLocal<Map<String, Integer>> threadLocks = new ThreadLocal<>();

    private Map<String, Integer> getCurrentThreadLocks() {
        Map<String, Integer> refs = threadLocks.get();
        if (refs != null) {
            return refs;
        }
        threadLocks.set(new HashMap<>());
        return threadLocks.get();
    }

    public boolean lockWithSetNx(String key, String value) {
        //1. 通过 setnx 抢锁
        long result = jedis.setnx(key, value);
        //2. 结果判断
        if (result == 1) {//成功获取锁
            return true;
        } else {  //获取锁失败
            return false;
        }
    }

    public boolean lockWithSetExtend(String key, String value, int second) {
        String result = jedis.set(key, value, SetParams.setParams().ex(second).nx());
        if ("OK".equals(result)) {//成功获取锁
            return true;
        }
        return false;
    }

    public boolean lockWithLua(String key, String value, int expiredSecond) {

        //放到服务端执行的lua脚本
        String luaSrcipt = "if redis.call('setnx',KEYS[1],ARGV[1]) == 1 then\n" +
                                "redis.call('expire',KEYS[1],ARGV[2])\n" +
                            "return 1 \n" +
                            "else\n" +
                                    "return 0\n" +
                            "end";

        //1. 通过 setnx 抢锁
        List<String> placeHolderKeys = Lists.newArrayList(key);
        List<String> placeHolderValues = Lists.newArrayList(value, expiredSecond+"");
        Object result = jedis.eval(luaSrcipt, placeHolderKeys, placeHolderValues);//核心改动
        System.out.println("result is:" + result);
        //2. 结果判断
        if ("1".equals(result.toString())) {//成功获取锁（跟lua脚本的返回1对应）
            return true;
        } else {  //获取锁失败
           return false;
        }
    }


    public void unLockAfterCompare(String key, String value) {
        if (value != null && value.equals(jedis.get(key))) {//判断value是自身设置才删除
            jedis.del(key);
        }
    }

    public boolean unLockAfterCompareWithLua(String key, String value) {
        String luaSrcipt = "if redis.call('get',KEYS[1]) == ARGV[1] then\n" +
                "redis.call('del',KEYS[1])\n" +
                "return 1 \n" +
                "else\n" +
                "return 0\n" +
                "end";

        List<String> placeHolderKeys = Lists.newArrayList(key);
        List<String> placeHolderValues = Lists.newArrayList(value);
        Object result = jedis.eval(luaSrcipt, placeHolderKeys, placeHolderValues);//核心改动
        if (result != null  && "1".equals(result.toString())) {
            return true;
        }
        return false;
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
        boolean ok = this.lockWithSetExtend(key, value, expireSecond);
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
            unLockAfterCompare(key, value);
        }
        return true;
    }

}
