package org.cyy.exception;

/**
 * @author cyy
 * @date 2022/2/15 15:06
 * @description 验证过程中返回的验证为空异常，表明cookie失效
 */
public class AuthIsNullException extends Exception{
    public AuthIsNullException() {
    }
    public AuthIsNullException(String message) {
        super(message);
    }
}
