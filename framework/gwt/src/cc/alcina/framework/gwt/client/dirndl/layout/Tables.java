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
import java.util.function.Predicate;
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

	@Directed
	public static class ColumnName extends Model.Fields {
		@Binding(type = Type.INNER_TEXT)
		String text;

		// for css selection
		@Binding(type = Type.PROPERTY)
		String name;

		public ColumnName(String name) {
			this.name = name;
			this.text = name;
		}
	}

	/**
	 * Overrides individual columnwidths
	 */
	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	public @interface ColumnsWidth {
		public String value();
	}

	/**
	 * Used in the generated gridTemplateColumns style attribute of a generated
	 * grid
	 *
	 */
	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	public @interface ColumnWidth {
		public String value();
	}

	/**
	 * Exclude a property from the grid
	 */
	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
	public @interface Exclude {
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

		@Directed
		class IntermediateModel extends Model.All
				implements Directed.NonClassTag {
			@Binding(type = Type.STYLE_ATTRIBUTE)
			String gridTemplateColumns;

			@Directed.Wrap("column-names")
			List<ColumnName> columnNames;

			List<? extends Model> rows;

			@Directed.Exclude
			String gridColumnWidth;

			IntermediateModel(List<? extends Model> rows) {
				ColumnWidth defaultWidth = node.annotationLocation
						.getAnnotation(ColumnWidth.class);
				gridColumnWidth = defaultWidth != null ? defaultWidth.value()
						: "auto";
				this.rows = rows;
				Predicate<Property> propertyFilter = p -> p.provideRenderable()
						&& !p.has(Exclude.class);
				if (rows.isEmpty()) {
					this.columnNames = new ArrayList<>();
				} else {
					Model template = rows.iterator().next();
					columnNames = Reflections.at(template).properties().stream()
							.filter(propertyFilter).map(property -> {
								Directed.Property propertyAnnotation = property
										.annotation(Directed.Property.class);
								return propertyAnnotation != null
										? propertyAnnotation.value()
										: CommonUtils
												.deInfix(property.getName());
							}).map(ColumnName::new)
							.collect(Collectors.toList());
					ColumnsWidth columnsWidth = node
							.annotation(ColumnsWidth.class);
					gridTemplateColumns = columnsWidth != null
							? columnsWidth.value()
							: Reflections.at(template).properties().stream()
									.filter(propertyFilter).map(p -> {
										ColumnWidth columnWidth = p
												.annotation(ColumnWidth.class);
										return columnWidth != null
												? columnWidth.value()
												: gridColumnWidth;
									}).collect(Collectors.joining(" "));
				}
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
		public static class IntermediateModel extends Model
				implements Directed.NonClassTag {
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
}