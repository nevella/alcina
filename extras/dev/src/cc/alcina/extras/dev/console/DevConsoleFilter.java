package cc.alcina.extras.dev.console;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.collections.PropertyConverter;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;

public abstract class DevConsoleFilter {
	public static String
			describeFilters(Class<? extends DevConsoleFilter> filterClass) {
		return Registry.query(filterClass).implementations()
				.map(new PropertyConverter<DevConsoleFilter, String>("key"))
				.collect(Collectors.joining("|"));
	}

	public static String getFilters(
			Class<? extends DevConsoleFilter> registryPoint, String[] argv) {
		return getFilters(registryPoint, argv, null);
	}

	public static String getFilters(
			Class<? extends DevConsoleFilter> filterClass, String[] argv,
			Predicate<String> allowFilter) {
		List<String> filters = new ArrayList<String>();
		StringMap kv = new StringMap();
		for (int i = 0; i < argv.length; i += 2) {
			kv.put(argv[i], argv[i + 1]);
		}
		Registry.query(filterClass).implementations().forEach(impl -> {
			if (kv.containsKey(impl.getKey()) || impl.hasDefault()) {
				String filterString = impl.getFilter(kv.get(impl.getKey()));
				if (allowFilter == null || allowFilter.test(filterString)) {
					filters.add(filterString);
				}
			}
		});
		return CommonUtils.join(filters, " and ");
	}

	public abstract String getFilter(String value);

	public abstract String getKey();

	protected boolean hasDefault() {
		return false;
	}
}