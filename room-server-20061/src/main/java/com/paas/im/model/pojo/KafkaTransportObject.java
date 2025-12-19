package com.paas.im.model.pojo;

import com.paas.im.model.proto.Packet;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

@Getter
public class KafkaTransportObject implements Serializable {

    @Serial
    private static final long serialVersionUID = -261618420269816047L;

    private Packet packet;

    private long time;

    public KafkaTransportObject(Packet packet, long time) {
        this.packet = packet;
        this.time = time;
    }

}
