package cc.alcina.framework.entity.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.PropertyPathAccessor;
import cc.alcina.framework.entity.projection.GraphProjection;

public abstract class TableGrouping<T> {
	List<Mapping> mappings = new ArrayList<>();

	private String prefix = "";

	public List<ArrayList<String>> doConvert(List<T> objects) {
		List list = objects.stream().map(r -> apply(r))
				.collect(Collectors.toList());
		doTotal(objects, list);
		return list;
	}

	public void doTotal(List<T> objects, List list) {
	}

	public List<String> apply(T t) {
		List<String> result = new ArrayList<>();
		for (Mapping mapping : mappings) {
			result.add(mapping.stringValue(t));
		}
		return result;
	}

	public String getPrefix() {
		return this.prefix;
	}

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

	protected Mapping define(String propertyPath) {
		return define(propertyPath, null, null);
	}

	protected Mapping define(String propertyPath, String alias) {
		return define(propertyPath, alias, null);
	}

	protected Mapping define(String propertyPath, String alias,
			Function<T, Object> function) {
		Mapping mapping = new Mapping(propertyPath, alias, function);
		mappings.add(mapping);
		return mapping;
	}

	protected void defineChildWithMultiple(String prefix, String... paths) {
		for (String path : paths) {
			define(String.format("%s.%s", prefix, path));
		}
	}

	protected void defineChildWithPrefix(Class clazz, String prefix,
			String fieldName, List<String> ignores) {
		try {
			Class<?> type = null;
			try {
				Field field = clazz.getDeclaredField(fieldName);
				type = field.getType();
			} catch (NoSuchFieldException e) {
				type = Reflections.propertyAccessor().getPropertyType(clazz,
						fieldName);
			}
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

	// protected void loadFromColumnMappings(
	// List<ColumnMapper<T>.ColumnMapping> mappings) {
	// for (ColumnMapping mapping : mappings) {
	// define(mapping.name, null, mapping.mapping);
	// }
	// }
	protected void setTotalValue(List list, String headerName, String value) {
		int index = getHeaderIndex(headerName);
		List<String> row = (List<String>) list.get(list.size() - 1);
		row.set(index, value);
	}

	public static class FieldAccessor {
		private String fieldName;

		public FieldAccessor(String fieldName) {
			this.fieldName = fieldName;
		}

		public Object get(Object t) {
			try {
				return t.getClass().getField(fieldName).get(t);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	public class Mapping {
		String propertyPath;

		Function<T, Object> function;

		String alias;

		PropertyPathAccessor accessor;

		private FieldAccessor fieldAccessor;

		private Function mapper;

		public Mapping(String propertyPath, String alias,
				Function<T, Object> function) {
			this.propertyPath = propertyPath;
			this.alias = alias;
			this.function = function;
			accessor = new PropertyPathAccessor(propertyPath);
		}

		public Mapping auDate() {
			mapper = d -> d == null ? null : Ax.dateSlash((Date) d);
			return this;
		}

		public Mapping dollarTwoPlaces() {
			mapper = d -> d == null ? null
					: Ax.format("$%s", CommonUtils.roundNumeric((Double) d, 2));
			return this;
		}

		public Mapping field() {
			accessor = null;
			fieldAccessor = new FieldAccessor(propertyPath);
			return this;
		}

		public Mapping friendly() {
			mapper = o -> o == null ? null : Ax.friendly(o);
			return this;
		}

		public String getName() {
			return alias != null ? alias : propertyPath;
		}

		public boolean nameIs(String headerName) {
			return getName().equals(headerName);
		}

		public String stringValue(T t) {
			Object value = null;
			if (function != null) {
				value = function.apply(t);
			} else if (accessor != null) {
				value = accessor.getChainedProperty(t);
			} else {
				value = fieldAccessor.get(t);
			}
			if (mapper != null) {
				value = mapper.apply(value);
			}
			return CommonUtils.nullSafeToString(value);
		}

		@Override
		public String toString() {
			return Ax.format("Path: %s - alias: %s - function: %s",
					propertyPath, alias, function);
		}
	}
}
