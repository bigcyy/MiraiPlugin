package org.cyy.service;


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
     * @param groupId 主群id
     * @param childGroupId  子群id
     * @return 返回的成功还是失败的信息
     */
    public String addPushGroup(String groupId, String childGroupId) {
        if(Plugin.MY_BOT.getGroup(Long.parseLong(childGroupId)) == null){
            //如果机器人未添加该子群
            return ReplyMessage.childGroupNotFindMsg;
        }
        PushMsgObj pushMsgObj;
        pushMsgObj = pushMsgObjDao.getPushMsgObjByFile(groupId);    //从文件中尝试获取该群的配置文件
        if(pushMsgObj == null){
            //如果配置文件为空，则创建新的配置对象
            pushMsgObj = new PushMsgObj();
            pushMsgObj.setGroupId(groupId); //设置群id
            pushMsgObj.setNum(1);   //设置子群数
            ArrayList<String> childGroup = new ArrayList<>();   //实例化子群集合
            childGroup.add(childGroupId);
            pushMsgObj.setChildGroupId(childGroup); //将子群加入集合中
        }else{
            //存在配置文件
            pushMsgObj.setNum(pushMsgObj.getNum()+1);
            boolean sign = false;
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
        pushMsgObjDao.updatePushMsgObjToFile(String.valueOf(groupId),pushMsgObj);   //保存到本地
        return  ReplyMessage.addChildGroupSuccessMsg;
//                group.sendMessage(new At(sender.getId()).plus("添加成功！"));

    }

    /**
     * 删除推送的子群
     * @param groupId   群id
     * @param childGroupId  子群id
     * @return  成功还是失败的信息
     */
    public String deletePushGroup(String groupId, String childGroupId){
        PushMsgObj pushMsgObj;
        pushMsgObj = pushMsgObjDao.getPushMsgObjByFile(String.valueOf(groupId));
        if(pushMsgObj == null){ //不存在子群配置文件
            return ReplyMessage.deleteChildGroupUnSuccessMsg;
        }
        List<String> childGroupIds = pushMsgObj.getChildGroupId();
        boolean remove = childGroupIds.remove(childGroupId);
        if(!remove){
            return ReplyMessage.deleteChildGroupUnSuccessMsg;
        }
        pushMsgObj.setNum(pushMsgObj.getNum()-1);
        pushMsgObjDao.updatePushMsgObjToFile(String.valueOf(groupId),pushMsgObj);
        return ReplyMessage.deleteChildGroupSuccessMsg;
    }

    /**
     * 获取所有子群
     * @param groupId  群id
     * @return  子群信息
     */
    public String getAllChildGroup(String groupId){
        PushMsgObj pushMsgObj;
        pushMsgObj = pushMsgObjDao.getPushMsgObjByFile(groupId);
        if(pushMsgObj == null){
            //子群配置文件为空
            return ReplyMessage.noChildGroupMsg;
        }
        if(pushMsgObj.getChildGroupId().size() == 0){
            //子群配置文件为空
            return  ReplyMessage.noChildGroupMsg;
        }
        StringBuilder builder = new StringBuilder();
        pushMsgObj.getChildGroupId().forEach((childGroupId)->{  //遍历每一个子群
            Group childGroup = Plugin.MY_BOT.getGroup(Long.parseLong(childGroupId));    //从机器人群列表中获取子群
            String childGroupName = "机器人未添加该群";
            if(childGroup!=null){
                childGroupName = childGroup.getName();
            }
            builder.append(childGroupId).append(":").append(childGroupName).append("\n");
        });
        return String.format(ReplyMessage.childGroupMsg,builder);
    }
}
