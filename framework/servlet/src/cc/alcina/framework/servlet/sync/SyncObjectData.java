package cc.alcina.framework.servlet.sync;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class SyncObjectData implements Serializable {
	private String objectKey;

	private Map<String, Object> nonNullValues = new LinkedHashMap<String, Object>();

	public String getObjectKey() {
		return this.objectKey;
	}

	public void setObjectKey(String objectKey) {
		this.objectKey = objectKey;
	}

	public Map<String, Object> getNonNullValues() {
		return this.nonNullValues;
	}

	public void setNonNullValues(Map<String, Object> nonNullValues) {
		this.nonNullValues = nonNullValues;
	}

	
}
