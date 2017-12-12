package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import cc.alcina.framework.common.client.util.ColumnMapper.ColumnMapping;

public abstract class ColumnMapper<T> {
	protected List<ColumnMapping> mappings = new ArrayList<>();

	public List<ColumnMapping> getMappings() {
		defineMappings();
		return this.mappings;
	}

	protected abstract void defineMappings();

	public class ColumnMapping {
		public String name;

		public Function<T, Object> mapping;

		public boolean asHtml;

		public ColumnMapping(String name, Function mapping, boolean asHtml) {
			this.name = name;
			this.mapping = mapping;
			this.asHtml = asHtml;
		}
	}

	protected void define(String name, Function<T, Object> mapping) {
		mappings.add(new ColumnMapping(name, mapping,false));
	}
	protected void defineHtml(String name, Function<T, Object> mapping) {
		mappings.add(new ColumnMapping(name, mapping,true));
	}
}
