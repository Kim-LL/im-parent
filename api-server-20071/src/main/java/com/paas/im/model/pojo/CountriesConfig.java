package com.paas.im.model.pojo;

import java.util.HashMap;
import java.util.Map;

public class CountriesConfig {

    private CountryConfig[] countryConfig;

    private Map<String,String> countryConfigMap;

    private Map<String,Boolean> countryConfigIsRandom;

    public void setCountryConfig(CountryConfig[] countryConfig) {
        this.countryConfig = countryConfig;
        if (countryConfigMap == null) {
            countryConfigMap = new HashMap<>();
        }else{
            countryConfigMap.clear();
        }

        if(countryConfigIsRandom == null){
            countryConfigIsRandom = new HashMap<>();
        }else{
            countryConfigIsRandom.clear();
        }
        for (CountryConfig countryConfig1 : countryConfig) {
            countryConfigMap.put(countryConfig1.getCountryCode(), countryConfig1.getRouterNums());
            countryConfigIsRandom.put(countryConfig1.getCountryCode(), countryConfig1.isRandom());
        }
    }
}
