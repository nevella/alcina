package cc.alcina.framework.servlet.transform.fragment.pos;

import java.util.Objects;
import java.util.Optional;

import cc.alcina.framework.servlet.transform.fragment.StyledDocument;
import cc.alcina.framework.servlet.transform.fragment.StyledNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TextUtils;

public interface ListItemLabelProvider {
	public static final String RIGHT_ALIGNED = "right-aligned";

	Range getListLabel(StyledNode styledNode);

	default Measure getListLabelMeasure(StyledNode styledNode) {
		Range range = getListLabel(styledNode);
		if (range == null) {
			return null;
		} else {
			Measure measure = Measure.fromRange(range,
					LayoutTokens.ListItemLabel.TYPE);
			measure.setData(getListLabelData(styledNode));
			return measure;
		}
	}

	default String getListLabelText(StyledNode styledNode) {
		return getListLabel(styledNode).text();
	}

	default Object getListLabelData(StyledNode styledNode) {
		return null;
	}

	public static class MsoSpan implements ListItemLabelProvider {
		/*
		 * return the first label domnode in the node, but only if this is the
		 * direct block ancestor of the label
		 */
		public Range getListLabel(StyledNode styledNode) {
			if (!styledNode.styles().isBlock()) {
				return null;
			}
			if (!hasMsoListLabel(styledNode.get())) {
				return null;
			}
			StyledDocument styledDocument = styledNode.getStyledDocument();
			Optional<DomNode> firstLabel = styledNode.get().tree().stream()
					.filter(this::isMsoListLabel).findFirst();
			return firstLabel
					.filter(n -> styledDocument.styledNode(n)
							.blockAncestor() == styledNode)
					.map(DomNode::asRange).orElse(null);
		}

		boolean hasMsoListLabel(DomNode node) {
			Optional<DomNode> nextNonWhitespaceTextNode = node.tree()
					.nextNonWhitespaceTextNode();
			if (nextNonWhitespaceTextNode.isPresent()) {
				String labelText = nextNonWhitespaceTextNode.get().ntc();
				DomNode textContainer = nextNonWhitespaceTextNode.get()
						.parent();
				if (isLabelSpan(textContainer)) {
					return true;
				}
			}
			return false;
		}

		private boolean isLabelSpan(DomNode textContainer) {
			return textContainer.tagIs("span") && textContainer
					.attrMatches("style", "mso-list:[ \n]*Ignore");
		}

		boolean isMsoListLabel(DomNode node) {
			if (!node.tagIs("span")) {
				return false;
			}
			DomNode pcestor = node.ancestors().get("p");
			Optional<DomNode> nextNonWhitespaceTextNode = node.tree()
					.nextNonWhitespaceTextNode();
			if (nextNonWhitespaceTextNode.isPresent()) {
				String labelText = nextNonWhitespaceTextNode.get().ntc();
				DomNode textContainer = nextNonWhitespaceTextNode.get()
						.parent();
				if (isLabelSpan(textContainer)) {
					// find the maximal ancestor
					DomNode cursor = textContainer.parent();
					while (true) {
						if (!cursor.tagIs("span")) {
							return false;
						}
						if (!Objects.equals(cursor.ntc(), labelText)) {
							return false;
						}
						if (cursor == node) {
							return true;
						}
						cursor = cursor.parent();
					}
				}
			}
			return false;
		}
	}

	public static class MsoMulti implements ListItemLabelProvider {
		MsoTabCount tabDelegate = new MsoTabCount();

		MsoSpan spanDelegate = new MsoSpan();

		MsoSpaceRun spaceRunDelegate = new MsoSpaceRun();

		@Override
		public Range getListLabel(StyledNode styledNode) {
			Range range = tabDelegate.getListLabel(styledNode);
			if (range == null) {
				range = spanDelegate.getListLabel(styledNode);
			}
			if (range == null) {
				range = spaceRunDelegate.getListLabel(styledNode);
			}
			return range;
		}
	}

	public static class MsoTabCount implements ListItemLabelProvider {
		/*
		 * return the first label domnode in the node, but only if this is the
		 * direct block ancestor of the label
		 */
		public Range getListLabel(StyledNode styledNode) {
			if (!styledNode.styles().isBlock()) {
				return null;
			}
			DomNode msoListLabel = getMsoListLabel(styledNode.get());
			StyledDocument styledDocument = styledNode.getStyledDocument();
			return Optional.ofNullable(msoListLabel)
					.filter(n -> styledDocument.styledNode(n)
							.blockAncestor() == styledNode)
					.map(this::labelRange).orElse(null);
		}

