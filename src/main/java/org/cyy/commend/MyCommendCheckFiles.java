package org.cyy.commend;

import net.mamoe.mirai.console.command.CommandSender;
import net.mamoe.mirai.console.command.java.JSimpleCommand;
import org.cyy.Plugin;

import java.io.File;


public final class MyCommendCheckFiles extends JSimpleCommand {

    public static final MyCommendCheckFiles INSTANCE = new MyCommendCheckFiles();

    private MyCommendCheckFiles() {
        super(Plugin.INSTANCE, "checkFiles", new String[]{"查看文件夹"}, Plugin.INSTANCE.getParentPermission());
        this.setDescription("这是查看文件夹命令");
    }

    @Handler
    public void onCommand(CommandSender sender,String filePath) {
        File file = new File(filePath);
        String ans = "";
        if(file.isDirectory()){
            String[] list = file.list();
            if(list.length == 0){
                sender.getSubject().sendMessage("文件夹为空");
            }
            for (int i = 0; i < list.length; i++) {
                ans+=list[i]+"\n";
            }
            sender.getSubject().sendMessage(ans);
        }
    }
}
