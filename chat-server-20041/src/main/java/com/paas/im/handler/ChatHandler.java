package com.paas.im.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import com.paas.im.constant.Constants;
import com.paas.im.model.pojo.ChannelInfo;
import com.paas.im.model.pojo.ErrorResult;
import com.paas.im.model.proto.MessageBuf;
import com.paas.im.model.proto.Packet;
import com.paas.im.service.ChannelService;
import com.paas.im.service.ChatService;
import com.paas.im.service.RPCService;
import com.paas.im.tool.data.DataManager;
import com.paas.im.tool.zookeeper.ZKConfigManager;
import com.paas.im.utils.MessageUtils;
import io.netty.channel.ChannelHandlerContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 私聊聊业务
 */
@Slf4j
@Component
public class ChatHandler implements IBaseHandler {

    @Resource
    private ExecutorService executorService;

    @Resource
    private ChatService chatService;

    @Resource
    private ChannelService channelService;

    @Resource
    private RPCService rpcService;

    @Override
    public void execute(ChannelHandlerContext ctx, Packet packet) {
        try {
            executorService.execute(() -> {
                log.info("ChatHandler execute packet:{}", packet);
                chatMessage(ctx, packet);
            });
        }catch (Exception e){
            log.error("chat is error:{}", e.getMessage(), e);
        }
    }

    private void chatMessage(ChannelHandlerContext ctx, Packet packet) {
        // 是否使用关键字检查
        boolean useShuMeiWordChat = ZKConfigManager.getInstance().getImConfig().isShuMeiWordChat();
        MessageBuf.IMMessage finalMessage = null;
        try {
            MessageBuf.IMMessage message;
            try {
                message = MessageBuf.IMMessage.parseFrom(packet.getBody());
            } catch (InvalidProtocolBufferException e){
                log.error("InvalidProtocolBufferException: {}", e.getMessage());
                return;
            }
            finalMessage = MessageUtils.convertMessage(message);
            // 保存消息(保存到 redis 里, 未读消息只存 msgId)
            boolean saveStatus = saveMessage(finalMessage);
            if (saveStatus) {
                // 成功回执
                messageAck(finalMessage);
            }else {
                // 失败回执
                errorAck(finalMessage);
            }

            distributionMsg(useShuMeiWordChat, finalMessage, packet);
        } catch (Exception e){
            // 失败还需要回执
            Optional.ofNullable(finalMessage).ifPresent(this::errorAck);
            log.error("Chat is error:{}", e.getMessage());
        }
    }

    /**
     * 消息回执
     */
    public void messageAck(MessageBuf.IMMessage message){
        List<ChannelInfo> fromChannelList = channelService.getUserChannelList(message.getFrom(), message.getAppId());
        for(ChannelInfo fromChannel : fromChannelList){
            // 不向 gateway 链接推数据
            MessageBuf.MessageAck.Builder msgAck = MessageBuf.MessageAck.newBuilder();
            msgAck.setMsgId(message.getMsgId());
            msgAck.setType(message.getType());
            msgAck.setClientMsgId(message.getClientMsgId());
            msgAck.setServerTime(message.getServerTime());
            msgAck.setSequence(message.getSequence());

            MessageBuf.Ack.Builder ackBuilder = MessageBuf.Ack.newBuilder();
            // 回执给消息发送者
            ackBuilder.setTo(message.getFrom());
            // 因为用户可能登陆多个设备，要给当前这个用户的所有设备告知
            ackBuilder.setDeviceId(fromChannel.getDeviceId());
            ackBuilder.setType(MessageBuf.AckTypeEnum.SEND_MSG_ACK_VALUE);
            ackBuilder.setStateCode(MessageBuf.AckCodeEnum.ACK_SUCCESS_VALUE);
            ackBuilder.addMessageAck(msgAck.build());
            ackBuilder.setClientMsgId(message.getClientMsgId());
            ackBuilder.setMsgId(message.getMsgId());
            MessageBuf.Ack ackBuf = ackBuilder.build();

            Packet packet = new Packet((byte) MessageBuf.TypeEnum.ACK_VALUE, (byte) MessageBuf.SubTypeEnum.TEXT_VALUE,
                    0, 0, ackBuf.toByteArray());
            try {
                // 发送 ack 消息给 sender 连接的那个服务
                rpcService.sendMessage(fromChannel.getIp(), Constants.CHAT_RPC_PORT, packet);
            } catch (Exception e) {
                log.error("rpcService.sendMessage ip:{},port:{},packet:{},error:{}", fromChannel.getIp(),
                        Constants.CHAT_RPC_PORT, packet, e);
            }

        }
    }

