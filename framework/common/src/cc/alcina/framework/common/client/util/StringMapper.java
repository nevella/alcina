package cc.alcina.framework.common.client.util;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class StringMapper implements Function<String, String> {
	private List<String> list;

	public StringMapper(String... strings) {
		this.list = Arrays.asList(strings);
	}

	@Override
	public String apply(String t) {
		int idx = list.indexOf(t);
		if (idx == -1 || idx % 2 == 1) {
			throw new IllegalArgumentException(t);
		}
		return list.get(idx + 1);
	}
}
