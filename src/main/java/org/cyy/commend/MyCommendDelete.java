package org.cyy.commend;

import net.mamoe.mirai.console.command.CommandSender;
import net.mamoe.mirai.console.command.java.JSimpleCommand;
import org.cyy.Plugin;

import java.io.File;

public final class MyCommendDelete extends JSimpleCommand {

    public static final MyCommendDelete INSTANCE = new MyCommendDelete();

    private MyCommendDelete() {
        super(Plugin.INSTANCE, "delete", new String[]{"删除"}, Plugin.INSTANCE.getParentPermission());
        this.setDescription("这是删除文件命令");
    }

    @Handler
    public void onCommand(CommandSender sender,String filePath, String name) {

        try {
            File file = new File(filePath+"/"+name + ".properties");
            if(file.exists()){
                file.delete();
                sender.getSubject().sendMessage("删除成功");
            }else{
                sender.getSubject().sendMessage("文件不存在");
            }

        } catch (Exception e) {
            sender.getSubject().sendMessage(e.getMessage());
        }
    }
}
