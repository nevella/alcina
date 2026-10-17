package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.util.Multiset;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode;

/**
 * Note that the real marker of a split (and thus an unsplit) is the first
 * non-whitespace text node - this is particularly true when unsplitting lists,
 * since the list label will have empty content (and no split marker)
 */
public class ReversePagination {
	FragmentDocument<?> doc;

	public ReversePagination(FragmentDocument<?> doc) {
		this.doc = doc;
	}

	public void dePaginate(boolean replaceFootnoteRefsWithContents) {
		doc.byType(Pagination.PageBreakBlock.class).toList()
				.forEach(FragmentNode::removeFromParent);
		// remove all except the first indentstyles
		doc.byType(Block.IndentStyles.class).skip(1).toList()
				.forEach(FragmentNode::removeFromParent);
		doc.byType(Pagination.Page.class).toList()
				.forEach(n -> n.nodes().strip());
		doc.footnotes()
				.depaginateFootnoteReferences(replaceFootnoteRefsWithContents);
		doc.byType(FootnoteNode.FootnotesContainer.class).toList()
				.forEach(FragmentNode::removeFromParent);
		Multiset<FragmentNode, Set<FragmentNode>> topLevelSplitDescendants = new Multiset<>();
		Set<FragmentNode> split = new LinkedHashSet<>();
		doc.root.stream().filter(this::isSplit).forEach(n -> {
			FragmentNode cursor = n;
			while (cursor != null) {
				if (topLevelSplitDescendants.containsKey(cursor)) {
					topLevelSplitDescendants.add(cursor, n);
					return;
				}
				cursor = cursor.parent();
			}
			topLevelSplitDescendants.add(n, n);
		});
		topLevelSplitDescendants.entrySet().forEach(this::unSplit);
	}

	boolean isSplit(FragmentNode n) {
		return n != null && n.domNode().attrIs("split", "true");
	}

	void unSplit(Map.Entry<FragmentNode, Set<FragmentNode>> entry) {
		FragmentNode mergeFromCursor = entry.getKey();
		FragmentNode mergeToCursor = entry.getKey().nodes().previousSibling();
		/*
		 * at each split level, merge non-first children
		 * 
		 * at the first non-split level, merge all children
		 */
		for (;;) {
			try {
				Preconditions.checkState(
						Objects.equals(mergeFromCursor.domNode().name(),
								mergeToCursor.domNode().name()));
				if (mergeFromCursor.domNode().isElement()) {
					Preconditions.checkState(Objects.equals(
							mergeFromCursor.domNode().getClassName(),
							mergeToCursor.domNode().getClassName()));
				}
			} catch (Exception e) {
				e.printStackTrace();
				// DEVEX - 0
				// FIXME - fragmentDocument
				return;
			}
			FragmentNode nextMergeFromCursor = mergeFromCursor.nodes()
					.firstChild();
			FragmentNode splitSynthetic = null;
			if (nextMergeFromCursor != null && !isSplit(nextMergeFromCursor)) {
				/*
				 * handle empty, non-split li-label in mergeFrom
				 * 
				 * FIXME - fragmentDocument - mark the empty label as
				 * split-synthetic
				 */
				FragmentNode testNext = nextMergeFromCursor.nodes()
						.nextSibling();
				if (testNext != null && isSplit(testNext)) {
					splitSynthetic = nextMergeFromCursor;
					nextMergeFromCursor = testNext;
				}
			}
			FragmentNode nextMergeToCursor = mergeToCursor.nodes().lastChild();
			boolean nextIsSplit = isSplit(nextMergeFromCursor);
			/*
			 * merge by moving mergeFromCursor children to mergeToCursor
			 */
			int firstAppendOffset = 0;
			if (splitSynthetic != null) {
				/*
				 * skip synthetic (blank label), then skip split li contents
				 */
				firstAppendOffset = 2;
			} else if (nextIsSplit) {
				firstAppendOffset = 1;
			}
			List<? extends FragmentNode> appendNonSplitNodesList = mergeFromCursor
					.children().skip(firstAppendOffset).toList();
			DomNode fromCursorNode = mergeFromCursor.domNode();
			if (mergeToCursor.getClass() == Block.ListItem.class) {
				if (appendNonSplitNodesList.size()
						+ mergeToCursor.childList().size() > 2) {
					/*
					 * possible issue here
					 */
				}
			}
			appendNonSplitNodesList.forEach(mergeToCursor.nodes()::append);
			entry.getValue().remove(mergeFromCursor);
			if (splitSynthetic != null) {
				/*
				 * will be removed by parent removal
				 */
				// removeSynthetic.nodes().removeFromParent();
			}
			if (nextIsSplit) {
				mergeFromCursor = nextMergeFromCursor;
				mergeToCursor = nextMergeToCursor;
			} else {
				break;
			}
		}
		entry.getKey().nodes().removeFromParent();
		Preconditions.checkState(entry.getValue().isEmpty());
	}
}
