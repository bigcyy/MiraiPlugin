package org.cyy.bean;

/**
 * @author CYY
 * @date 2022年06月22日 下午4:55
 * @description 从学校后台获取到的json中的每位同学信息的映射类
 */
public class PersonModel {
    private String Id;
    private String StudentId;
    private String Name;
    private String MoveTel;
    private String FDYName;
    private String CollegeName;
    private String ClassName;
    private String NowAddress;
    private String RegisterDate;
    private String Status;

    public PersonModel() {
    }

    public PersonModel(String id, String studentId, String name, String moveTel, String FDYName, String collegeName, String className, String nowAddress, String registerDate, String status) {
        Id = id;
        StudentId = studentId;
        Name = name;
        MoveTel = moveTel;
        this.FDYName = FDYName;
        CollegeName = collegeName;
        ClassName = className;
        NowAddress = nowAddress;
        RegisterDate = registerDate;
        Status = status;
    }

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public String getStudentId() {
        return StudentId;
    }

    public void setStudentId(String studentId) {
        StudentId = studentId;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getMoveTel() {
        return MoveTel;
    }

    public void setMoveTel(String moveTel) {
        MoveTel = moveTel;
    }

    public String getFDYName() {
        return FDYName;
    }

    public void setFDYName(String FDYName) {
        this.FDYName = FDYName;
    }

    public String getCollegeName() {
        return CollegeName;
    }

    public void setCollegeName(String collegeName) {
        CollegeName = collegeName;
    }

    public String getClassName() {
        return ClassName;
    }

    public void setClassName(String className) {
        ClassName = className;
    }

    public String getNowAddress() {
        return NowAddress;
    }

    public void setNowAddress(String nowAddress) {
        NowAddress = nowAddress;
    }

    public String getRegisterDate() {
        return RegisterDate;
    }

    public void setRegisterDate(String registerDate) {
        RegisterDate = registerDate;
    }

    public String getStatus() {
        return Status;
    }

    public void setStatus(String status) {
        Status = status;
    }
}
