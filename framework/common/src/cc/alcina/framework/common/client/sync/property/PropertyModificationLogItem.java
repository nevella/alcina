package cc.alcina.framework.common.client.sync.property;

import java.util.Date;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.DateStyle;

@Bean
public class PropertyModificationLogItem {
	private String propertyName;

	private String value;

	private long modificationTime;

	private String source;

	private String objectId;

	private String objectClassName;

	private long transformId;

	public PropertyModificationLogItem() {
	}

	public PropertyModificationLogItem(String propertyName, String value,
			long modificationTime, String source, String objectId,
			String objectClassName, long transformId) {
		this.propertyName = propertyName;
		this.value = value;
		this.modificationTime = modificationTime;
		this.source = source;
		this.objectId = objectId;
		this.objectClassName = objectClassName;
		this.transformId = transformId;
	}

	public long getModificationTime() {
		return this.modificationTime;
	}

	public String getObjectClassName() {
		return this.objectClassName;
	}

	public String getObjectId() {
		return this.objectId;
	}

	public String getPropertyName() {
		return this.propertyName;
	}

	public String getSource() {
		return this.source;
	}

	public long getTransformId() {
		return this.transformId;
	}

	public String getValue() {
		return this.value;
	}

	public void setModificationTime(long modificationTime) {
		this.modificationTime = modificationTime;
	}

	public void setObjectClassName(String objectClassName) {
		this.objectClassName = objectClassName;
	}

	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public void setTransformId(long transformId) {
		this.transformId = transformId;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return Ax.format("%s\t%s\t%s",
				CommonUtils.padStringRight(source, 20, ' '),
				DateStyle.DATE_TIME_HUMAN.format(new Date(modificationTime)),
				value);
	};
}
