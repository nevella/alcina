package cc.alcina.framework.common.client.sync.property;

public class PropertyModificationLogItem {
	private String propertyName;

	private String value;

	private long modificationTime;

	private String source;

	public long getModificationTime() {
		return this.modificationTime;
	}

	public String getPropertyName() {
		return this.propertyName;
	}

	public String getSource() {
		return this.source;
	}

	public String getValue() {
		return this.value;
	}

	public void setModificationTime(long modificationTime) {
		this.modificationTime = modificationTime;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
