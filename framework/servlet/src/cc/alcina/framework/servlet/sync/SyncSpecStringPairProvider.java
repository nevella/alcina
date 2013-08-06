package cc.alcina.framework.servlet.sync;

import java.util.LinkedHashMap;
import java.util.Map;

public class SyncSpecStringPairProvider {
	public Map<String, SyncConversionSpec> byName1 = new LinkedHashMap<String, SyncConversionSpec>();

	public SyncSpecStringPairProvider(String[] pairs) {
		for (int i = 0; i < pairs.length; i += 2) {
			SyncConversionSpec spec = new SyncConversionSpec(pairs[i],
					pairs[i + 1]);
			byName1.put(spec.getName1(),spec);
		}
	}
}
