package cc.alcina.framework.servlet.transform.fragment.html;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.servlet.transform.fragment.CleanupSelection;
import cc.alcina.framework.servlet.transform.fragment.output.MeasureNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Inline;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Inline.Text;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.util.TextUtils;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode;

/**
 * <p>
 * All text node whitespace is normalised (multiple whitespace -> single space).
 * This does *not* affect hard spaces (\\ u00a0)
 * 
 */
class OutputMergeAndNormaliseContiguousTextNode extends Layer<MeasureNode> {
	OutputCleanupLayer.Peer peer;

	@Override
	protected void onBeforeIteration() {
		super.onBeforeIteration();
		peer = state.traversalContext(OutputCleanupLayer.Peer.Has.class)
				.getOutputCleanupLayerPeer();
		selections(MeasureNode.class).forEach(selection -> {
			if (!(selection.get() instanceof Inline.Text)) {
				return;
			}
			Inline.Text text = (Inline.Text) selection.get();
			textSelections.put(text, selection);
		});
	}

	Map<Inline.Text, MeasureNode> textSelections = new LinkedHashMap<>();

	@Override
	public void process(MeasureNode selection) throws Exception {
		if (!peer.isNormaliseWhitespace()) {
			return;
		}
		if (!(selection.get() instanceof Inline.Text)) {
			return;
		}
		Inline.Text text = (Inline.Text) selection.get();
		if (!text.provideIsBound()) {
			return;
		}
		FragmentNode previousSibling = text.nodes().previousSibling();
		if (previousSibling instanceof Inline.Text) {
			Inline.Text previousText = (Text) previousSibling;
			String textContent = previousText.getValue() + text.getValue();
			String normalised = TextUtils.normalizeWhitespace(textContent);
			MeasureNode previousTextSelection = textSelections
					.get(previousText);
			Inline.Text replacement = new Inline.Text(normalised);
			previousText.nodes().replaceWith(replacement);
			text.nodes().removeFromParent();
			select(new CleanupSelection(previousTextSelection, replacement));
			CleanupSelection addedCleanupSelection = new CleanupSelection(
					selection, replacement);
			select(addedCleanupSelection);
			textSelections.put(replacement, selection);
		}
	}

	@Override
	protected void onAfterIteration() {
		super.onAfterIteration();
		textSelections.clear();
	}
}