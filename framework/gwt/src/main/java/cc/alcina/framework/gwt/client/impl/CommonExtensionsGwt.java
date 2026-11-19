package cc.alcina.framework.gwt.client.impl;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.serializer.FlatTreeSerializer;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.service.Common;

public class CommonExtensionsGwt {
	public static void register() {
		Common.extensions = new CommonExtensionsImpl();
		ReflectiveSerializer.extension = new ReflectiveSerializer_ExtensionImpl();
		FlatTreeSerializer.extension = new FlatTreeSerializer_ExtensionImpl();
		if (GWT.isScript()) {
			Common.scriptableExtensions = new CommonScriptableExtensionsImpl();
		}
	}
}
