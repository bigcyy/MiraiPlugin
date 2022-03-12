package org.cyy.config;

import org.cyy.utils.YmlAndPropUtil;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * @author cyy
 * @date 2022/2/18 18:28
 * @description 插件配置类
 */
public class MyPluginConfig {

    /*
        静态配置文件地址
     */
    public static String redisConfigPath = "base/redis.properties";
    public static String pluginConfigPath = "base/pluginConfig.properties";
    public static String pushMsgConfigFilePath = "group/push/";    //推送信息配置文件夹路径

    /*
        redis，或者map的key
     */
    public static String mappingMapKey = "%d:map";        //群成员映射mapKey，群号:map
    public static String groupKey = "%d:group";           //群配置文件key   群号:group

    /*
        签到相关
     */
    public static boolean isSign;       //是否启用签到功能
    public static boolean isAutoSign;   //是否启动自动催签到
    public static long recallTime = 90 * 1000 * 60;      //撤回消息的时间,毫秒


    /*
        登录相关
     */
    public static boolean isLogin;      //是否启用登录功能
    public static int loginExTime = 60;       //登录超时时间,秒
    public static String loginUserQueueKey = "loginUserQueue";  //登录用户队列的key
    public static String loginUserValue = "%s:%s";  //登录用户的value,群号:qq号
    public static String loginTimerKeyPreFix = "loginTimer:";    //登录计时器的key的前缀,loginTimer:
    public static String loginTimerValue = "loginTimer";    //登录计时器的value
    public static String loginResubmitFlagKey = "loginResubmitFlag";    //重复登录标识key
    public static String baseCookie;    //基本cookie，用于登录功能

    /*
        信息推送相关
     */
    public static boolean isPushMsg;    //是否启用信息推送
    //public static int
    public static String pushMsgTimerKey = "pushMsgTimer:%s:%s";    //推送信息计时器key
    public static int pushMsgExTime = 60;   //推送信息超时
    /*
        其他配置
     */
    public static boolean agreeAddFriend;   //是否同意自动同意添加朋友
    public static boolean agreeAddGroup;    //是否启用自动同意加群
    public static Long master;          //主人qq号
    
    /**
     * 加载配置文件
     */
    public static void load(){
        loadBeforeCreateFile();
        Properties properties = YmlAndPropUtil.loadProperties(MyPluginConfig.pluginConfigPath);
        baseCookie = YmlAndPropUtil.getValue(properties, "baseCookie");
        isAutoSign = Boolean.parseBoolean(YmlAndPropUtil.getValue(properties, "isAutoSign","false"));
        isSign = Boolean.parseBoolean(YmlAndPropUtil.getValue(properties, "isSign","false"));
        isPushMsg = Boolean.parseBoolean(YmlAndPropUtil.getValue(properties, "isAutoNotice","false"));
        isLogin = Boolean.parseBoolean(YmlAndPropUtil.getValue(properties, "isLogin","false"));
        agreeAddFriend = Boolean.parseBoolean(YmlAndPropUtil.getValue(properties,"agreeAddFriend","false"));
        agreeAddGroup = Boolean.parseBoolean(YmlAndPropUtil.getValue(properties,"agreeAddGroup","false"));
        master = Long.valueOf(YmlAndPropUtil.getValue(properties,"maser","1597081640"));
    }

    public static void saveBaseCookieToFile(){
        Properties properties = YmlAndPropUtil.loadProperties(MyPluginConfig.pluginConfigPath);
        YmlAndPropUtil.saveProperties(properties,"baseCookie",baseCookie, MyPluginConfig.pluginConfigPath);
    }
    public static void saveIsAutoSignToFile(){
        Properties properties = YmlAndPropUtil.loadProperties(MyPluginConfig.pluginConfigPath);
        YmlAndPropUtil.saveProperties(properties,"isAutoSign", String.valueOf(isAutoSign), MyPluginConfig.pluginConfigPath);
    }
    public static void saveIsSignToFile(){
        Properties properties = YmlAndPropUtil.loadProperties(MyPluginConfig.pluginConfigPath);
        YmlAndPropUtil.saveProperties(properties,"isSign", String.valueOf(isSign), MyPluginConfig.pluginConfigPath);
    }
    public static void saveIsLoginToFile(){
        Properties properties = YmlAndPropUtil.loadProperties(MyPluginConfig.pluginConfigPath);
        YmlAndPropUtil.saveProperties(properties,"isLogin", String.valueOf(isLogin), MyPluginConfig.pluginConfigPath);
    }
    public static void saveIsPushMsgToFile(){
        Properties properties = YmlAndPropUtil.loadProperties(MyPluginConfig.pluginConfigPath);
        YmlAndPropUtil.saveProperties(properties,"isPushMsg", String.valueOf(isPushMsg), MyPluginConfig.pluginConfigPath);
    }
    public static void saveAgreeAddFriendToFile(){
        Properties properties = YmlAndPropUtil.loadProperties(MyPluginConfig.pluginConfigPath);
        YmlAndPropUtil.saveProperties(properties,"agreeAddFriend", String.valueOf(agreeAddFriend), MyPluginConfig.pluginConfigPath);
    }
    public static void saveAgreeAddGroupToFile(){
        Properties properties = YmlAndPropUtil.loadProperties(MyPluginConfig.pluginConfigPath);
        YmlAndPropUtil.saveProperties(properties,"agreeAddGroup", String.valueOf(agreeAddGroup), MyPluginConfig.pluginConfigPath);
    }


    /**
     * 加载前，判断是否存在文件，没有则创建文件
     */
    private static void loadBeforeCreateFile(){
        File redis = new File(MyPluginConfig.redisConfigPath);
        File config = new File(MyPluginConfig.pluginConfigPath);
        File group = new File("group");
        if(!group.exists()){
            group.mkdir();
        }
        if(!redis.exists()){
            try {
                redis.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(!config.exists()){
            try {
                config.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
