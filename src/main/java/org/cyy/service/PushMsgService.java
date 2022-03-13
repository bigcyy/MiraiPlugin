package org.cyy.service;


import net.mamoe.mirai.contact.Friend;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.message.data.At;
import org.cyy.Plugin;
import org.cyy.bean.PushMsgObj;
import org.cyy.config.ReplyMessage;
import org.cyy.dao.PushMsgObjDao;

import java.util.ArrayList;
import java.util.List;

/**
 * @author cyy
 * @date 2022/2/24 16:38
 * @description 推送服务的服务类
 */
public class PushMsgService {

    private final PushMsgObjDao pushMsgObjDao = new PushMsgObjDao();

    /**
     * 添加推送子群
     * @param pushMsgObjId 信息推送者id
     * @param childGroupId  待添加的子群id
     * @return 返回的成功还是失败的信息
     */
    public String addPushGroup(String pushMsgObjId, String childGroupId) {
        if(Plugin.MY_BOT.getGroup(Long.parseLong(childGroupId)) == null){
            //如果机器人未添加该子群
            return String.format(ReplyMessage.childGroupNotFindMsg,childGroupId);
        }
        PushMsgObj pushMsgObj;
        pushMsgObj = pushMsgObjDao.getPushMsgObjByFile(pushMsgObjId);    //从文件中尝试获取该信息推送者
        if(pushMsgObj == null){
            //如果信息推送者为空，则创建新的信息推送者
            pushMsgObj = new PushMsgObj();
            pushMsgObj.setPushMsgObjId(pushMsgObjId); //设置信息推送者id
            ArrayList<String> childGroup = new ArrayList<>();   //实例化子群集合
            ArrayList<String> childPerson = new ArrayList<>();   //实例化个人集合
            childGroup.add(childGroupId);
            pushMsgObj.setChildPersonId(childPerson);
            pushMsgObj.setChildGroupId(childGroup); //将子群加入集合中

        }else{
            //不为空
            boolean sign = false;
            if(pushMsgObj.getChildGroupId() == null){
                ArrayList<String> childGroup = new ArrayList<>();
                pushMsgObj.setChildGroupId(childGroup);
            }
            for(String oldGroup:pushMsgObj.getChildGroupId()){
                //判断集合是否已经包含新添加的子群
                if(oldGroup.equals(childGroupId)){
                    sign = true;
                    break;
                }
            }
            if(sign){   //如果包含
                return ReplyMessage.repeatAddChildGroupMsg;
            }
            //不包含
            pushMsgObj.getChildGroupId().add(childGroupId);
        }
        pushMsgObjDao.updatePushMsgObjToFile(String.valueOf(pushMsgObjId),pushMsgObj);   //保存到本地
        return  ReplyMessage.addChildSuccessMsg;
//                group.sendMessage(new At(sender.getId()).plus("添加成功！"));

    }

    /**
     * 添加推送个人
     * @param pushMsgObjId 信息推送对象id
     * @param childPersonId  待添加的个人id
     * @return 返回的成功还是失败的信息
     */
    public String addPushPerson(String pushMsgObjId, String childPersonId) {
        if(Plugin.MY_BOT.getFriend(Long.parseLong(childPersonId)) == null){
            //如果机器人未添加该好友
            return String.format(ReplyMessage.childPersonNotFindMsg,childPersonId);
        }
        PushMsgObj pushMsgObj;
        pushMsgObj = pushMsgObjDao.getPushMsgObjByFile(pushMsgObjId);    //从文件中尝试获取该信息推送者
        if(pushMsgObj == null){
            //如果为空 创建新的配置对象
            pushMsgObj = new PushMsgObj();
            pushMsgObj.setPushMsgObjId(pushMsgObjId); //设置信息推送对象id
            ArrayList<String> childPerson = new ArrayList<>();
            ArrayList<String> childGroup = new ArrayList<>();
            childPerson.add(childPersonId);
            pushMsgObj.setChildGroupId(childGroup);
            pushMsgObj.setChildPersonId(childPerson); //实例化信息推送对象的个人集合
        }else{
            //不为空
            boolean sign = false;
            if(pushMsgObj.getChildPersonId() == null){  //健壮性判断
                ArrayList<String> childPerson = new ArrayList<>();
                pushMsgObj.setChildPersonId(childPerson);
            }
            for(String oldPerson:pushMsgObj.getChildPersonId()){
                //判断集合是否已经包含新添加的个人
                if(oldPerson.equals(childPersonId)){
                    sign = true;
                    break;
                }
            }
            if(sign){   //如果包含
                return ReplyMessage.repeatAddChildPersonMsg;
            }
            //不为空
            pushMsgObj.getChildPersonId().add(childPersonId);
        }
        pushMsgObjDao.updatePushMsgObjToFile(String.valueOf(pushMsgObjId),pushMsgObj);   //保存到本地
        return  ReplyMessage.addChildSuccessMsg;
//                group.sendMessage(new At(sender.getId()).plus("添加成功！"));

    }



