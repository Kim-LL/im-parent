package com.paas.im.model.pojo;

import cn.hutool.core.util.StrUtil;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * token 验证服务的ip和端口号
 */
@Data
public class TokenServiceConfig {

    // 校验服务的ip，可以逗号隔开多个服务，轮询调用
    private String tokenServiceIp;

    // 为了对外更好的提供配置，提供的辅助信息和tokenServiceIp 相对应
    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    private String[] tokenServiceIpArray = null;

    // 校验服务的端口号
    private int port;

    // 是否调用校验标识
    private boolean validate;

    // socket连接的超时时间
    private int socketTimeoutMilliseconds;

    private String appKey;

    // thrift连接池默认值
    private int thriftPoolMaxTotal = 1000;
    private int thriftPoolMaxIdle = 100;
    private int thriftPoolMinIdle = 50;

    // 是否调用校验标识
    private boolean toBTokenValidate;

    public void setTokenServiceIp(String tokenServiceIp) {
        this.tokenServiceIp = tokenServiceIp;
        tokenServiceIpArray = tokenServiceIp.split(",");
    }

    public String getServerIp(long flag) {
        if (tokenServiceIpArray == null) {
            if (StrUtil.isNotEmpty(tokenServiceIp)) {
                tokenServiceIpArray = tokenServiceIp.split(",");
            }
        }
        if (tokenServiceIpArray != null) {
            return tokenServiceIpArray[(int) (flag % tokenServiceIpArray.length)];
        }
        return null;
    }

    public int getServiceSize() {
        if (tokenServiceIpArray == null) {
            if (StrUtil.isNotEmpty(tokenServiceIp)) {
                tokenServiceIpArray = tokenServiceIp.split(",");
            }
        }

        if (tokenServiceIpArray != null) {
            return tokenServiceIp.split(",").length;
        }

        return 0;
    }
}
