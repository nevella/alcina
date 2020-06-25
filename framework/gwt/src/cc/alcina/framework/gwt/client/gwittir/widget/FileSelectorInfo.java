package cc.alcina.framework.gwt.client.gwittir.widget;

import java.io.Serializable;

import cc.alcina.framework.common.client.csobjects.BaseSourcesPropertyChangeEvents;
import cc.alcina.framework.common.client.util.TopicPublisher.Topic;

public class FileSelectorInfo extends BaseSourcesPropertyChangeEvents
		implements Serializable {

	private String fileName;

	private byte[] bytes;

	private transient Topic<FileSelectorInfo> clearTopicSupport = Topic.local();

	public byte[] getBytes() {
		return this.bytes;
	}

	public String getFileName() {
		return this.fileName;
	}

	public void setBytes(byte[] bytes) {
		byte[] old_bytes = this.bytes;
		this.bytes = bytes;
		propertyChangeSupport().firePropertyChange("bytes", old_bytes, bytes);
	}

	public void setFileName(String fileName) {
		String old_fileName = this.fileName;
		this.fileName = fileName;
		propertyChangeSupport().firePropertyChange("fileName", old_fileName,
				fileName);
	}

	public void clear() {
		setBytes(null);
		setFileName(null);
		topicClear().publish(this);
	}

	public Topic<FileSelectorInfo> topicClear() {
		return clearTopicSupport;
	}
	
	
}
