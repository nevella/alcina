package cc.alcina.framework.servlet.transform.fragment.generic;

import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.servlet.transform.fragment.DocumentElementLayer;
import cc.alcina.framework.servlet.transform.fragment.DocumentTransformerSelection;
import cc.alcina.framework.servlet.transform.fragment.StyledNodeLayer;
import cc.alcina.framework.servlet.transform.fragment.html.HtmlLayer;
import cc.alcina.framework.servlet.transform.fragment.pos.PositioningLayer;
import cc.alcina.framework.servlet.transform.fragment.structure.StructureLayer;

/*
 */
class InputLayer extends Layer<DocumentTransformerSelection> {
	InputLayer() {
		addChild(new StyledNodeLayer.DocumentTransformerLayer());
		addChild(new DocumentElementLayer());
		addChild(new HtmlLayer());
		addChild(new PositioningLayer());
		addChild(new StructureLayer.Toc());
	}
}