package com.paas.im.model.proto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Packet implements Serializable {

    @Serial
    private static final long serialVersionUID = 2135797210006675506L;

    //包头长度
    public static final int HEADER_LENGTH = 15;

    //心跳传输内容
    public static final byte KEEPALIVE_BYTE = MessageBuf.TypeEnum.KEEPALIVE_VALUE;

    //拉取消息内容
    public static final byte PULL_MSG_BYTE = MessageBuf.TypeEnum.PULL_VALUE;

    //命令 对应协议中 TypeEnum
    private byte cmd;

    //二级分类  对应协议中 SubTypeEnum
    private byte subType;

    //ROOM DIY消息自定义类型
    private int diyType;

    //数据ID
    private int dataId;

    //transient
    private byte[] body;

    public Packet(byte cmd){
        this.cmd = cmd;
    }

    @Override
    public String toString() {
        return "Packet [cmd="+ cmd +", subType="+ subType +", diyType=" + diyType + ", dataId=" + dataId + ", body=" + getBody().length + "]";
    }
}
