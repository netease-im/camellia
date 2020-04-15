package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.ArrayList;
import java.util.List;

public class CommandDecoder extends ReplayingDecoder<Void> {

    private List<Command> commands = new ArrayList<>();
    private byte[][] bytes;
    private int index = 0;

    private int commandMaxBatchSize = 256;

    public CommandDecoder(int commandDecodeMaxBatchSize) {
        super();
        if (commandDecodeMaxBatchSize > 0) {
            this.commandMaxBatchSize = commandDecodeMaxBatchSize;
        }
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        try {
            if (bytes != null) {
                int numArgs = bytes.length;
                for (int i = index; i < numArgs; i++) {
                    if (in.readByte() == '$') {
                        long l = Utils.readLong(in);
                        if (l > Integer.MAX_VALUE) {
                            throw new IllegalArgumentException("Java only supports arrays up to " + Integer.MAX_VALUE + " in size");
                        }
                        int size = (int) l;
                        bytes[i] = new byte[size];
                        in.readBytes(bytes[i]);
                        if (in.bytesBefore((byte) Utils.CR) != 0) {
                            throw new IllegalArgumentException("Argument doesn't end in CRLF");
                        }
                        in.skipBytes(2);
                        checkpoint();
                        index = i+1;
                    } else {
                        throw new IllegalArgumentException("Unexpected character");
                    }
                }
                try {
                    Command command = new Command(bytes);
                    commands.add(command);
                    if (commands.size() >= commandMaxBatchSize) {
                        out.add(commands);
                        commands = new ArrayList<>();
                    }
                } finally {
                    bytes = null;
                    index = 0;
                }
                decode(ctx, in, out);
            } else if (in.readByte() == '*') {
                long l = Utils.readLong(in);
                if (l > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException("Java only supports arrays up to " + Integer.MAX_VALUE + " in size");
                }
                int numArgs = (int) l;
                if (numArgs < 0) {
                    throw new IllegalArgumentException("Invalid size: " + numArgs);
                }
                bytes = new byte[numArgs][];
                checkpoint();
                decode(ctx, in, out);
            } else {
                throw new IllegalArgumentException("Command not start with *");
            }
        } catch (Throwable e) {
            if (!commands.isEmpty()) {
                out.add(commands);
                commands = new ArrayList<>();
            }
            throw e;
        }
    }

}
