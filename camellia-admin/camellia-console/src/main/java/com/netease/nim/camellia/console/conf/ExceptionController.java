package com.netease.nim.camellia.console.conf;

import com.netease.nim.camellia.console.model.WebResult;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;


/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
@ControllerAdvice
public class ExceptionController {

    @ResponseBody
    @ExceptionHandler(Exception.class)
    public WebResult globalAppException(HttpServletResponse response, Exception e){
        return WebResult.Exception(e);
    }


}
