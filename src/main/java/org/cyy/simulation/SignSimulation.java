package org.cyy.simulation;

import okhttp3.*;
import org.cyy.bean.QueryAccount;
import org.cyy.bean.QueryObject;
import org.cyy.config.MyPluginConfig;
import org.cyy.exception.AuthIsNullException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author cyy
 * @date 2022/2/16 20:37
 * @description 模拟请求获取名单，催签到的类
 */
public class SignSimulation {
    private final OkHttpClient client = SingleOkHttpClient.getInstance();
    private final String baseUrl = "http://xg.cdnu.edu.cn/SPCP/PhoneApi/api/Health/GetDCStuReseltWebList?topId=&inputDate=&stuId=&stuName=&status=%s&collegeNo=%s&specialtyNo=%s&classNo=%s&speGrade=%s&fdyName=&pageIndex=";

    /**
     * 根据传入的queryObject,生成url，发送请求获取名单的总页数
     * @param queryAccount 存储了账户密码cookie的对象
     * @param queryObject 保存查询参数的对象
     * @param status 签到状态(1:已经签到,2:未签到)
     * @return 页数
     * @throws IOException 发送请求时未成功抛出异常
     */
    private int getAllPage(QueryAccount queryAccount, QueryObject queryObject, int status) throws IOException {

        String allPage = "-1";
        String url = String.format(baseUrl+"1", status, queryObject.getCollegeNo(), queryObject.getSpecialtyNo(), queryObject.getClassNo(), queryObject.getSpeGrade());
        //System.out.println(tempUrl);
        Request request = new Request.Builder() //创建请求体
                .url(url)
                .addHeader("authorization", queryAccount.getAuthorization())
                .build();
        //System.out.println(queryAccount.getAuthorization());
        Response response = client.newCall(request).execute();

        String result = Objects.requireNonNull(response.body()).string();
        response.close();
        //匹配出页数
        String regex = "\"TotalPages\":(\\d+)";
        Pattern p = Pattern.compile(regex);
        Matcher matcher = p.matcher(result);
        while(matcher.find()){
            allPage = matcher.group(1);
        }
        return Integer.parseInt(allPage); //返回页数

    }

    /**
     * 根据状态获取所有名单
     * @param queryAccount 存储了账户密码cookie的对象
     * @param queryObjects 存储了多个查询对象的集合
     * @param status 签到状态(1:已经签到,2:未签到)
     * @return 查询到的名单
     * @throws IOException 发送请求异常
     */
    public List<String> getNameByStatus(QueryAccount queryAccount,ArrayList<QueryObject> queryObjects , int status) throws IOException {
        List<String> list = new ArrayList<>();      //记录总名单
        //long begin = System.currentTimeMillis();    //开始时间
        int allPage;    //记录总页数
        //取出群里的每个催签到查询对象(专业)，获取每个对象未签到的名单
        for (QueryObject queryObject : queryObjects) {
            allPage = getAllPage(queryAccount, queryObject,status);   //获取所有页
            //System.out.println(allPage);
            if (allPage == -1) {
                return null; //获取页数时失败，直接返回null
            } else {
                CountDownLatch counter = new CountDownLatch(allPage);   //线程记录
                String url = String.format(baseUrl, status, queryObject.getCollegeNo(), queryObject.getSpecialtyNo(), queryObject.getClassNo(), queryObject.getSpeGrade());
                for (int j = 1; j <= allPage; j++) {
                    String pageUrl = url + j;
                    Request request = new Request.Builder()
                            .url(pageUrl)
                            .addHeader("authorization", queryAccount.getAuthorization())
                            .build();
                    //异步请求
                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {
                            counter.countDown();
                            e.printStackTrace();
                        }

                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                            String result = Objects.requireNonNull(response.body()).string();
                            response.close();
                            String regex = "\"Name\":\"(.*?)\"";
                            Pattern p = Pattern.compile(regex);
                            Matcher matcher = p.matcher(result);
                            while (matcher.find()) {
                                list.add(matcher.group(1));
                            }
                            counter.countDown();
                        }
                    });
                }
                //线程等待
                try {
                    counter.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        //结束时间
//        long end = System.currentTimeMillis();
//        System.out.println(end-begin);
        return list;    //返回名单
    }



