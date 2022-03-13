package org.cyy.bean;

import java.util.List;

/**
 * @author cyy
 * @date 2022/2/20 12:39
 * @description 推送信息对象
 */
public class PushMsgObj {
    private String pushMsgObjId; //信息推送对象id
    private List<String> childGroupId;  //子群集合
    private List<String> childPersonId; //个人集合

    public String getPushMsgObjId() {
        return pushMsgObjId;
    }

    public void setPushMsgObjId(String pushMsgObjId) {
        this.pushMsgObjId = pushMsgObjId;
    }

    public List<String> getChildGroupId() {
        return childGroupId;
    }

    public void setChildGroupId(List<String> childGroupId) {
        this.childGroupId = childGroupId;
    }

    public List<String> getChildPersonId() {
        return childPersonId;
    }

    public void setChildPersonId(List<String> childPersonId) {
        this.childPersonId = childPersonId;
    }
}
