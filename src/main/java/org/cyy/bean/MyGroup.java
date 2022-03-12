package org.cyy.bean;

import java.util.ArrayList;


/**
 * @author cyy
 * @date 2022/2/15 15:06
 * @description 群
 */
public class MyGroup {
    private int num;     //每个群包含的不同班级数(一些群里不仅有计科技还有数媒)
    private String groupId; //群号，唯一标识，一个群对应一个配置文件
    private QueryAccount queryAccount;
    private ArrayList<QueryObject> queryObjectList; //正如num注释，每一个queryObject可理解为一个专业班级，比如数媒20级，比如计科20级

    public MyGroup() {
    }

    public MyGroup(int num, String groupId, QueryAccount queryAccount, ArrayList<QueryObject> queryObjectList) {
        this.num = num;
        this.groupId = groupId;
        this.queryAccount = queryAccount;
        this.queryObjectList = queryObjectList;
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

    public QueryAccount getQueryAccount() {
        return queryAccount;
    }

    public void setQueryAccount(QueryAccount queryAccount) {
        this.queryAccount = queryAccount;
    }

    public ArrayList<QueryObject> getQueryObjectList() {
        return queryObjectList;
    }

    public void setQueryObjectList(ArrayList<QueryObject> queryObjectList) {
        this.queryObjectList = queryObjectList;
    }

    @Override
    public String toString() {
        return "MyGroup{" +
                "num=" + num +
                ", groupId='" + groupId + '\'' +
                ", queryAccount=" + queryAccount +
                ", queryObjectList=" + queryObjectList +
                '}';
    }
}
