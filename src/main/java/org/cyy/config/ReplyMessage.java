package org.cyy.config;

/**
 * @author cyy
 * @date 2022/2/18 11:23
 * @description 整个插件系统所以回复信息的配置文件
 */
public interface ReplyMessage {
    /*
        异常回应信息
     */
    String authExceptionMsg = "抱歉啦,需要重新登录，请@我回复（登录）进行登录.";
    String ioExceptionMsg = "出现意外啦,再试一次吧，多次出现问题请及时联系：1597081640.";
    String groupFileExceptionMsg = "抱歉啦,没有该群的配置文件请联系:1597081640添加吧.";

    /*
        签到相关信息
     */
    String allIsSignMsg = "今日已全部签到";

    /*
        登录相关
     */
    String baseCookieIsErrorMsg = "这个cookie已经使用太久啦,联系我1597081640进行更换吧！";
    String pushToLoginQueueMsg = "共有%d位在登录队列中，您在第%d位,当到您登录时，系统会发送验证码给您，请等待.";
    String notIsFrontMsg = "请等待他人操作完毕后再进行操作.";
    String isFrontMsg = "请您回复验证码进行登录";
    String pleaseSendCode = "请回复验证码\n(格式为:\"验证码:xxxx\")\n";
    String loginTimeOutMsg = "超时未操作！";
    String loginOkMsg = "登录成功！";
    String loginNotOkMsg = "登录失败！请重新登录，多次失败请联系我1597081640";

    /*
        使用方法
     */
    String useMethodMsg ="使用方法:\n" +
            "    联系(qq:1597081640,下文代指我),添加配置文件，之后便可使用。\n" +
            "    机器人支持手动和定时自动催签到，①手动:直接在群里发送\"催签到\"即可，机器人便会at未签到同学，②定时自动:机器人会在固定时间{9.00,10.00,11.00,12.00,14.00}自动at未签到同学。\n" +
            "  关于at失败:请先检查同学备注是否为自己的真实姓名(包含真实姓名即可)，如果备注正确仍然at失败请截图并将同学的备注发给我。\n" +
            "    关于cookie失效:根据提示at机器人回复登录二字，然后根据机器人回复的验证码图片，回复\"验证码:xxxx\"，注意回复格式。登录成功后便可以正常使用了。\n" +
            "    最后，需要添加其他功能或者定制机器人请联系我！\n";
    /*
        推送信息相关
     */
    String childGroupNotFindMsg = "机器人未添加该群%s,请先邀请机器人进群再添加！";
    String childPersonNotFindMsg = "机器人未添加该好友%s,请先让该用户添加机器人为好友再添加！";

    String repeatAddChildGroupMsg = "请勿重复添加子群！";
    String repeatAddChildPersonMsg = "请勿重复添加个人！";
    String addChildSuccessMsg = "添加成功！";

    String deleteChildUnSuccessMsg = "删除失败，未配置任何子群和个人！";
    String deleteChildSuccessMsg = "删除成功!";

    String noChildMsg = "还没有子群和个人。";
    String allChildMsg = "可以推送的目标有:\n群:\n%s好友:\n%s";
    String rePushMsg = "正在推送，请勿重复发送指令.";
    String beginPushMsg = "ok,请发送消息，发送完成后回复“结束推送”,结束转发";
    String endPushIsOkMsg = "ok";
    String pushMsgPrefix = "来自群(%s)的消息:\n";
    String pushMsgOkMsg = "已经向以下群：\n%s成功推送消息";
    String pushMsgNotOkMsg = "机器人未加入一下群,推送失败：\n";
    /*
        缓存相关
     */
    String cacheIsOk = "缓存成功";
    String cacheNotOk = "以下成员缓存失败:%s";
}
