package org.cyy.handler.second;

import net.mamoe.mirai.console.command.CommandManager;
import net.mamoe.mirai.console.command.CommandSenderOnMessage;
import net.mamoe.mirai.console.command.FriendCommandSenderOnMessage;
import net.mamoe.mirai.contact.*;
import net.mamoe.mirai.event.events.FriendMessageEvent;
import net.mamoe.mirai.message.MessageReceipt;
import net.mamoe.mirai.message.data.*;
import org.cyy.Plugin;
import org.cyy.bean.PushMsgObj;
import org.cyy.config.MyPluginConfig;
import org.cyy.config.ReplyMessage;
import org.cyy.dao.PushMsgObjDao;
import org.cyy.exception.AuthIsNullException;
import org.cyy.exception.BaseCookieIsErrorException;
import org.cyy.exception.GroupFileNotFindException;
import org.cyy.job.AutoSignRunnable;
import org.cyy.redis.RedisTimerListener;
import org.cyy.redis.RedisTimerListenerRunnable;
import org.cyy.redis.RedislUtil;
import org.cyy.service.LoginService;
import org.cyy.service.PushMsgService;
import org.cyy.service.SignService;
import org.cyy.utils.PushMsg;
import org.cyy.utils.YmlAndPropAndIOUtil;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import java.io.IOException;
import java.util.*;

/**
 * @author cyy
 * @date 2022/2/16 14:38
 * @description 对功能的事件进行处理
 */
public class SecondEventHandler {
    /**
     * 催签到事件的处理
     * @param group 需要催签到的群
     */
    public void sign(Group group){
        SignService signService = new SignService(); //service层
        try{
            ArrayList<Message> messages = signService.makeSignMessages(group);  //调用service的方法获取名单，找出未签到的同学，制作信息
            for (Message message : messages) {
                MessageReceipt<Group> groupMessageReceipt = group.sendMessage(message);
                if (!message.contentToString().contains(ReplyMessage.allIsSignMsg)) {
                    //自动撤回
                    groupMessageReceipt.recallIn(MyPluginConfig.recallTime);
                }
            }
            /*
             * 统一的异常处理
             */
        }catch (IOException ioException){
            group.sendMessage(ReplyMessage.ioExceptionMsg);
            ioException.printStackTrace();
        }catch (AuthIsNullException authIsNullException) {
            group.sendMessage(ReplyMessage.authExceptionMsg);
            authIsNullException.printStackTrace();
        } catch (GroupFileNotFindException groupFileNotFindException) {
            group.sendMessage(ReplyMessage.groupFileExceptionMsg);
            groupFileNotFindException.printStackTrace();
        }


    }

    /**
     * 使用方法事件处理
     * @param group 询问的群
     */
    public void useMethod(Group group){
        group.sendMessage(ReplyMessage.useMethodMsg);
    }

    /**
     * 开始登录事件处理
     * @param group 触发的群
     * @param sender 发生登录命令的用户
     */
    public void beginLogin(Group group, Member sender) throws IOException {
        LoginService loginService = new LoginService();   //登录service
        String key = MyPluginConfig.loginUserQueueKey;   //登录用户队列key
        String value = RedislUtil.makeQueueLoginValue(group.getId(), sender.getId());   //制作存储在队列中的value
        if(RedislUtil.queueSize(key) == 0){
            //如果当前队列为空，直接将此次要进行登录的对象存入队中
            RedislUtil.queuePush(key, value);
        }
        String queueFront = RedislUtil.queueFront(key); //查出队首
        if(value.equals(queueFront)){
            //如果当前要执行登录的对象是队首，则进行开始登录操作
            try {
                loginService.beginLogin(group);
                String loginTimerKey = MyPluginConfig.loginTimerKeyPreFix+queueFront;   //设置登录计时器key
                RedislUtil.setEx(loginTimerKey,MyPluginConfig.loginTimerValue,MyPluginConfig.loginExTime); //设置登录计时器
            } catch (BaseCookieIsErrorException e) {
                e.printStackTrace();
                group.sendMessage(ReplyMessage.baseCookieIsErrorMsg);
                RedislUtil.queuePop(key);//出现异常，出队
            }
        }else {
            //前要执行登录的对象不是队首，提醒他等待
            long queueNowSize = RedislUtil.queuePush(key, value);   //将其入队
            long queueSize = RedislUtil.queueSize(key);         //获取入队后的队列大小
            String msg = String.format(ReplyMessage.pushToLoginQueueMsg,queueSize,queueNowSize);
            group.sendMessage(new At(sender.getId()).plus(new PlainText(msg)));
        }

    }

