package org.cyy.service;

import net.mamoe.mirai.contact.ContactList;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.NormalMember;
import net.mamoe.mirai.message.data.*;
import org.cyy.Plugin;
import org.cyy.bean.MyGroup;
import org.cyy.bean.PersonModel;
import org.cyy.bean.QueryAccount;
import org.cyy.config.MyPluginConfig;
import org.cyy.config.ReplyMessage;
import org.cyy.dao.MyGroupDao;
import org.cyy.exception.AuthIsNullException;
import org.cyy.exception.GroupFileNotFindException;
import org.cyy.redis.RedislUtil;
import org.cyy.simulation.SignSimulation;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author cyy
 * @date 2022/2/21 00:30
 * @description 签到服务类，封装了有关催签到功能的方法
 */
public class SignService {

    private final MyGroupDao groupDao = new MyGroupDao();
    private final SignSimulation simulationRequest = new SignSimulation();

    /**
     * 制作未签到名单,旧方法，测试还挺快，就不改了
     * @param group qq群
     * @return 返回多条信息，如果一切正常则返回未签到的信息
     * @throws IOException 抛出Io异常，说明发送网络请求出现问题
     * @throws AuthIsNullException 抛出该异常，说明cookie已经失效了，需要重新登录
     * @throws GroupFileNotFindException 抛出该异常，说明群配置文件未找到
     *
     */
    public ArrayList<Message> makeSignMessages(Group group) throws IOException, AuthIsNullException, GroupFileNotFindException{
        ArrayList<Message> messages = new ArrayList<>();
        //通过配置文件获取被签到的群的bean对象，后续可能会用数据库代替
        MyGroup myGroup = groupDao.getMyGroupByFile(String.valueOf(group.getId()));
        QueryAccount queryAccount = myGroup.getQueryAccount();
        //向健康打卡系统发送请求获取验证,此处会抛出authIsNullException,或者ioException
        String authorization = simulationRequest.getAuthorization(queryAccount);
        //设置验证，以便于后面获取未签到名单时使用
        queryAccount.setAuthorization(authorization);
        int status = 2;//需要查询的状态
        List<PersonModel> personModels = simulationRequest.getNameByStatus(queryAccount, myGroup.getQueryObjectList(), status);
        int count = 0;
        //名单中没有人，表明全部完成签到
        if(personModels.size() == 0){
            PlainText plainText = new PlainText(ReplyMessage.allIsSignMsg);
            messages.add(plainText);
            return messages;
        }

        MessageChain msg;                                               //信息链
        At at;                                                           //at信息
        MessageChainBuilder builder = new MessageChainBuilder();         //信息构造

        builder.append("同学们签到啦！！！");
        ContactList<NormalMember> members = group.getMembers(); //获取qq群中的所有成员
        StringBuilder unSuccessAtName = new StringBuilder();   //未成功at的人
        //对每个未签到的姓名进行遍历
        for (PersonModel personModel : personModels) {

            String name = personModel.getName();
            String studentId = personModel.getStudentId();

            ArrayList<NormalMember> atAimList = new ArrayList<>();
            String qqName = null;
            //与每个群成员群昵称进行匹配
            for (NormalMember normalMember : members) {
                qqName = normalMember.getNameCard();
                if (qqName.contains(name)) {
                    atAimList.add(normalMember);
                }
            }
            /*每个未签到的姓名，对查询出来的结果进行判断，一共有三种情况
             *情况1：atAimList为0，群成员没有改备注
             *情况2：如果大于1，则为重名或者名字包含关系（陈红，陈红燕，“陈红燕”包含了“陈红”，contain判断都会为true）这类情况
             *情况3：等于1，查询到唯一一位
             */
            if (atAimList.size() == 0) {
                //如果at目标集合长度为0，则表明该人未成功@
                unSuccessAtName.append(name).append(" ");
            } else if (atAimList.size() > 1) {
                //如果at目标大于1，则为重名或者名字包含关系（陈红，陈红燕，“陈红燕”包含了“陈红”，contain判断都会为true）这类情况
                //List<PersonModel> signList = simulationRequest.getNameByStatus(queryAccount,myGroup.getQueryObjectList(),(status==2?1:2));   //获取已经签到的同学的名单

                //已经签到的集合加上未签到的为总的名单
                //未签到集合与签到集合合并为总名单
                //signList.addAll(list);
                NormalMember normalMember = selectTrueAtAim(studentId, atAimList);
                if(normalMember == null){
                    //临时解决
                    unSuccessAtName.append(name).append(studentId).append("(学号有误) ");
                }else {
                    at = new At(normalMember.getId());
                    builder.append(at);
                    count++;
                }
            } else {
                at = new At(atAimList.get(0).getId());
                builder.append(at);
                count++;
            }
        }

        msg = builder.build();
        messages.add(msg);
        PlainText plainText = new PlainText("未签到总人数:" + personModels.size() + ",成功@人数:" + count + (personModels.size() == count ? "":("，@失败："+ unSuccessAtName)));
        messages.add(plainText);

        //long end = System.currentTimeMillis();
        //PlainText timeText = new PlainText("老催签到所用时间"+(end-begin));
        //messages.add(timeText);

        return messages;
    }


    /**
     * 选择出正确的at对象,通过学号筛选
     * @param studentId 待at的学号
     * @param atAimList at目标集合
     * @return 一个at目标
     */
    private NormalMember selectTrueAtAim(String studentId, ArrayList<NormalMember> atAimList){
//        System.out.println(studentId);
        AtomicReference<NormalMember> normalMember = new AtomicReference<>();
        atAimList.forEach(member -> {
//            System.out.println(member.getNameCard());
            if(member.getNameCard().contains(studentId)){
                normalMember.set(member);
            }
        });

        return normalMember.get();
    }

}
