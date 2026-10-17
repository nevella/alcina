package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc;

import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FootnoteNode.FootnoteReference;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FootnoteNode.FootnoteReferenceTarget;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FootnoteNode.FootnotesContainer;

public class Footnotes {
	FragmentDocument<?> doc;

	Map<String, FootnoteReferenceTarget> numberReferenceTarget;

	Map<String, FootnoteReference> numberReference;

	public Footnotes(FragmentDocument<?> doc) {
		this.doc = doc;
	}

	public void depaginateFootnoteReferences(
			boolean replaceFootnoteRefsWithContents) {
		buildRefTargetByNumberMap();
		if (replaceFootnoteRefsWithContents) {
			/*
			 * insert the reffed contents next to the refs, remove the refs and
			 * denumber the reffed
			 * 
			 * they'll be split out later (during multi-version footnote
			 * repagination)
			 */
			long start = System.currentTimeMillis();
			doc.byType(FootnoteReference.class).toList().forEach(ref -> {
				FootnoteReferenceTarget target = numberReferenceTarget
						.get(ref.getNumber());
				/*
				 * set number before move fixes an edge case TLC issue (although
				 * that should be fixed lower)
				 */
				target.setNumber(null);
				/*
				 * we need to move the target, and then null the number (rather
				 * than just say the target contents) because the downstream ui
				 * will reconstitute the footnotes to show fn diffs
				 */
				ref.nodes().insertAfterThis(target);
				ref.removeFromParent();
			});
			numberReferenceTarget = null;
		}
	}

	public void buildRefTargetByNumberMap() {
		numberReferenceTarget = doc.byType(FootnoteReferenceTarget.class)
				.collect(AlcinaCollectors
						.toKeyMap(FootnoteReferenceTarget::getNumber));
		numberReference = doc.byType(FootnoteReference.class).collect(
				AlcinaCollectors.toKeyMap(FootnoteReference::getNumber));
	}

	FootnoteReferenceTarget getReferenceTarget(FootnoteReference ref) {
		return numberReferenceTarget.get(ref.getNumber());
	}

	/**
	 * * //FIX - sel.hist
	 * 
	 * @param b
	 * 
	 * 
	 * @param footnoteSourceDoc
	 *            - either this object's containing doc (if footnotes were
	 *            renumbered) - or the section container. This could (should) be
	 *            simplified by always moving footnotes to the section
	 * 
	 */
	public void renderSelfContainedFootnotes(
			FragmentDocument<?> targetSourceDoc,
			boolean ensureFootnoteReferences) {
		List<FootnoteReference> references = doc.byType(FootnoteReference.class)
				.toList();
		if (references.isEmpty() && ensureFootnoteReferences) {
			doc.byType(FootnoteReferenceTarget.class).toList()
					.forEach(target -> {
						target.nodes()
								.insertBeforeThis(new FootnoteReference());
					});
			references = doc.byType(FootnoteReference.class).toList();
		}
		if (references.isEmpty()) {
			return;
		}
		if (targetSourceDoc.footnotes().numberReferenceTarget == null) {
			/*
			 * detached, renumber
			 */
			Preconditions.checkState(targetSourceDoc == doc);
			renumberFootnotes();
			buildRefTargetByNumberMap();
		}
		FootnotesContainer ctr = new FootnotesContainer();
		doc.root.nodes().append(ctr);
		references.forEach(ref -> {
			FootnoteReferenceTarget referenceTarget = targetSourceDoc
					.footnotes().getReferenceTarget(ref);
			if (referenceTarget.document == ctr.document) {
				ctr.nodes().append(referenceTarget);
			} else {
				ctr.domNode().children.importFrom(referenceTarget.domNode());
			}
		});
		// targetSourceDoc.byType(FootnoteReferenceTarget.class).filter(
		// target -> target.ancestor(FootnotesContainer.class).isEmpty())
		// .toList().forEach(n -> n.nodes().removeFromParent());
	}

	void renumberFootnotes() {
		int counter = 1;
		for (FootnoteReferenceTarget target : doc
				.byType(FootnoteReferenceTarget.class).toList()) {
			target.setNumber(String.valueOf(counter++));
		}
		counter = 1;
		for (FootnoteReference ref : doc.byType(FootnoteReference.class)
				.toList()) {
			ref.setNumber(String.valueOf(counter++));
		}
	}

	public FootnoteReference getReference(FootnoteReferenceTarget target) {
		return numberReference.get(target.getNumber());
	}
}
