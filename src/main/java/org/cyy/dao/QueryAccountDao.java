package org.cyy.dao;

import org.cyy.bean.QueryAccount;
import org.cyy.config.MyPluginConfig;
import org.cyy.exception.GroupFileNotFindException;
import org.cyy.utils.YmlAndPropAndIOUtil;

import java.util.Properties;

public class QueryAccountDao {

    /**
     * 通过群配置文件获取查询账户
     * @param groupNo 群号
     * @return 返回QueryAccount实例
     * @throws GroupFileNotFindException 抛出该异常，说明群配置文件未找到
     */
    public QueryAccount getQueryAccountByFile(String groupNo) throws GroupFileNotFindException {
        QueryAccount queryAccount = new QueryAccount();
        Properties properties = YmlAndPropAndIOUtil.loadProperties("group/" + groupNo + ".properties");
        //如果配置文件不存在直接返回null
        if (properties == null) {
            throw new GroupFileNotFindException();
        }
        return this.getQueryAccountByProp(properties);
    }

    /**
     * 通过properties获取查询账户
     * @param properties 加载后的properties
     * @return QueryAccount实例
     */
    public QueryAccount getQueryAccountByProp(Properties properties) {
        QueryAccount queryAccount = new QueryAccount();
        queryAccount.setLoginCookie(properties.getProperty("loginCookie"));
        queryAccount.setCollegeNo(properties.getProperty("collegeNo"));
        queryAccount.setUsername(properties.getProperty("username"));
        queryAccount.setPassword(properties.getProperty("password"));
        return queryAccount;
    }

    /**
     * 更新loginCookie
     * @param groupId 群id
     * @param loginCookie 获取的登录后的cookie
     * @throws GroupFileNotFindException 抛出该异常，说明群配置文件未找到
     */
    public void updateLoginCookie(String groupId,String loginCookie) throws GroupFileNotFindException {
        String path = "group/" + groupId + ".properties";
        //读取文件
        Properties p = YmlAndPropAndIOUtil.loadProperties(path);
        if(p==null){
            throw new GroupFileNotFindException();
        }
        loginCookie = MyPluginConfig.baseCookie+";"+loginCookie;
        //设置cookie保存文件
        YmlAndPropAndIOUtil.saveProperties(p,"loginCookie",loginCookie,path);
    }

    /**
     * 通过群号和获取用户名和密码
     * @param groupNo 群号
     * @return 查询账户
     * @throws GroupFileNotFindException 群配置文件未找到
     */
    @Deprecated
    public QueryAccount getQueryAccountUsernameAndPassword(String groupNo) throws  GroupFileNotFindException{

        QueryAccount queryAccount = new QueryAccount();
        //加载文件
        Properties prop = YmlAndPropAndIOUtil.loadProperties("group/" + groupNo + ".properties");
        if(prop == null){
            throw new GroupFileNotFindException();
        }
        //读取用户名和密码
        String username = prop.getProperty("username");
        String password = prop.getProperty("password");

        queryAccount.setUsername(username);
        queryAccount.setPassword(password);
        queryAccount.setCollegeNo(groupNo);

        return queryAccount;
    }


}
