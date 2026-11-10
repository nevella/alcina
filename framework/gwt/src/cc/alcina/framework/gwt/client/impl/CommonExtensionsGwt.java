package cc.alcina.framework.gwt.client.impl;

import cc.alcina.framework.common.client.serializer.FlatTreeSerializer;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.service.CommonExtensions;

public class CommonExtensionsGwt {
	public static void register() {
		CommonExtensions.INSTANCE = new CommonExtensionsImpl();
		ReflectiveSerializer.extension = new ReflectiveSerializer_ExtensionImpl();
		FlatTreeSerializer.extension = new FlatTreeSerializer_ExtensionImpl();
	}
}
