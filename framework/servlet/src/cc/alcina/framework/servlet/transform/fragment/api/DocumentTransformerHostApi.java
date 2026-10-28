package cc.alcina.framework.servlet.transform.fragment.api;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public interface DocumentTransformerHostApi {
	public static DocumentTransformerHostApi get() {
		return Registry.impl(DocumentTransformerHostApi.class);
	}

	byte[] getImageBytes(DomNode domNode);

	String getImageSourceUrl(DomNode domNode);

	String getNormalisedSectionTypeText(String test);
}
