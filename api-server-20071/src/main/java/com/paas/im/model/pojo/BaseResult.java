package com.paas.im.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class BaseResult<T> {

    public static final BaseResult<?> SUCCESS = new BaseResult<>(200, "success");
    public static final BaseResult<?> FAILED = new BaseResult<>(1, "failed");

    // ================================== CHAT ==================================
    public static final BaseResult<?> CHAT_SAVE_MESSAGE = new BaseResult<>(2001, "redis保存单聊消息异常");

    private int code;

    private String msg;

    @Setter
    private T data;

    public BaseResult(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public String toString(){
        return "[ErrorResult: code: " + this.code + " msg: " + this.msg + "]";
    }
}