    /**
     * 传入所需的账户对象，返回验证
     * @param queryAccount:查询所需的账户对象
     * @return 成功会返回后续查询名单所需要的验证
     * @throws IOException 抛出Io异常，说明发送网络请求出现问题
     * @throws AuthIsNullException 抛出该异常，说明cookie已经失效了，需要重新登录
     */
    public String getAuthorization(QueryAccount queryAccount) throws IOException, AuthIsNullException {

        String code = authorizationProcessOne(queryAccount.getLoginCookie());
        String token = authorizationProcessTwo(queryAccount.getLoginCookie(),code);
        String result = authorizationProcessThree(queryAccount.getLoginCookie(), queryAccount.getCollegeNo(),token);

        return "Bearer " + result;
    }


    /**
     * 获取Authorization流程中的第一次请求
     * @param cookie 用户的登录凭证
     * @return 返回第二步需要使用的参数code
     * @throws IOException 请求出错
     * @throws AuthIsNullException cookie已经失效了，需要重新登录
     */
    private String authorizationProcessOne(String cookie) throws IOException, AuthIsNullException {
        if(cookie == null){
            throw new AuthIsNullException();
        }
        String url = "http://xg.cdnu.edu.cn/SPCP/Web/Account/ChooseSys";   //请求地址
        String code = "";   //接收响应返回的code
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Cookie",cookie)
                .build();
        Response response = client.newCall(request).execute();  //获取响应
        String postUrl = response.header("Location"); //获取头中的location项的信息，code在location中
        response.close();
        //正则取出结果中的id
        String regex = "OpenId=(.*+)";
        Pattern p = Pattern.compile(regex);
        Matcher matcher = p.matcher(postUrl);
        while (matcher.find()) {
            code = matcher.group(1);
        }
        if ("".equals(code)) {
            //如果code为空，表明cookie失效
            throw new AuthIsNullException("code为空，cookie失效！");
        }
        return code;
    }

    /**
     * 获取Authorization流程中的第二次请求
     * @param cookie 用户的登录凭证
     * @param code 第二次请求获取的token
     * @return 返回token
     * @throws IOException 请求出错
     */
    private String authorizationProcessTwo(String cookie,String code) throws IOException {
        String refreshToken = "";   //记录token
        RequestBody requestBody = new FormBody.Builder()
                .add("grant_type","password")
                .build();
        Request request = new Request.Builder()
                .url("http://xg.cdnu.edu.cn/SPCP/PhoneApi/api/Account/Login?code=" + code)
                .post(requestBody)
                .header("Cookie",cookie)
                .build();
        Response response = client.newCall(request).execute();
        String result = Objects.requireNonNull(response.body()).string();
        response.close();
        //正则匹配结果
        Pattern p = Pattern.compile("\"refresh_token\":\"(.*?)\"");
        Matcher matcher = p.matcher(result);
        while (matcher.find()) {
            refreshToken = matcher.group(1);
        }
        return refreshToken;
    }

    /**
     * 获取Authorization流程中的第三次请求
     * @param cookie 用户的登录凭证
     * @param collegeNo 学院号
     * @param token 第二次请求获得的token
     * @return 返回验证令牌
     * @throws IOException 请求出错
     */
    private String authorizationProcessThree(String cookie,String collegeNo, String token) throws IOException {

        String authorization = "";
        RequestBody requestBody = new FormBody.Builder()
                .add("grant_type","refresh_token")
                .add("refresh_token",token)
                .build();
        Request request = new Request.Builder()
                .url("http://xg.cdnu.edu.cn/SPCP/PhoneApi/api/Account/Login?userType=T&collegeNo=" + collegeNo)
                .post(requestBody)
                .header("Cookie",cookie)
                .build();
        Response response = client.newCall(request).execute();
        String result = Objects.requireNonNull(response.body()).string();
        response.close();
        //正则匹配结果
        Pattern p = Pattern.compile("\"access_token\":\"(.*?)\"");
        Matcher matcher = p.matcher(result);
        while (matcher.find()) {
            authorization = matcher.group(1);
        }
        return authorization;
    }

}
