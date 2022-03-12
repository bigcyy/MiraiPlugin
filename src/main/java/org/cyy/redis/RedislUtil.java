package org.cyy.redis;

import com.google.gson.Gson;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.message.data.At;
import org.cyy.Plugin;
import org.cyy.config.MyPluginConfig;
import org.cyy.config.ReplyMessage;
import org.cyy.exception.BaseCookieIsErrorException;
import org.cyy.service.LoginService;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RedislUtil {


    /**
     * 设置key的有效期，单位是秒
     *
     * @param key
     * @param exTime
     * @return
     */
    public static Long expire(String key, int exTime) {
        Jedis jedis = null;
        Long result = null;
        try {
            //从Redis连接池中获得Jedis对象
            jedis = RedisPool.getJedis();
            //设置成功则返回Jedis对象
            result = jedis.expire(key, exTime);
        } catch (Exception e) {
            //Plugin.MY_BOT.getLogger().error(String.format("expire key:? error", key),e);
            e.printStackTrace();
            return result;
        }finally {
            RedisPool.closeRedis(jedis);
        }
        return result;
    }

    //exTime的单位是秒
    //设置key-value并且设置超时时间
    public static String setEx(String key, String value, int exTime) {
        Jedis jedis = null;
        String result;
        try {
            jedis = RedisPool.getJedis();
            result = jedis.setex(key, exTime, value);
        } catch (Exception e) {
            //Plugin.MY_BOT.getLogger().error("setex key:{} value:{} error",key,value,e);
            e.printStackTrace();
            return null;
        }finally {
            RedisPool.closeRedis(jedis);
        }

        return result;
    }

    public static String set(String key, String value) {
        Jedis jedis = null;
        String result;

        try {
            jedis = RedisPool.getJedis();
            result = jedis.set(key, value);
        } catch (Exception e) {
            //log.error("set key:{} value:{} error",key,value,e);
            e.printStackTrace();
            return null;
        }finally {
            RedisPool.closeRedis(jedis);
        }
        return result;
    }
    public static long queuePush (String key, String value) {
        Jedis jedis = null;
        long result = -1;

        try {
            jedis = RedisPool.getJedis();
            result = jedis.rpush(key, value);
        } catch (Exception e) {
            e.printStackTrace();
            return result;
        }finally {
            RedisPool.closeRedis(jedis);
        }
        return result;
    }

    public static String queuePop (String key) {
        Jedis jedis = null;
        String result;

        try {
            jedis = RedisPool.getJedis();
            result = jedis.lpop(key);
        } catch (Exception e) {
            //log.error("set key:{} value:{} error",key,value,e);
            e.printStackTrace();
            return null;
        }finally {
            RedisPool.closeRedis(jedis);
        }
        return result;
    }

    public static String queueFront (String key) {
        Jedis jedis = null;
        String result = null;
        try {
            jedis = RedisPool.getJedis();
            List<String> range = jedis.lrange(key, 0, 0);
            if(range.size() == 1) {
                result = range.get(0);
            }
        } catch (Exception e) {
            //log.error("set key:{} value:{} error",key,value,e);
            e.printStackTrace();
            return null;
        }finally {
            RedisPool.closeRedis(jedis);
        }
        return result;
    }

    public static long queueSize (String key) {
        Jedis jedis = null;
        long result;
        try {
            jedis = RedisPool.getJedis();
            result = jedis.llen(key);
        } catch (Exception e) {
            //log.error("set key:{} value:{} error",key,value,e);
            e.printStackTrace();
            return -1;
        }finally {
            RedisPool.closeRedis(jedis);
        }
        return result;
    }

    public static String get(String key) {
        Jedis jedis = null;
        String result = null;
        try {
            jedis = RedisPool.getJedis();
            result = jedis.get(key);
        } catch (Exception e) {
            //log.error("get key:{} error",key,e);
            e.printStackTrace();
            return result;
        }finally {
            RedisPool.closeRedis(jedis);
        }
        return result;
    }

    public static Long del(String key) {
        Jedis jedis = null;
        Long result = null;
        try {
            jedis = RedisPool.getJedis();
            result = jedis.del(key);
        } catch (Exception e) {
            //log.error("del key:{} error",key,e);
            return result;
        }finally {
            RedisPool.closeRedis(jedis);
        }
        return result;
    }

    public static void setJsonObj(String key,Object obj){
        Gson gson = new Gson();
        String json = gson.toJson(obj);
        set(key,json);
    }
    public static Object getJsonObj(String key,Class clazz){
        Gson gson = new Gson();
        String json = get(key);
        return gson.fromJson(json,clazz);
    }
    public static String makeQueueLoginValue(long groupId,long userId){
        return String.format(MyPluginConfig.loginUserValue,groupId,userId);
    }
    public static List<Long> getGroupIdAndSenderIdFromValue(String value){
        return Arrays.stream(value.split(":")).map(Long::parseLong).collect(Collectors.toList());
    }
    public static long getGroupIdFromValue(String value){
        return RedislUtil.getGroupIdAndSenderIdFromValue(value).get(0);
    }
    public static long getSenderIdFromValue(String value){
        return RedislUtil.getGroupIdAndSenderIdFromValue(value).get(1);
    }

    /**
     * 处理新的队首
     * @param key
     */
    public static void dealNewFront(String key){
        if(RedislUtil.queueSize(key) == 0){
            //判断队中是否还要元素，没有则退出
            return;
        }
        /*
            对新队首进行处理
         */
        String newFront = RedislUtil.queueFront(key);    //获取新队首
        assert newFront != null;
        List<Long> list = RedislUtil.getGroupIdAndSenderIdFromValue(newFront);
        Group newFrontGroup = Plugin.MY_BOT.getGroup(list.get(0));
        newFrontGroup.sendMessage(new At(list.get(1)).plus(ReplyMessage.isFrontMsg)); //提醒新队首进行登录
        try {
            new LoginService().beginLogin(newFrontGroup); //开始登录
            String loginTimerKey = MyPluginConfig.loginTimerKeyPreFix+newFront;   //设置登录计时器key
            RedislUtil.setEx(loginTimerKey,MyPluginConfig.loginTimerValue,MyPluginConfig.loginExTime); //设置登录计时器
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BaseCookieIsErrorException e) {
            e.printStackTrace();
            newFrontGroup.sendMessage(ReplyMessage.baseCookieIsErrorMsg);
        }

    }
}