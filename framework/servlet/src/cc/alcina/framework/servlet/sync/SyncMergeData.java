package cc.alcina.framework.servlet.sync;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import cc.alcina.framework.common.client.util.StringMap;

public class SyncMergeData {
	private SyncObjectData sourceData;

	private SyncObjectData targetData;

	public Type type = Type.MERGE;

	public String key = null;

	public Map<String, Object> values = new LinkedHashMap<String, Object>();

	public SyncMergeData() {
	}

	public SyncMergeData(SyncObjectData sourceData, SyncObjectData targetData,
			SyncLandscape targetLandscape) {
		this.sourceData = sourceData;
		this.targetData = targetData;
		key = targetLandscape.getPreferredKey(sourceData, targetData);
		if (targetData == null) {
			type = Type.CREATE;
		} else if (sourceData == null) {
			type = Type.DELETE;
		}
		if (type != Type.DELETE) {
			for (String key : sourceData.getNonNullValues().keySet()) {
				if (targetData == null
						|| !targetData.getNonNullValues().keySet()
								.contains(key)) {
					values.put(key, sourceData.getNonNullValues().get(key));
				}
			}
		}
	}

	public SyncObjectData getSourceData() {
		return this.sourceData;
	}

	public SyncObjectData getTargetData() {
		return this.targetData;
	}

	public Type getType() {
		return this.type;
	}

	public Map<String, Object> getValues() {
		return this.values;
	}

	public void setSourceData(SyncObjectData sourceData) {
		this.sourceData = sourceData;
	}

	public void setTargetData(SyncObjectData targetData) {
		this.targetData = targetData;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public void setValues(Map<String, Object> values) {
		this.values = values;
	}

	public Map<String, String> stringValues() {
		StringMap map = new StringMap();
		for (Entry<String, Object> entry : values.entrySet()) {
			map.put(entry.getKey(), String.valueOf(entry.getValue()));
		}
		return map;
	}

	public enum Type {
		CREATE, MERGE, DELETE
	}
}
