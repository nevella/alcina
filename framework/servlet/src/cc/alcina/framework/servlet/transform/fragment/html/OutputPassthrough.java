package cc.alcina.framework.servlet.transform.fragment.html;

import java.util.Objects;
import java.util.regex.Pattern;

import cc.alcina.framework.servlet.transform.fragment.DocumentTransformerSelection;
import cc.alcina.framework.servlet.transform.fragment.StyledNode;
import cc.alcina.framework.servlet.transform.fragment.output.MeasureNode;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.util.Ax;

/**
 * <p>
 * An abstract class (basically here as a documentation/classification of
 * subtypes - it does nothing itself) which collects passthrough actions (e.g.
 * passing @id attributes from the source to the target document). Child layers
 * should generally traverse {@link MeasureNode}
 */
public abstract class OutputPassthrough
		extends Layer<DocumentTransformerSelection> {
	public OutputPassthrough() {
	}

	/**
	 * Note that all matches are regex matches
	 */
	public static abstract class AttributePassthrough
			extends Layer<MeasureNode> {
		/**
		 * Passthrough attribute names matching the regex, e.g. "id|class". By
		 * default pass all attributes
		 */
		protected String attributeName;

		/**
		 * Passthrough attribute values matching the regex. By default pass all
		 * non-empty values
		 */
		protected String attributeValue;

		/**
		 * if null, match any tag
		 */
		protected String outputTag;

		/**
		 * Only passthrough is the ancestor {@link StyledNode} matches this tag.
		 * If the value is null, the ancestor must have the same tag as
		 * {@link #outputTag}
		 */
		protected String inputTag;

		/**
		 * For development, as a logic check of the passthrough
		 */
		protected boolean throwIfNonMatchingAncestor;

		Pattern inputTagPattern;

		Pattern outputTagPattern;

		Pattern attributeNamePattern;

		Pattern attributeValuePattern;

		boolean inputMatchesOutput;

		@Override
		protected void onBeforeIteration() {
			super.onBeforeIteration();
			if (Ax.isBlank(inputTag)) {
				inputMatchesOutput = true;
			} else {
				inputTagPattern = Pattern.compile(inputTag);
			}
			outputTagPattern = Pattern.compile(Ax.blankTo(outputTag, ".*"));
			attributeNamePattern = Pattern
					.compile(Ax.blankTo(attributeName, ".*"));
			attributeValuePattern = Pattern
					.compile(Ax.blankTo(attributeValue, ".+"));
		}

		@Override
		public void process(MeasureNode selection) {
			DomNode outputNode = selection.get().domNode();
			if (outputTagPattern.matcher(outputNode.name()).matches()) {
				StyledNode inputSelection = selection
						.ancestor(StyledNode.class);
				DomNode inputNode = inputSelection.get();
				boolean matchesInput = false;
				if (inputMatchesOutput) {
					matchesInput = Objects.equals(inputNode.name(),
							outputNode.name());
				} else {
					matchesInput = inputTagPattern.matcher(inputNode.name())
							.matches();
				}
				if (matchesInput) {
					inputNode.attributes().forEach((k, v) -> {
						if (attributeNamePattern.matcher(k).matches()
								&& attributeValuePattern.matcher(v).matches()) {
							outputNode.setAttr(k, v);
						}
					});
				} else {
					if (throwIfNonMatchingAncestor) {
						throw new IllegalStateException(
								Ax.format("No matching input :: %s",
										Ax.ntrim(inputSelection.get().ntc())));
					}
				}
			}
		}
	}

	/**
	 * This class assumes HTML input, and passes selected table descendant
	 * attributes from input to output doc, if possible
	 */
	public static abstract class PassthroughTableAttrs
			extends AttributePassthrough {
		public PassthroughTableAttrs() {
			inputTag = "th|td";
			outputTag = "th|td";
		}

		public static class CellSpans extends PassthroughTableAttrs {
			public CellSpans() {
				attributeName = "rowspan|colspan";
			}
		}

		public static class Width extends PassthroughTableAttrs {
			public Width() {
				attributeName = "width";
			}
		}
	}
}