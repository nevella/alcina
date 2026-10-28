package cc.alcina.framework.servlet.transform.fragment;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.traversal.AbstractSelection;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

/**
 * <p>
 * Models a domnode which will be ignored for output (early processing
 * simplification)
 *
 * <p>
 * See the package doc for a discussion of how "ignore this" is implemented in
 * the pipeline
 *
 * 
 *
 */
public class IgnoredNodeSelection extends AbstractSelection<DomNode> {
	public IgnoredNodeSelection(StyledDocument.AccessorSelection parent,
			DomNode node) {
		super((Selection) parent, node);
	}

	public boolean hasText() {
		return get().isNonWhitespaceTextContent();
	}

	@Override
	public String toString() {
		return Ax.format("%s :: %s", get().name(),
				CommonUtils.trimToWsChars(get().ntc(), 50));
	}
}
