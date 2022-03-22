package org.cyy.dao;

import org.cyy.bean.PushMsgObj;
import org.cyy.config.MyPluginConfig;
import org.cyy.utils.YmlAndPropAndIOUtil;

/**
 * @author cyy
 * @date 2022/2/20 12:43
 * @description
 */
public class PushMsgObjDao {
    public PushMsgObj getPushMsgObjByFile(String groupId){
        String path = MyPluginConfig.pushMsgConfigFilePath + groupId+".yml";
        PushMsgObj pushMsgObj = (PushMsgObj) YmlAndPropAndIOUtil.loadToObjFromPath(path, PushMsgObj.class);
        if(pushMsgObj == null){
            return null;
        }
        return pushMsgObj;
    }

    public void updatePushMsgObjToFile(String groupId,Object obj){
        String path = MyPluginConfig.pushMsgConfigFilePath + groupId+".yml";
        YmlAndPropAndIOUtil.saveObjToFile(path,obj);
    }
}
