package org.cyy.utils;

import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Friend;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.file.AbsoluteFile;
import net.mamoe.mirai.message.data.FileMessage;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.message.data.SingleMessage;
import net.mamoe.mirai.utils.ExternalResource;
import okhttp3.*;
import org.cyy.Plugin;
import org.cyy.config.ReplyMessage;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class PushMsg {
    private final Contact msgOrigin;      //消息来源
    private final String aimContact;    //推送目标类型,"子群","好友"
    private final List<String> aimList;    //推送目标集合
    private final MessageChain message; //推送信息


    private PushMsg(PushMsgBuilder pushMsgBuilder) {
        this.msgOrigin = pushMsgBuilder.msgOrigin;
        this.aimContact = pushMsgBuilder.aimContact;
        this.aimList = pushMsgBuilder.aimList;
        this.message = pushMsgBuilder.message;
    }

    public static class PushMsgBuilder{
        private Contact msgOrigin;      //消息来源
        private String aimContact;    //推送目标类型
        private List<String> aimList;    //推送目标集合
        private MessageChain message; //推送信息

        public PushMsgBuilder setAimContact(String aimContact){
            this.aimContact = aimContact;
            return this;
        }
        public PushMsgBuilder setMsgOrigin(Contact msgOrigin){
            this.msgOrigin = msgOrigin;
            return this;
        }
        public PushMsgBuilder setAimList(List<String> aimList){
            this.aimList = aimList;
            return this;
        }
        public PushMsgBuilder setMessage(MessageChain message){
            this.message = message;
            return this;
        }
        public PushMsg build(){
            return new PushMsg(this);
        }
    }

    /**
     * 用于外部调用进行推送信息
     * @return 成功和失败的map,一般不为空，只有出现未定义的会话中进行推送返回值为null
     * 成功的集合的key为“success”,value一定不为空，若全部未推送成功那么成功集合的size为0
     * 不成功的集合的key为“unSuccess”,value一定不为空，若全部推送成功那么不成功集合的size为0
     */
    public Map<String,List<String>> pushMsg(){
        if(msgOrigin instanceof Group){ //如果推送者是主群
            if("group".equals(aimContact)){    //待推送的是子群
                boolean isFileMsg = message.stream().anyMatch((singleMessage) -> singleMessage instanceof FileMessage); //判断是否是文件信息
                if(isFileMsg){  //如果是文件
                    return pushFileMsgToGroup();
                }else{  //不是文件
                    return pushMsgToGroup();
                }
            }else if("friend".equals(aimContact)){  //待推送的是好友
                return pushMsgToFriend();
            }
        }else if(msgOrigin instanceof Friend){ //如果推送者是好友
            if("group".equals(aimContact)){    //待推送的是子群
                return pushMsgToGroup();
            }else if("friend".equals(aimContact)){  //待推送的是好友
                return pushMsgToFriend();
            }
        }
        return null;
    }

    /**
     * 推送文件信息到群
     * @return 成功和失败的map,一般不为空
     * 成功的集合的key为“success”,value一定不为空，若全部未推送成功那么成功集合的size为0
     * 不成功的集合的key为“unSuccess”,value一定不为空，若全部推送成功那么不成功集合的size为0
     */
    private Map<String,List<String>> pushFileMsgToGroup(){
        Group group = (Group)msgOrigin;
        Optional<SingleMessage> first = message.stream().filter((singleMessage) -> singleMessage instanceof FileMessage).findFirst();   //找到文件信息
        ArrayList<String> sucGroups = new ArrayList<>();   //用于记录推送成功的群的群名称
        ArrayList<String> unSucGroups = new ArrayList<>(); //用于记录未推送成功的群的群号
        if(first.isPresent()) { //stream流的非空判断
            SingleMessage singleMessage = first.get();    //获取文件信息
            AbsoluteFile absoluteFile = ((FileMessage) singleMessage).toAbsoluteFile(group);    //从刚刚发送文件的的群里获取该文件
            assert absoluteFile != null;
            String fileName = absoluteFile.getName();   //获取到文件名
            String url = absoluteFile.getUrl(); //得到文件url
            OkHttpClient okHttpClient = new OkHttpClient(); //用okhttp发送请求，转成流
            /*
                向子群发送文件
             */
            aimList.forEach((groupId)-> {  //foreach每一个待推送的群
                Group pushGroup = Plugin.MY_BOT.getGroup(Long.parseLong(groupId));  //从bot中查询到该群
                if(pushGroup != null) {
                    Request request = new Request.Builder().url(url).build();
                    okHttpClient.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {
                            e.printStackTrace();
                            unSucGroups.add(groupId);
                        }

                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                            InputStream inputStream = null;
                            try {
                                inputStream = response.body().byteStream(); //转成流
                                ExternalResource res = ExternalResource.create(inputStream).toAutoCloseable();  //用mirai的ExternalResource包装
                                pushGroup.getFiles().uploadNewFile("/" + fileName, res);    //上传并发送文件
                                sucGroups.add(groupId);
                            } catch (Exception e) {
                                e.printStackTrace();
                                unSucGroups.add(groupId);
                            } finally {
                                //关闭流
                                YmlAndPropUtil.closeResource(inputStream, null);
                            }
                        }
                    });
                }else{
                    unSucGroups.add(groupId);
                }
            });
        }
        return returnSucAndUnSucMap(sucGroups,unSucGroups);
    }

    /**
     * 推送普通信息到子群
     * @return 成功和失败的map,
     * 成功的集合的key为“success”,value一定不为空，若全部未推送成功那么成功集合的size为0
     * 不成功的集合的key为“unSuccess”,value一定不为空，若全部推送成功那么不成功集合的size为0
     */
    private Map<String,List<String>> pushMsgToGroup() {
        ArrayList<String> sucGroups = new ArrayList<>();   //用于记录推送成功的群的群名称
        ArrayList<String> unSucGroups = new ArrayList<>(); //用于记录未推送成功的群的群号
        //普通消息
        aimList.forEach((groupId) -> {  //foreach每一个待推送的目标群
            Group pushGroup = Plugin.MY_BOT.getGroup(Long.parseLong(groupId));  //从bot中查询到该目标
            if (pushGroup != null) {   //子群查找到了才进行推送
                String msg = String.format(ReplyMessage.pushMsgSuccessPrefixMsg, conventToChines());
                pushGroup.sendMessage(new PlainText(msg).plus(message));
                sucGroups.add(pushGroup.getName());
            }else {  //没有查到该群
                unSucGroups.add(groupId);
            }
        });
        return returnSucAndUnSucMap(sucGroups,unSucGroups);

    }

    private String conventToChines(){
        if(msgOrigin instanceof Group) {
            Group group = (Group) msgOrigin;
            return "群("+group.getName()+":"+group.getId()+")";
        }
        else if(msgOrigin instanceof Friend) {
            Friend friend = (Friend) msgOrigin;
            return "好友("+friend.getNick()+":"+friend.getId()+")";
        }
        else
            return "未知物种";
    }
    /**
     * 推送普通信息到好友
     * @return 成功和失败的map,
     * 成功的集合的key为“success”,value一定不为空，若全部未推送成功那么成功集合的size为0
     * 不成功的集合的key为“unSuccess”,value一定不为空，若全部推送成功那么不成功集合的size为0
     */
    private Map<String,List<String>> pushMsgToFriend(){
        ArrayList<String> sucFriends = new ArrayList<>();   //用于记录推送成功的好友的名称
        ArrayList<String> unSucFriends = new ArrayList<>(); //用于记录未推送成功的好友的qq号
        //普通消息
        aimList.forEach((friendId)-> {  //foreach每一个待推送的目标群
            Friend friend = Plugin.MY_BOT.getFriend(Long.parseLong(friendId));  //从bot中查询到该目标
            if(friend != null) {
                String msg = String.format(ReplyMessage.pushMsgSuccessPrefixMsg, conventToChines(), friend.getNick(), friendId);

                friend.sendMessage(new PlainText(msg).plus(message));
                sucFriends.add(friend.getNick());
            }else{
                unSucFriends.add(friendId);
            }
        });
        return returnSucAndUnSucMap(sucFriends,unSucFriends);
    }

    /**
     * 提供成功和不成功的集合，将其封装到map中并返回
     * @param suc 成功的集合
     * @param unSuc 不成功的集合
     * @return 封装了成功和不成功集合的map
     */
    private Map<String,List<String>> returnSucAndUnSucMap(List<String> suc,List<String> unSuc){
        //用于存储推送成功和失败的信息
        HashMap<String, List<String>> stringListHashMap = new HashMap<>();
        stringListHashMap.put("success",suc);
        stringListHashMap.put("unSuccess",unSuc);
        return stringListHashMap;
    }

}
