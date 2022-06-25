package org.cyy.job;

import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.message.MessageReceipt;
import net.mamoe.mirai.message.data.Message;
import org.cyy.Plugin;
import org.cyy.config.MyPluginConfig;
import org.cyy.exception.AuthIsNullException;
import org.cyy.exception.GroupFileNotFindException;
import org.cyy.service.SignService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class AutoToSignJob implements Job {
    @Override
    public void execute(JobExecutionContext context) {

        System.out.println("--------------正在执行自动催签到任务！！！--------------------");
        //service层
        SignService signService = new SignService();
        File FileGroup = new File("group");
        File[] files = FileGroup.listFiles();

        for (int i = 0; i < Objects.requireNonNull(files).length; i++) {
            String name = files[i].getName();
            String[] id = name.split("\\.");
            Group group = Plugin.MY_BOT.getGroup(Long.parseLong(id[0]));
            if(group != null){
                //group.sendMessage("正在测试功能：主动发送未签到消息");
                //判断群是否授权
                ArrayList<Message> messages = null;
                try {
                    messages = signService.makeSignMessages(group);
                    for (Message message:messages) {
                        MessageReceipt<Group> groupMessageReceipt = group.sendMessage(message);
                        if (!message.contentToString().contains("今日已全部签到")) {
                            //除了今日已经签到的信息其他都自动撤回
                            groupMessageReceipt.recallIn(MyPluginConfig.recallTime);
                        }
                    }
                } catch (IOException ioException){
                    group.sendMessage("出现意外啦！再试一次吧，多次出现问题，请及时联系：1597081640");
                    ioException.printStackTrace();
                }catch (AuthIsNullException authIsNullException){
                    group.sendMessage("抱歉啦需要重新登录，请@我回复（登录）进行登录");
                    authIsNullException.printStackTrace();
                }catch (GroupFileNotFindException groupFileNotFindException){
                    group.sendMessage("抱歉啦没有该群的配置文件！请联系:1597081640");
                    groupFileNotFindException.printStackTrace();
                }
            }
        }

    }
}
