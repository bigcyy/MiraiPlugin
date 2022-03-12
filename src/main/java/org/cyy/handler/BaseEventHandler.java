package org.cyy.handler;

import kotlin.coroutines.CoroutineContext;
import net.mamoe.mirai.contact.*;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.*;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.AtAll;
import net.mamoe.mirai.message.data.MessageChain;
import org.cyy.Plugin;
import org.cyy.config.MyPluginConfig;
import org.cyy.config.ReplyMessage;
import org.cyy.handler.second.SecondEventHandler;
import org.cyy.job.AutoSignRunnable;
import org.cyy.redis.RedisTimerListenerRunnable;
import org.cyy.redis.RedislUtil;
import org.jetbrains.annotations.NotNull;
import org.quartz.SchedulerException;

import java.io.IOException;
import java.util.Objects;


/**
 * @author cyy
 * @date 2022/2/16 12:18
 * @description 事件分发处理器，对事件进行统一监听，监听到对应事件后分发处理
 * 例如:
 *  mirai定义的事件是一级事件，能直接进行监听，需要对信息进行处理的事件被称为二级事件
 *  机器人对监听到一级事件后，将(如果有二级事件)信息进行处理,分发给二级事件，并处理二级事件
 */
public class BaseEventHandler extends SimpleListenerHost {
    private final SecondEventHandler secondEventHandler = new SecondEventHandler();

    /**
     * 未处理的异常，会统一在此处理，将异常信息发送到机器主人手中(需要配置文件配置个人qq号)
     * @param context mirai上下文
     * @param exception 异常
     */
    @Override
    public void handleException(@NotNull CoroutineContext context, @NotNull Throwable exception) {

        Throwable cause = exception.getCause().getCause();  //得到真实异常
        cause.printStackTrace();    //控制台打印异常
        Long masterId = MyPluginConfig.master; //从插件上下文中获取master的账号
        if(masterId == null){
            return;
        }
        Friend master = Plugin.MY_BOT.getFriend(masterId);  //从机器人好友中获取到master
        Objects.requireNonNull(master).sendMessage(cause+":"+cause.getMessage());   //发送异常信息
    }

    /**
     * 一级事件：接收到群信息时触发
     *      其中的二级事件：
     *          催签到事件：群里发送：”催签到“
     *          更新事件：群里发送：“更新”
     *          登录事件： 群里发送：@bot+”登录“
     *          处理验证码事件：群里发送：“验证码:xxxx”
     *          添加子群：群里发送：“添加子群:xxxxx”
     *          删除子群：群里发送：“删除子群：xxxxx”
     *          查看子群：群里发送：“查看子群”
     *          查看食用方法事件: 群里发送：”使用方法“
     *
     *       按照功能来分(多个事件组成一个功能)：
     *          催签到功能：催签到事件和更新事件组合成催签到功能(目前未使用更新事件)
     *          登录功能：登录事件和处理验证码事件组合成登录功能
     *          信息推送功能：增加查看删除子群命令和开始推送命令结束推送命令以及at全体成员信息命令组成信息推送功能
     * @param groupMessageEvent 群信息事件对象
     */
    @EventHandler
    public void onGroupMessageEvent(GroupMessageEvent groupMessageEvent) throws IOException {
        Member sender = groupMessageEvent.getSender();      //获取信息发送者
        MessageChain message = groupMessageEvent.getMessage();  //获取信息链
        String content = message.contentToString(); //将信息转为纯内容
        Group group = groupMessageEvent.getGroup(); //获取触发一级事件的群

        /*
            催签到功能
         */
        if(MyPluginConfig.isSign && sender.getId() != Plugin.MY_BOT.getId()){
            if(content.equals("催签到")){
                //催签到指令
                secondEventHandler.sign(group);
            }
        }

        /*
            登录功能
         */
        if(MyPluginConfig.isLogin && sender.getId() != Plugin.MY_BOT.getId()){
            if(message.contains(new At(Plugin.MY_BOT.getId())) && content.contains("登录")){
                //登录指令
                secondEventHandler.beginLogin(group,groupMessageEvent.getSender());
            }else if(content.startsWith("验证码")){
                //回复验证码指令
                secondEventHandler.receiveCode(group,groupMessageEvent.getSender(),content);
            }
        }

        if(content.equals("使用方法")&& sender.getId() != Plugin.MY_BOT.getId()){
            secondEventHandler.useMethod(group);
        }

        /*
            信息推送功能
         */
        if(MyPluginConfig.isPushMsg && sender.getId() != Plugin.MY_BOT.getId() && sender.getPermission().getLevel() == 1){

            long groupId = group.getId();
            long senderId = sender.getId();
            String key = String.format(MyPluginConfig.pushMsgTimerKey,groupId,senderId);   //推送信息计时器
            if(RedislUtil.get(key) != null && !content.equals("结束推送")){
                if(content.equals("开始推送")){
                    group.sendMessage(ReplyMessage.rePushMsg);
                    return;
                }
                secondEventHandler.pushSomeMsg(group, message);
                return;
            }
            //820315251
            if(content.startsWith("添加子群:")){
                secondEventHandler.addPushGroup(group,sender,content);
            }else if(content.startsWith("删除子群:")){
                secondEventHandler.deletePushGroup(group,sender,content);
            }else if(content.startsWith("查看子群")){
                secondEventHandler.getPushGroup(group,sender);
            }else if(content.equals("开始推送")){
                secondEventHandler.beginPushSomeMsg(group,sender);
            }else if(message.contains(AtAll.INSTANCE)){
                secondEventHandler.pushAtAllMsg(group,sender,content);
            }else if(content.equals("结束推送")){
                secondEventHandler.endPushSomMsg(group,sender);
            }
        }

    }

