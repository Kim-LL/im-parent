package com.paas.im.tool.mongo;

import com.mongodb.ServerAddress;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@EqualsAndHashCode(callSuper = false)
@Data
public class ServerAddressExtends extends ServerAddress {

    @Serial
    private static final long serialVersionUID = -5742532740892273013L;

    private String dbName;

    private String username;

    private String pwd;

    private ServerAddress serverAddress;


}
