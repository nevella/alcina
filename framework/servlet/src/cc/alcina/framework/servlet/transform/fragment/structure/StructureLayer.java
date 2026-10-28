package cc.alcina.framework.servlet.transform.fragment.structure;

import cc.alcina.framework.servlet.transform.fragment.DocumentTransformerSelection;
import cc.alcina.framework.common.client.traversal.Layer;

/**
 */
public abstract class StructureLayer
		extends Layer<DocumentTransformerSelection> {
	public static class Toc extends StructureLayer {
		public Toc() {
			addChild(new TocLayer());
			addChild(new SectionHeadingTocLayer());
		}
	}

	public static class Tag extends StructureLayer {
		public Tag() {
			addChild(new TocLayer());
			addChild(new SectionHeadingTagLayer());
		}
	}
}