    /**
     * 错误回执
     */
    public void errorAck(MessageBuf.IMMessage message) {
        List<ChannelInfo> fromChannelList = channelService.getUserChannelList(message.getFrom(), message.getAppId());
        for(ChannelInfo fromChannel : fromChannelList){
            MessageBuf.MessageAck.Builder msgAckBuilder = MessageBuf.MessageAck.newBuilder();

            msgAckBuilder.setMsgId(message.getMsgId());
            msgAckBuilder.setType(message.getType());
            msgAckBuilder.setClientMsgId(message.getClientMsgId());
            msgAckBuilder.setServerTime(message.getServerTime());
            msgAckBuilder.setSequence(message.getSequence());

            MessageBuf.Ack.Builder ackBuilder = MessageBuf.Ack.newBuilder();
            ackBuilder.setTo(message.getFrom());
            ackBuilder.setDeviceId(fromChannel.getDeviceId());
            ackBuilder.setType(MessageBuf.AckTypeEnum.SEND_MSG_ACK_VALUE);
            ackBuilder.setStateCode(ErrorResult.CHAT_SAVE_MESSAGE.getCode());
            ackBuilder.addMessageAck(msgAckBuilder);
            MessageBuf.Ack ackBuf = ackBuilder.build();

            Packet packet = new Packet(
                    (byte) MessageBuf.TypeEnum.ACK_VALUE,
                    (byte) MessageBuf.SubTypeEnum.TEXT_VALUE,
                    0, 0, ackBuf.toByteArray()
            );
            try {
                rpcService.sendMessage(fromChannel.getIp(), Constants.CHAT_RPC_PORT, packet);
            }catch (Exception e){
                log.error("rpcService.sendMessage ip:{},port:{},packet:{},error:{}", fromChannel.getIp(),
                        Constants.CHAT_RPC_PORT, packet, e);
            }
            log.info("发送错误回执channelInfo: {}, packet: {},  ackBuf: {}", fromChannel, packet, ackBuf);
        }
    }

    /**
     * 保存消息
     */
    public boolean saveMessage(MessageBuf.IMMessage message) {
        return chatService.saveMessage(message);
    }

    private void distributionMsg(boolean useShuMeiWordChat, MessageBuf.IMMessage message, Packet packet){
        AtomicBoolean isCloseMsgLog = ZKConfigManager.getInstance().getImConfig().getCloseMsgLog();

        //消息推送
        sendMessage(message);

        if(!isCloseMsgLog.get()){
            log.info("sendMessage ok sendMsgToDataServer is begin msgId:{}", message.getMsgId());
        }
        executorService.execute(() -> {
            sendMsgToDataServer(packet, message);
            if(isCloseMsgLog.get()){
                log.info("send chat msg to data! msgId:{}", message.getMsgId());
            }
        });
        // 活动线程警告预知 通过活动线程数和阈值来输出日志
    }

    /**
     * 传输消息至dataServer
     */
    public void sendMsgToDataServer(Packet packet, MessageBuf.IMMessage message){
        try {
            // 获取 from 用户的 dataServer 地址
            String rpcServer = DataManager.getInstance().getDataServerIP(message.getFrom().hashCode());
            Packet packetConstruct = new  Packet(
                    packet.getCmd(),
                    packet.getSubType(),
                    packet.getDiyType(),
                    packet.getDataId(),
                    message.toByteArray()
            );
            if(message.getSaveDB()){
                rpcService.sendMessage(rpcServer, Constants.DATA_RPC_PORT, packetConstruct);
            }
        }catch (Exception e){
            log.error("单聊消息发送 data_server 异常 e:{}, packet: {}", e.getMessage(), packet);
        }
    }

    /**
     * 消息推送, 正常下发消息
     */
    public void sendMessage(MessageBuf.IMMessage message){
        Packet packet;
        // 发送消息
        try {
            // 查询接受者所有设备信息 appId 接受者和发送者都是同一个app，所有可以共用，但是库里通过appId区分不同用户
            List<ChannelInfo> receiveChannelList = channelService.getUserChannelList(message.getTo(), message.getAppId());
            // 发送接受者， 存在链接的情况下
            for(ChannelInfo receiveChannel : receiveChannelList){
                try {
                    // 重新包装消息实体
                    packet = packageMessage(message, receiveChannel.getDeviceId());

                }catch (Exception e){
                    log.info("push chat msg channel error. msg:{},channelInfo:{},e:{}", message,
                            receiveChannel.toString(), e);
                }
            }
        }catch (Exception e){

        }
    }

    /**
     * 封装发送/接受者消息
     */
    private Packet packageMessage(MessageBuf.IMMessage message, String deviceId){
        MessageBuf.IMMessage.Builder msgBuilder = MessageBuf.IMMessage.newBuilder();
        msgBuilder.setAppId(message.getAppId());
        msgBuilder.setFrom(message.getFrom());

        //接受者
        msgBuilder.setTo(message.getTo());
        //接受者设备id
        msgBuilder.setDeviceId(deviceId);

        msgBuilder.setType(message.getType());
        msgBuilder.setSequence(message.getSequence());
        msgBuilder.setTitle(message.getTitle());
        msgBuilder.setMsgId(message.getMsgId());
        msgBuilder.setSequence(message.getSequence());
        msgBuilder.setFlag(message.getFlag());
        msgBuilder.setContent(message.getContent());
        msgBuilder.setClientTime(message.getClientTime());
        msgBuilder.setServerTime(message.getServerTime());
        msgBuilder.setClientMsgId(message.getClientMsgId());
        msgBuilder.setBizStatus(message.getBizStatus());
        msgBuilder.setDeviceType(message.getDeviceType());
        msgBuilder.setMsgUnreadNum(message.getMsgUnreadNum());
        MessageBuf.IMMessage messageBuf = msgBuilder.build();
        return new Packet((byte) MessageBuf.TypeEnum.CHAT_VALUE, (byte)MessageBuf.SubTypeEnum.TEXT_VALUE, 0, 0, messageBuf.toByteArray());
    }
}
