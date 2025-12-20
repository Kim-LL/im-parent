package com.paas.im.controller;

import com.alibaba.fastjson2.JSONObject;
import com.paas.im.constant.Constants;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;


import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@RestController
@RequestMapping(value = "/api/rest", produces = {MediaType.APPLICATION_JSON_VALUE})
public class ApiController {

    @GetMapping(value = "/cluster")
    public Flux<@NonNull String> getCluster(){
        List<String> cluster = new ArrayList<>();
        cluster.add(Constants.API_CLUSTER);
        cluster.add(Constants.CHAT_CLUSTER);
        cluster.add(Constants.GROUP_CHAT_CLUSTER);
        cluster.add(Constants.ROOM_CLUSTER);
        return Flux.fromIterable(cluster);
    }

    @GetMapping(value = "/login")
    public Mono<@NonNull String> login(){
        JSONObject job  = new JSONObject();
        job.put("username", "admin");
        job.put("password", "123456");
        job.put("role", "admin");
        return Mono.create(new Consumer<MonoSink<@NonNull String>>() {
            @Override
            public void accept(MonoSink<@NonNull String> stringMonoSink) {
                stringMonoSink.success(job.toJSONString());
            }
        });
    }

}
