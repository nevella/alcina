package cc.alcina.framework.entity.util;

/**
 * A simple container for filedata, sufficient to be transformed to a download
 */
public class FileData {
	public byte[] bytes;

	public String name;

	public String contentType;

	public FileData() {
	}

	public FileData(byte[] bytes, String name, String contentType) {
		this.bytes = bytes;
		this.name = name;
		this.contentType = contentType;
	}
}
