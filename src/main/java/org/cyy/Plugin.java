package org.cyy;

import net.mamoe.mirai.Bot;
import net.mamoe.mirai.console.command.CommandManager;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.event.GlobalEventChannel;
import org.cyy.commend.*;
import org.cyy.config.MyPluginConfig;
import org.cyy.handler.BaseEventHandler;
import org.cyy.redis.RedisPool;

import java.util.HashMap;
import java.util.Map;

public final class Plugin extends JavaPlugin {
    public static final Plugin INSTANCE = new Plugin();
    public static Bot MY_BOT;           //储存当前bot
    public static Map<String,Object> contextMap = new HashMap<>();    //应用上下文，用于存储全局配置

    private Plugin() {
        super(new JvmPluginDescriptionBuilder("org.cyy.sign-push", "1.0")
                .name("推送助手")
                .author("cyy")
                .build());
    }

    /**
     * 插件加载
     */
    @Override
    public void onEnable() {
        //插件加载信息
        getLogger().info("插件加载完成!");
        //加载配置文件
        getLogger().info("加载配置文件！");
        MyPluginConfig.load();
        getLogger().info("配置文件加载完成！");
        //加载redis
        getLogger().info("初始化redis连接池！");
        RedisPool.load();
        getLogger().info("redis连接池初始完成！");
        //注册事件分发器
        getLogger().info("注册事件分发器！");
        GlobalEventChannel.INSTANCE.registerListenerHost(new BaseEventHandler());
        getLogger().info("事件分发器注册完成！");
        //命令注册
        getLogger().info("注册命令！");
        this.registerComment();
        getLogger().info("命令注册完成！");
    }
    /**
     * 命令注册
     * @author cyy
     * @date 2021/10/3015:45
     *
     */
    public void registerComment(){
        CommandManager.INSTANCE.registerCommand(MyCommendAdd.INSTANCE, true);   //注册add命令
        CommandManager.INSTANCE.registerCommand(MyCommendDelete.INSTANCE, true);   //注册delete命令
        CommandManager.INSTANCE.registerCommand(MyCommendCheck.INSTANCE, true);   //注册check命令
        CommandManager.INSTANCE.registerCommand(MyCommendCookie.INSTANCE, true);   //注册cookie命令
        CommandManager.INSTANCE.registerCommand(MyCommendCheckFiles.INSTANCE, true);   //注册checkFiles命令
        CommandManager.INSTANCE.registerCommand(MyCommendNotice.INSTANCE, true);   //注册notice命令
    }
}