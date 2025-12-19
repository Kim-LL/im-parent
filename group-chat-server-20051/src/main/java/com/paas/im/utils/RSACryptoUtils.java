package com.paas.im.utils;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.asymmetric.Sign;
import cn.hutool.crypto.asymmetric.SignAlgorithm;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Properties;

/**
 * RSA加密解密工具类（适配JDK 25.0.1+8-LTS + Hutool 5.8.42，基于实际源码修正）
 * 包名、类名及API调用完全匹配你的环境
 */
@Slf4j
public class RSACryptoUtils {

    /**
     * 加密算法RSA
     */
    public static final String KEY_ALGORITHM = "RSA";

    /**
     * 签名算法（Hutool 5.8.42的SignAlgorithm枚举，包路径为asymmetric）
     */
    private static final SignAlgorithm SIGN_ALGORITHMS = SignAlgorithm.SHA1withRSA;

    /**
     * 内置公钥（BASE64编码，用于验证文件中的签名）
     */
    private static final String INNER_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDI29uhJc74WmjP291uOgp12aN5NiAiyAsDjOWx0zYwUY7Kok+j7ZaBA5vYd3fWhgf2/iwz0WwtM+S9XWcpTyoUAIj/bQ41G9e4kOwE6ekS6Ub1RSLnls40SSglbBF0JQf3W77WQbiO1v7CHIIECUSTKOXQdWZMWwyvr+U7mThgFQIDAQAB";

    /**
     * 复用系统属性，JDK 25中System.getProperties()仍保持兼容
     */
    static final Properties PROPERTIES = new Properties(System.getProperties());

    /**
     * 公钥加密（保留原有方法名和入参）
     *
     * @param message       源数据
     * @param publicKeyFile 公钥文件路径(公钥内容BASE64编码)
     * @return 加密后数据（BASE64编码）
     * @throws Exception 异常
     */
    public static String encryptByPublicKey(String message, String publicKeyFile) throws Exception {
        // 1. 检查文件是否存在（Hutool的FileUtil兼容JDK 25）
        if (!FileUtil.exist(publicKeyFile)) {
            return "";
        }

        // 2. 读取文件内容（Hutool的FileUtil简化读取，自动处理流关闭）
        String fcontent = FileUtil.readString(publicKeyFile, StandardCharsets.UTF_8);

        // 3. 拆分签名和公钥内容（按-----BEGIN分割）
        String sign;
        String publicKey;
        int index = fcontent.indexOf("-----BEGIN");
        if (index >= 0) {
            sign = fcontent.substring(0, index).trim(); // 去除签名前后空格
            publicKey = fcontent.substring(index);
        } else {
            return "";
        }

        // 4. 校验签名和公钥非空（JDK 25的String.isBlank()原生支持）
        if (sign.isBlank() || publicKey.isBlank()) {
            return "";
        }

        // 5. 验证签名（使用内置公钥验证文件中的签名）
        if (!verifySignature(publicKey, sign, INNER_PUBLIC_KEY)) {
            return "";
        }

        // 6. 清理公钥中的标识和换行符（支持BEGIN PUBLIC KEY和BEGIN RSA PUBLIC KEY）
        String cleanPublicKey = publicKey.replaceAll("-----BEGIN (RSA )?PUBLIC KEY-----", "")
                .replaceAll("-----END (RSA )?PUBLIC KEY-----", "")
                .replaceAll("\\n", "")
                .replaceAll("\\r", "") // 额外处理回车符，避免Windows系统问题
                .trim();

        // 7. 使用Hutool的RSA工具加密（注意KeyType是PublicKey，而非PUBLIC）
        RSA rsa = new RSA(null, Base64.decode(cleanPublicKey));
        byte[] encryptedBytes = rsa.encrypt(message.getBytes(StandardCharsets.UTF_8), KeyType.PublicKey);

        // 8. BASE64编码返回（Hutool的Base64）
        return Base64.encode(encryptedBytes);
    }

