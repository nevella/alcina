package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransformNodeRenderer.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.layout.SimpleTable.Transform.LModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class SimpleTable {
    @ClientInstantiable
    public static class Transform implements ModelTransform<Object, LModel> {
        @Directed(tag = "table")
        public static class LModel extends Model {
            public LModel() {
            }

            private List<LModel.Row> rows;

            @Directed
            public List<LModel.Row> getRows() {
                return this.rows;
            }

            @Directed(tag = "tr")
            public static class Row extends Model {
                public Row() {
                }

                private String key;

                @Directed(tag = "th")
                public String getKey() {
                    return this.key;
                }

                @Directed(tag = "td")
                public String getValue() {
                    return this.value;
                }

                private String value;

                public Row(PropertyReflector r, Bindable model) {
                    key = CommonUtils.titleCase(r.getPropertyName());
                    Object propertyValue = r.getPropertyValue(model);
                    if (propertyValue == null) {
                    } else if (propertyValue instanceof Enum) {
                        value = Ax.friendly(r.getPropertyValue(model));
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
            }

            public static LModel ofBindable(Bindable model) {
                LModel result = new LModel();
                Collection<PropertyReflector> reflectors = Reflections.classLookup()
                        .getPropertyReflectors(model.getClass()).values();
                result.rows = reflectors.stream().map(r -> new Row(r, model)).collect(Collectors.toList());
                return result;
            }

            public static LModel ofKeyValues(KeyValues kvs) {
                LModel result = new LModel();
                result.rows = kvs.elements.stream().map(kv -> new Row(kv.key, kv.value)).collect(Collectors.toList());
                return result;
            }
        }

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