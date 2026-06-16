package cc.alcina.framework.entity;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public class MimeContent implements Serializable {
	public byte[] bytes;

	public String contentType;

	public MimeContent() {
	}

	public MimeContent(byte[] bytes, String contentType) {
		this.bytes = bytes;
		this.contentType = contentType;
	}

	public MimeContent(InputStream content, String contentType) {
		try {
			this.bytes = Io.read().fromStream(content).asBytes();
			this.contentType = contentType;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public MimeContent(String out, String contentType) {
		this(new ByteArrayInputStream(out.getBytes(StandardCharsets.UTF_8)),
				contentType);
	}
}