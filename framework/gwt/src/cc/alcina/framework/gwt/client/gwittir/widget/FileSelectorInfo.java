package cc.alcina.framework.gwt.client.gwittir.widget;

import java.io.Serializable;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.util.TopicPublisher.Topic;

@Reflected
public class FileSelectorInfo extends Bindable
		implements Serializable, TreeSerializable {
	private String fileName;

	private byte[] bytes;

	private transient Topic<FileSelectorInfo> clearTopic = Topic.local();

	public void clear() {
		setBytes(null);
		setFileName(null);
		topicClear().publish(this);
	}

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

	public Topic<FileSelectorInfo> topicClear() {
		return clearTopic;
	}
}
