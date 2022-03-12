package org.cyy.commend;

import net.mamoe.mirai.console.command.CommandSender;
import net.mamoe.mirai.console.command.java.JSimpleCommand;
import net.mamoe.mirai.contact.Group;
import org.cyy.Plugin;

import java.io.File;

/**
 * @Auther: cyy
 * @Date: 2021/10/16 19 33
 * @Description:
 */
public final class MyCommendNotice extends JSimpleCommand {

    public static final MyCommendNotice INSTANCE = new MyCommendNotice();

    private MyCommendNotice() {
        super(Plugin.INSTANCE, "notice", new String[]{"通知"}, Plugin.INSTANCE.getParentPermission());
        this.setDescription("这是向用户群通知的命令");
    }

    @Handler
    public void onCommand(CommandSender sender, String msg) {

        File FileGroup = new File("group");
        File[] files = FileGroup.listFiles();

        for (int i = 0; i < files.length; i++) {
            String name = files[i].getName();
            String[] id = name.split("\\.");
            Group group = Plugin.MY_BOT.getGroup(Long.parseLong(id[0]));
            if(group != null){
                group.sendMessage(msg);
            }
        }
    }
}
