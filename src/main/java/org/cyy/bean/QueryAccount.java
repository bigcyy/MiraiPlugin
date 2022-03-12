package org.cyy.bean;

/**
 * QueryAccount对应每个群的查询账号
 * username:该学院某位老师用户名
 * password:密码
 * collegeNo:学院标号，唯一标识，collegeNo对应学院配置文件的命名，（这是由学校系统决定的，抓包时的请求参数）
 * cookie：cookie有效期为一周，过期后就要重新登录获得
 * authorization:验证，通过cookie发送几次请求可以得到，查询签到信息需要他
 */
public class QueryAccount {
    private String username;
    private String password;
    private String loginCookie;
    private String authorization;
    private String collegeNo;

    public QueryAccount() {
    }

    public QueryAccount(String username, String password, String collegeNo, String loginCookie, String authorization) {
        this.username = username;
        this.password = password;
        this.collegeNo = collegeNo;
        this.loginCookie = loginCookie;
        this.authorization = authorization;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCollegeNo() {
        return collegeNo;
    }

    public void setCollegeNo(String collegeNo) {
        this.collegeNo = collegeNo;
    }

    public String getLoginCookie() {
        return loginCookie;
    }

    public void setLoginCookie(String loginCookie) {
        this.loginCookie = loginCookie;
    }

    public String getAuthorization() {
        return authorization;
    }

    public void setAuthorization(String authorization) {
        this.authorization = authorization;
    }

    @Override
    public String toString() {
        return "QueryAccount{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", collegeNo='" + collegeNo + '\'' +
                ", loginCookie='" + loginCookie + '\'' +
                ", authorization='" + authorization + '\'' +
                '}';
    }
}
