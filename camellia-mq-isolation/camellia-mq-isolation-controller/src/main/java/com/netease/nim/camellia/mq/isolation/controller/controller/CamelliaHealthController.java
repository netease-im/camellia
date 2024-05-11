package com.netease.nim.camellia.mq.isolation.controller.controller;

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
        if (CamelliaMqIsolationControllerStatus.getStatus() == CamelliaMqIsolationControllerStatus.Status.ONLINE) {
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
        CamelliaMqIsolationControllerStatus.setStatus(CamelliaMqIsolationControllerStatus.Status.ONLINE);
        CamelliaMqIsolationControllerStatus.invokeOnlineCallback();
        return ResponseEntity.ok(OK);
    }

    @RequestMapping(value = "/offline")
    public ResponseEntity<String> offline() {
        CamelliaMqIsolationControllerStatus.setStatus(CamelliaMqIsolationControllerStatus.Status.OFFLINE);
        CamelliaMqIsolationControllerStatus.invokeOfflineCallback();
        if (CamelliaMqIsolationControllerStatus.isIdle()) {
            return ResponseEntity.ok(OK);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERROR);
        }
    }
}
