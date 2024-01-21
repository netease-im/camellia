package com.netease.nim.camellia.redis.proxy.http;


import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.reply.ReplyPack;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2024/1/21
 */
public class HttpCommandTask {

    private static final Logger logger = LoggerFactory.getLogger(HttpCommandTask.class);
    private final HttpCommandTaskQueue taskQueue;
    private final HttpCommandTaskRequest request;
    private HttpCommandTaskResponse response;
    private final List<Reply> replyList = new ArrayList<>();
    private boolean finish = false;

    public HttpCommandTask(HttpCommandTaskQueue taskQueue, HttpCommandTaskRequest request) {
        this.taskQueue = taskQueue;
        this.request = request;
    }

    public void addResponse(Object response) {
        if (finish) {
            logger.error("HttpCommandTask is finish, duplicate response");
            return;
        }
        if (response instanceof HttpResponse) {
            this.response = new HttpCommandTaskResponse((HttpResponse) response, request.isKeepAlive());
            taskQueue.callback();
            finish = true;
            return;
        }
        HttpCommandRequest httpCommandRequest = request.getHttpCommandRequest();
        if (response instanceof HttpCommandReply) {
            HttpResponse httpResponse = toHttpResponse(request, (HttpCommandReply) response);
            this.response = new HttpCommandTaskResponse(httpResponse, request.isKeepAlive());
            taskQueue.callback();
            finish = true;
            return;
        }
        Reply reply;
        if (response instanceof ReplyPack) {
            reply = ((ReplyPack) response).getReply();
        } else if (response instanceof Reply) {
            reply = (Reply) response;
        } else {
            taskQueue.getChannelInfo().getCtx().close();
            throw new IllegalArgumentException("illegal response");
        }
        replyList.add(reply);
        if (httpCommandRequest.getCommands().size() == replyList.size()) {
            List<Object> replies = HttpCommandReplyConverter.convert(replyList, httpCommandRequest.isReplyBase64());
            HttpCommandReply httpCommandReply = new HttpCommandReply();
            httpCommandReply.setRequestId(httpCommandRequest.getRequestId());
            httpCommandReply.setCode(200);
            httpCommandReply.setCommands(httpCommandRequest.getCommands());
            httpCommandReply.setReplies(replies);

            HttpResponse httpResponse = toHttpResponse(request, httpCommandReply);
            this.response = new HttpCommandTaskResponse(httpResponse, request.isKeepAlive());
            taskQueue.callback();
            finish = true;
            replyList.clear();
        }
    }

    private HttpResponse toHttpResponse(HttpCommandTaskRequest request, HttpCommandReply httpCommandReply) {
        String string = JSONObject.toJSONString(httpCommandReply);
        if (httpCommandReply.getCode() != 200) {
            taskQueue.failMonitor("code=" + httpCommandReply.getCode() + ",msg=" + httpCommandReply.getMsg());
        }
        ByteBuf byteBuf = Unpooled.wrappedBuffer(string.getBytes(StandardCharsets.UTF_8));
        return new DefaultFullHttpResponse(request.getHttpVersion(), HttpResponseStatus.OK, byteBuf);
    }

    public HttpCommandTaskResponse getResponse() {
        return response;
    }
}
