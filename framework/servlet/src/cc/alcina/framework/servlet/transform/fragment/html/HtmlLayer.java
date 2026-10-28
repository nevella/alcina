package cc.alcina.framework.servlet.transform.fragment.html;

import cc.alcina.framework.servlet.transform.fragment.DocumentTransformerSelection;
import cc.alcina.framework.common.client.traversal.Layer;

/*
 * Overlays with simple html measures (blocks, styled spans, text nodes)
 */
public class HtmlLayer extends Layer<DocumentTransformerSelection> {
	public HtmlLayer() {
	}

	@Override
	public void ensureChildren() {
		addChild(new TableLayer());
		addChild(new ParaLayer());
		addChild(new LeafBlockLayer());
		addChild(new ImageLayer());
		addChild(new LinkLayer());
		addChild(new PagebreakLayer());
		addChild(new VolumeStartLayer());
		addChild(new StyleAttributeLayer());
		addChild(new TextLayer());
	}
}