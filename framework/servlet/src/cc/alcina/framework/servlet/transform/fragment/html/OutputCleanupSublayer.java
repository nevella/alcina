package cc.alcina.framework.servlet.transform.fragment.html;

import java.util.Collection;
import java.util.List;

import cc.alcina.framework.servlet.transform.fragment.CleanupSelection;
import cc.alcina.framework.servlet.transform.fragment.output.MeasureNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocumentNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Inline;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode;

/*
 * Any changes made here should emit a cleanup selection (for mutation tracking)
 * as well as performing the FragmentDocumentNode transformation
 */
public abstract class OutputCleanupSublayer<T extends FragmentDocumentNode>
		extends Layer<MeasureNode> {
	private Class<? extends FragmentDocumentNode> firstGenericBound;

	public OutputCleanupSublayer() {
		super();
		inputType = MeasureNode.class;
		firstGenericBound = Reflections.at(this).firstGenericBound();
	}

	protected void removePreservePagebreaks(MeasureNode selection,
			FragmentDocumentNode node) {
		select(new CleanupSelection(selection, node));
		// to be honest max 1
		List<? extends FragmentNode> pagebreaks = node.stream()
				.filter(n -> n instanceof Inline.Pagebreak).toList();
		List<? extends FragmentNode> list = node.stream().toList();
		pagebreaks
				.forEach(pagebreak -> node.nodes().insertBeforeThis(pagebreak));
		node.nodes().removeFromParent();
	}

	@Override
	public Collection<MeasureNode> computeInputs() {
		Collection<MeasureNode> measureNodes = super.computeInputs();
		return measureNodes.stream().filter(mn -> {
			FragmentDocumentNode node = mn.get();
			if (node.isDetached()) {
				return false;
			}
			if (node.getClass() != firstGenericBound) {
				return false;
			}
			return true;
		}).toList();
	}

	@Override
	public final void process(MeasureNode selection) throws Exception {
		T node = (T) selection.get();
		if (node.isDetached()) {
			return;
		}
		process0(selection, node);
	}

	protected Class<T> getCleanupNodeType() {
		return Reflections.at(this).firstGenericBound();
	}

	protected abstract void process0(MeasureNode selection, T node);
}