    /**
     * 私钥解密（保留原有方法名和入参）
     *
     * @param message        已加密数据(BASE64编码)
     * @param privateKeyFile 私钥文件路径(私钥内容BASE64编码)
     * @return 解密数据
     * @throws Exception 异常
     */
    public static String decryptByPrivateKey(String message, String privateKeyFile) throws Exception {
        // 1. 检查文件是否存在
        if (!FileUtil.exist(privateKeyFile)) {
            return "";
        }

        // 2. 读取文件内容
        String fcontent = FileUtil.readString(privateKeyFile, StandardCharsets.UTF_8);

        // 3. 拆分签名和私钥内容
        String sign;
        String privateKey;
        int index = fcontent.indexOf("-----BEGIN");
        if (index >= 0) {
            sign = fcontent.substring(0, index).trim();
            privateKey = fcontent.substring(index);
        } else {
            return "";
        }

        // 4. 校验签名和私钥非空
        if (sign.isBlank() || privateKey.isBlank()) {
            return "";
        }

        // 5. 验证签名（使用内置公钥验证文件中的签名）
        if (!verifySignature(privateKey, sign, INNER_PUBLIC_KEY)) {
            return "";
        }

        // 6. 清理私钥中的标识和换行符（支持BEGIN PRIVATE KEY和BEGIN RSA PRIVATE KEY）
        String cleanPrivateKey = privateKey.replaceAll("-----BEGIN (RSA )?PRIVATE KEY-----", "")
                .replaceAll("-----END (RSA )?PRIVATE KEY-----", "")
                .replaceAll("\\n", "")
                .replaceAll("\\r", "")
                .trim();

        // 7. 使用Hutool的RSA工具解密（注意KeyType是PrivateKey，而非PRIVATE）
        RSA rsa = new RSA(Base64.decode(cleanPrivateKey), null);
        byte[] decryptedBytes = rsa.decrypt(Base64.decode(message), KeyType.PrivateKey);

        // 8. 转换为字符串返回（UTF-8编码）
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * 核心签名验证方法（适配Hutool 5.8.42的Sign和SecureUtil API）
     *
     * @param content   要验证的内容（公钥/私钥文本）
     * @param sign      签名值（BASE64编码）
     * @param publicKey 验证用的公钥（BASE64编码）
     * @return 验证是否成功
     */
    private static boolean verifySignature(String content, String sign, String publicKey) {
        try {
            // 1. 解码公钥（BASE64）
            byte[] publicKeyBytes = Base64.decode(publicKey);
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(publicKeyBytes);
            PublicKey publicK = KeyUtil.generatePublicKey(KEY_ALGORITHM, x509KeySpec);

            // 2. 适配Hutool 5.8.42：直接创建Sign对象（签名验证仅需公钥，私钥传null）
            // 注意：此处使用Sign的构造方法，避开SecureUtil.sign的重载限制
            Sign signer = new Sign(SIGN_ALGORITHMS, null, publicK); // 私钥null，公钥传入用于验证

            // 3. 验证签名：内容字节 + 签名字节（BASE64解码）
            return signer.verify(content.getBytes(StandardCharsets.UTF_8), Base64.decode(sign));
        } catch (Exception e) {
            log.error("RSACryptoUtils -- verifySignature error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 保留原有重载的verify方法（入参为公钥文件路径，兼容原有调用）
     *
     * @param publicKeyFile 公钥文件路径
     * @return 验证是否成功
     */
    public static boolean verify(String publicKeyFile) {
        try {
            if (!FileUtil.exist(publicKeyFile)) {
                return false;
            }

            String fcontent = FileUtil.readString(publicKeyFile, StandardCharsets.UTF_8);
            int index = fcontent.indexOf("-----BEGIN");
            if (index < 0) {
                return false;
            }

            String sign = fcontent.substring(0, index).trim();
            String publicKey = fcontent.substring(index);

            if (sign.isBlank() || publicKey.isBlank()) {
                return false;
            }

            // 调用核心签名验证方法
            return verifySignature(publicKey, sign, INNER_PUBLIC_KEY);
        } catch (Exception e) {
            log.error("RSACryptoUtils -- verify error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 保留原有verify方法（入参为内容、签名、公钥，兼容原有调用）
     *
     * @param content   原值
     * @param sign      签名值
     * @param publicKey 公钥（BASE64编码）
     * @return 验证是否成功
     */
    public static boolean verify(String content, String sign, String publicKey) {
        return verifySignature(content, sign, publicKey);
    }
}