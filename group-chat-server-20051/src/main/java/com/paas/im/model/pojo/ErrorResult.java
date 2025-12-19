package com.paas.im.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class ErrorResult <T> {

    public static final ErrorResult<?> SUCCESS = new ErrorResult<>(200, "success");
    public static final ErrorResult<?> FAILED = new ErrorResult<>(1, "failed");

    // ================================== CHAT ==================================
    public static final ErrorResult<?> CHAT_SAVE_MESSAGE = new ErrorResult<>(2001, "redis保存单聊消息异常");

    private int code;

    private String msg;

    @Setter
    private T data;

    public ErrorResult(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public String toString(){
        return "[ErrorResult: code: " + this.code + " msg: " + this.msg + "]";
    }
}
