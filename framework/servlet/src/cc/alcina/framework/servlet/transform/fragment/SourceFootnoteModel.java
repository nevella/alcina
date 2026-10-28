package cc.alcina.framework.servlet.transform.fragment;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.process.ContextObservable;
import cc.alcina.framework.common.client.traversal.SelectionTraversal.Selections;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.servlet.transform.fragment.pos.FootnoteReferenceLayer.FootnoteReferenceSelection;
import cc.alcina.framework.servlet.transform.fragment.pos.FootnoteReferenceTargetLayer.FootnoteReferenceTargetBlockSelection;

/**
 */
public class SourceFootnoteModel implements ContextObservable {
	public static class SourceFootnoteReference {
		public IntPair range;

		public String referenceText;

		SourceFootnoteReference() {
		}

		SourceFootnoteReference(FootnoteReferenceSelection selection) {
			range = selection.domNode().asRange().toIntPair();
			referenceText = selection.getFootnoteNumber();
		}
	}

	public static class SourceFootnoteReferenceTarget {
		public IntPair range;

		public String referenceText;

		SourceFootnoteReferenceTarget() {
		}

		SourceFootnoteReferenceTarget(
				FootnoteReferenceTargetBlockSelection selection) {
			range = selection.domNode().asRange().toIntPair();
			referenceText = selection.getFootnoteNumber();
		}

		public IntPair range() {
			return range;
		}

		String referenceText() {
			return referenceText;
		}
	}

	public List<SourceFootnoteReference> references;

	public List<SourceFootnoteReferenceTarget> referenceTargets;

	transient Map<String, SourceFootnoteReferenceTarget> referenceTextReferenceTarget;

	public SourceFootnoteModel(DocumentTransformer documentTransformer) {
		Selections selections = documentTransformer.peer.traversal.selections();
		references = selections.get(FootnoteReferenceSelection.class).stream()
				.map(SourceFootnoteReference::new).collect(Collectors.toList());
		referenceTargets = selections
				.get(FootnoteReferenceTargetBlockSelection.class).stream()
				.map(SourceFootnoteReferenceTarget::new)
				.collect(Collectors.toList());
	}

	SourceFootnoteModel() {
	}

	public List<SourceFootnoteReference>
			getContainedFootnoteReferences(IntPair inRange) {
		return references.stream()
				.filter(ref -> inRange.intersectsWithNonPoint(ref.range))
				.toList();
	}

	public Integer getFirstFootnoteReferenceTargetIndex() {
		SourceFootnoteReferenceTarget firstReferenceTarget = Ax
				.first(referenceTargets);
		return firstReferenceTarget == null ? null
				: firstReferenceTarget.range.i1;
	}

	public SourceFootnoteReferenceTarget
			getReferenceTarget(SourceFootnoteReference reference) {
		if (referenceTextReferenceTarget == null) {
			referenceTextReferenceTarget = referenceTargets.stream()
					.collect(AlcinaCollectors.toKeyMap(
							SourceFootnoteReferenceTarget::referenceText));
		}
		return referenceTextReferenceTarget.get(reference.referenceText);
	}
}