    /**
     * 删除推送子群
     * @param pushMsgObjId   信息推送对象id
     * @param childGroupId  待删除子群id
     * @return  成功还是失败的信息
     */
    public String deletePushGroup(String pushMsgObjId, String childGroupId){
        PushMsgObj pushMsgObj;
        pushMsgObj = pushMsgObjDao.getPushMsgObjByFile(String.valueOf(pushMsgObjId));
        if(pushMsgObj == null){ //不存在信息推送者
            return ReplyMessage.deleteChildUnSuccessMsg;
        }
        List<String> childGroupIds = pushMsgObj.getChildGroupId();
        boolean remove = childGroupIds.remove(childGroupId);
        if(!remove){
            return ReplyMessage.deleteChildUnSuccessMsg;
        }
        pushMsgObjDao.updatePushMsgObjToFile(String.valueOf(pushMsgObjId),pushMsgObj);
        return ReplyMessage.deleteChildSuccessMsg;
    }

    /**
     * 删除推送个人
     * @param pushMsgObjId   群id
     * @param childPersonId  待删除个人id
     * @return  成功还是失败的信息
     */
    public String deletePushPerson(String pushMsgObjId, String childPersonId){
        PushMsgObj pushMsgObj;
        pushMsgObj = pushMsgObjDao.getPushMsgObjByFile(String.valueOf(pushMsgObjId));
        if(pushMsgObj == null){ //不存在信息推送者
            return ReplyMessage.deleteChildUnSuccessMsg;
        }
        List<String> childPersonIds = pushMsgObj.getChildPersonId();
        boolean remove = childPersonIds.remove(childPersonId);
        if(!remove){
            return ReplyMessage.deleteChildUnSuccessMsg;
        }
        pushMsgObjDao.updatePushMsgObjToFile(String.valueOf(pushMsgObjId),pushMsgObj);
        return ReplyMessage.deleteChildSuccessMsg;
    }

    /**
     * 获取所有子群和个人
     * @param pushMsgObjId  信息推送者id
     * @return  子信息
     */
    public String getAllChild(String pushMsgObjId){
        PushMsgObj pushMsgObj;
        pushMsgObj = pushMsgObjDao.getPushMsgObjByFile(pushMsgObjId);
        if(pushMsgObj == null){
            //没有该信息推送者信息
            return ReplyMessage.noChildMsg;
        }

        StringBuilder groupBuilder = new StringBuilder();
        if(pushMsgObj.getChildGroupId() != null && pushMsgObj.getChildGroupId().size() != 0) {  //健壮性判断
            //查询子群
            pushMsgObj.getChildGroupId().forEach((childGroupId) -> {  //遍历每一个子群
                Group childGroup = Plugin.MY_BOT.getGroup(Long.parseLong(childGroupId));    //从机器人群列表中获取子群
                String childGroupName = "机器人未添加该群";
                if (childGroup != null) {
                    childGroupName = childGroup.getName();
                }
                groupBuilder.append(childGroupId).append(":").append(childGroupName).append("\n");
            });
        }
        StringBuilder personBuilder = new StringBuilder();
        if(pushMsgObj.getChildPersonId() != null && pushMsgObj.getChildPersonId().size() != 0) {  //健壮性判断
            //查询个人
            pushMsgObj.getChildPersonId().forEach((childPersonId) -> {  //遍历每一个个人
                Friend childPerson = Plugin.MY_BOT.getFriend(Long.parseLong(childPersonId));//从机器人好友列表中获取好友
                String childPersonName = "机器人未添加该好友";
                if (childPerson != null) {
                    childPersonName = childPerson.getNick();
                }
                personBuilder.append(childPersonId).append(":").append(childPersonName).append("\n");
            });
        }
        return String.format(ReplyMessage.allChildMsg,groupBuilder,personBuilder);
    }
}
