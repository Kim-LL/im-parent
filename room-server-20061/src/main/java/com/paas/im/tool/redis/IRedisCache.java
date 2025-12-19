package com.paas.im.tool.redis;

import java.util.Map;

public interface IRedisCache {

    /**
     * 同时将多个 field-value (域-值)对设置到哈希表 key 中。
     * 时间复杂度: O(N) (N为fields的数量)
     * @param key: key
     * @param hash: field-value的map
     * @return : 如果命令执行成功，返回 OK 。当 key 不是哈希表(hash)类型时，返回一个错误。
     */
    String multiSet(final String key, final Map<String, String> hash);
}
