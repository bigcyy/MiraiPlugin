package org.cyy.exception;

/**
 * @author cyy
 * @date 2022/2/15 15:06
 * @description 群配置文件没有找到异常
 */
public class GroupFileNotFindException extends Exception {
    public GroupFileNotFindException() {
    }

    public GroupFileNotFindException(String message) {
        super(message);
    }
}
