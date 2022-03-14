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
import java.util.ArrayList;
import java.util.Optional;

public class PushMsg {
    private final Contact msgOrigin;      //消息来源
    private final String aimContact;    //推送目标类型,"子群","好友"
    private final ArrayList<String> aimList;    //推送目标集合
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
        private ArrayList<String> aimList;    //推送目标集合
        private MessageChain message; //推送信息

        public PushMsgBuilder setAimContact(String aimContact){
            this.aimContact = aimContact;
            return this;
        }
        public PushMsgBuilder setMsgOrigin(Contact msgOrigin){
            this.msgOrigin = msgOrigin;
            return this;
        }
        public PushMsgBuilder setAimList(ArrayList<String> aimList){
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

    public void pushMsg(){
        if(msgOrigin instanceof Group){ //如果推送者是主群

            if("子群".equals(aimContact)){    //待推送的是子群
                boolean isFileMsg = message.stream().anyMatch((singleMessage) -> singleMessage instanceof FileMessage); //判断是否是文件信息
                if(isFileMsg){  //如果是文件
                    pushFileMsgToGroup();
                }else{
                    pushToGroupMsg();
                }
            }else if("好友".equals(aimContact)){  //待推送的是好友
                pushToFriendMsg();
            }
        }else if(msgOrigin instanceof Friend){
            if("子群".equals(aimContact)){    //待推送的是子群
                pushToGroupMsg();
            }else if("好友".equals(aimContact)){  //待推送的是好友
                pushToFriendMsg();
            }
        }

    }
//    private void pushFileMsgToPerson(Group group){
//        Optional<SingleMessage> first = message.stream().filter((singleMessage) -> singleMessage instanceof FileMessage).findFirst();   //找到文件信息
//        if(first.isPresent()) { //stream流的非空判断
//            SingleMessage singleMessage = first.get();    //获取文件信息
//            AbsoluteFile absoluteFile = ((FileMessage) singleMessage).toAbsoluteFile(group);    //从刚刚发送文件的的群里获取该文件
//            String fileName = absoluteFile.getName();   //获取到文件名
//            String url = absoluteFile.getUrl(); //得到文件url
//            OkHttpClient okHttpClient = new OkHttpClient(); //用okhttp发送请求，转成流
//            /*
//                向好友发送文件
//             */
//            aimList.forEach((personId)-> {  //foreach每一个待推送的群
//                Friend friend= Plugin.MY_BOT.getFriend(Long.parseLong(personId));  //从bot中查询到该群
//                Request request = new Request.Builder().url(url).build();
//                okHttpClient.newCall(request).enqueue(new Callback() {
//                    @Override
//                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
//                        e.printStackTrace();
//                    }
//
//                    @Override
//                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
//                        InputStream inputStream = null;
//                        try {
//                            inputStream = response.body().byteStream(); //转成流
//                            ExternalResource res = ExternalResource.create(inputStream).toAutoCloseable();  //用mirai的ExternalResource包装
//                            group.getFiles().uploadNewFile("/" + fileName, res);
//
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }finally {
//                            //关闭流
//                            YmlAndPropUtil.closeResource(inputStream,null);
//                        }
//                    }
//                });
//            });
//        }
//    }
    private void pushFileMsgToGroup(){
        Group group = (Group)msgOrigin;
        Optional<SingleMessage> first = message.stream().filter((singleMessage) -> singleMessage instanceof FileMessage).findFirst();   //找到文件信息
        if(first.isPresent()) { //stream流的非空判断
            SingleMessage singleMessage = first.get();    //获取文件信息
            AbsoluteFile absoluteFile = ((FileMessage) singleMessage).toAbsoluteFile(group);    //从刚刚发送文件的的群里获取该文件
            String fileName = absoluteFile.getName();   //获取到文件名
            String url = absoluteFile.getUrl(); //得到文件url
            OkHttpClient okHttpClient = new OkHttpClient(); //用okhttp发送请求，转成流
            /*
                向子群发送文件
             */
            aimList.forEach((groupId)-> {  //foreach每一个待推送的群
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
    }
    private void pushToGroupMsg() {
        //普通消息
        aimList.forEach((groupId) -> {  //foreach每一个待推送的目标群
            Group pushGroup = Plugin.MY_BOT.getGroup(Long.parseLong(groupId));  //从bot中查询到该目标
            assert pushGroup != null;
            String msg = String.format(ReplyMessage.pushMsgSuccessPrefixMsg, aimContact, pushGroup.getName(), groupId);
            pushGroup.sendMessage(new PlainText(msg).plus(message));
        });
    }

    private void pushToFriendMsg(){
        //普通消息
        aimList.forEach((friendId)-> {  //foreach每一个待推送的目标群
            Friend friend = Plugin.MY_BOT.getFriend(Long.parseLong(friendId));  //从bot中查询到该目标
            assert friend != null;
            String msg = String.format(ReplyMessage.pushMsgSuccessPrefixMsg, aimContact, friend.getNick(), friendId);
            friend.sendMessage(new PlainText(msg).plus(message));
        });
    }

}
