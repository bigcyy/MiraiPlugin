package org.cyy.job;

import org.cyy.Plugin;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

/**
 * @author cyy
 * @date 2022/2/18 22:15
 * @description
 */
public class AutoSignRunnable implements Runnable{
    @Override
    public void run() {
        // 1、创建调度器Scheduler
        SchedulerFactory schedulerFactory = new StdSchedulerFactory();
        Scheduler scheduler = null;
        try {
            scheduler = schedulerFactory.getScheduler();
        } catch (SchedulerException schedulerException) {
            schedulerException.printStackTrace();
        }
        // 2、创建JobDetail实例，并与PJob类绑定(Job执行内容)
        JobDetail jobDetail = JobBuilder.newJob(AutoToSignJob.class)
                .withIdentity("job1", "group1").build();
        // 3、创建CronTrigger实例，传入corn表达式
        CronTrigger cronTrigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger1", "group1")
                .startNow()//立即生效
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 9,11,12,13,14,15 * * ?"))
//                .withSchedule(CronScheduleBuilder.cronSchedule("0 40 0 * * ?"))
                .build();
        //4、执行
        try {
            scheduler.scheduleJob(jobDetail, cronTrigger);
            Plugin.contextMap.put("scheduler",scheduler);
            Plugin.MY_BOT.getLogger().info("--------自动催签到任务开始 ! ------------");
            scheduler.start();
        } catch (SchedulerException schedulerException) {
            schedulerException.printStackTrace();
        }
    }
}
