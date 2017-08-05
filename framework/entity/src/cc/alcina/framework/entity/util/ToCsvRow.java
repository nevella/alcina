package cc.alcina.framework.entity.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.PropertyPathAccessor;
import cc.alcina.framework.entity.projection.GraphProjection;

public abstract class ToCsvRow<T> implements IToCsvRow<T> {
	List<Mapping> mappings = new ArrayList<>();

	private String prefix = "";

	@Override
	public List<String> apply(T t) {
		List<String> result = new ArrayList<>();
		for (Mapping mapping : mappings) {
			if (mapping.function != null) {
				result.add(CommonUtils
						.nullSafeToString(mapping.function.apply(t)));
			} else {
				Object value = mapping.accessor.getChainedProperty(t);
				result.add(CommonUtils.nullSafeToString(value));
			}
		}
		return result;
	}

	public String getPrefix() {
		return this.prefix;
	}

	@Override
	public List<String> headers() {
		return mappings.stream().map(m -> prefix + (m.getName()))
				.collect(Collectors.toList());
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	private int getHeaderIndex(String headerName) {
		int idx = 0;
		for (Mapping mapping : mappings) {
			if (mapping.nameIs(headerName)) {
				return idx;
			}
			idx++;
		}
		return -1;
	}

	protected void setTotalValue(List list, String headerName, String value) {
		int index = getHeaderIndex(headerName);
		List<String> row = (List<String>) list.get(list.size() - 1);
		row.set(index, value);
	}

	protected void define(String propertyPath) {
		define(propertyPath, null, null);
	}

	protected void define(String propertyPath, String alias) {
		define(propertyPath, alias, null);
	}

	protected void define(String propertyPath, String alias,
			Function<T, Object> function) {
		mappings.add(new Mapping(propertyPath, alias, function));
	}

	protected void defineChildWithMultiple(String prefix, String... paths) {
		for (String path : paths) {
			define(String.format("%s.%s", prefix, path));
		}
	}

	protected void defineChildWithPrefix(Class clazz, String prefix,
			String fieldName, List<String> ignores) {
		try {
			Field field = clazz.getDeclaredField(fieldName);
			Class<?> type = field.getType();
			for (Field child : new GraphProjection().getFieldsForClass(type)) {
				if (Modifier.isTransient(child.getModifiers())) {
					continue;
				}
				if (ignores.contains(child.getName())) {
					continue;
				}
				define(String.format("%s.%s", fieldName, child.getName()));
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	protected void defineMultiple(String... paths) {
		for (String path : paths) {
			define(path);
		}
	}

	protected void generateTotalRow(List list) {
		List<String> totalRow = new ArrayList<>();
		mappings.forEach(m -> totalRow.add(""));
		list.add(totalRow);
	}

	class Mapping {
		String propertyPath;

		Function<T, Object> function;

		String alias;

		PropertyPathAccessor accessor;

		public Mapping(String propertyPath, String alias,
				Function<T, Object> function) {
			this.propertyPath = propertyPath;
			this.alias = alias;
			this.function = function;
			accessor = new PropertyPathAccessor(propertyPath);
		}

		public String getName() {
			return alias != null ? alias : propertyPath;
		}

		public boolean nameIs(String headerName) {
			return getName().equals(headerName);
		}

		@Override
		public String toString() {
			return CommonUtils.formatJ("Path: %s - alias: %s - function: %s",
					propertyPath, alias, function);
		}
	}
}
