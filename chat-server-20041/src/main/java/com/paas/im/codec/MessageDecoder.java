package com.paas.im.codec;

import com.paas.im.model.proto.MessageBuf;
import com.paas.im.model.proto.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

@ChannelHandler.Sharable
public final class MessageDecoder extends ByteToMessageDecoder {

    public static final MessageDecoder INSTANCE = new MessageDecoder();

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        decodeByte(byteBuf, list);
        decodeFrames(byteBuf, list);
    }

    private void decodeByte(ByteBuf in, List<Object> out){
        while (in.isReadable()) {
            byte b = in.readByte();
            if(b == Packet.KEEPALIVE_BYTE){
                Packet keepalivePacket = new Packet((byte) MessageBuf.TypeEnum.KEEPALIVE_VALUE);
                out.add(keepalivePacket);
            } else if(b == Packet.PULL_MSG_BYTE){
                Packet pullPacket = new Packet((byte) MessageBuf.TypeEnum.PULL_VALUE);
                out.add(pullPacket);
            } else {
              in.readerIndex(in.readerIndex() - 1);
              break;
            }
        }
    }

    private void decodeFrames(ByteBuf in, List<Object> out){
        if(in.readableBytes() < Packet.HEADER_LENGTH){
            // 当前获取的数据连最基本的 前15个固定字节都达不到
            return;
        }
        while (in.isReadable()) {
            //记录当前读取位置，如果读取到非完整的frame，恢复到该位置
            in.markReaderIndex();
            Packet packet = decodeFrame(in);
            if(packet == null){
                // 接收到的量不足以合成一个消息实体
                in.resetReaderIndex();
                break;
            }
            out.add(packet);
        }
    }

    /**
     * byteBuf.writeByte(Packet.HEADER_LENGTH); //写入1个字节 固定数值 15, 除了 body 后其他的加一起 15个字节
     * byteBuf.writeInt(bodyLength); //写入4个字节 写入消息实体长度
     * byteBuf.writeByte(packet.cmd); //写入1个字节 一级分类
     * byteBuf.writeByte(packet.subType); //写入1个字节 二级分类
     * byteBuf.writeInt(packet.diyType); //写入4个字节 ROOM DIY消息自定义类型
     * byteBuf.writeInt(packet.dataId); //写入4个字节 数据ID
     */
    private Packet decodeFrame(ByteBuf in){
        // 可读字节数
        int readableBytes = in.readableBytes();
        byte headerLength = in.readByte(); // 写入的时候传的 writeByte(15) 硬编码
        int bodyLength = in.readInt();
        if(readableBytes < (headerLength + bodyLength)){
            // 当前不够一个实体消息
            return null;
        }

        Packet packet = new Packet();
        // 之前已经读了一个 byte 和 一个 int，所以从第三个开始读
        packet.setCmd(in.readByte()); // 一级消息类型
        packet.setSubType(in.readByte()); // 二级消息类型
        packet.setDiyType(in.readInt());
        packet.setDataId(in.readInt());
        if(bodyLength > 0){
            byte[] body = new byte[bodyLength];
            in.readBytes(body);
            packet.setBody(body);
        }
        return packet;
    }
}
