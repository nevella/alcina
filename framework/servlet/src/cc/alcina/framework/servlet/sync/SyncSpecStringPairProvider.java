package cc.alcina.framework.servlet.sync;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.totsp.gwittir.client.beans.Converter;

public class SyncSpecStringPairProvider {
	public Map<String, SyncConversionSpec> byLeft = new LinkedHashMap<String, SyncConversionSpec>();

	public Map<String, SyncConversionSpec> byRight = new LinkedHashMap<String, SyncConversionSpec>();

	public SyncSpecStringPairProvider(String[] pairs) {
		for (int i = 0; i < pairs.length; i += 2) {
			SyncConversionSpec spec = new SyncConversionSpec(pairs[i],
					pairs[i + 1]);
			byLeft.put(spec.getLeft(), spec);
			byRight.put(spec.getRight(), spec);
		}
	}

	public void prepareMergeData(List<SyncMergeData> dataList,
			boolean reverseMap) {
		for (SyncMergeData data : dataList) {
			Map<String, Object> replace = new LinkedHashMap<String, Object>();
			for (Entry<String, Object> entry : data.values.entrySet()) {
				SyncConversionSpec spec = reverseMap ? byRight.get(entry
						.getKey()) : byLeft.get(entry.getKey());
				String otherKey = reverseMap ? spec.getLeft() : spec.getRight();
				Object value = entry.getValue();
				if (spec.getLeftToRight() != null) {
					Converter converter = reverseMap ? spec.getRightToLeft()
							: spec.getLeftToRight();
					value = converter.convert(value);
				}
				replace.put(otherKey, value);
			}
			data.setValues(replace);
		}
	}
}
