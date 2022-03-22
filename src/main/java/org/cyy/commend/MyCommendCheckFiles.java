package org.cyy.commend;

import net.mamoe.mirai.console.command.CommandSender;
import net.mamoe.mirai.console.command.java.JSimpleCommand;
import org.cyy.Plugin;

import java.io.File;
import java.util.Objects;

/**
 * @author CYY
 * @date 2022年03月20日 上午12:46
 * @description 查看某一文件夹的指令，用于查询配置文件，在聊天窗口或者命令行发送：/查看文件夹 文件名
 */
public final class MyCommendCheckFiles extends JSimpleCommand {

    public static final MyCommendCheckFiles INSTANCE = new MyCommendCheckFiles();

    private MyCommendCheckFiles() {
        super(Plugin.INSTANCE, "checkFiles", new String[]{"查看文件夹"}, Plugin.INSTANCE.getParentPermission());
        this.setDescription("这是查看文件夹命令");
    }

    @Handler
    public void onCommand(CommandSender sender,String filePath) {
        File file = new File(filePath);
        StringBuilder ans = new StringBuilder();
        if(file.isDirectory()){
            String[] list = file.list();
            assert list != null;
            if(list.length == 0){
                Objects.requireNonNull(sender.getSubject()).sendMessage("文件夹为空");
            }
            for (String s : list) {
                ans.append(s).append("\n");
            }
            Objects.requireNonNull(sender.getSubject()).sendMessage(ans.toString());
        }
    }
}
