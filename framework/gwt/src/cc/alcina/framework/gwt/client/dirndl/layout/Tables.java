package cc.alcina.framework.gwt.client.dirndl.layout;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.resolution.Annotations;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform.AbstractContextSensitiveModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class Tables {
	public static class ColumnHeaders extends Model.All {
		List<ColumnName> names;

		public ColumnHeaders(Class<? extends Bindable> clazz,
				DirectedLayout.Node node) {
			List<String> strings = Reflections.at(clazz).properties().stream()
					.map(p -> Annotations.resolve(p, Directed.Property.class,
							node.getResolver()))
					.filter(Objects::nonNull).map(Directed.Property::value)
					.collect(Collectors.toList());
			init(strings);
		}

		public ColumnHeaders(List<String> strings) {
			init(strings);
		}

		void init(List<String> strings) {
			names = strings.stream().map(ColumnName::new)
					.collect(Collectors.toList());
		}
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
	/**
	 * Used in the generated gridTemplateColumns style attribute of a generated
	 * grid
	 *
	 *
	 *
	 */
	public @interface ColumnNames {
		public String value();
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
	/**
	 * Used in the generated gridTemplateColumns style attribute of a generated
	 * grid
	 *
	 *
	 *
	 */
	public @interface ColumnWidth {
		public String value();
	}

	/*
	 * Constructs a multi-column table from a list of reflected objects. Column
	 * headers are either supplied or derived from fields
	 */
	public static class Multiple extends
			AbstractContextSensitiveModelTransform<List<? extends Model>, Multiple.IntermediateModel> {
		@Override
		public IntermediateModel apply(List<? extends Model> t) {
			return new IntermediateModel(t);
		}

		@Directed(
			tag = "multiple",
			bindings = @Binding(
				from = "gridTemplateColumns",
				type = Type.STYLE_ATTRIBUTE))
		public class IntermediateModel extends Model {
			private String gridTemplateColumns;

			private List<ColumnName> columnNames;

			private String gridColumnWidth;

			private List<? extends Model> rows;

			public IntermediateModel(List<? extends Model> rows) {
				ColumnWidth defaultWidth = node.annotationLocation
						.getAnnotation(ColumnWidth.class);
				gridColumnWidth = defaultWidth != null ? defaultWidth.value()
						: "auto";
				this.rows = rows;
				if (rows.isEmpty()) {
					this.columnNames = new ArrayList<>();
				} else {
					Model template = rows.iterator().next();
					columnNames = Reflections.at(template).properties().stream()
							.filter(Property::provideRenderable)
							.map(property -> {
								Directed.Property propertyAnnotation = node
										.annotation(Directed.Property.class);
								return propertyAnnotation != null
										? propertyAnnotation.value()
										: CommonUtils
												.deInfix(property.getName());
							}).map(ColumnName::new)
							.collect(Collectors.toList());
					gridTemplateColumns = Reflections.at(template).properties()
							.stream().filter(Property::provideRenderable)
							.map(p -> {
								ColumnWidth columnWidth = p
										.annotation(ColumnWidth.class);
								return columnWidth != null ? columnWidth.value()
										: gridColumnWidth;
							}).collect(Collectors.joining(" "));
				}
			}

			@Directed.Wrap("column-names")
			public List<ColumnName> getColumnNames() {
				return this.columnNames;
			}

			public String getGridColumnWidth() {
				return this.gridColumnWidth;
			}

			public String getGridTemplateColumns() {
				return this.gridTemplateColumns;
			}

			@Directed
			public List<? extends Model> getRows() {
				return this.rows;
			}

			public void setGridColumnWidth(String gridColumnWidth) {
				this.gridColumnWidth = gridColumnWidth;
			}
		}
	}

	@Reflected
	/*
	 * Constructs a two-column key/value table from a single reflected object
	 */
	public static class Single
			implements ModelTransform<Object, Single.IntermediateModel> {
		@Override
		public Single.IntermediateModel apply(Object t) {
			if (t instanceof Bindable) {
				return IntermediateModel.ofBindable((Bindable) t);
			} else if (t instanceof KeyValues) {
				return IntermediateModel.ofKeyValues((KeyValues) t);
			} else {
				throw new UnsupportedOperationException();
			}
		}

		@Directed(tag = "table")
		public static class IntermediateModel extends Model {
			public static IntermediateModel ofBindable(Bindable model) {
				IntermediateModel result = new IntermediateModel();
				List<Property> properties = Reflections.at(model).properties();
				result.rows = properties.stream()
						.filter(Property::provideNotDefaultIgnoreable)
						.map(r -> new Row(r, model))
						.collect(Collectors.toList());
				return result;
			}

			public static IntermediateModel ofKeyValues(KeyValues kvs) {
				IntermediateModel result = new IntermediateModel();
				result.rows = kvs.elements.stream()
						.map(kv -> new Row(kv.key, kv.value))
						.collect(Collectors.toList());
				return result;
			}

			private List<IntermediateModel.Row> rows;

			public IntermediateModel() {
			}

			@Directed
			public List<IntermediateModel.Row> getRows() {
				return this.rows;
			}

			@Directed(tag = "tr")
			public static class Row extends Model.Fields {
				private String key;

				private Object value;

				public Row() {
				}

				public Row(Property property, Bindable model) {
					key = CommonUtils
							.titleCase(CommonUtils.deInfix(property.getName()));
					Object propertyValue = property.get(model);
					if (propertyValue == null) {
					} else if (propertyValue instanceof Model) {
						value = propertyValue;
					} else if (propertyValue instanceof Collection) {
						value = propertyValue;
					} else if (propertyValue instanceof Enum) {
						value = Ax.friendly(propertyValue);
					} else if (propertyValue instanceof HasDisplayName) {
						value = ((HasDisplayName) propertyValue).displayName();
					} else if (propertyValue instanceof Date) {
						value = Ax.dateTimeSlash((Date) propertyValue);
					} else {
						value = propertyValue.toString();
					}
				}

				public Row(String key, String value) {
					this.key = key;
					this.value = value;
				}

				@Directed(tag = "th")
				public String getKey() {
					return this.key;
				}

				@Directed.Wrap("td")
				public Object getValue() {
					return this.value;
				}
			}
		}

		public static class KeyValues {
			List<Element> elements = new ArrayList<>();

			void add(String key, Object value) {
				elements.add(new Element(key, value.toString()));
			}

			static class Element {
				String key;

				String value;

				Element(String key, String value) {
					super();
					this.key = key;
					this.value = value;
				}
			}
		}
	}

	@Directed
	static class ColumnName extends Model.Fields {
		@Binding(type = Type.INNER_TEXT)
		String text;

		// for css selection
		@Binding(type = Type.PROPERTY)
		String name;

		ColumnName(String name) {
			this.name = name;
			this.text = name;
		}
	}
}