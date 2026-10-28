package cc.alcina.framework.servlet.transform.fragment.generic;

import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Inline;
import cc.alcina.framework.servlet.transform.fragment.CleanupSelection;
import cc.alcina.framework.servlet.transform.fragment.DocumentTransformerSelection;
import cc.alcina.framework.servlet.transform.fragment.output.MeasureNode;

/*
 */
class PolishLayer extends Layer<DocumentTransformerSelection> {
	PolishLayer() {
		addChild(new HardWhitespace());
	}

	static class HardWhitespace extends Layer<MeasureNode> {
		@Override
		public void process(MeasureNode selection) throws Exception {
			if (!(selection.get() instanceof Inline.Text)) {
				return;
			}
			Inline.Text text = (Inline.Text) selection.get();
			if (text.provideIsBound()
					&& text.domNode().textMatches("(\u00A0)+ ?(.+?)")) {
				// always have one (but only one) hard space
				Inline.Text repl = new Inline.Text(
						"\u00A0" + Ax.ntrim(text.domNode().textContent()));
				text.nodes().replaceWith(repl);
				select(new CleanupSelection(selection, repl));
			}
		}
	}
}