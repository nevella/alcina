package cc.alcina.extras.dev.console;

import cc.alcina.framework.common.client.collections.KeyValueMapper.StringKeyValueMapper;

public class ClassSimpleNameMapper extends StringKeyValueMapper<Class> {

	@Override
	public String getKey(Class o) {
		return o.getSimpleName();
	}
	
}
