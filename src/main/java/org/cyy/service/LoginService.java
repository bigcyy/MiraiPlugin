package org.cyy.service;

import net.mamoe.mirai.contact.Group;
import org.cyy.bean.QueryAccount;
import org.cyy.config.MyPluginConfig;
import org.cyy.dao.QueryAccountDao;
import org.cyy.exception.BaseCookieIsErrorException;
import org.cyy.exception.GroupFileNotFindException;
import org.cyy.redis.RedislUtil;
import org.cyy.simulation.LoginSimulation;

import java.io.IOException;

//1.获取验证码图片向用户发送验证码图片
//2.获取用户名和密码
//3.获取html中的重复提交标识
//4.接收验证码
//5.submit
public class LoginService {
    private final LoginSimulation simulationRequest = new LoginSimulation();
    private final QueryAccountDao queryAccountDao = new QueryAccountDao();

    /**
     * 开始登录方法
     * @param group 触发的群
     * @throws IOException 抛出该异常，说明baseCookie配置文件未找到
     * @throws BaseCookieIsErrorException 抛出该异常表示baseCookie不存在或者已经过期
     */
    public void beginLogin(Group group) throws IOException, BaseCookieIsErrorException {
        String baseCookie = MyPluginConfig.baseCookie;  //从配置文件获取baseCookie
        String loginResubmitFlag = simulationRequest.getLoginResubmitFlag(baseCookie);
        RedislUtil.set(MyPluginConfig.loginResubmitFlagKey,loginResubmitFlag);   //将重复提交标识设置到redis中，提交表单时方便取出
        simulationRequest.getAndSendCode(baseCookie,group);
    }

    /**
     * 接收验证码并提交表单，发送信息应该在plugin但耦合进了service，总之就是懒得改了
     * @author cyy
     * @date 2021/10/3019:39
     *
     */
    public boolean receiveCodeAndSubmit(Group group,String code) throws IOException, GroupFileNotFindException{
        String baseCookie = MyPluginConfig.baseCookie;
        String resubmitFlag = RedislUtil.get(MyPluginConfig.loginResubmitFlagKey);
        String groupId = String.valueOf(group.getId());
        QueryAccount queryAccount = queryAccountDao.getQueryAccountByFile(groupId);

        String loginCookie = simulationRequest.submitLoginForm(queryAccount,baseCookie, resubmitFlag, code);
        if(loginCookie == null){
            return false;
        }
        new Thread(()->{
            try {
                queryAccountDao.updateLoginCookie(groupId,loginCookie);
            } catch (GroupFileNotFindException e) {
                e.printStackTrace();
            }
        }).start();
        return true;
    }

}
