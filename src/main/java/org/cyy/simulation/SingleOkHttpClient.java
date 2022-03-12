package org.cyy.simulation;

import okhttp3.OkHttpClient;

/**
 * @Author: cyy
 * @Date: 2022/2/14 20 47
 * @Description: okClient的单例模式
 */
public class SingleOkHttpClient extends OkHttpClient {
    private volatile static OkHttpClient instance;
    private SingleOkHttpClient(){};
    public static OkHttpClient getInstance(){
        if(instance == null){
            synchronized (SingleOkHttpClient.class){
                if(instance == null){
                    //设置302不自动跳转
                    return new OkHttpClient().newBuilder().followRedirects(false).build();
                }
            }
        }
        return instance;
    }
}
