package org.cyy.redis;

import org.cyy.Plugin;

/**
 * @author cyy
 * @date 2022/2/19 13:41
 * @description 监听redis登录超时事件线程的runnable
 */
public class RedisTimerListenerRunnable implements Runnable{
    @Override
    public void run() {
        RedisTimerListener redisTimerListener = new RedisTimerListener();
        Plugin.contextMap.put("redisTimerListener",redisTimerListener);
        RedisPool.getJedis().subscribe(redisTimerListener, "__keyevent@0__:expired");
    }
}
