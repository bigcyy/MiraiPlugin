package org.cyy.commend;

import net.mamoe.mirai.console.command.CommandSender;
import net.mamoe.mirai.console.command.java.JSimpleCommand;
import org.cyy.Plugin;
import org.cyy.utils.YmlAndPropUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

public final class MyCommendCheck extends JSimpleCommand {

    public static final MyCommendCheck INSTANCE = new MyCommendCheck();

    private MyCommendCheck() {
        super(Plugin.INSTANCE, "check", new String[]{"查看文件"}, Plugin.INSTANCE.getParentPermission());
        this.setDescription("这是查看文件命令");
    }

    @Handler
    public void onCommand(CommandSender sender,String filePath, String name) {
        File file = new File(name);
        InputStream is = null;
        try {
            is = new FileInputStream(filePath+"/"+name + ".properties");
            Properties p = new Properties();
            p.load(is);
            Set<String> setKey = p.stringPropertyNames();
            if(!setKey.isEmpty()) {
                String msg = "";
                Iterator<String> iterator = setKey.iterator();
                while (iterator.hasNext()) {
                    String next = iterator.next();
                    msg += next + "=" + p.getProperty(next)+"\n";
                }
                if (sender.getUser() != null) {
                    sender.getSubject().sendMessage(msg);
                }
            }else{
                sender.getSubject().sendMessage("未添加属性");
            }

        } catch (Exception e) {
            sender.getSubject().sendMessage(e.getMessage());
        } finally {
            YmlAndPropUtil.closeResource(is,null);
        }
    }
}
