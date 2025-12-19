package com.paas.im.model.pojo;


/**
 * 统计消息及业务消息日志输出配置
 */
public class StatisticMessageConfig {

    /**
     * 是否打开消息数量统计
     */
    private static boolean isOpenAppMessageNumStatistic = false;

    /**
     * app 消息数量统计时间，间隔
     */
    private static int AppMessageNumStatisticSleepSecond = 60;

    /**
     * 是否开启消息日志输出
     */
    private static boolean isOpenAppMessageLogWrite = false;

    public static boolean isIsOpenAppMessageNumStatistic() {
        return isOpenAppMessageNumStatistic;
    }

    public static void setIsOpenAppMessageNumStatistic(boolean isOpenAppMessageNumStatistic) {
        StatisticMessageConfig.isOpenAppMessageNumStatistic = isOpenAppMessageNumStatistic;
    }

    public static int getAppMessageNumStatisticSleepSecond() {
        return AppMessageNumStatisticSleepSecond;
    }

    public static void setAppMessageNumStatisticSleepSecond(int appMessageNumStatisticSleepSecond) {
        AppMessageNumStatisticSleepSecond = appMessageNumStatisticSleepSecond;
    }

    public static boolean isIsOpenAppMessageLogWrite() {
        return isOpenAppMessageLogWrite;
    }

    public static void setIsOpenAppMessageLogWrite(boolean isOpenAppMessageLogWrite) {
        StatisticMessageConfig.isOpenAppMessageLogWrite = isOpenAppMessageLogWrite;
    }
}
