package org.cyy.handler;

import kotlin.coroutines.CoroutineContext;
import net.mamoe.mirai.contact.*;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.*;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.AtAll;
import net.mamoe.mirai.message.data.Message;
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
 * @description 事件分发处理，对mirai事件进行监听
 * 机器人的功能实现依靠这些基础事件，机器人的功能又被分解为不同自定义事件
 * 例如:
 *  当mirai监听到群信息时，回调本类中的onGroupMessageEvent方法，如果该群信息正好是我自定义的功能催签到事件的触发条件，
 *  则执行我所自定义的催签到功能的催签到事件代码
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
     * mirai事件：接收到群信息时触发
     *
     *      基于群信息事件的所有功能,多个事件组成一个功能：
     *          催签到功能：催签到事件和更新事件组合成催签到功能(目前未使用更新事件)
     *          登录功能：登录事件和处理验证码事件组合成登录功能
     *          群推送功能：增加查看删除子群命令和开始推送命令结束推送命令以及at全体成员信息命令组成信息推送功能
     *      使用方法:
     *          催签到功能:
     *              催签到事件：群里发送：”催签到“
     *              更新事件：群里发送：“更新”
     *              登录事件： 群里发送：@bot+”登录“
     *              处理验证码事件：群里发送：“验证码:xxxx”
     *
     *          群推送功能(群信息推送给群，个人)：
     *              推送到群的配置相关事件:
     *                  添加推送子群事件：群里发送：“添加推送子群:xxxxx”,xxx为qq号，以下都是
     *                  删除推送子群事件：群里发送：“删除推送子群：xxxxx”
     *                  查看推送子群事件：群里发送：“查看推送子群”
     *              推送到个人的配置相关事件：
     *                  添加推送个人事件：群里发送：“添加推送好友:xxxxx”
     *                  删除推送个人事件：群里发送：“删除推送好友：xxxxx”
     *                  查看推送个人事件：群里发送：“查看推送好友”
     *              开始批量信息推送事件:群里发送：“开始推送”
     *              结束批量信息推送事件:群里发送：“结束推送”
     *          使用方法功能:
     *              查看食用方法事件: 群里发送：”使用方法“
     *
     * @param groupMessageEvent 群信息事件对象，mirai会将事件的触发者，触发信息等封装到此
     */
    @EventHandler
    public void onGroupMessageEvent(GroupMessageEvent groupMessageEvent) throws IOException {
        Member sender = groupMessageEvent.getSender();      //获取信息发送者
        MessageChain message = groupMessageEvent.getMessage();  //获取信息链
        String content = message.contentToString(); //将信息转为纯内容
        Group group = groupMessageEvent.getGroup(); //获取触发群消息事件的群
        /*
            催签到功能
         */
        if(MyPluginConfig.isSign && sender.getId() != Plugin.MY_BOT.getId()){
            if(content.equals("催签到")){
                //催签到事件
                secondEventHandler.sign(group);
            }else if(content.equals("催签到介绍")){
                secondEventHandler.showUseMethod(group,ReplyMessage.signUseMethodMsg);
            }
        }

        /*
            登录功能
         */
        if(MyPluginConfig.isLogin && sender.getId() != Plugin.MY_BOT.getId()){
            if(message.contains(new At(Plugin.MY_BOT.getId())) && content.contains("登录")){
                //登录事件
                secondEventHandler.beginLogin(group,groupMessageEvent.getSender());
            }else if(content.startsWith("验证码")){
                //回复验证码事件
                secondEventHandler.receiveCode(group,groupMessageEvent.getSender(),content);
            }
        }
        /*
            使用方法功能
         */
        if(content.equals("使用方法")&& sender.getId() != Plugin.MY_BOT.getId()){
            secondEventHandler.showUseMethod(group,ReplyMessage.newUseMethodMsg);
        }

        /*
            群推送功能
         */
        if(MyPluginConfig.isPushMsg && sender.getId() != Plugin.MY_BOT.getId()){

            long groupId = group.getId();   //获取群id
            long senderId = sender.getId(); //获取发送信息者id
            String key = String.format(MyPluginConfig.pushMsgTimerKey,groupId,senderId);   //推送信息计时器

            if(RedislUtil.get(key) != null){
                //从redis中获取是否已经开启批量推送，如果开启再判断信息内容是不是结束推送，不是结束推送则将信息进行推送
                if(content.equals("结束推送")){
                    secondEventHandler.endPushSomMsg(group,senderId);
                    return;
                }
                if(content.equals("开始推送")){
                    //已经在推送，再次批量推送则发出警告
                    group.sendMessage(ReplyMessage.rePushMsg);
                    return;
                }
                //进行信息推送
                secondEventHandler.pushSomeMsg(group, message);
                return;
            }

            if(content.startsWith("添加推送子群:") || content.startsWith("添加推送好友:")){
                secondEventHandler.addPushObj(group,senderId,content);    //添加推送对象
            }else if(content.startsWith("删除推送子群:") || content.startsWith("删除推送好友:")){
                secondEventHandler.deletePushObj(group,senderId,content);
            }else if(content.startsWith("查看推送目标")){
                secondEventHandler.getAllPushObj(group,senderId);
            }else if(content.equals("开始推送")){
                secondEventHandler.beginPushSomeMsg(group,senderId);
            }else if(message.contains(AtAll.INSTANCE)){
                secondEventHandler.pushAtAllMsg(group,sender,content);
            }else if(content.equals("信息推送功能介绍")){
                secondEventHandler.showUseMethod(group,ReplyMessage.pushMsgUseMethodMsg);
            }
        }

    }

    /**
     * mirai事件：接收到好友信息时触发
     *      基于好友信息事件的功能:
     *          执行命令功能：在聊天窗口执行命令事件
     *          动态开启机器人功能的功能：动态开启事件
     *      使用方法：
     *          个人推送功能
     *              推送到群配置:
     *                  添加推送子群事件：私聊机器人发送：“添加推送子群:xxxxx”
     *                  删除推送子群事件：私聊机器人发送：“删除推送子群：xxxxx”
     *                  查看推送子群事件：私聊机器人发送：“查看推送子群”
     *              推送到个人配置:
     *                  添加推送个人事件：私聊机器人发送：“添加推送好友:xxxxx”
     *                  添加推送个人事件：私聊机器人发送：“删除推送好友：xxxxx”
     *                  添加推送个人事件：私聊机器人发送：“查看推送好友”
     *
     * @param friendMessageEvent 好友信息事件对象
     */
    @EventHandler
    public void onFriendMessageEvent(FriendMessageEvent friendMessageEvent) throws SchedulerException {
        MessageChain message = friendMessageEvent.getMessage();
        String content = message.contentToString();
        Friend friend = friendMessageEvent.getFriend();
        Friend sender = friendMessageEvent.getSender();
        //仅bot主人可以执行命令
        if(friendMessageEvent.getSender().getId() == MyPluginConfig.master && content.startsWith("/")){
            secondEventHandler.executeCmd(friendMessageEvent);
        }else if(friendMessageEvent.getSender().getId() == MyPluginConfig.master && content.startsWith("开启")){
            secondEventHandler.openFunction(friendMessageEvent.getSender(),content);
        } else if(friendMessageEvent.getSender().getId() == MyPluginConfig.master && content.startsWith("关闭")){
            secondEventHandler.closeFunction(friendMessageEvent.getSender(),content);
        }
        /*
            好友中的使用方法介绍
         */
        if(content.equals("使用方法")){
            secondEventHandler.showUseMethod(friend,ReplyMessage.newUseMethodMsg);
        }else if(content.equals("催签到介绍")){
            secondEventHandler.showUseMethod(friend,ReplyMessage.signUseMethodMsg);
        }
        /*
            信息推送功能
         */
        if(MyPluginConfig.isPushMsg && sender.getId() != Plugin.MY_BOT.getId()) {
            long senderId = sender.getId();   //获取信息发送者id
            String key = String.format(MyPluginConfig.pushMsgTimerKey,senderId,senderId);   //推送信息计时器

            if(RedislUtil.get(key) != null){
                //从redis中获取是否已经开启批量推送，如果开启再判断信息内容是不是结束推送，不是结束推送则将信息进行推送
                if(content.equals("结束推送")){
                    secondEventHandler.endPushSomMsg(friend,senderId);
                    return;
                }
                if(content.equals("开始推送")){
                    //已经在推送，再次批量推送则发出警告
                    friend.sendMessage(ReplyMessage.rePushMsg);
                    return;
                }
                //进行信息推送
                secondEventHandler.pushSomeMsg(friend, message);
                return;
            }

            if (content.startsWith("添加推送子群:") || content.startsWith("添加推送好友:")) {
                secondEventHandler.addPushObj(friend, sender.getId(), content);    //添加推送对象
            } else if (content.startsWith("删除推送子群:") || content.startsWith("删除推送好友:")) {
                secondEventHandler.deletePushObj(friend, sender.getId(), content);
            } else if (content.startsWith("查看推送目标")) {
                secondEventHandler.getAllPushObj(friend, sender.getId());
            }else if(content.equals("开始推送")){
                secondEventHandler.beginPushSomeMsg(friend,senderId);
            }else if(content.equals("信息推送介绍")){
                secondEventHandler.showUseMethod(friend,ReplyMessage.pushMsgUseMethodMsg);
            }
        }

    }

    /**
     * mirai事件:机器人登录事件,机器人登录后触发
     *      总功能:
     *         自动催签到功能
     *      其他作用：
     *         记录当前登录的bot
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
     * mirai事件：邀请机器人进群时触发
     *      功能：
     *          用于自动同意加群请求
     * @param botInvitedJoinGroupRequestEvent 邀请机器人进群对象
     */
    @EventHandler
    public void onJoinGroup(BotInvitedJoinGroupRequestEvent botInvitedJoinGroupRequestEvent){
        if(MyPluginConfig.agreeAddGroup) {
            botInvitedJoinGroupRequestEvent.accept();   //同意加群请求
        }
    }

    /**
     * mirai事件：有新朋友加机器人为好友时触发
     *      功能:
     *          用于自动同意加好友请求
     * @param newFriendRequestEvent 新朋友
     */
    @EventHandler
    public void onAddFriend(NewFriendRequestEvent newFriendRequestEvent){
        if(MyPluginConfig.agreeAddFriend) {
            newFriendRequestEvent.accept(); //同意加好友请求
        }
    }

    /**
     * mirai事件：用户戳一戳触发
     *      功能：
     *          用户戳机器人时发送使用方法
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
        secondEventHandler.showUseMethod(subject,ReplyMessage.newUseMethodMsg);
    }
}
