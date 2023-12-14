package com.netease.nim.camellia.id.gen.springboot.segment;

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
        if (CamelliaIdGenSegmentServerStatus.getStatus() == CamelliaIdGenSegmentServerStatus.Status.ONLINE) {
            return new ResponseEntity<>(OK, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/check")
    public ResponseEntity<String> check() {
        return new ResponseEntity<>(OK, HttpStatus.OK);
    }

    @RequestMapping(value = "/online")
    public ResponseEntity<String> online() {
        CamelliaIdGenSegmentServerStatus.setStatus(CamelliaIdGenSegmentServerStatus.Status.ONLINE);
        return new ResponseEntity<>(OK, HttpStatus.OK);
    }

    @RequestMapping(value = "/offline")
    public ResponseEntity<String> offline() {
        CamelliaIdGenSegmentServerStatus.setStatus(CamelliaIdGenSegmentServerStatus.Status.OFFLINE);
        if (CamelliaIdGenSegmentServerStatus.isIdle()) {
            return new ResponseEntity<>(OK, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
