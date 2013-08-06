package cc.alcina.framework.servlet.sync;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.totsp.gwittir.client.beans.Converter;

public class SyncSpecStringPairProvider {
	public Map<String, SyncConversionSpec> byName1 = new LinkedHashMap<String, SyncConversionSpec>();

	public Map<String, SyncConversionSpec> byName2 = new LinkedHashMap<String, SyncConversionSpec>();

	public SyncSpecStringPairProvider(String[] pairs) {
		for (int i = 0; i < pairs.length; i += 2) {
			SyncConversionSpec spec = new SyncConversionSpec(pairs[i],
					pairs[i + 1]);
			byName1.put(spec.getName1(), spec);
			byName2.put(spec.getName2(), spec);
		}
	}

	public void prepareMergeData(List<SyncMergeData> dataList, boolean reverseMap){
		for (SyncMergeData data : dataList) {
			Map<String,Object> replace=new LinkedHashMap<String, Object>();
			for (Entry<String, Object> entry : data.values.entrySet()) {
				SyncConversionSpec spec=reverseMap?byName2.get(entry.getKey()):byName1.get(entry.getKey());
				String otherKey=reverseMap?spec.getName1():spec.getName2();
				Object value=entry.getValue();
				if(spec.getConverter()!=null){
					Converter converter=reverseMap?spec.getReverseConverter():spec.getConverter();
					value=converter.convert(value);
				}
				replace.put(otherKey, value);
			}
			data.setValues(replace);
		}
	}
}
