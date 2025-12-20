package com.paas.im.utils;

import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;

public class PathMatcherUtils {

    private static final PathPatternParser PATTERN_PARSER = new PathPatternParser();

    //  基础通配符：
    //  ?：单个字符（非/）；
    //  *：单级路径（不跨/）；
    //  /**：多级路径（跨/）；
    //  /**/xxx：任意多级路径下的xxx路径段。

    // **不能单独使用，必须和/组合(/**),
    //  ✅ 合法：/**/login, ❌ 非法：**/login

    // **只能出现在路径的开头、中间或结尾，且必须是完整的路径段.
    // ✅ 合法：/api/**,/**/login,/user/**/info, ❌ 非法：/user**
    public static boolean match(String path, List<String> patterns) {
        // 鲁棒性处理：路径或模式为空，直接返回false
        if (path == null || path.isEmpty() || patterns == null || patterns.isEmpty()) {
            return false;
        }
        PathContainer pathContainer = PathContainer.parsePath(path);
        // 预编译模式为PathPattern（可缓存，提升性能，这里简化处理）
        List<PathPattern> pathPatterns = patterns.stream()
                .map(String::trim) // 去空格
                .filter(pattern -> !pattern.isEmpty()) // 过滤空字符串
                // 解析模式为PathPattern（注意：PathPattern不支持**/login，必须用/**/login）
                .map(PATTERN_PARSER::parse)
                .toList();
        // 遍历匹配，只要有一个模式匹配就返回true
        return pathPatterns.stream().anyMatch(pattern -> pattern.matches(pathContainer));
    }
}