    /**
     * 收到验证码处理
     * @param group 触发的群
     * @param sender 信息发送者
     * @param msg 发送的信息，用于取出code
     */
    public void receiveCode(Group group,Member sender,String msg) throws IOException {
        String key = MyPluginConfig.loginUserQueueKey;
        String value = RedislUtil.makeQueueLoginValue(group.getId(),sender.getId());    //制作value
        String queueFront = RedislUtil.queueFront(key); //获取队首
        if(!value.equals(queueFront)){
            //如果不是队首进行操作
            group.sendMessage(ReplyMessage.notIsFrontMsg);
            return;
        }
        //获取传入的验证码
        String tempCode = msg.trim();
        String code = tempCode.substring(tempCode.length()-4);
        LoginService loginService = new LoginService();

        try {
            boolean isTrue = loginService.receiveCodeAndSubmit(group,code); //进行提交表单登录操作
            if(isTrue){
                group.sendMessage(ReplyMessage.loginOkMsg);
            }else{
                group.sendMessage(ReplyMessage.loginNotOkMsg);
            }
        } catch (GroupFileNotFindException groupFileNotFindException){
            group.sendMessage(ReplyMessage.groupFileExceptionMsg);
            RedislUtil.queuePop(key);//出现异常将其出队
            groupFileNotFindException.printStackTrace();
        }
        /*
            队首对象操作完后
        */
        String queuePop = RedislUtil.queuePop(key);//将其出队
        RedislUtil.del(MyPluginConfig.loginTimerKeyPreFix+queuePop);   //删除其计时器
        RedislUtil.dealNewFront(key);
    }

    /**
     * 执行指令
     * @param friendMessageEvent 指令
     */
    public void executeCmd(FriendMessageEvent friendMessageEvent) {
        CommandSenderOnMessage<FriendMessageEvent> commandSender = new FriendCommandSenderOnMessage(friendMessageEvent);
        CommandManager.INSTANCE.executeCommand(commandSender,friendMessageEvent.getMessage(),true);
    }

    /**
     * 开启功能事件的处理，发生的触发语句必须相同
     * @param sender 发送者
     * @param msg 触发信息
     */
    public void openFunction(Friend sender, String msg) {
        if(msg.equals("开启自动催签到") && !MyPluginConfig.isAutoSign){
            MyPluginConfig.isAutoSign = true;
            MyPluginConfig.saveIsAutoSignToFile();
            new Thread((new AutoSignRunnable())).start();
        }else if(msg.equals("开启催签到") && !MyPluginConfig.isSign){
            MyPluginConfig.isSign = true;
            MyPluginConfig.saveIsSignToFile();
        }else if(msg.contains("开启登录") && !MyPluginConfig.isLogin){
            MyPluginConfig.isLogin = true;
            MyPluginConfig.saveIsLoginToFile();
            new Thread(new RedisTimerListenerRunnable()).start();
        }else if(msg.equals("开启加好友") && !MyPluginConfig.agreeAddFriend){
            MyPluginConfig.agreeAddFriend = true;
            MyPluginConfig.saveAgreeAddFriendToFile();
        }else if(msg.equals("开启加群") && !MyPluginConfig.agreeAddGroup){
            MyPluginConfig.agreeAddGroup = true;
            MyPluginConfig.saveAgreeAddGroupToFile();
        }else if(msg.equals("开启信息推送") && !MyPluginConfig.isPushMsg){
            MyPluginConfig.isPushMsg = true;
            MyPluginConfig.saveIsPushMsgToFile();
        }else{
            sender.sendMessage("未找到对应配置");
            return;
        }
        sender.sendMessage("ok");
    }
    /**
     * 关闭功能事件的处理，发生的触发语句必须相同
     * @param sender 发送者
     * @param msg 触发信息
     */
    public void closeFunction(Friend sender, String msg) throws SchedulerException {
        if(msg.equals("关闭自动催签到") && MyPluginConfig.isAutoSign){
            MyPluginConfig.isAutoSign = false;
            MyPluginConfig.saveIsAutoSignToFile();
            Scheduler scheduler = (Scheduler)Plugin.contextMap.get("scheduler");
            scheduler.shutdown();
        }else if(msg.equals("关闭催签到") && MyPluginConfig.isSign){
            MyPluginConfig.isSign = false;
            MyPluginConfig.saveIsSignToFile();
        }else if(msg.contains("关闭登录") && MyPluginConfig.isLogin){
            MyPluginConfig.isLogin = false;
            MyPluginConfig.saveIsLoginToFile();
            RedislUtil.del("loginUser");
            RedislUtil.del("loginTimer");
            RedisTimerListener redisTimerListener = (RedisTimerListener) Plugin.contextMap.get("redisTimerListener");
            redisTimerListener.unsubscribe("__keyevent@0__:expired");
        }else if(msg.equals("关闭加好友") && MyPluginConfig.agreeAddFriend){
            MyPluginConfig.agreeAddFriend = false;
            MyPluginConfig.saveAgreeAddFriendToFile();
        }else if(msg.equals("关闭加群") && MyPluginConfig.agreeAddGroup){
            MyPluginConfig.agreeAddGroup = false;
            MyPluginConfig.saveAgreeAddGroupToFile();
        }else if(msg.equals("关闭自动通知") && MyPluginConfig.isAutoSign){
            MyPluginConfig.isPushMsg = false;
            MyPluginConfig.saveIsPushMsgToFile();
        }else{
            sender.sendMessage("未找到对应配置");
            return;
        }
        sender.sendMessage("ok");
    }

