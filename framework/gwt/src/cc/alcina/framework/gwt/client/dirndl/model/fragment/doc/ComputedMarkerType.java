package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocument.TexAttributes;

/**
 * When transforming to "marked up markup" - this enum specifies text "around"
 * the xml nodes - such as say "\begin{list}...\end{list} for tex, or
 * <list>....</list> for fragmentDocument/xml". In particular, this controls
 * whitespace
 */
public enum ComputedMarkerType {
	XML {
		@Override
		public String computeMarkerContents(NodeBoundary boundary) {
			String closeMarker = boundary.open ? "" : "/";
			String attrs = boundary.inline && boundary.open ? " inline=\"true\""
					: "";
			// actually, determine spacer from tex...?
			return Ax.format("<%s%s%s>", closeMarker, boundary.tag, attrs);
		}
	},
	TEX {
		/**
		 * <h2>Newline rules (newlines are always added before the content of a
		 * boundary marker)
		 * <ul>
		 * <li>open[cmp.block]-open[cmp.block] :: 2
		 * <li>close[]
		 * </ul>
		 */
		@Override
		public String computeMarkerContents(NodeBoundary boundary) {
			String contents = "";
			String attributes = "";
			if (boundary.open) {
				attributes = TexAttributes
						.texAttributeRepresentation(boundary.fragmentNode);
			}
			if (boundary.fragmentNode instanceof Block.Para) {
				contents = "";
			} else {
				String tag = boundary.tag;
				// FIX - non-declarative
				if (tag.equals("doc")) {
					String className = boundary.fragmentNode.provideNode()
							.getRendered().asW3cElement().getAttribute("class");
					if (Ax.notBlank(className)) {
						tag = className;
					}
				}
				if (boundary.simpleChildren || boundary.inline) {
					// FIX - non-declarative
					boolean attributeOfParent = tag.equals("label");
					if (boundary.open) {
						/*
						 * list labels should be (in tex) an attribute of the
						 * parent list environment
						 */
						String attributeOpenWrapper = attributeOfParent ? "["
								: "";
						contents = Ax.format("%s\\%s%s{", attributeOpenWrapper,
								tag, attributes);
					} else {
						String attributeCloseWrapper = attributeOfParent ? "]"
								: "";
						contents = Ax.format("}%s", attributeCloseWrapper);
					}
				} else {
					String boundaryMarker = boundary.open ? "begin" : "end";
					contents = Ax.format("\\%s{%s}%s", boundaryMarker, tag,
							attributes);
				}
			}
			int lfs = 0;
			NodeBoundary previousBoundary = boundary.previousBoundary;
			if (previousBoundary != null) {
				boolean priorWasBlock = !previousBoundary.inline;
				boolean block = !boundary.inline;
				if (boundary.open) {
					boolean collapseBoundaries = boundary.collapseBoundaries
							&& (previousBoundary.inline
									|| previousBoundary.open);
					if (block) {
						lfs = collapseBoundaries ? 1 : 2;
					} else {
						if (boundary.separateLine) {
							lfs = 1;
						}
					}
				} else {
					if (previousBoundary.open) {
						// simple content - <a>---</a> or <? --- ?>. No lfs
					} else {
						if (!previousBoundary.inline) {
							boolean collapse = previousBoundary.collapseBoundaries
									&& boundary.collapseBoundaryContainer;
							lfs = collapse ? 1 : 2;
						}
					}
				}
				for (int idx = 0; idx < lfs; idx++) {
					contents = "\n" + contents;
				}
			}
			return contents;
		}
	};

	public abstract String computeMarkerContents(NodeBoundary boundary);
}