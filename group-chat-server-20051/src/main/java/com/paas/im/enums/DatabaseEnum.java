package com.paas.im.enums;

import lombok.Getter;

@Getter
public enum DatabaseEnum {

    CHAT_MSG("chat-msg"),

    SESSION("session"),

    GROUP_CHAT("group-chat"),

    USER_RELATION("user-relation"),;

    private final String name;

    DatabaseEnum(String name) {
        this.name = name;
    }

    public static  DatabaseEnum getInstance(String name) {
        for (DatabaseEnum databaseEnum : DatabaseEnum.values()) {
            if (databaseEnum.name.equals(name)) {
                return databaseEnum;
            }
        }
        return null;
    }
}