    /**
     *
     * 更新群昵称和健康系统姓名的映射，并存储到redis中
     * @param group 待更新的群
     * @param content 触发信息
     */
    public void updateGroupToRedis(Group group, String content) {
        try {
            SignService signService = new SignService();
            ArrayList<Message>  messages = new ArrayList<>();
            if(content.contains("redis")){
                messages = signService.updateGroupToRedis(group);
            }else if(content.contains("cache")){
                messages = signService.updateGroupToCache(group);
            }
            for(Message message:messages){
                group.sendMessage(message);
            }
        }catch (IOException ioException){
            group.sendMessage(ReplyMessage.ioExceptionMsg);
            ioException.printStackTrace();
        }catch (AuthIsNullException authIsNullException) {
            group.sendMessage(ReplyMessage.authExceptionMsg);
            authIsNullException.printStackTrace();
        } catch (GroupFileNotFindException groupFileNotFindException) {
            groupFileNotFindException.printStackTrace();

        }
    }

    /**
     * at全体成员时推送信息给子群的处理
     * @param group 群
     * @param sender 发送者
     * @param message 待推送的内容,转为string可以过滤掉at全体成员
     */
    public void pushAtAllMsg(Group group, Member sender, String message) {

        PushMsgObj pushMsgObj = new PushMsgObjDao().getPushMsgObjByFile(String.valueOf(group.getId())); //从文件中获取信息推送者·
        if(checkNotCanPushMsg(pushMsgObj)){
            return;
        }
        List<String> sucGroups = null;
        List<String> unSucGroups = null;
        List<String> sucPersons = null;
        List<String> unSucPersons = null;
        /*
            向群推送信息
         */
        if(pushMsgObj.getChildGroupId() != null) {  //防止空指针
            MessageChain messageChain  = new MessageChainBuilder()
                    .append(message)
                    .build();
            Map<String, List<String>> ansMap = new PushMsg.PushMsgBuilder()
                    .setMsgOrigin(group)
                    .setAimContact("group")
                    .setAimList(pushMsgObj.getChildGroupId())
                    .setMessage(messageChain)
                    .build().pushMsg();
            sucGroups = ansMap.get("success");
            unSucGroups = ansMap.get("unSuccess");

        }

        /*
         *  向个人推送信息
         */
        if(pushMsgObj.getChildPersonId() != null) {  //防止空指针
            MessageChain messageChain  = new MessageChainBuilder()
                    .append(message)
                    .build();
            Map<String, List<String>> ansMap = new PushMsg.PushMsgBuilder()
                    .setMsgOrigin(group)
                    .setAimContact("friend")
                    .setAimList(pushMsgObj.getChildPersonId())
                    .setMessage(messageChain)
                    .build().pushMsg();
            sucPersons = ansMap.get("success");
            unSucPersons = ansMap.get("unSuccess");
        }


        StringBuilder builder = new StringBuilder();    //构造推送成功的string字符串
        /*
            回复推送成功的群
         */
        assert sucGroups != null;
        StringBuilder sucGroupsMsg = createPushMsgAimBody(sucGroups);
        /*
            回复推送成功的好友
         */
        assert sucPersons != null;
        StringBuilder sucPersonsMsg = createPushMsgAimBody(sucPersons);
        String formatMsg = String.format(ReplyMessage.pushMsgOkMsg, sucGroupsMsg.toString() + sucPersonsMsg);
        sendAtOrNormalMessage(group,sender.getId(),formatMsg);
        /*
            回复推送失败的群和好友
         */
        if(!unSucGroups.isEmpty() || !unSucPersons.isEmpty()){
            StringBuilder pushMsgGroupBody = createPushMsgAimBody(unSucGroups);
            StringBuilder pushMsgPersonBody = createPushMsgAimBody(unSucPersons);
            sendAtOrNormalMessage(group,sender.getId(),ReplyMessage.pushMsgNotOkMsg+pushMsgGroupBody+pushMsgPersonBody);
        }

    }



