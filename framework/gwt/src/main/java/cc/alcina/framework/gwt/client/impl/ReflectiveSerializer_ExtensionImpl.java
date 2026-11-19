package cc.alcina.framework.gwt.client.impl;

import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.gwt.client.place.BasePlace;

public class ReflectiveSerializer_ExtensionImpl
		implements ReflectiveSerializer.Extension {
	@Override
	public Class getExtendedValueType(Class<? extends Object> valueType) {
		if (Reflections.isAssignableFrom(BasePlace.class, valueType)) {
			return BasePlace.class;
		}
		return null;
	}
}
