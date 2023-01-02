package com.netease.nim.camellia.dashboard.conf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netease.nim.camellia.dashboard.controller.WebResult;
import com.netease.nim.camellia.dashboard.exception.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * Created by caojiajun on 2018/5/14.
 */
@Component
public class ExceptionHandler implements HandlerExceptionResolver {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public ModelAndView resolveException(HttpServletRequest httpServletRequest,
                                         HttpServletResponse httpServletResponse,
                                         Object o, Exception ex) {
        try {
            AppException exception;
            httpServletResponse.setContentType(MediaType.APPLICATION_JSON.toString());
            if (ex instanceof AppException) {
                exception = (AppException) ex;
                httpServletResponse.setStatus(exception.getCode());
            } else {
                httpServletResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                exception = new AppException();
                exception.setMsg(ex.toString());
                exception.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
                logger.error("error, uri = {}", httpServletRequest.getRequestURI(), ex);
            }
            mapper.writeValue(httpServletResponse.getOutputStream(), new WebResult(exception.getCode(), exception.getMsg()));
        } catch (Exception e) {
            logger.error("response error, uri = {}", httpServletRequest.getRequestURI(), ex);
        }
        return new ModelAndView();
    }
}
