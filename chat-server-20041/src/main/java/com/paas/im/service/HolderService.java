package com.paas.im.service;

import com.paas.im.handler.IBaseHandler;

public interface HolderService {

    IBaseHandler getHandler(int cmd);
}
