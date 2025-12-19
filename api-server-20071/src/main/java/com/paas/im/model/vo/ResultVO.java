package com.paas.im.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@AllArgsConstructor
public class ResultVO<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 5549949801228295218L;

    private Integer code;
    private String msg;
    private T data;
}
