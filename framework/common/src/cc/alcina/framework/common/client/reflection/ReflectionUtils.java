package cc.alcina.framework.common.client.reflection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.PropertyOrder;
import cc.alcina.framework.common.client.util.CommonUtils;

public class ReflectionUtils {
	public static <T> String logBeans(Class<T> clazz, List<T> beans) {
		StringBuilder sb = new StringBuilder();
		ClassReflector<T> reflector = Reflections.at(clazz);
		PropertyOrder order = reflector.annotation(PropertyOrder.class);
		List<Column> columns = new ArrayList<>();
		Arrays.stream(order.value()).forEach(name -> {
			Column column = new Column(reflector.property(name));
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

		void addValue(T t) {
			Object value = property.get(t);
			String string = value == null ? null : value.toString();
			values.add(string);
			maxLength = Math.max(maxLength, string.length());
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
