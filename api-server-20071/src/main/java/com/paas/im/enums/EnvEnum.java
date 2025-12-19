package com.paas.im.enums;

import lombok.Getter;

/**
 * 环境定义
 */
public enum EnvEnum {

    UNIT("unit"),
    TEST("test"),
    INTE("inte"),
    MONI("moni"),
    PRE("pre"),
    PROD("prod"),
    INTE2("inte2"),
    INTE3("inte3"),
    INTE1("inte"),
    MEAST("meast"),
    CMSHOW("cmshow");

    // 成员变量
    @Getter
    private final String name;

    // 构造方法
    EnvEnum(String name) {
        this.name = name;
    }

    public static String getEnvironmentFlag() {
        return System.getProperty("config.type");
    }

    public static boolean isProd() {
        String configType = getEnvironmentFlag();
        return !isEmpty(configType) && configType.contains(PROD.getName());
    }

    private static boolean isEmpty(String str) {
        return str == null || "".equals(str);
    }

    // 是否单元测试
    public static boolean isUnit() {
        String configType = getEnvironmentFlag();
        return !isEmpty(configType) && (configType.contains(UNIT.getName()));
    }

    // 是否集测环境
    public static boolean isInte() {
        String configType = getEnvironmentFlag();
        return !isEmpty(configType) && (configType.contains(INTE.getName()));
    }

    public static boolean isInte1() {
        return !isInte2() && !isInte3() && isInte();
    }


    public static boolean isInte2() {
        String configType = getEnvironmentFlag();
        return !isEmpty(configType) && (configType.contains(INTE2.getName()));
    }

    public static boolean isInte3() {
        String configType = getEnvironmentFlag();
        return !isEmpty(configType) && (configType.contains(INTE3.getName()));
    }

    public static boolean isCmShow() {
        String configType = getEnvironmentFlag();
        return !isEmpty(configType) && (configType.contains(CMSHOW.getName()));
    }


    public static boolean isMoni() {
        String configType = getEnvironmentFlag();
        return !isEmpty(configType) && configType.contains(MONI.getName());
    }
    public static boolean isPre() {
        String configType = getEnvironmentFlag();
        return !isEmpty(configType) && configType.contains(PRE.getName());
    }

    public static boolean isMeast() {
        String configType = getEnvironmentFlag();
        return !isEmpty(configType) && configType.contains(MEAST.getName());
    }

    // 是否开发环境
    public static boolean isDev() {
        String configType = getEnvironmentFlag();
        return isEmpty(configType) || configType.contains(TEST.getName());
    }

    public static EnvEnum getCurrentEnv() {
        if (isUnit()){
            return EnvEnum.UNIT;
        }
        if (isInte()) {
            return EnvEnum.INTE;
        }
        if (isMoni()) {
            return EnvEnum.MONI;
        }
        if (isProd()) {
            return EnvEnum.PROD;
        }
        return EnvEnum.TEST;
    }
}
