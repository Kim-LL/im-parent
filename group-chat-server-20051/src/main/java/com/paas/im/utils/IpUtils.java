package com.paas.im.utils;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.net.NetUtil;
import cn.hutool.http.HttpUtil;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.LinkedHashSet;

@Slf4j
public class IpUtils {

    private static final String AWS_LOCAL_IP_API = "http://169.254.169.254/latest/meta-data/local-ipv4";

    /**
     * 获取本地非回环的IPv4地址
     */
    public static String getLocalIP() {

        // 步骤1：筛选出非回环的IPv4地址（核心筛选条件）
        LinkedHashSet<InetAddress> addressList = NetUtil.localAddressList(
                inetAddress -> !inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address
        );
        // 步骤2：如果没有筛选到地址，返回null
        if (CollectionUtil.isEmpty(addressList)) {
            return null;
        }

        InetAddress siteLocalAddress = null;
        // 步骤3：遍历地址，优先返回非站点本地地址（公网IP），记录第一个站点本地地址（内网IP）
        for (InetAddress address : addressList) {
            // 判断一个 IPv4 地址是否属于内网保留地址段（站点本地地址）
            // 10.0.0.0/8（10.x.x.x） 172.16.0.0/12（172.16.x.x ~ 172.31.x.x）192.168.0.0/16（192.168.x.x）
            if (!address.isSiteLocalAddress()) {
                return address.getHostAddress();
            }
            // 记录第一个内网IP
            if (siteLocalAddress == null) {
                siteLocalAddress = address;
            }
        }
        return siteLocalAddress != null ? siteLocalAddress.getHostAddress() : null;
    }

    /**
     * AWS 的 EC2 实例网络环境是虚拟化的，传统的本地 IP 获取方式存在局限性
     * <a href="http://169.254.169.254/latest/meta-data/local-ipv4">AWS获取ip接口</a>
     * 这个接口是 AWS 为云实例提供的专属元数据服务，能稳定、准确地返回实例的核心网络信息
     * return aws 获取为空的话，则返回代码获取的ip
     */
    public static String getAWSLocalIP(){
        String awsLocalIp = null;
        try {
            // 超时时间（IMDS接口响应快，设置短超时即可）
            awsLocalIp = HttpUtil.get(AWS_LOCAL_IP_API, 1000);
        } catch (Exception e){
            log.info("获取 AWS 私有ip 失败: {}", e.getMessage());
        }
        if(StringUtils.isBlank(awsLocalIp)){
            return getLocalIP();
        }
        return awsLocalIp;
    }

    static void main() {
        log.info(IpUtils.getAWSLocalIP());
    }
}
