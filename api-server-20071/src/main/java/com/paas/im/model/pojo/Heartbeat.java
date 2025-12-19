package com.paas.im.model.pojo;

import lombok.Data;

@Data
public class Heartbeat {

    private String min;

    private String def;

    private String max;

    private String step;

    private String type;

}
