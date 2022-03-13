package org.cyy.handler.second;

import net.mamoe.mirai.console.command.CommandManager;
import net.mamoe.mirai.console.command.CommandSenderOnMessage;
import net.mamoe.mirai.console.command.FriendCommandSenderOnMessage;
import net.mamoe.mirai.contact.*;
import net.mamoe.mirai.contact.file.AbsoluteFile;
import net.mamoe.mirai.event.events.FriendMessageEvent;
import net.mamoe.mirai.message.MessageReceipt;
import net.mamoe.mirai.message.data.*;
import net.mamoe.mirai.utils.ExternalResource;
import okhttp3.*;
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
import org.cyy.utils.YmlAndPropUtil;
import org.jetbrains.annotations.NotNull;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

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
     * @param message 待推送的内容
     */
    public void pushAtAllMsg(Group group, Member sender, String message) {
        PushMsgObj pushMsgObj = new PushMsgObjDao().getPushMsgObjByFile(String.valueOf(group.getId())); //从文件中获取子群·
        if(checkNotCanPushMsg(pushMsgObj)){
            return;
        }
        ArrayList<String> groups = new ArrayList<>();   //用于记录推送成功的群的群名称
        ArrayList<String> unSucGroups = new ArrayList<>(); //用于记录未推送成功的群的群号
        pushMsgObj.getChildGroupId().forEach((groupId)->{   //遍历每一个子群
            Group pushGroup = Plugin.MY_BOT.getGroup(Long.parseLong(groupId));  //从机器人群列表中中查找子群
            if(pushGroup!=null) {   //子群查找到了才进行推送
                pushGroup.sendMessage(new PlainText(String.format(ReplyMessage.pushMsgPrefix,pushGroup.getName())).plus(message));
                groups.add(pushGroup.getName());
            }else{  //没有查到该群
                unSucGroups.add(groupId);
            }
        });
        /*
            推送成功的群
         */
        StringBuilder builder = new StringBuilder();    //构造推送成功的string字符串
        groups.forEach((groupName)->{   //开始构造
            builder.append(groupName).append("\n");
        });
        group.sendMessage(new At(sender.getId()).plus(String.format(ReplyMessage.pushMsgOkMsg,builder)));
        /*
            推送失败的群
         */
        if(!unSucGroups.isEmpty()){
            StringBuilder unSucBuilder = new StringBuilder();    //构造推送失败的string字符串
            unSucGroups.forEach((groupName)->{   //开始构造
                unSucBuilder.append(groupName).append("\n");
            });
            group.sendMessage(ReplyMessage.pushMsgNotOkMsg+unSucBuilder);
        }

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
     * 获取所有子群处理器
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
            contact.sendMessage(new At(atId).plus(msg));
        }
        else if(contact instanceof Friend) {
            contact.sendMessage(msg);
        }
    }

    /**
     * 开始批量推送信息处理
     * @param group 待推送的主群
     * @param sender 发送者
     */
    public void beginPushSomeMsg(Group group, Member sender) {

        PushMsgObj pushMsgObj = new PushMsgObjDao().getPushMsgObjByFile(String.valueOf(group.getId())); //从文件中获取子群·
        if(checkNotCanPushMsg(pushMsgObj)){
            return;
        }
        String key = String.format(MyPluginConfig.pushMsgTimerKey,group.getId(),sender.getId());
        RedislUtil.setEx(key, key, MyPluginConfig.pushMsgExTime);
        group.sendMessage(new At(sender.getId()).plus(ReplyMessage.beginPushMsg));
    }

    /**
     * 判断是否能推送信息，该群是否有子群
     * @param pushMsgObj 推送信息的配置对象
     * @return 是否能推送信息,返回true表示不能,false表示能
     */
    private boolean checkNotCanPushMsg(PushMsgObj pushMsgObj){

        if(pushMsgObj == null){
            //该群没有配置推送信息
            return true;
        }
        if(pushMsgObj.getChildGroupId() == null){
            //该群没有配置推送群
            return true;
        }
        return pushMsgObj.getChildGroupId().size() == 0;    //该群没有配置推送群

    }

    public void pushSomeMsg(Group group, MessageChain message) {
        PushMsgObj pushMsgObj = new PushMsgObjDao().getPushMsgObjByFile(String.valueOf(group.getId()));
        boolean isFileMsg = message.stream().anyMatch((singleMessage) -> singleMessage instanceof FileMessage); //判断是否是文件信息
        Stream<String> filterStream = pushMsgObj.getChildGroupId().stream().filter((groupId) -> {   //先进行过滤
            return Plugin.MY_BOT.getGroup(Long.parseLong(groupId)) != null;  //从bot中查询到该群
        }); //过滤后的流，方便下面群发信息
        if (isFileMsg) { //如果是文件
            Optional<SingleMessage> first = message.stream().filter((singleMessage) -> singleMessage instanceof FileMessage).findFirst();
            SingleMessage singleMessage;
            if(first.isPresent()) { //stream流的非空判断
                singleMessage = first.get();    //获取文件信息
                AbsoluteFile absoluteFile = ((FileMessage) singleMessage).toAbsoluteFile(group);    //从刚刚发送文件的的群里获取该文件
                String fileName = absoluteFile.getName();   //获取到文件名
                String url = absoluteFile.getUrl(); //得到文件url
                OkHttpClient okHttpClient = new OkHttpClient(); //用okhttp发送请求，转成流
                /*
                    向子群发送文件
                 */
                filterStream.forEach((groupId)-> {  //foreach每一个待推送的群
                    Group pushGroup = Plugin.MY_BOT.getGroup(Long.parseLong(groupId));  //从bot中查询到该群
                    Request request = new Request.Builder().url(url).build();
                    okHttpClient.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {
                            e.printStackTrace();
                        }

                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                            InputStream inputStream = null;
                            try {
                                inputStream = response.body().byteStream(); //转成流
                                ExternalResource res = ExternalResource.create(inputStream).toAutoCloseable();  //用mirai的ExternalResource包装
                                pushGroup.getFiles().uploadNewFile("/" + fileName, res);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }finally {
                                //关闭流
                                YmlAndPropUtil.closeResource(inputStream,null);
                            }
                        }
                    });
                });
            }
        }else {
            //普通消息
            filterStream.forEach((groupId)-> {  //foreach每一个待推送的群
                Group pushGroup = Plugin.MY_BOT.getGroup(Long.parseLong(groupId));  //从bot中查询到该群
                assert pushGroup != null;
                pushGroup.sendMessage(new PlainText("来自群(" + group.getName() + ")的消息:\n").plus(message));
            });
        }


    }

    /**
     *
     * 结束推送信息处理
     * @param group 群
     * @param sender 发送者
     */
    public void endPushSomMsg(Group group,Member sender) {
        String key = String.format(MyPluginConfig.pushMsgTimerKey,group.getId(),sender.getId());
        Long del = RedislUtil.del(key);
        if(del != 0) {
            group.sendMessage(new At(sender.getId()).plus(ReplyMessage.endPushIsOkMsg));
        }
    }

    /**
     * 戳一戳机器人显示使用方法处理
     * @param subject 会话
     * @param from  谁戳一戳机器人
     */
    public void showUseMethod(Contact subject, UserOrBot from) {
        subject.sendMessage(new At(from.getId()).plus(ReplyMessage.useMethodMsg));
    }

}
