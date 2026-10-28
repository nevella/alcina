package cc.alcina.framework.servlet.transform.fragment.generic;

import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.servlet.transform.fragment.DocumentTransformerSelection;

/*
 * Traverses the input document, outputs akbn
 */
class RootLayer extends Layer<DocumentTransformerSelection> {
	RootLayer(boolean isVersioned) {
		addChild(new InputLayer());
		addChild(new OutputLayer(isVersioned));
		addChild(new PolishLayer());
	}
}