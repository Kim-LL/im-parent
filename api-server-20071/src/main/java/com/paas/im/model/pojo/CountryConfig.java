package com.paas.im.model.pojo;

import lombok.Data;

@Data
public class CountryConfig {

    private String countryCode;

    private String routerNums;

    private boolean random;
}
