package com.netease.nim.camellia.hot.key.sdk.samples;

import com.netease.nim.camellia.codec.*;
import org.bouncycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

/**
 * Created by caojiajun on 2023/7/27
 */
public class TestProps {

    public static void main(String[] args) throws InterruptedException {
        testProps();
        System.out.println("=====");
        testXProps();

    }

    private static void test() {
        Pack pack1 = new Pack();
        pack1.putVarUlong(127);
        pack1.getBuffer().capacity(pack1.getBuffer().readableBytes());
        byte[] array1 = pack1.getBuffer().array();
        System.out.println(Hex.toHexString(array1));
        System.out.println(array1.length);

        Pack pack2 = new Pack();
        pack2.putVarbin("3".getBytes(StandardCharsets.UTF_8));
        pack2.getBuffer().capacity(pack2.getBuffer().readableBytes());
        byte[] array2 = pack2.getBuffer().array();
        System.out.println(Hex.toHexString(array2));
        System.out.println(array2.length);
    }

    private static void testProps() {
        System.out.println("Props");
        Props props = new Props();
        fillValue(props);

        Pack pack = new Pack();
        pack.putMarshallable(props);

        pack.getBuffer().capacity(pack.getBuffer().readableBytes());
        byte[] array = pack.getBuffer().array();
//        System.out.println(Hex.toHexString(array));
        System.out.println(array.length);

        Unpack unpack = new Unpack(array);

        Props props1 = new Props();
        unpack.popMarshallable(props1);

        Props props2 = props.duplicate();

//        System.out.println(props);
//        System.out.println(props1);
//        System.out.println(props2);

        System.out.println(props.equals(props1));
        System.out.println(props.equals(props2));
        System.out.println(props1.equals(props2));
    }

    private static void testXProps() {
        System.out.println("XProps");
        XProps props = new XProps();
        fillValue(props);

        Pack pack = new Pack();
        pack.putMarshallable(props);

        pack.getBuffer().capacity(pack.getBuffer().readableBytes());
        byte[] array = pack.getBuffer().array();
//        System.out.println(Hex.toHexString(array));
        System.out.println(array.length);

        Unpack unpack = new Unpack(array);

        XProps props1 = new XProps();
        unpack.popMarshallable(props1);

        XProps props2 = props.duplicate();

//        System.out.println(props);
//        System.out.println(props1);
//        System.out.println(props2);

        System.out.println(props.equals(props1));
        System.out.println(props.equals(props2));
        System.out.println(props1.equals(props2));
    }

    private static void fillValue(IProps props) {
        props.putByte(1, (byte) 1);
        props.putShort(2, (short) 2);
        props.putInteger(3, 3);
        props.putLong(4, 4L);
        props.putBytes(50, "abc".getBytes(StandardCharsets.UTF_8));
        props.putString(60, "defdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdef");
        props.putFloat(5, 1.0f);
        props.putDouble(6, 6.0);
        props.putDouble(7, Double.MAX_VALUE);
        props.putInteger(8, Integer.MAX_VALUE);
        props.putLong(9, Long.MAX_VALUE);

        System.out.println(props.getByte(1) == (byte) 1);
        System.out.println(props.getShort(2) == (short) 2);
        System.out.println(props.getInteger(3) == 3);
        System.out.println(props.getLong(4) == 4L);
        System.out.println(Arrays.equals(props.getBytes(50), "abc".getBytes(StandardCharsets.UTF_8)));
        System.out.println(props.getString(60).equals("defdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdef"));
        System.out.println(props.getFloat(5) == 1.0f);
        System.out.println(props.getDouble(6) == 6.0);
        System.out.println(props.getDouble(7) == Double.MAX_VALUE);
        System.out.println(props.getInteger(8) == Integer.MAX_VALUE);
        System.out.println(props.getLong(9) == Long.MAX_VALUE);

//        for (int i=10000; i<20000; i++) {
//            props.putString(i, UUID.randomUUID().toString());
//        }
//        for (int i=20000; i<30000; i++) {
//            props.putLong(i, i);
//        }
//        for (int i=30000; i<40000; i++) {
//            props.putDouble(i, i*10.111112222232332232222);
//        }
    }
}
