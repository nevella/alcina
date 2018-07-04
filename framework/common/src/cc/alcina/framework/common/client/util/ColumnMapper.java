package cc.alcina.framework.common.client.util;

import java.util.List;

import cc.alcina.framework.gwt.client.cell.ColumnsBuilder;

public abstract class ColumnMapper<T> {
	protected ColumnsBuilder<T> builder;

	protected ColumnsBuilder<List<T>> totalBuilder;

	public ColumnMapper() {
		builder = new ColumnsBuilder<T>(null, builderClass());
		totalBuilder = new ColumnsBuilder<List<T>>(null, null);
	}

	public List<ColumnsBuilder<T>.ColumnBuilder> getMappings() {
		if (builder.getPending().isEmpty()) {
			defineMappings();
		}
		return builder.getPending();
	}

	public List<ColumnsBuilder<List<T>>.ColumnBuilder> getTotalMappings() {
		if (totalBuilder.getPending().isEmpty()) {
			defineTotalMappings();
		}
		return totalBuilder.getPending();
	}

	protected abstract Class<T> builderClass();

	protected abstract void defineMappings();

	protected void defineTotalMappings() {
	}
}
