package cc.alcina.framework.common.client.util;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.search.grouping.GroupedResult.GroupKey;
import cc.alcina.framework.gwt.client.cell.ColumnsBuilder;

public abstract class ColumnMapper<T> {
	protected ColumnsBuilder<T> builder;

	protected ColumnsBuilder<List<T>> totalBuilder;

	public ColumnMapper() {
		builder = new ColumnsBuilder<T>(null, mappedClass());
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

	protected abstract void defineMappings();

	protected void defineTotalMappings() {
	}

	protected abstract Class<T> mappedClass();

	@XmlAccessorType(XmlAccessType.FIELD)
	@RegistryLocation(registryPoint = JaxbContextRegistration.class)
	public static class RowModel_SingleCell implements Serializable {
		public String value;

		public RowModel_SingleCell() {
		}

		public RowModel_SingleCell(String value) {
			this.value = value;
		}

		public GroupKey asRowKey() {
			return new GroupKey();
		}
	}

	public static class SingleCellColumnMapper
			extends ColumnMapper<RowModel_SingleCell> {
		@Override
		protected void defineMappings() {
			builder.col("Value").function(row -> row.value).asUnsafeHtml(true)
					.add();
		}

		@Override
		protected Class<RowModel_SingleCell> mappedClass() {
			return RowModel_SingleCell.class;
		}
	}
}
