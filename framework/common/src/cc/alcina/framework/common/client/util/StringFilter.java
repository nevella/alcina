package cc.alcina.framework.common.client.util;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class StringFilter implements Predicate<String>{
	private List<String> list;

	public StringFilter(String...strings) {
		this.list = Arrays.asList(strings);
	}

	@Override
	public boolean test(String t) {
		return list.contains(t);
	}
}
