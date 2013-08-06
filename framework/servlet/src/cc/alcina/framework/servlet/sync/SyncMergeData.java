package cc.alcina.framework.servlet.sync;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import cc.alcina.framework.common.client.util.StringMap;

public class SyncMergeData {
	private SyncObjectData sourceData;

	private SyncObjectData targetData;

	public SyncMergeData() {
	}

	public SyncMergeData(SyncObjectData sourceData, SyncObjectData targetData) {
		this.sourceData = sourceData;
		this.targetData = targetData;
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

	public enum Type {
		CREATE, MERGE, DELETE
	}

	public Type type = Type.MERGE;

	public Map<String, Object> values = new LinkedHashMap<String, Object>();
	
	public Map<String, String> stringValues(){
		StringMap map=new StringMap();
		for (Entry<String, Object> entry : values.entrySet()) {
			map.put(entry.getKey(), String.valueOf(entry.getValue()));
		}
		return map;
		
	}

	public SyncObjectData getSourceData() {
		return this.sourceData;
	}

	public void setSourceData(SyncObjectData sourceData) {
		this.sourceData = sourceData;
	}

	public SyncObjectData getTargetData() {
		return this.targetData;
	}

	public void setTargetData(SyncObjectData targetData) {
		this.targetData = targetData;
	}

	public Type getType() {
		return this.type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Map<String, Object> getValues() {
		return this.values;
	}

	public void setValues(Map<String, Object> values) {
		this.values = values;
	}
}