    /**
     * 成功推送信息后，构造通知推送成功的目标或者失败的目标的方法
     * @param bodyList 等待构造的集合
     * @return 例如:小红\n小明
     */
    private StringBuilder createPushMsgAimBody(List<String> bodyList){
        StringBuilder builder = new StringBuilder();
        bodyList.forEach((body)->{   //开始构造
            builder.append(body).append("\n");
        });
        return builder;
    }

    /**
     * 成功推送信息后，传入一个builder，在此基础上构造通知推送成功的目标或者失败的目标
     * @param bodyList 等待构造的集合
     * @param builder 提供的stringBuilder 可预先携带一些信息(一些前拽,群：).例如：
     * 群：小明\n小红
     */
    private void createPushMsgAimBody(List<String> bodyList,StringBuilder builder){
        bodyList.forEach((body)->{   //开始构造
            builder.append(body).append("\n");
        });
    }

    /**
     * 添加推送对象事件处理器
     * @param contact 对话，用于获取信息推送者id
     * @param senderId  发送者
     * @param content   信息内容
     */
    public void addPushObj(Contact contact, long senderId, String content) {
        PushMsgService pushMsgService = new PushMsgService();
        long pushMsgObjId = contact.getId();  //信息推送者的id
        String[] split = content.split(":");    //处理添加的内容
        String message = "";
        if("添加推送子群".equals(split[0])) {
            //添加子群
            message = pushMsgService.addPushGroup(String.valueOf(pushMsgObjId), split[1]);
        }
        else if("添加推送好友".equals(split[0])) {
            //添加个人
            message = pushMsgService.addPushPerson(String.valueOf(pushMsgObjId), split[1]);
        }
        sendAtOrNormalMessage(contact,senderId,message);
    }

    /**
     * 删除推送对象事件处理器
     * @param contact 对话，用于获取信息推送者id
     * @param senderId    发送者
     * @param content   信息内容
     */
    public void deletePushObj(Contact contact, long senderId, String content) {
        PushMsgService pushMsgService = new PushMsgService();
        long pushMsgObjId = contact.getId();
        String[] split = content.split(":");    //处理删除的内容
        String message = "";
        if("删除推送子群".equals(split[0])) {
            //删除子群
            message = pushMsgService.deletePushGroup(String.valueOf(pushMsgObjId), split[1]);
        }
        else if("删除推送好友".equals(split[0])) {
            //删除个人
            message = pushMsgService.deletePushPerson(String.valueOf(pushMsgObjId), split[1]);
        }
        sendAtOrNormalMessage(contact,senderId,message);
    }

    /**
     * 获取所有子群事件处理器
     * @param contact 对话，用于获取信息推送者id
     * @param senderId 发送者
     */
    public void getAllPushObj(Contact contact, long senderId)  {
        PushMsgService pushMsgService = new PushMsgService();
        long pushMsgObjId = contact.getId();
        String allChild = pushMsgService.getAllChild(String.valueOf(pushMsgObjId));
        sendAtOrNormalMessage(contact,senderId,allChild);
    }

