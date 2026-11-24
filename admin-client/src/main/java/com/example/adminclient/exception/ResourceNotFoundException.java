package com.example.adminclient.exception;

/**
 * 资源不存在业务异常。
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
