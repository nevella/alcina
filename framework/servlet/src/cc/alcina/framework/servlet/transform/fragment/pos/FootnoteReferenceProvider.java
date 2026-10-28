package cc.alcina.framework.servlet.transform.fragment.pos;

import cc.alcina.framework.servlet.transform.fragment.StyledDocument.Styles;
import cc.alcina.framework.common.client.dom.DomNode;

public interface FootnoteReferenceProvider {
	public interface FootnoteCommon {
		String getFootnoteNumber();

		boolean isReference();

		boolean is();
	}

	public static class Mso implements FootnoteReferenceProvider {
		static class MsoFootnoteRefTarget implements FootnoteCommon {
			boolean is;

			String ref;

			MsoFootnoteRefTarget(DomNode node) {
				String regex = "#_ftnref(\\d+)";
				if (node.tagIs("a") && node.attrMatches("href", regex)) {
					is = true;
					ref = node.attr("href").replaceFirst(regex, "$1");
				}
			}

			@Override
			public boolean is() {
				return is;
			}

			@Override
			public String getFootnoteNumber() {
				return ref;
			}

			@Override
			public boolean isReference() {
				return false;
			}
		}

		/*
		 * A reference to a footnote
		 */
		static class MsoFootnoteRef implements FootnoteCommon {
			boolean is;

			String ref;

			@Override
			public boolean is() {
				return is;
			}

			@Override
			public String getFootnoteNumber() {
				return ref;
			}

			@Override
			public boolean isReference() {
				return is;
			}

			MsoFootnoteRef(DomNode node) {
				String regex = "#_ftn(\\d+)";
				if (node.tagIs("a") && node.attrMatches("href", regex)) {
					is = true;
					ref = node.attr("href").replaceFirst(regex, "$1");
				}
			}
		}

		@Override
		public boolean isFootnoteReference(Styles styles) {
			return new MsoFootnoteRef(styles.getNode()).is;
		}

		@Override
		public boolean isFootnoteReferenceTarget(Styles styles) {
			return new MsoFootnoteRefTarget(styles.getNode()).is;
		}

		@Override
		public FootnoteCommon getFootnoteReference(Styles styles) {
			return new MsoFootnoteRef(styles.getNode());
		}

		@Override
		public FootnoteCommon getFootnoteReferenceTarget(Styles styles) {
			return new MsoFootnoteRefTarget(styles.getNode());
		}
	}

	public static class FdGeneric implements FootnoteReferenceProvider {
		static class FdGenericFootnoteRefTarget implements FootnoteCommon {
			boolean is;

			String ref;

			FdGenericFootnoteRefTarget(DomNode node) {
				// hack - returns the parent if this is the direct child of a
				// footnote
				// (because the caller - LogicalRunSequenceLayer - doesn't
				// include all container blocks)
				//
				// FIXME - cit.v2 - high - make the lrsl contiguous for
				// treeindex, not just index
				DomNode footnoteTargetContainer = node.ancestors().orSelf()
						.tagAndClassIs("block", "footnote-inpage");
				if (footnoteTargetContainer != null) {
					ref = footnoteTargetContainer.attr("number");
					is = true;
				}
			}

			@Override
			public boolean is() {
				return is;
			}

			@Override
			public String getFootnoteNumber() {
				return ref;
			}

			@Override
			public boolean isReference() {
				return false;
			}
		}

		/*
		 * A reference to a footnote
		 */
		static class FdGenericFootnoteRef implements FootnoteCommon {
			boolean is;

			String ref;

			@Override
			public boolean is() {
				return is;
			}

			@Override
			public String getFootnoteNumber() {
				return ref;
			}

			@Override
			public boolean isReference() {
				return is;
			}

			FdGenericFootnoteRef(DomNode node) {
				DomNode footnoteContainer = node.ancestors().orSelf()
						.tagAndClassIs("inline", "footnote");
				if (footnoteContainer != null) {
					ref = footnoteContainer.attr("number");
					is = true;
				}
			}
		}

		@Override
		public boolean isFootnoteReference(Styles styles) {
			return new FdGenericFootnoteRef(styles.getNode()).is;
		}

		@Override
		public boolean isFootnoteReferenceTarget(Styles styles) {
			return new FdGenericFootnoteRefTarget(styles.getNode()).is;
		}

		@Override
		public FootnoteCommon getFootnoteReference(Styles styles) {
			return new FdGenericFootnoteRef(styles.getNode());
		}

		@Override
		public FootnoteCommon getFootnoteReferenceTarget(Styles styles) {
			return new FdGenericFootnoteRefTarget(styles.getNode());
		}
	}

	boolean isFootnoteReference(Styles styles);

	boolean isFootnoteReferenceTarget(Styles styles);

	FootnoteCommon getFootnoteReference(Styles styles);

	FootnoteCommon getFootnoteReferenceTarget(Styles styles);
}
