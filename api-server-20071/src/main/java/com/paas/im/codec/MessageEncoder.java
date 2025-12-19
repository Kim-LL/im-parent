package com.paas.im.codec;

import com.paas.im.model.proto.MessageBuf;
import com.paas.im.model.proto.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 消息解码
 * @author wgx
 * @version 2017年10月16日
 *
 *                      Protocol
 *  __ __ __ __ __ __ __ __ __ ____ __ __ __ __ __ __ ____ __ __ _____ __ __ ____ __ __ __ __ __ __ __ __
 * |              |              |           |           |           |           |                         |
 *         1              4            1           1           4           4             Uncertainty
 * |__ __ __ __ __|__ __ __ __ __|__ __ __ __|__ __ __ __|__ __ __ __|__ __ __ __|_ __ __ __ __ __ __ __ __|
 * |              |              |           |           |           |           |                         |
 *   HeaderLength    BodyLength       Cmd       SubType     DiyType      DataId          BodyContent
 * |__ __ __ __ __|__ __ __ __ __|__ __ __ __|__ __ __ __|__ __ __ __|__ __ __ __|__ __ __ ____ __ __ __ __|
 *
 * 协议头15个字节定长
 *     HeaderLength//byte  :包头长，
 *     BodyLength  //int   :包体长，
 *     Cmd         //byte  :同消息type类型
 *     SubType     //byte  :同消息subType类型
 *     DiyType     //int   :直播间自定义消息类型对应数字，如:App:@TXT=1
 *     DataId      //int   :单聊/群聊传cMsgId，直播间为roomId的HashCode
 *     Body 	   //byte[]:协议内容
 *     注：心跳需特殊处理，只传输1个字节的空包，值为-99，无需包头和包体
 */
@ChannelHandler.Sharable
public class MessageEncoder extends MessageToByteEncoder<Packet> {

    public static final MessageEncoder INSTANCE = new MessageEncoder();

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Packet packet, ByteBuf byteBuf) throws Exception {
        if(packet.getCmd() == MessageBuf.TypeEnum.KEEPALIVE_VALUE){
            // 发送的心跳
            byteBuf.writeByte(Packet.KEEPALIVE_BYTE);
        } else if(packet.getCmd() == MessageBuf.TypeEnum.PULL_VALUE){
            // 拉取消息内容
            byteBuf.writeByte(Packet.PULL_MSG_BYTE);
        } else {
            byte[] body = packet.getBody();
            int bodyLength = body != null ? body.length : 0;
            byteBuf.writeByte(Packet.HEADER_LENGTH); //写入1个字节 固定数值 15
            byteBuf.writeInt(bodyLength); //写入4个字节 写入消息实体长度
            byteBuf.writeByte(packet.getCmd()); //写入1个字节 一级分类
            byteBuf.writeByte(packet.getSubType()); //写入1个字节 二级分类
            byteBuf.writeInt(packet.getDiyType()); //写入4个字节 ROOM DIY消息自定义类型
            byteBuf.writeInt(packet.getDataId()); //写入4个字节 数据ID
            if(bodyLength > 0){
                byteBuf.writeBytes(body);
            }
        }

        // 如果私聊，释放内存
        if(packet.getCmd() == MessageBuf.TypeEnum.CHAT_VALUE){
            packet.setBody(null);
        }
    }
}
