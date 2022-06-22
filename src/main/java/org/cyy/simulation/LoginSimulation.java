package org.cyy.simulation;

import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.utils.ExternalResource;
import okhttp3.*;
import org.cyy.bean.QueryAccount;
import org.cyy.config.ReplyMessage;
import org.cyy.exception.BaseCookieIsErrorException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author cyy
 * @date 2022/2/16 20:37
 * @description 模拟登录请求的类
 */
public class LoginSimulation {
    private final OkHttpClient client = SingleOkHttpClient.getInstance();
    /**
     * 获取第一次访问健康系统时，系统返回的cookie
     * @return cookie
     * @throws IOException 抛出该异常，说明请求出错（无需处理）
     */
    @Deprecated
    public String getBaseCookie() throws IOException {
        String url = "http://xg.cdnu.edu.cn/SPCP/Sys/";
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = client.newCall(request).execute();
        String ans = response.header("Set-Cookie");
        String regex = "(.*); path";
        Pattern p = Pattern.compile(regex);
        Matcher matcher = p.matcher(ans);
        String res = "";
        while (matcher.find()) {
            res = matcher.group(1);
        }
        return res;
    }

    /**
     * 提交登录表单
     * @param queryAccount 查询账号
     * @param baseCookie    基础cookie
     * @param resubmitFlag 重复登录标志
     * @param code  验证码
     * @return  返回cookie，失败返回空字符串“”
     * @throws IOException  该异常无需处理
     */
    public String submitLoginForm(QueryAccount queryAccount, String baseCookie, String resubmitFlag, String code) throws IOException {

        String url = "http://xg.cdnu.edu.cn/SPCP/Web/Account/TLogin";
        RequestBody requestBody = new FormBody.Builder()
                .add("ReSubmitFlag",resubmitFlag)
                .add("txtUid",queryAccount.getUsername())
                .add("txtPwd",queryAccount.getPassword())
                .add("code",code)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Cookie",baseCookie)
                .post(requestBody)
                .build();
        Response response = client.newCall(request).execute();
        //System.out.println(response.body().string());
        if(302 != response.code()){
            return null;
        }
        String setCookie = response.header("Set-Cookie");
        String regex = "(.*); expires";
        Pattern p = Pattern.compile(regex);
        Matcher matcher = p.matcher(setCookie);
        String res = "";
        while (matcher.find()) {
            res = matcher.group(1);
        }
        return res;
    }
    /**
     * 获取页面的重复提交标识
     * @return 重复提交的标识
     * @throws IOException 抛出该异常，说明请求出错（无需处理）
     * @throws BaseCookieIsErrorException 抛出该异常表示baseCookie不存在或者已经过期
     */
    public String getLoginResubmitFlag(String baseCookie) throws IOException, BaseCookieIsErrorException {
        if(baseCookie == null){
            throw new BaseCookieIsErrorException();
        }
        String url = "http://xg.cdnu.edu.cn/SPCP/Web/Account/TLogin";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Cookie",baseCookie)
                .build();
        Response response = client.newCall(request).execute();
        String result = response.body().string();
        String regex = "<input name=\"ReSubmiteFlag\" type=\"hidden\" value=\"(.*?)\" />";
        Pattern p = Pattern.compile(regex);
        Matcher matcher = p.matcher(result);
        String ans = "";
        while(matcher.find()){
            ans = matcher.group(1);
        }
        if(ans.equals("")){
            throw new BaseCookieIsErrorException();
        }
        return ans;
    }
    /**
     * 该方法采用异步请求加快响应速度
     * @param baseCookie 第一次访问健康打卡系统时的cookie，该cookie仅仅用于登录
     * @param group qq群对象，用于发送验证码图片
     */
    public void getAndSendCode(String baseCookie, Group group){
        String url = "http://xg.cdnu.edu.cn/SPCP/Web/Account/GetLoginVCode?dt="+new Date().getTime();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Cookie",baseCookie)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                byte[] bytes = Objects.requireNonNull(response.body()).bytes();
                /*
                    将获取到的byte数组转为图片，mirai图片需要先将图片上传到服务器才能发送，上传后返回image对象
                 */
                ExternalResource externalResource = ExternalResource.create(bytes);
                Image image = group.uploadImage(externalResource);
                //发送验证码图片
                group.sendMessage(new PlainText(ReplyMessage.pleaseSendCode).plus(image));
            }
        });
    }
}
