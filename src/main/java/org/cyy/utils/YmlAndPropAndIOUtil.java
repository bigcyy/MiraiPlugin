package org.cyy.utils;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * @author cyy
 * @date 2022/2/20 13:47
 * @description
 */
public class YmlAndPropAndIOUtil {
    /**
     * 将yaml文件转为map
     * @param path  路径
     * @return yaml文件对应的map，出现异常返回null
     */
    public static Map<String,Object> loadToMapFromPath(String path){
        Yaml yaml = new Yaml();
        Map<String,Object> map = null;
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(path);
            map  = yaml.load(fileInputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            closeResource(fileInputStream,null);
        }
        return map;
    }

    public static Object loadToObjFromPath(String path,Class clazz){
        Object load = null;
        Yaml yaml = new Yaml(new Constructor(clazz));
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(path);
            load = yaml.load(fileInputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            closeResource(fileInputStream,null);
        }
        return load;
    }

    public static String[] getAllFileNameByPath(String path){
        File file = new File(path);
        return file.list();
    }

    public static void saveObjToFile(String path,Object obj){
        Yaml yaml = new Yaml();
        FileWriter writer = null;
        try {
            writer = new FileWriter(path);
            yaml.dump(obj,writer);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            closeResource(writer);
        }
    }

    public static void closeResource(Writer out){
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 关闭输入输出流工具
     * @param in 输入流
     * @param out 输出流
     */
    public static void closeResource(InputStream in,OutputStream out){
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 根据文件路径，加载文件，文件不存在时返回null
     * @param path 文件路径
     * @return 加载后的properties
     */
    public static Properties loadProperties(String path){
        InputStream is = null;
        Properties properties = null;
        try {
            is = new FileInputStream(path);
            properties = new Properties();
            properties.load(is);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            YmlAndPropAndIOUtil.closeResource(is,null);
        }
        return properties;
    }

    /**
     * 保存ker-value到指定path的properties文件
     * @param properties 加载后的proprietary
     * @param key 存储的key
     * @param value 储存的值
     * @param path 路径
     */
    public static void saveProperties(Properties properties,String key,String value,String path){
        FileOutputStream out = null;
        properties.setProperty(key, value);
        try {
            out = new FileOutputStream(path);
            properties.store(out, null);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            YmlAndPropAndIOUtil.closeResource(null, out);
        }
    }



    /**
     * 获取value
     * @param path 文件路径
     * @param key key
     * @return value，如果不存在这样的key返回null
     */
    public static String getValue(String path,String key) {
        Properties properties = YmlAndPropAndIOUtil.loadProperties(path);
        return properties.getProperty(key);
    }

    /**
     * 获取value
     * @param properties 加载后的properties
     * @param key key
     * @return value，如果不存在这样的key返回null
     */
    public static String getValue(Properties properties,String key) {
        return properties.getProperty(key);
    }

    /**
     * 获取value
     * @param properties 加载后的properties
     * @param key key
     * @param defaultValue 返回null时默认返回值
     * @return value，如果不存在这样的key返回defaultValue
     */
    public static String getValue(Properties properties,String key,String defaultValue) {
        String value = properties.getProperty(key);
        return value == null?defaultValue:value;
    }

    /**
     * 传入文件夹目录和要匹配该目录下的文件的模式串，根据模式串匹配文件并删除
     * @param path 目录
     * @param pattern 模式串
     */
    public static void deleteFileByFilePattern(String path,String pattern){
        File file = new File(path);
        if(file.isDirectory()){
            Arrays.stream(Objects.requireNonNull(file.listFiles()))
                    .filter((tempFile)-> tempFile.getName().contains(pattern))
                    .forEach(File::delete);
        }
    }

}
