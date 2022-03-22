package org.cyy.commend;

import net.mamoe.mirai.console.command.CommandSender;
import net.mamoe.mirai.console.command.java.JSimpleCommand;
import org.cyy.Plugin;
import org.cyy.utils.YmlAndPropAndIOUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public final class MyCommendAdd extends JSimpleCommand {

    public static final MyCommendAdd INSTANCE = new MyCommendAdd();

    private MyCommendAdd() {
        super(Plugin.INSTANCE, "add", new String[]{"授权","添加"}, Plugin.INSTANCE.getParentPermission());
        this.setDescription("这是授权命令");
    }
    @Handler
    public void onCommand(CommandSender sender,String filePath,String id,String message) {

            FileOutputStream out = null;
            try {
                //写入配置文件
                File file = new File(filePath+"/"+id+".properties");
                if(!file.exists()) {
                    try {
                        file.createNewFile();
                        sender.getSubject().sendMessage("文件创建成功！");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                String[] str = message.split("=");
                Properties p = YmlAndPropAndIOUtil.loadProperties(filePath + "/" + id + ".properties");

                p.setProperty(str[0], str[1]);

                out = new FileOutputStream(filePath+"/"+ id + ".properties");
                p.store(out,null);

                //写入后给予反馈
                if(sender.getUser() != null) {
                    sender.getSubject().sendMessage("添加成功:"+str[0]+"="+str[1]);
                }

            }catch (Exception e){
                sender.getSubject().sendMessage(e.getMessage());
            }finally {
                YmlAndPropAndIOUtil.closeResource(null,out);
            }
        }
}
