package cc.alcina.framework.gwt.client.gwittir.widget;

import java.io.Serializable;
import java.util.function.Consumer;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.util.Topic;

@Reflected
public class FileData extends Bindable
		implements Serializable, TreeSerializable {
	public static void fromFile(Html5File file, Consumer<FileData> consumer) {
		FileData fileData = new FileData();
		fileData.setFileName(file.getFileName());
		readAsBinaryString(file, new AsyncCallback<String>() {
			@Override
			public void onFailure(Throwable caught) {
				throw new WrappedRuntimeException(caught);
			}

			@Override
			public void onSuccess(String result) {
				int length = result.length();
				byte[] bytes = new byte[length];
				for (int i = 0; i < length; i++) {
					bytes[i] = (byte) result.charAt(i);
				}
				fileData.setBytes(bytes);
				consumer.accept(fileData);
			}
		});
	}

	static native void readAsBinaryString(Html5File file,
			AsyncCallback<String> callback)/*-{
    var reader = new FileReader();
    reader.onloadend = function() {
      callback.@com.google.gwt.user.client.rpc.AsyncCallback::onSuccess(Ljava/lang/Object;)(reader.result);

    };
    reader.readAsBinaryString(file);
	}-*/;

	private String fileName;

	private byte[] bytes;

	private transient Topic<FileData> clearTopic = Topic.create();

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

	public Topic<FileData> topicClear() {
		return clearTopic;
	}
}