		Range labelRange(DomNode nonWhitespace) {
			DomNode preSpacer = nonWhitespace.relative().previousSibling();
			DomNode postSpacer = nonWhitespace.relative().nextSibling();
			return new Range(preSpacer.asLocation(), postSpacer.asRange().end);
		}

		DomNode getMsoListLabel(DomNode node) {
			DomNode nonWhitespace = node.tree().nextNonWhitespaceTextNode()
					.orElse(null);
			if (nonWhitespace != null) {
				String labelText = nonWhitespace.ntc();
				DomNode preSpacer = nonWhitespace.relative().previousSibling();
				DomNode postSpacer = nonWhitespace.relative().nextSibling();
				if (isTabCount(preSpacer) && isTabCount(postSpacer)) {
					return nonWhitespace;
				}
			}
			return null;
		}

		boolean isTabCount(DomNode node) {
			return node != null && node.tagIs("span")
					&& TextUtils.removeWhitespace(node.attr("style"))
							.equals("mso-tab-count:1");
		}
	}

	public static class MsoSpaceRun implements ListItemLabelProvider {
		/*
		 * return the first label domnode in the node, but only if this is the
		 * direct block ancestor of the label
		 */
		public Range getListLabel(StyledNode styledNode) {
			if (!styledNode.styles().isBlock()) {
				return null;
			}
			DomNode msoListLabel = getMsoListLabel(styledNode.get());
			StyledDocument styledDocument = styledNode.getStyledDocument();
			return Optional.ofNullable(msoListLabel)
					.filter(n -> styledDocument.styledNode(n)
							.blockAncestor() == styledNode)
					.map(DomNode::asRange).orElse(null);
		}

		DomNode getMsoListLabel(DomNode node) {
			DomNode nonWhitespace = node.tree().nextNonWhitespaceTextNode()
					.orElse(null);
			if (nonWhitespace != null) {
				String labelText = nonWhitespace.ntc();
				DomNode postSpacer = nonWhitespace.relative().nextSibling();
				if (isTabCount(postSpacer)) {
					return nonWhitespace;
				}
			}
			return null;
		}

		boolean isTabCount(DomNode node) {
			return node != null && node.tagIs("span")
					&& TextUtils.removeWhitespace(node.attr("style"))
							.equals("mso-spacerun:yes");
		}
	}

	public static class NonBreakingSpaces implements ListItemLabelProvider {
		public Range getListLabel(StyledNode styledNode) {
			return styledNode.styles().getIndents()
					.testSpaceSeparatedLi(styledNode.styles().node).labelRange;
		}
	}

	public static class FdGeneric implements ListItemLabelProvider {
		public Range getListLabel(StyledNode styledNode) {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * The journal's ListItemLayerPeer should return 'true' for
	 * isStructuredLists
	 */
	public static class HTML implements ListItemLabelProvider {
		public Range getListLabel(StyledNode styledNode) {
			DomNode domNode = styledNode.get();
			if (domNode.tagIs("li")) {
				return new Location.Range(domNode.asLocation(),
						domNode.asLocation());
			} else {
				return null;
			}
		}

		// TODO - use resolved css rules
		@Override
		public boolean isList(StyledNode styledNode) {
			return styledNode.get().tagIsOneOf("ol", "ul", "li");
		}

		public String getListLabelText(StyledNode styledNode) {
			Range listLabel = getListLabel(styledNode);
			if (listLabel == null) {
				return null;
			}
			DomNode domNode = styledNode.get().asDomNode();
			if (domNode.has("value")) {
				return domNode.attr("value");
			}
			DomNode parent = domNode.parent();
			if (parent.tagIs("ul")) {
				return "·";
			}
			int index = parent.children.elements().indexOf(domNode);
			return getListStyle(styledNode).format(index);
		}

		protected ListStyle getListStyle(StyledNode styledNode) {
			return ListStyle.numeric;
		}

		public enum ListStyle {
			numeric {
				@Override
				String format(int index) {
					return Ax.format("%s.", index + 1);
				}
			},
			bracketed_alpha {
				@Override
				String format(int index) {
					return Ax.format("(%s)", (char) ('a' + index));
				}
			};

			abstract String format(int index);
		}
	}

	default String getListLabelClasses(StyledNode styledNode) {
		return null;
	}

	default boolean isList(StyledNode styledNode) {
		return getListLabel(styledNode) != null;
	}
}
