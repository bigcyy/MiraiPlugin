package org.cyy.dao;

import org.cyy.bean.MyGroup;
import org.cyy.bean.QueryAccount;
import org.cyy.bean.QueryObject;
import org.cyy.exception.GroupFileNotFindException;
import org.cyy.utils.YmlAndPropUtil;

import java.util.ArrayList;
import java.util.Properties;

public class MyGroupDao {

    /**
     * 获取MyGroup实列
     * @param qun 群号
     * @return 返回MyGroup实例
     * @throws GroupFileNotFindException 群配置文件未找到异常
     */
    public MyGroup getMyGroupByFile(String qun) throws GroupFileNotFindException {

        Properties prop = YmlAndPropUtil.loadProperties("group/"+qun+".properties");
        //如果文件不存在直接返回null
        if(prop == null){
            throw new GroupFileNotFindException();
        }

        /*
            从properties封装queryAccount
         */
        QueryAccountDao queryAccountDao = new QueryAccountDao();
        QueryAccount queryAccount = queryAccountDao.getQueryAccountByProp(prop);
        /*
            从properties封装queryObjects
         */
        ArrayList<QueryObject> queryObjects = new ArrayList<>();
        String num = prop.getProperty("num");
        int intNum = Integer.parseInt(num);
        for (int i = 1; i <= intNum; i++) {
            QueryObject queryObject = new QueryObject();
            queryObject.setSpecialtyNo((prop.getProperty("specialtyNo" + i) == null) ? "" : prop.getProperty("specialtyNo" + i));
            queryObject.setSpeGrade((prop.getProperty("speGrade" + i) == null) ? "" : prop.getProperty("speGrade" + i));
            queryObject.setClassNo((prop.getProperty("classNo" + i) == null) ? "" : prop.getProperty("classNo" + i));
            queryObject.setCollegeNo((prop.getProperty("collegeNo") == null) ? "" : prop.getProperty("collegeNo"));
            queryObjects.add(queryObject);
        }
        return new MyGroup(intNum,qun,queryAccount,queryObjects);
    }


    /**
     * 通过群号从文件获取学院号
     * @param qun qq群号
     * @return 学院号
     * @throws GroupFileNotFindException 群配置文件未找到异常
     */
    public String queryCollegeNoByFile(String qun) throws GroupFileNotFindException {
        String fileName = "group/"+qun+".properties";
        Properties prop = YmlAndPropUtil.loadProperties(fileName);
        if(prop== null){
            throw new GroupFileNotFindException();
        }
        return prop.getProperty("collegeNo");
    }
}
