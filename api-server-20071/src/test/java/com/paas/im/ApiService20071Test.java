package com.paas.im;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest(classes = ApiServer20071.class)
public class ApiService20071Test {

    static void main() {
        String server = "127.0.0.1";
        String a = null;

        log.info("result: {}", server + a);
    }

}
