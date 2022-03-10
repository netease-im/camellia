package com.netease.nim.camellia.feign;

import feign.*;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2022/3/1
 */
public class CamelliaFeignProps {

    private final List<RequestInterceptor> requestInterceptors = new ArrayList<>();
    private Logger.Level logLevel = Logger.Level.NONE;
    private Contract contract = new Contract.Default();
    private Client client = new Client.Default(null, null);
    private Retryer retryer = new Retryer.Default();
    private Logger logger = new Logger.NoOpLogger();
    private Encoder encoder = new Encoder.Default();
    private Decoder decoder = new Decoder.Default();
    private ErrorDecoder errorDecoder = new ErrorDecoder.Default();
    private Request.Options options = new Request.Options();
    private InvocationHandlerFactory invocationHandlerFactory = new InvocationHandlerFactory.Default();
    private boolean decode404;

    public List<RequestInterceptor> getRequestInterceptors() {
        return requestInterceptors;
    }

    public Logger.Level getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(Logger.Level logLevel) {
        this.logLevel = logLevel;
    }

    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Retryer getRetryer() {
        return retryer;
    }

    public void setRetryer(Retryer retryer) {
        this.retryer = retryer;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public Encoder getEncoder() {
        return encoder;
    }

    public void setEncoder(Encoder encoder) {
        this.encoder = encoder;
    }

    public Decoder getDecoder() {
        return decoder;
    }

    public void setDecoder(Decoder decoder) {
        this.decoder = decoder;
    }

    public ErrorDecoder getErrorDecoder() {
        return errorDecoder;
    }

    public void setErrorDecoder(ErrorDecoder errorDecoder) {
        this.errorDecoder = errorDecoder;
    }

    public Request.Options getOptions() {
        return options;
    }

    public void setOptions(Request.Options options) {
        this.options = options;
    }

    public InvocationHandlerFactory getInvocationHandlerFactory() {
        return invocationHandlerFactory;
    }

    public void setInvocationHandlerFactory(InvocationHandlerFactory invocationHandlerFactory) {
        this.invocationHandlerFactory = invocationHandlerFactory;
    }

    public boolean isDecode404() {
        return decode404;
    }

    public void setDecode404(boolean decode404) {
        this.decode404 = decode404;
    }
}
