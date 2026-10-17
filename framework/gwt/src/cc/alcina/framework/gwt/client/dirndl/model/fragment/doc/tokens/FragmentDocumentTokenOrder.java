package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens;

import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.dom.Measure.Token;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;

public class FragmentDocumentTokenOrder extends Measure.Token.Order.Simple {
	public static final int STRUCT_BLOCK_CONTAINS_INDENT_RUN = 32;

	@Override
	public int compare(Token o1, Token o2) {
		return super.compare(o1, o2);
	}

	@Override
	public int customCompare(MeasureSelection o1, MeasureSelection o2) {
		MeasureSelection leafBlockSelection = null;
		MeasureSelection imageSelection = null;
		if (o1.get().token == LayoutTokens.LeafBlock.TYPE) {
			leafBlockSelection = o1;
		}
		if (o2.get().token == LayoutTokens.LeafBlock.TYPE) {
			leafBlockSelection = o2;
		}
		if (o1.get().token == LayoutTokens.Image.TYPE) {
			imageSelection = o1;
		}
		if (o2.get().token == LayoutTokens.Image.TYPE) {
			imageSelection = o2;
		}
		if (leafBlockSelection != null && imageSelection != null) {
			if (leafBlockSelection.get().startContainingNode().isText()) {
				int cmp = leafBlockSelection.get().startContainingNode()
						.asLocation().compareTo(imageSelection.get()
								.startContainingNode().asLocation());
				boolean o1IsLeaf = leafBlockSelection == o1;
				return cmp * (o1IsLeaf ? 1 : -1);
			}
		}
		return 0;
	}

	@Override
	protected int classOrdering(Class<? extends Token> class1,
			Class<? extends Token> class2) {
		int c1 = classWeight(class1);
		int c2 = classWeight(class2);
		int cmp = c1 - c2;
		return cmp;
	}

	/*
	 * This (+ per-token implementation of 'AllowsContainment') is probably the
	 * most important point of conversion
	 * 
	 * e.g. indentrun is contains listitem *for identical ranges*, but is
	 * allowed to be contained by listitem (and not truncated, which
	 * section/indent overlaps are)
	 * 
	 * that also means they can't overlap!
	 */
	/*
	 * Gotcha - make sure the treeindex as well as the index is equal for
	 * comparisons. In particular, truncations must modify the *truncated* to
	 * raise its node index to that of the truncated - e.g. ListItemContents,
	 * ListItemLabel
	 */
	protected int classWeight(Class<? extends Token> clazz) {
		if (clazz == LayoutTokens.RootToken.class) {
			return 0;
		}
		// the 20-30ss are all guaranteed logically non-intersecting
		else if (clazz == LayoutTokens.FootnoteReferenceTarget.class) {
			return 20;
		} else if (clazz == LayoutTokens.Section.class) {
			return 20;
		} else if (clazz == LayoutTokens.TableOfContents.class) {
			return 20;
			//
		} else if (clazz == LayoutTokens.FootnoteReferenceTargetContents.class) {
			return 25;
		} else if (clazz == LayoutTokens.IndentRoot.class) {
			return 30;
		} else if (clazz == StructuralTokens.CitedLegislationRun.class) {
			/*
			 * this must be higher than indent run (so will split indent runs if
			 * partial - e.g. CCBB)
			 */
			return STRUCT_BLOCK_CONTAINS_INDENT_RUN;
		} else if (Reflections.isAssignableFrom(TableToken.class, clazz)) {
			// several of these, TABLE, TBODY, TR, TD - their containment will
			// always be OK
			return 35;
			/*
			 * JAD-19990 fix containment - this used to come before table
			 * tokens, but if by chance there's a partial overlap (wip -
			 * doctrans - check why these runs exist, since the overlap look to
			 * be containing ranges from different indent content roots) - we'd
			 * be in trouble
			 */
		} else if (clazz == LayoutTokens.IndentRun.class) {
			return 40;
		} else if (clazz == LayoutTokens.SectionHeader.class) {
			return 45;
		} else if (clazz == LayoutTokens.SubHeading.class) {
			return 45;
		} else if (clazz == LayoutTokens.ListItem.class) {
			return 50;
		} else if (clazz == LayoutTokens.ListItemContents.class) {
			return 55;
		} else if (clazz == LayoutTokens.ListItemLabel.class) {
			// this is rendered as a block - and it's really output that's the
			// determinant here
			return 56;
		} else if (clazz == LayoutTokens.PositionedBlock.class) {
			return 57;
		} else if (clazz == LayoutTokens.ParaBlock.class) {
			return 60;
		} else if (clazz == LayoutTokens.FootnoteReferenceTargetNumber.class) {
			// psuedo-inline - ditto
			return 72;
		} else if (clazz == LayoutTokens.LeafBlock.class) {
			// note that since this can correspond to a text node (in source),
			// there's a specific code path for comparison with
			// LayoutTokens.Image (#customCompare)
			return 74;
		}
		// inlines
		else if (clazz == LayoutTokens.FootnoteReference.class) {
			return 80;
		} else if (clazz == LayoutTokens.Styled.class) {
			return 85;
		} else if (clazz == LayoutTokens.Image.class) {
			return 85;
		} else if (clazz == LayoutTokens.Pagebreak.class) {
			return 85;
		} else if (clazz == LayoutTokens.Text.class) {
			return 90;
		} else {
			// generic block
			return 70;
		}
	}
}