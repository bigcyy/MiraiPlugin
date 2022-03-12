package org.cyy.service;

import net.mamoe.mirai.contact.ContactList;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.NormalMember;
import net.mamoe.mirai.message.data.*;
import org.cyy.Plugin;
import org.cyy.bean.MyGroup;
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
        //long begin = System.currentTimeMillis();
        ArrayList<Message> messages = new ArrayList<>();
        //通过配置文件获取被签到的群的bean对象，后续可能会用数据库代替
        MyGroup myGroup = groupDao.getMyGroupByFile(String.valueOf(group.getId()));
        QueryAccount queryAccount = myGroup.getQueryAccount();
        //向健康打卡系统发送请求获取验证,此处会抛出authIsNullException,或者ioException
        String authorization = simulationRequest.getAuthorization(queryAccount);
        //设置验证，以便于后面获取未签到名单时使用
        queryAccount.setAuthorization(authorization);
        int status = 1;//需要查询的状态
        List<String> list = simulationRequest.getNameByStatus(queryAccount, myGroup.getQueryObjectList(), status);
        int count = 0;
        //名单中没有人，表明全部完成签到
        if(list.size() == 0){
            PlainText plainText = new PlainText(ReplyMessage.allIsSignMsg);
            messages.add(plainText);
            return messages;
        }

        MessageChain msg;                                               //信息链
        At at;                                                           //at信息
        MessageChainBuilder builder = new MessageChainBuilder();         //信息构造

        builder = builder.append("同学们签到啦！！！");
        ContactList<NormalMember> members = group.getMembers(); //获取qq群中的所有成员
        StringBuilder unSuccessAtName = new StringBuilder();   //未成功at的人
        //对每个未签到的姓名进行遍历
        for (int i = 0; i < list.size(); i++) {
            String name = list.get(i);
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
            if(atAimList.size() == 0){
                //如果at目标集合长度为0，则表明该人未成功@
                unSuccessAtName.append(name).append(" ");
            }else if(atAimList.size() > 1){
                //如果at目标大于1，则为重名或者名字包含关系（陈红，陈红燕，“陈红燕”包含了“陈红”，contain判断都会为true）这类情况
                List<String> signList = simulationRequest.getNameByStatus(queryAccount,myGroup.getQueryObjectList(),(status==2?1:2));
                //已经签到的集合加上未签到的为总的
                //未签到集合与签到集合合并为总名称表
                signList.addAll(list);
                NormalMember normalMember = selectTrueAtAim(qqName, atAimList, (ArrayList<String>) signList);
                at = new At(normalMember.getId());
                builder = builder.append(at);
                count++;
            }else{
                at = new At(atAimList.get(0).getId());
                builder = builder.append(at);
                count++;
            }
        }

        msg = builder.build();
        messages.add(msg);
        PlainText plainText = new PlainText("未签到总人数:" + list.size() + ",成功@人数:" + count + (list.size() == count ? "":("，@失败："+ unSuccessAtName)));
        messages.add(plainText);

        //long end = System.currentTimeMillis();
        //PlainText timeText = new PlainText("老催签到所用时间"+(end-begin));
        //messages.add(timeText);

        return messages;
    }

    @Deprecated
    public ArrayList<Message> makeSignMessagesFromFile(Group group) throws GroupFileNotFindException, IOException, AuthIsNullException {
        long begin = System.currentTimeMillis();
        MyGroup myGroup = groupDao.getMyGroupByFile(String.valueOf(group.getId()));
        ContactList<NormalMember> members = group.getMembers(); //获取qq群中的所有成员
        QueryAccount queryAccount = myGroup.getQueryAccount();
        String authorization = simulationRequest.getAuthorization(queryAccount);    //向健康打卡系统发送请求获取验证,此处会抛出authIsNullException,或者ioException
        queryAccount.setAuthorization(authorization);       //设置验证，以便于后面获取未签到名单时使用
        HashMap<String, String> map = this.listToMap(myGroup, members);
        ArrayList<Message> messages = makeSignMessages(myGroup, map);   //制作消息

        long end = System.currentTimeMillis();  //记录结束时间
        PlainText timeText = new PlainText("新催签到所用时间"+(end-begin));
        messages.add(timeText);
        return messages;
    }
    /**
     * 从redis中获取映射文件并制作催签到信息
     * @param group 待催签到的群
     * @return  催签到信息
     * @throws AuthIsNullException  验证失败，需要重新登录
     * @throws IOException  发送请求失败
     */
    @Deprecated
    public ArrayList<Message> makeSignMessagesFromRedis(Group group) throws AuthIsNullException, IOException {
        long begin = System.currentTimeMillis();    //记录当前时间
        long groupId = group.getId();
        /*
            制作key
         */
        String myGroupKey = String.format(MyPluginConfig.groupKey,groupId);       //group的key
        String mapKey = String.format(MyPluginConfig.mappingMapKey,groupId);  //map的key
        /*
            从redis获取对象
         */
        MyGroup myGroup = (MyGroup) RedislUtil.getJsonObj(myGroupKey, MyGroup.class);
        HashMap<String,String> map = (HashMap<String, String>) RedislUtil.getJsonObj(mapKey, HashMap.class);

        ArrayList<Message> messages = makeSignMessages(myGroup, map);   //制作消息

        long end = System.currentTimeMillis();  //记录结束时间
        PlainText timeText = new PlainText("内存缓存后所用时间"+(end-begin));
        messages.add(timeText);
        return messages;
    }

    /**
     * 从Cache中获取映射文件并制作催签到信息
     * @param group 待催签到的群
     * @return  催签到信息
     * @throws AuthIsNullException  验证失败，需要重新登录
     * @throws IOException  发送请求失败
     */
    @Deprecated
    public ArrayList<Message> makeSignMessagesFromCache(Group group) throws AuthIsNullException, IOException {
        long begin = System.currentTimeMillis();    //记录当前时间
        /*
            制作key
         */
        long groupId = group.getId();
        String myGroupKey = String.format(MyPluginConfig.groupKey,groupId);       //group的key
        String mapKey = String.format(MyPluginConfig.mappingMapKey,groupId);  //map的key
        /*
            从上下文获取对象
         */
        MyGroup myGroup = (MyGroup) Plugin.contextMap.get(myGroupKey);
        HashMap<String,String> map = (HashMap<String, String>) Plugin.contextMap.get(mapKey);
        ArrayList<Message> messages = makeSignMessages(myGroup, map);

        long end = System.currentTimeMillis();  //记录结束时间
        PlainText timeText = new PlainText("内存缓存后所用时间"+(end-begin));
        messages.add(timeText);

        return messages;
    }

    /**
     * 从映射map中制作催签到信息
     * @param myGroup   群配置文件
     * @param map   映射关系
     * @return  催签到的信息
     * @throws AuthIsNullException  验证失败异常，需要重新登录
     * @throws IOException  发送请求异常
     */
    @Deprecated
    private ArrayList<Message> makeSignMessages(MyGroup myGroup,HashMap<String,String> map) throws AuthIsNullException, IOException {

        ArrayList<Message> messages = new ArrayList<>();    //信息集合

        QueryAccount queryAccount = myGroup.getQueryAccount();
        String authorization = simulationRequest.getAuthorization(queryAccount);    //向健康打卡系统发送请求获取验证,此处会抛出authIsNullException,或者ioException
        queryAccount.setAuthorization(authorization);       //设置验证，以便于后面获取未签到名单时使用
        int status = 2; //需要查询的状态
        List<String> list = simulationRequest.getNameByStatus(queryAccount, myGroup.getQueryObjectList(), status);  //查询到姓名集合

        //名单中没有人，表明全部完成签到
        if(list.size() == 0){
            PlainText plainText = new PlainText(ReplyMessage.allIsSignMsg);
            messages.add(plainText);
            return messages;
        }

        /*
            构造信息
         */
        MessageChain msg;                                               //信息链
        MessageChainBuilder builder = new MessageChainBuilder();         //信息构造

        builder = builder.append("同学们签到啦！！！");
        StringBuilder unSuccessAtName = new StringBuilder();   //未成功at的人
        int count = 0;
        for(String name:list){
            //对每个未签到的姓名进行遍历
            String nameQQ = map.get(name);
            if(nameQQ!=null) {
                builder.append(new At(Long.parseLong(nameQQ)));
                count++;
            }else{
                unSuccessAtName.append(name).append(" ");
            }
        };
        msg = builder.build();
        messages.add(msg);
        PlainText plainText = new PlainText("未签到总人数:" + list.size() + ",成功@人数:" + count + (list.size() == count ? "":("，@失败："+ unSuccessAtName)));
        messages.add(plainText);
        return messages;
    }

    /**
     * 更新映射到redis
     * @param group 待映射的群
     * @return  映射成功和映射失败的信息
     * @throws GroupFileNotFindException    群配置文件未找到
     * @throws AuthIsNullException      验证失败，需要重新登录
     * @throws IOException      发送请求出错
     */
    @Deprecated
    public ArrayList<Message> updateGroupToRedis(Group group) throws GroupFileNotFindException, AuthIsNullException, IOException {

        long groupId = group.getId();
        MyGroup myGroup = groupDao.getMyGroupByFile(String.valueOf(groupId));
        QueryAccount queryAccount = myGroup.getQueryAccount();
        /*
            将group缓存到redis
         */
        String myGroupKey = String.format(MyPluginConfig.groupKey,groupId);       //group的key
        RedislUtil.setJsonObj(myGroupKey,myGroup); //存入redis
        /*
            将映射map缓存到redis
         */
        String authorization = simulationRequest.getAuthorization(queryAccount);    //向健康打卡系统发送请求获取验证,此处会抛出authIsNullException,或者ioException
        queryAccount.setAuthorization(authorization);       //设置验证，以便于后面获取未签到名单时使用
        HashMap<String, String> map = listToMap(myGroup, group.getMembers());  //映射到map
        String unSuccess = map.remove("unSuccess");    //从映射map中取出并删除未成功映射的名单字符串
        String mapKey = String.format(MyPluginConfig.mappingMapKey,groupId);  //制作key
        RedislUtil.setJsonObj(mapKey,map);      //将map存入redis
        ArrayList<Message> message = new ArrayList<>();

        if(unSuccess != null){
            message.add(new PlainText(String.format(ReplyMessage.cacheNotOk,unSuccess)));   //如果存在缓存不成功则构造回复信息
        }
        message.add(new PlainText(ReplyMessage.cacheIsOk));     //构造缓存成功的信息
        return message; //返回信息集合



    }

    /**
     * 更新映射到上下文
     * @param group 待映射的群
     * @return  映射成功和映射失败的信息
     * @throws GroupFileNotFindException    群配置文件未找到
     * @throws AuthIsNullException      验证失败，需要重新登录
     * @throws IOException      发送请求出错
     */
    @Deprecated
    public ArrayList<Message> updateGroupToCache(Group group) throws GroupFileNotFindException, AuthIsNullException, IOException {
        //通过配置文件获取被签到的群的bean对象，后续可能会用数据库代替
        long groupId = group.getId();
        MyGroup myGroup = groupDao.getMyGroupByFile(String.valueOf(groupId));
        QueryAccount queryAccount = myGroup.getQueryAccount();
        /*
            将group缓存到上下文
         */
        String myGroupKey = String.format(MyPluginConfig.groupKey,groupId);       //group的key
        Plugin.contextMap.put(myGroupKey,myGroup);                              //存入上下文

        /*
            将映射map缓存到上下文
         */
        String authorization = simulationRequest.getAuthorization(queryAccount);    //向健康打卡系统发送请求获取验证,此处会抛出authIsNullException,或者ioException
        queryAccount.setAuthorization(authorization);       //设置验证，以便于后面获取未签到名单时使用
        HashMap<String, String> map = listToMap(myGroup, group.getMembers());  //映射到map
        String unSuccess = map.remove("unSuccess");    //从映射map中取出并删除未成功映射的名单字符串
        String mapKey = String.format(MyPluginConfig.mappingMapKey,groupId);
        Plugin.contextMap.put(mapKey,map);  //存入内存
        ArrayList<Message> message = new ArrayList<>();

        if(unSuccess != null){
            message.add(new PlainText(String.format(ReplyMessage.cacheNotOk,unSuccess)));   //如果存在缓存不成功则构造回复信息
        }
        message.add(new PlainText(ReplyMessage.cacheIsOk));     //构造缓存成功的信息
        return message; //返回信息集合
    }

    /**
     * 选择出正确的at对象
     * @param qqName qq昵称
     * @param atAimList at目标集合
     * @param allList 所有查询到的集合
     * @return 一个at目标
     */
    private NormalMember selectTrueAtAim(String qqName, ArrayList<NormalMember> atAimList, ArrayList<String> allList) {

        //筛选出含有特定子串（名字）的字符串（名字）
        for (int i = 0; i < allList.size(); i++) {
            if(!qqName.contains(allList.get(i))){
                allList.remove(i);
            }
        }
        //对其进行排序
        Collections.sort(allList, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o2.length() - o1.length();
            }
        });

        for (int i = 0; i < allList.size(); i++) {
            System.out.println(allList.get(i));
        }
        //对atAimList进行筛选
        for (int i = 0; i < allList.size(); i++) {
            for (int j = 0; j < atAimList.size(); j++) {
                if(atAimList.get(j).getNameCard().contains(allList.get(i))){
                    atAimList.remove(j);
                    break;
                }
            }
        }
        return atAimList.get(0);
    }

    /**
     * 将list映射到map
     * @param myGroup  查询所需的群配置
     * @param members   群中的所有成员
     * @return  映射文件，k:unSuccess,v:未成功映射的成员，k:realName,v:qq号
     * @throws IOException  抛出该异常说明发送请求异常
     */
    @Deprecated
    private HashMap<String,String> listToMap(MyGroup myGroup,ContactList<NormalMember> members) throws IOException {

        QueryAccount queryAccount = myGroup.getQueryAccount();
        List<String> listSign = simulationRequest.getNameByStatus(queryAccount, myGroup.getQueryObjectList(), 1);
        List<String> listNoSign = simulationRequest.getNameByStatus(queryAccount, myGroup.getQueryObjectList(), 2);
        listSign.addAll(listNoSign);    //两个集合合并
        //先对集合进行按照姓名长度排序，之后遍历到群昵称后，将其从集合中移除，防止出现名字包含关系（陈红，陈红燕，“陈红燕”包含了“陈红”，contain判断都会为true）这类情况
        listSign.sort((o1, o2) -> o2.length()-o1.length());
        HashMap<String,String> map = new HashMap<>();   //map用于存储映射
        //获取真实名字集合的迭代器
        Iterator<String> realNameIterator = listSign.iterator();
        while(realNameIterator.hasNext()){
            String realName = realNameIterator.next();  //获取真实名字
            Iterator<NormalMember> iterator = members.stream().iterator();  //获取群成员集合的迭代器
            while(iterator.hasNext()){
                NormalMember member = iterator.next();  //获取群成员
                if(member.getNameCard().contains(realName)){    //如果满足映射关系
                    map.put(realName, String.valueOf(member.getId()));  //集成在map中
                    iterator.remove();  //将群成员从集合移除
                    realNameIterator.remove();  //将真实姓名从其集合中移除
                }
            }
        }

        if(listSign.size() ==0 ){
            //如果全部缓存成功则直接返回map
            return map;
        }
        /*
            对未缓存成功的进行处理
         */
        StringBuilder unSuccess = new StringBuilder();   //未成功映射的人
        listSign.forEach((unSuccessName)->{
            unSuccess.append(unSuccessName).append(" ");
        });
        map.put("unSuccess", String.valueOf(unSuccess));
        return map;
    }


}
