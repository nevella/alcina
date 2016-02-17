package cc.alcina.framework.common.client.sync.property;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public interface HasPropertyLog extends HasIdAndLocalId {
	default PropertyModificationLog providePropertyModificationLog() {
		return new PropertyModificationLogSerializer()
				.deserialize(getPropertyModificationLog());
	}

	String getPropertyModificationLog();

	default void putPropertyModificationLog(PropertyModificationLog log) {
		setPropertyModificationLog(
				new PropertyModificationLogSerializer().serialize(log));
	}

	void setPropertyModificationLog(String propertyModificationLog);
}
