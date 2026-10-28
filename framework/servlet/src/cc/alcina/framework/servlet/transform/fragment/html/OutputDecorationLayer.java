package cc.alcina.framework.servlet.transform.fragment.html;

import cc.alcina.framework.servlet.transform.fragment.DocumentTransformerSelection;
import cc.alcina.framework.common.client.traversal.Layer;

/*
 * Any changes made here should emit a change selection (for mutation tracking)
 * as well as performing the FragmentDocumentNode transformation
 */
public class OutputDecorationLayer extends Layer<DocumentTransformerSelection> {
	public OutputDecorationLayer() {
		addChild(new OutputMarkCoverPageImageContainer());
	}
}