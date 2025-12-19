package com.paas.im.controller;

import com.alibaba.fastjson2.JSON;
import com.paas.im.constant.Constants;
import com.paas.im.service.ChatService;
import jakarta.annotation.Resource;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Set;

@RestController
@RequestMapping(value = {"/chat"}, produces = {"application/json;charset=UTF-8"})
public class ChatController {

    @Resource
    private ChatService chatService;

    @GetMapping(value = {"/cluster/servers"})
    public Mono<@NonNull String> getClusterServers(){
        return Mono.fromCallable(() -> {
            Set<String> servers = chatService.getServers(Constants.CHAT_CLUSTER);
            return JSON.toJSONString(servers);
        });
    }
}
