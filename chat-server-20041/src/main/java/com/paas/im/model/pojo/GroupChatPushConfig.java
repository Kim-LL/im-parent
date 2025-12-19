package com.paas.im.model.pojo;

import lombok.Data;

@Data
public class GroupChatPushConfig {

    private String androidVersion;

    private String iosVersion;

    private boolean push;
}
