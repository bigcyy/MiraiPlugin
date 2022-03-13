package org.cyy.redis;

import net.mamoe.mirai.contact.Friend;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.utils.MiraiLogger;
import org.cyy.Plugin;
import org.cyy.config.MyPluginConfig;
import org.cyy.config.ReplyMessage;
import org.cyy.service.LoginService;
import redis.clients.jedis.JedisPubSub;

import java.io.IOException;
import java.util.List;

/**
 * @author cyy
 * @date 2022/2/17 22:01
 * @description 监听redis中的key失效
 */
public class RedisTimerListener extends JedisPubSub {
    MiraiLogger logger = Plugin.MY_BOT.getLogger();

    /**
     * 监听器被加载时调用
     * @param pattern 监听方式
     * @param subscribedChannels 监听的频道
     */
    @Override
    public void onPSubscribe(String pattern, int subscribedChannels) {
        logger.info("监听器加载！"+pattern+"在"+subscribedChannels);
    }

    /**
     * 监听到健失效时调用
     * @param channel 监听频道
     * @param message 失效的key
     */
    @Override
    public void onMessage(String channel, String message) {
        logger.info("message");
        if(message.startsWith(MyPluginConfig.pushMsgTimerKeyPrefix)){
            String[] split = message.split(":");
            Group group = Plugin.MY_BOT.getGroup(Long.parseLong(split[1]));
            Friend friend = Plugin.MY_BOT.getFriend(Long.parseLong(split[1]));
            if(group == null && friend == null){
                return;
            }
            if(group != null)
                group.sendMessage(new At(Long.parseLong(split[2])).plus(ReplyMessage.pushMsgTimeoutMsg));
            else
                friend.sendMessage(ReplyMessage.pushMsgTimeoutMsg);
        }
        else if(message.startsWith(MyPluginConfig.loginTimerKeyPreFix)){
            //如果过期的是登录计时器key
            String key = MyPluginConfig.loginUserQueueKey;   //队列的key
            /*
                处理超时的队首
             */
            String queueFront = RedislUtil.queueFront(key); //过时将队首出队
            if(!message.equals(MyPluginConfig.loginTimerKeyPreFix+queueFront)){
                return;
            }
            String queuePop = RedislUtil.queuePop(key);
            List<Long> list = RedislUtil.getGroupIdAndSenderIdFromValue(queuePop);  //获取超时的群号和发送登录的登录者
            String exMsg = ReplyMessage.loginTimeOutMsg;
            Plugin.MY_BOT.getGroup(list.get(0)).sendMessage(new At(list.get(1)).plus(exMsg));    //从机器人全局对象中查询群制作信息发送

            /*
                对新队首进行处理
             */
            RedislUtil.dealNewFront(key);
        }

    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
        logger.info("监听器加载"+subscribedChannels);
    }

    @Override
    public void unsubscribe() {
        logger.info("取消");
    }
}
