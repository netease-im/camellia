package com.netease.nim.camellia.id.gen.springboot.snowflake;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by caojiajun on 2023/1/15
 */
@RestController
@RequestMapping("/camellia/health")
public class CamelliaHealthController {

    private static final String OK = "{\"code\":200}";
    private static final String ERROR = "{\"code\":500}";

    @RequestMapping(value = "/status")
    public ResponseEntity<String> status() {
        if (CamelliaIdGenSnowflakeServerStatus.getStatus() == CamelliaIdGenSnowflakeServerStatus.Status.ONLINE) {
            return ResponseEntity.ok(OK);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERROR);
        }
    }

    @RequestMapping(value = "/check")
    public ResponseEntity<String> check() {
        return ResponseEntity.ok(OK);
    }

    @RequestMapping(value = "/online")
    public ResponseEntity<String> online() {
        CamelliaIdGenSnowflakeServerStatus.setStatus(CamelliaIdGenSnowflakeServerStatus.Status.ONLINE);
        CamelliaIdGenSnowflakeServerStatus.invokeOnlineCallback();
        return ResponseEntity.ok(OK);
    }

    @RequestMapping(value = "/offline")
    public ResponseEntity<String> offline() {
        CamelliaIdGenSnowflakeServerStatus.setStatus(CamelliaIdGenSnowflakeServerStatus.Status.OFFLINE);
        CamelliaIdGenSnowflakeServerStatus.invokeOfflineCallback();
        if (CamelliaIdGenSnowflakeServerStatus.isIdle()) {
            return ResponseEntity.ok(OK);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERROR);
        }
    }

}

