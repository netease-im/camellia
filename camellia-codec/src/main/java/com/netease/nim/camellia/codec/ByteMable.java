package com.netease.nim.camellia.codec;


public class ByteMable implements Marshallable {

	private byte data;

	public ByteMable(byte _in) {
		data = _in;
	}

	public ByteMable() {
	}

	public byte getData() {
		return data;
	}

	public void marshal(Pack p) {
		p.putByte(data);
	}

	public void unmarshal(Unpack up) {
		data = up.popByte();
	}
}
