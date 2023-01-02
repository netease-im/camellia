package com.netease.nim.camellia.console.conf;

import com.netease.nim.camellia.console.model.WebResult;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
@ControllerAdvice
public class ExceptionController {

    @ResponseBody
    @ExceptionHandler(Exception.class)
    public WebResult globalAppException(HttpServletResponse response,Exception e){
        return WebResult.Exception(e);
    }


}