    /**
     * 根据聊天环境，判断是否需要发送at信息
     * @param contact 聊天环境
     * @param atId 如果为群需要at的id
     * @param msg 发送的信息
     */
    private void sendAtOrNormalMessage(Contact contact,long atId,String msg){
        if(contact instanceof Group) {
            contact.sendMessage(new At(atId).plus(new PlainText("\n")).plus(msg));
        }
        else if(contact instanceof Friend) {
            contact.sendMessage(msg);
        }
    }

    /**
     * 开始批量推送信息处理
     * @param contact 聊天环境
     * @param senderId 发送者id
     */
    public void beginPushSomeMsg(Contact contact, long senderId) {

        PushMsgObj pushMsgObj = new PushMsgObjDao().getPushMsgObjByFile(String.valueOf(contact.getId())); //从文件中获取子群·
        if(checkNotCanPushMsg(pushMsgObj)){
            return;
        }
        String key = String.format(MyPluginConfig.pushMsgTimerKey,contact.getId(),senderId);
        RedislUtil.setEx(key, key, MyPluginConfig.pushMsgExTime);
        sendAtOrNormalMessage(contact,senderId,ReplyMessage.beginPushMsg);
    }

    /**
     * 判断是否能推送信息，该推送者是否有可推送对象(子群，好友)
     * @param pushMsgObj 推送信息的配置对象
     * @return 是否能推送信息,返回true表示不能,false表示能
     */
    private boolean checkNotCanPushMsg(PushMsgObj pushMsgObj){

        if(pushMsgObj == null){
            //该群没有配置推送信息
            return true;
        }
        if(pushMsgObj.getChildGroupId() == null && pushMsgObj.getChildPersonId() == null){
            //该群没有配置推送群
            return true;
        }
        return pushMsgObj.getChildGroupId().size() == 0 && pushMsgObj.getChildPersonId().size() == 0;    //该群没有配置推送群

    }

    public void pushSomeMsg(Contact contact, MessageChain message) {
        PushMsgObj pushMsgObj = new PushMsgObjDao().getPushMsgObjByFile(String.valueOf(contact.getId())); //从文件中获取信息推送者·
        if(checkNotCanPushMsg(pushMsgObj)){
            return;
        }
        if(pushMsgObj.getChildGroupId() != null) {
            new PushMsg.PushMsgBuilder()
                    .setAimContact("group")
                    .setMsgOrigin(contact)
                    .setMessage(message)
                    .setAimList(pushMsgObj.getChildGroupId())
                    .build().pushMsg();
        }
        if(pushMsgObj.getChildPersonId() != null) {
            new PushMsg.PushMsgBuilder()
                    .setAimContact("friend")
                    .setMsgOrigin(contact)
                    .setMessage(message)
                    .setAimList(pushMsgObj.getChildPersonId())
                    .build().pushMsg();
        }
    }

    /**
     * 结束推送信息处理
     * @param contact 聊天环境
     * @param senderId 发送者id
     */
    public void endPushSomMsg(Contact contact,long senderId) {
        String key = String.format(MyPluginConfig.pushMsgTimerKey,contact.getId(),senderId);
        Long del = RedislUtil.del(key);
        if(del != 0) {
            sendAtOrNormalMessage(contact,senderId,ReplyMessage.endPushIsOkMsg);
        }
    }

    /**
     * 戳一戳机器人显示使用方法处理
     * @param subject 会话
     */
    public void showUseMethod(Contact subject, String msg) {
        //subject.sendMessage(new At(from.getId()).plus(ReplyMessage.useMethodMsg));
        subject.sendMessage(msg);
    }

    /**
     * 根据id清除配置文件
     * @param id 群或者好友的id
     */
    public void cleanConfig(long id) {
        YmlAndPropAndIOUtil.deleteFileByFilePattern(MyPluginConfig.pushMsgConfigFilePath,String.valueOf(id));
        YmlAndPropAndIOUtil.deleteFileByFilePattern(MyPluginConfig.signConfigFilePath,String.valueOf(id));
    }


}
