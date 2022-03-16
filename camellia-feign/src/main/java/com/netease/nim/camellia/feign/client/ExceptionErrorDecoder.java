package com.netease.nim.camellia.feign.client;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.feign.exception.CamelliaFeignException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 *
 * Created by caojiajun on 2022/3/16
 */
public class ExceptionErrorDecoder implements ErrorDecoder {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionErrorDecoder.class);

    private final Class<? extends Exception> exceptionClazz;

    public ExceptionErrorDecoder(Class<? extends Exception> exceptionClazz) {
        this.exceptionClazz = exceptionClazz;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        Exception exception;
        try {
            InputStream inputStream = response.body().asInputStream();
            String body = responseToString(inputStream);
            exception = JSONObject.parseObject(body, exceptionClazz);
        } catch (Exception e) {
            exception = new CamelliaFeignException("http.code=" + response.status());
        }
        if (response.status() == 500) {
            logger.error("request error, methodKey = {}", methodKey, exception);
            return new CamelliaFeignException(exception.getMessage(), exception);
        } else {
            return exception;
        }
    }

    private String responseToString(InputStream stream) throws IOException {
        int bufferSize = 1024;
        char[] buffer = new char[bufferSize];
        StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(stream, StandardCharsets.UTF_8);
        for (int numRead; (numRead = in.read(buffer, 0, buffer.length)) > 0; ) {
            out.append(buffer, 0, numRead);
        }
        return out.toString();
    }
}
