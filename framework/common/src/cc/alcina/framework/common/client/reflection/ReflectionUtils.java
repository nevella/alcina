package cc.alcina.framework.common.client.reflection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.reflection.PropertyOrder;
import cc.alcina.framework.common.client.util.CommonUtils;

public class ReflectionUtils {
	public static <T> String logBeans(Class<T> clazz, List<T> beans) {
		StringBuilder sb = new StringBuilder();
		ClassReflector<T> reflector = Reflections.at(clazz);
		List<Column> columns = new ArrayList<>();
		List<Property> properties = reflector.properties();
		PropertyOrder order = reflector.annotation(PropertyOrder.class);
		if (order != null) {
			properties = Arrays.stream(order.value()).map(reflector::property)
					.collect(Collectors.toList());
		}
		properties.forEach(p -> {
			Column column = new Column(p);
			columns.add(column);
			beans.forEach(column::addValue);
		});
		columns.forEach(col -> col.writeName(sb));
		sb.append("|\n");
		columns.forEach(col -> col.writeLine(sb));
		sb.append("|\n");
		for (int idx = 0; idx < beans.size(); idx++) {
			for (Column col : columns) {
				col.writeValue(sb, idx);
			}
			sb.append("|\n");
		}
		return sb.toString();
	}

	static class Column<T> {
		private Property property;

		int maxLength;

		List<String> values = new ArrayList<>();

		Column(Property property) {
			this.property = property;
			maxLength = property.getName().length();
		}

		void addValue(T t) {
			Object value = property.get(t);
			String string = value == null ? null : value.toString();
			values.add(string);
			maxLength = Math.max(maxLength, string.length());
		}

		private void write(StringBuilder sb, String string, boolean line) {
			char spacer = line ? '-' : ' ';
			sb.append("|");
			sb.append(spacer);
			sb.append(spacer);
			sb.append(string);
			for (int idx = string.length(); idx < maxLength + 2; idx++) {
				sb.append(spacer);
			}
		}

		void writeLine(StringBuilder sb) {
			String line = CommonUtils.padStringLeft("", maxLength, '-');
			write(sb, line, true);
		}

		void writeName(StringBuilder sb) {
			write(sb, property.getName(), false);
		}

		void writeValue(StringBuilder sb, int idx) {
			write(sb, values.get(idx), false);
		}
	}
}
