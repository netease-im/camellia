package com.netease.nim.camellia.feign;

import feign.*;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.okhttp.OkHttpClient;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2022/3/1
 */
public class CamelliaFeignProps {

    public static final okhttp3.OkHttpClient okHttpClient;
    static {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(2048);
        dispatcher.setMaxRequestsPerHost(256);
        okHttpClient = new okhttp3.OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(256, 5, TimeUnit.SECONDS))
                .dispatcher(dispatcher)
                .build();
    }

    public static final Logger.Level defaultLevel = Logger.Level.NONE;
    public static final Contract defaultContract = new Contract.Default();
    public static final Client defaultClient = new OkHttpClient(okHttpClient);
    public static final Retryer defaultRetry = Retryer.NEVER_RETRY;
    public static final Logger defaultLogger = new Logger.NoOpLogger();
    public static final Encoder defaultEncoder = new Encoder.Default();
    public static final Decoder defaultDecoder = new Decoder.Default();
    public static final ErrorDecoder defaultErrorDecoder = new ErrorDecoder.Default();
    public static final Request.Options defaultOptions = new Request.Options();
    public static final InvocationHandlerFactory defaultInvocationHandlerFactory = new InvocationHandlerFactory.Default();

    private final List<RequestInterceptor> requestInterceptors = new ArrayList<>();
    private Logger.Level logLevel;
    private Contract contract;
    private Client client;
    private Retryer retryer;
    private Logger logger;
    private Encoder encoder;
    private Decoder decoder;
    private ErrorDecoder errorDecoder;
    private Request.Options options;
    private InvocationHandlerFactory invocationHandlerFactory;
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
