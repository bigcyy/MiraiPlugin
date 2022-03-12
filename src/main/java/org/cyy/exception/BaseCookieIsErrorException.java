package org.cyy.exception;

/**
 * @author cyy
 * @date 2022/2/23 17:57
 * @description config配置文件中的baseCookie不存在或者已经过期
 */
public class BaseCookieIsErrorException extends Exception{
    public BaseCookieIsErrorException() {
    }

    public BaseCookieIsErrorException(String message) {
        super(message);
    }
}
