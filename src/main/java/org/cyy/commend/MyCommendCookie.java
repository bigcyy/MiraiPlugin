package org.cyy.commend;

import net.mamoe.mirai.console.command.CommandSender;
import net.mamoe.mirai.console.command.java.JSimpleCommand;
import org.cyy.Plugin;
import org.cyy.utils.YmlAndPropAndIOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Properties;

public final class MyCommendCookie extends JSimpleCommand {

    public static final MyCommendCookie INSTANCE = new MyCommendCookie();

    private MyCommendCookie() {
        super(Plugin.INSTANCE, "cookie", new String[]{"设置cookie"}, Plugin.INSTANCE.getParentPermission());
        this.setDescription("这是直接设置cookie命令");
    }

    @Handler
    public void onCommand(CommandSender sender, String zy, String message) {

        InputStream is = null;
        FileOutputStream out = null;
        try {
            File file = new File("college/"+zy + ".properties");
            //写入配置文件
            if (!file.exists()) {
                file.createNewFile();
                sender.getSubject().sendMessage("文件创建成功");
            }
            is = new FileInputStream("college/"+zy + ".properties");
            Properties p = new Properties();
            p.load(is);

            p.setProperty("cookie", message);
            out = new FileOutputStream("college/"+zy + ".properties");
            p.store(out, null);

            //写入后给予反馈
            if (sender.getUser() != null) {
                sender.getSubject().sendMessage("cookie添加成功");
            }

        } catch (Exception e) {
            sender.getSubject().sendMessage(e.getMessage());
        } finally {
            YmlAndPropAndIOUtil.closeResource(is,out);
        }
    }
}

