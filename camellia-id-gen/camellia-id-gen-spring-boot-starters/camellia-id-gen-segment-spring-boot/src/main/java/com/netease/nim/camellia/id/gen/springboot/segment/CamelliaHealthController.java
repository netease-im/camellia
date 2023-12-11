package com.netease.nim.camellia.id.gen.springboot.segment;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

/**
 * Created by caojiajun on 2023/1/15
 */
@RestController
@RequestMapping("/camellia/health")
public class CamelliaHealthController {

    private static final String OK = "{\"code\":200}";
    private static final String ERROR = "{\"code\":500}";

    @RequestMapping(value = "/status")
    public void status(HttpServletResponse response) throws Exception {
        if (CamelliaIdGenSegmentServerStatus.getStatus() == CamelliaIdGenSegmentServerStatus.Status.ONLINE) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println(OK);
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(ERROR);
        }
    }

    @RequestMapping(value = "/check")
    public void check(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println(OK);
    }

    @RequestMapping(value = "/online")
    public void online(HttpServletResponse response) throws Exception {
        CamelliaIdGenSegmentServerStatus.setStatus(CamelliaIdGenSegmentServerStatus.Status.ONLINE);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println(OK);
    }

    @RequestMapping(value = "/offline")
    public void offline(HttpServletResponse response) throws Exception {
        CamelliaIdGenSegmentServerStatus.setStatus(CamelliaIdGenSegmentServerStatus.Status.OFFLINE);
        if (CamelliaIdGenSegmentServerStatus.isIdle()) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println(OK);
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(ERROR);
        }
    }

}