    /**
     * 一级事件：接收到好友信息时触发
     *      其中的二级事件
     *          在聊天窗口执行命令
     *          动态开启功能
     * @param friendMessageEvent 好友信息事件对象
     */
    @EventHandler
    public void onFriendMessageEvent(FriendMessageEvent friendMessageEvent) throws SchedulerException {
        String msg = friendMessageEvent.getMessage().contentToString();
        //仅bot主人可以执行命令
        if(friendMessageEvent.getSender().getId() == MyPluginConfig.master && msg.startsWith("/")){
            secondEventHandler.executeCmd(friendMessageEvent);
        }else if(friendMessageEvent.getSender().getId() == MyPluginConfig.master && msg.startsWith("开启")){
            secondEventHandler.openFunction(friendMessageEvent.getSender(),msg);
        }
        else if(friendMessageEvent.getSender().getId() == MyPluginConfig.master && msg.startsWith("关闭")){
            secondEventHandler.closeFunction(friendMessageEvent.getSender(),msg);
        }

    }
    /**
     * 一级事件：机器人登录后触发，用于开启自动催签到，记录当前登录的bot
     * @param botOnlineEvent 机器人登录事件对象
     */
    @EventHandler
    public void onBotLogin(BotOnlineEvent botOnlineEvent){
        Plugin.MY_BOT = botOnlineEvent.getBot();    //注册全局bot方便整个应用进行访问
        if(MyPluginConfig.isAutoSign) {
            //开启自动签到
            new Thread((new AutoSignRunnable())).start();
        }
        if(MyPluginConfig.isLogin) {
            //开启redis登录计时器监听器
            new Thread(new RedisTimerListenerRunnable()).start();
        }
    }

    /**
     * 一级事件：邀请机器人进群时触发，用于自动同意加群请求
     * @param botInvitedJoinGroupRequestEvent 邀请机器人进群对象
     */
    @EventHandler
    public void onJoinGroup(BotInvitedJoinGroupRequestEvent botInvitedJoinGroupRequestEvent){
        if(MyPluginConfig.agreeAddGroup) {
            botInvitedJoinGroupRequestEvent.accept();   //同意加群请求
        }
    }

    /**
     * 一级事件：有新朋友加机器人为好友时触发，用于自动同意加好友请求
     * @param newFriendRequestEvent 新朋友
     */
    @EventHandler
    public void onAddFriend(NewFriendRequestEvent newFriendRequestEvent){
        if(MyPluginConfig.agreeAddFriend) {
            newFriendRequestEvent.accept(); //同意加好友请求
        }
    }

    /**
     * 一级事件：用户戳一戳触发
     *      其中二级事件：
     *          用户戳机器人时触发
     * @param botNudgedEvent  戳一戳事件
     */
    @EventHandler
    public void onBotNudged(NudgeEvent botNudgedEvent){
        UserOrBot target = botNudgedEvent.getTarget();
        if(!target.equals(Plugin.MY_BOT)){
            //检测是不是机器人被戳
            return;
        }
        Contact subject = botNudgedEvent.getSubject();
        UserOrBot from = botNudgedEvent.getFrom();
        secondEventHandler.showUseMethod(subject,from);
    }
}
