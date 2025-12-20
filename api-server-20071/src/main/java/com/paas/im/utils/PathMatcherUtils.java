package com.paas.im.utils;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.List;

public class PathMatcherUtils {

    private static final PathMatcher PATH_MATCHER = new AntPathMatcher();

    /**
     * 判断路径是否匹配指定的模式列表
     * @param path 待匹配的路径（如/api/user/1）
     * @param patterns 路径模式列表（如[/api/**, /user/*]）
     * @return 是否匹配
     */
    public static boolean match(String path, List<String> patterns) {
        if(patterns == null || patterns.isEmpty()) {
            return false;
        }
        return patterns.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }
}
