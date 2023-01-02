package com.netease.nim.camellia.redis.proxy.reply;

import com.netease.nim.camellia.redis.proxy.util.Utils;
import io.netty.buffer.ByteBuf;

import java.io.IOException;

public interface Reply {

  byte[] CRLF = new byte[] {Utils.CR, Utils.LF};

  void write(ByteBuf byteBuf) throws IOException;
}
