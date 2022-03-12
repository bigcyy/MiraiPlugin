package org.cyy.bean;

import java.util.List;

/**
 * @author cyy
 * @date 2022/2/20 12:39
 * @description 推送信息对象
 */
public class PushMsgObj {
    private int num;    //有多少个子群
    private String groupId; //群id
    private List<String> childGroupId;  //子群集合

    public PushMsgObj() {
    }

    public PushMsgObj(int num, String groupId, List<String> childGroupId) {
        this.num = num;
        this.groupId = groupId;
        this.childGroupId = childGroupId;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public List<String> getChildGroupId() {
        return childGroupId;
    }

    public void setChildGroupId(List<String> childGroupId) {
        this.childGroupId = childGroupId;
    }

    @Override
    public String toString() {
        return "PushMsgObj{" +
                "num=" + num +
                ", groupId='" + groupId + '\'' +
                ", childGroupId=" + childGroupId +
                '}';
    }
}
