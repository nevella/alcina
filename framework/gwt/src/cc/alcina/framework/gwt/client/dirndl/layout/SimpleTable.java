package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransformNodeRenderer.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.layout.SimpleTable.Transform.LModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class SimpleTable {
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

	@Reflected
	public static class Transform implements ModelTransform<Object, LModel> {
		@Override
		public Transform.LModel apply(Object t) {
			if (t instanceof Bindable) {
				return LModel.ofBindable((Bindable) t);
			} else if (t instanceof KeyValues) {
				return LModel.ofKeyValues((KeyValues) t);
			} else {
				throw new UnsupportedOperationException();
			}
		}

		@Directed(tag = "table")
		public static class LModel extends Model {
			public static LModel ofBindable(Bindable model) {
				LModel result = new LModel();
				List<Property> properties = Reflections.at(model.getClass())
						.properties();
				result.rows = properties.stream()
						.filter(Property::provideNotDefaultIgnoreable)
						.map(r -> new Row(r, model))
						.collect(Collectors.toList());
				return result;
			}

			public static LModel ofKeyValues(KeyValues kvs) {
				LModel result = new LModel();
				result.rows = kvs.elements.stream()
						.map(kv -> new Row(kv.key, kv.value))
						.collect(Collectors.toList());
				return result;
			}

			private List<LModel.Row> rows;

			public LModel() {
			}

			@Directed
			public List<LModel.Row> getRows() {
				return this.rows;
			}

			@Directed(tag = "tr")
			public static class Row extends Model {
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

				@Directed(tag = "td")
				public Object getValue() {
					return this.value;
				}
			}
		}
	}
}