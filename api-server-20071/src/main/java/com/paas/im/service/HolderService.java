package com.paas.im.service;

import com.paas.im.handler.IDataHandler;

public interface HolderService {

    IDataHandler getHandler(int cmd);
}
