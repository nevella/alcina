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

	public String getPrefix() {
		return this.prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
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
	}

	protected void define(String propertyPath) {
		define(propertyPath, null, null);
	}

	protected void defineMultiple(String... paths) {
		for (String path : paths) {
			define(path);
		}
	}

	protected void define(String propertyPath, String alias) {
		define(propertyPath, alias, null);
	}

	protected void define(String propertyPath, String alias,
			Function<T, Object> function) {
		mappings.add(new Mapping(propertyPath, alias, function));
	}

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

	@Override
	public List<String> headers() {
		return mappings.stream()
				.map(m -> prefix + (m.alias != null ? m.alias : m.propertyPath))
				.collect(Collectors.toList());
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
}
