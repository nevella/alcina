package cc.alcina.framework.gwt.client.gwittir.widget;

import java.io.Serializable;

import cc.alcina.framework.common.client.csobjects.BaseSourcesPropertyChangeEvents;

public class FileSelectorInfo extends BaseSourcesPropertyChangeEvents implements Serializable {
	private String fileName;

	private byte[] bytes;

	public String getFileName() {
		return this.fileName;
	}

	public void setFileName(String fileName) {
		String old_fileName = this.fileName;
		this.fileName = fileName;
		propertyChangeSupport.firePropertyChange("fileName", old_fileName,
				fileName);
	}

	public byte[] getBytes() {
		return this.bytes;
	}

	public void setBytes(byte[] bytes) {
		byte[] old_bytes = this.bytes;
		this.bytes = bytes;
		propertyChangeSupport.firePropertyChange("bytes", old_bytes, bytes);
	}
}
