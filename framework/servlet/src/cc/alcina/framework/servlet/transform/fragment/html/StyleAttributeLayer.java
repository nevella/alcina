package cc.alcina.framework.servlet.transform.fragment.html;

import java.util.LinkedHashSet;
import java.util.Set;

import cc.alcina.framework.servlet.transform.fragment.StyledDocument.Styles;
import cc.alcina.framework.servlet.transform.fragment.StyledMeasureSelection;
import cc.alcina.framework.servlet.transform.fragment.StyledNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.StringMap;

/**
 * <p>
 * This layer checks all source doc styled nodes for attributes of interest, and
 * emits a StyleAttributes measure selection if it finds any
 * 
 * <p>
 * Note that this just wraps text nodes (rather than tracking the source inline
 * structure) - so only reflects inline attributes
 */
public class StyleAttributeLayer extends Layer<StyledNode> {
	Peer peer;

	@Override
	protected void onBeforeIteration() {
		super.onBeforeIteration();
		peer = state.traversalContext(Peer.Has.class)
				.getStyleAttributeLayerPeer();
	}

	@Override
	public void process(StyledNode node) throws Exception {
		DomNode domNode = node.get();
		if (domNode.isText()) {
			if (node.isIgnorableWhitespace()) {
				return;
			}
			Set<String> classNames = new LinkedHashSet<>();
			StringMap styleAttributes = new StringMap();
			Styles parentStyles = node.parentStyles();
			if (parentStyles.isBold()) {
				classNames.add("bold");
			}
			if (parentStyles.isItalic()) {
				classNames.add("italic");
			}
			if (parentStyles.isSuperscript()) {
				classNames.add("sup");
			}
			if (parentStyles.isSubscript()) {
				classNames.add("sub");
			}
			if (parentStyles.isStrikethrough()) {
				classNames.add("strike");
			}
			if (parentStyles.isUnderline()) {
				classNames.add("underline");
			}
			if (peer.isPassthroughColorAttribute()) {
				if (parentStyles.hasColor()) {
					String color = parentStyles.getColor();
					if (!color.matches(peer.getDefaultColorValue())) {
						styleAttributes.put("color", color);
					}
				}
			}
			/*
			 * TD backgrounds are handled at the TD level
			 */
			if (!node.get().ancestors().has("td")) {
				if (peer.isPassthroughBackgroundColorAttribute()) {
					if (parentStyles.hasBackgroundColor()) {
						String backgroundColor = parentStyles
								.getBackgroundColor();
						styleAttributes.put("background-color",
								backgroundColor);
					}
				}
			}
			StyledNode cursor = node;
			while (!cursor.parentStyles().isBlock()) {
				peer.addStyleAttributes(cursor, styleAttributes);
				peer.addClassNames(cursor, classNames);
				cursor = cursor.styledParent();
			}
			if (classNames.isEmpty() && styleAttributes.isEmpty()) {
				return;
			}
			select(new StyleAttributes(node,
					Measure.fromNode(domNode, LayoutTokens.Styled.TYPE),
					classNames, styleAttributes));
		}
	}

	public static class Peer implements StylePassthrough {
		public boolean isPassthroughColorAttribute() {
			return false;
		}

		public boolean isPassthroughBackgroundColorAttribute() {
			return false;
		}

		public void addClassNames(StyledNode domTextNode,
				Set<String> classNames) {
		}

		public void addStyleAttributes(StyledNode domTextNode,
				StringMap styleAttributes) {
		}

		public String getDefaultColorValue() {
			return "#000000|windowtext|black";
		}

		public interface Has {
			Peer getStyleAttributeLayerPeer();
		}
	}

	static class StyleAttributes extends StyledMeasureSelection
			implements LayoutTokens.HasStyles {
		private Set<String> classNames;

		private StringMap styleAttributes;

		StyleAttributes(Selection parent, Measure measure,
				Set<String> classNames, StringMap styleAttributes) {
			super(parent, measure);
			this.classNames = classNames;
			this.styleAttributes = styleAttributes;
		}

		public Set<String> getClassNames() {
			return classNames;
		}

		public StringMap getStyleAttributes() {
			return styleAttributes;
		}

		@Override
		public MeasureSelection truncateTo(IntPair range) {
			Location.Range locationRange = get().truncateAbsolute(range.i1,
					range.i2);
			Measure measure = Measure.fromRange(locationRange, get().token);
			return new StyleAttributes(parentSelection(), measure, classNames,
					styleAttributes);
		}

		static class View extends MeasureSelection.View<StyleAttributes> {
			@Override
			public String computeText(StyleAttributes selection) {
				String text = super.computeText(selection);
				StyledNode domTextNode = (StyledNode) selection
						.parentSelection();
				StyledNode styledParent = domTextNode.styledParent();
				return FormatBuilder.keyValues("text", text,
						"\nstyleAttributes", selection.styleAttributes,
						"\nclassNames", selection.classNames, "\nstyledParent",
						styledParent.get());
			}
		}
	}
}