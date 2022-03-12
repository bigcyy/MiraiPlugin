package org.cyy.bean;

/**
 * @Author cyy
 * @Descreption 向学校服务器发送请求所需参数的类，可理解为专业类
 * status:状态，1为已经签到的，2为未签到 默认为2
 * specialtyNo:专业代码
 * speGrade:年级
 * classNo:班级
 * collegeNo:学院代码
 * 配置文件中的num为必要参数，表明一个群包含了几个需要向服务器发送请求的催签到对象
 */
public class QueryObject {
    private String specialtyNo;
    private String speGrade;
    private String classNo;
    private String collegeNo;

    public QueryObject() {
    }

    public QueryObject(String specialtyNo, String speGrade, String classNo, String collegeNo) {
        this.specialtyNo = specialtyNo;
        this.speGrade = speGrade;
        this.classNo = classNo;
        this.collegeNo = collegeNo;
    }



    public String getSpecialtyNo() {
        return this.specialtyNo;
    }

    public void setSpecialtyNo(String specialtyNo) {
        this.specialtyNo = specialtyNo;
    }

    public String getSpeGrade() {
        return this.speGrade;
    }

    public void setSpeGrade(String speGrade) {
        this.speGrade = speGrade;
    }

    public String getClassNo() {
        return this.classNo;
    }

    public void setClassNo(String classNo) {
        this.classNo = classNo;
    }

    public String getCollegeNo() {
        return this.collegeNo;
    }

    public void setCollegeNo(String collegeNo) {
        this.collegeNo = collegeNo;
    }
}
