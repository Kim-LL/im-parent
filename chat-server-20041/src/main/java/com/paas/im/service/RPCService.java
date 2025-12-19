package com.paas.im.service;

import com.paas.im.model.proto.Packet;

public interface RPCService {

    void sendMessage(String server, int port, Packet packet);
}
