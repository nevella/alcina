package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Selection;
import com.google.gwt.dom.client.Text;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNode.DomNodeText.SplitResult;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.ContextSensitiveReverseTransform;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.ContextSensitiveTransform;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Commit;
import cc.alcina.framework.gwt.client.dirndl.layout.FragmentNode;
import cc.alcina.framework.gwt.client.dirndl.model.dom.RelativeSelection;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentModel;

/**
 * 
 */
public abstract class DecoratorNode<WT, SR> extends FragmentNode {
	/**
	 * The characteristics of the decorator, such as the key sequence which
	 * triggers its creation
	 *
	 */
	public static abstract class Descriptor<WT, SR, DN extends DecoratorNode>
			implements ModelEvents.Commit.Handler {
		public abstract DN createNode();

		public abstract Function<WT, String> itemRenderer();

		@Override
		public abstract void onCommit(Commit event);

		public abstract String triggerSequence();

		DN splitAndWrap(RelativeSelection relativeSelection,
				FragmentModel fragmentModel) {
			String triggerSequence = getTriggerSequence(relativeSelection);
			SplitResult splits = relativeSelection
					.splitAtTriggerRange(triggerSequence);
			DomNode splitContents = splits.contents;
			// may need to flush (to populate FNs) - note for romcom, want to
			// not force remote
			LocalDom.flush();
			FragmentNode textFragment = fragmentModel
					.getFragmentNode(splitContents);
			DN created = createNode();
			created.setContentEditable(true);
			textFragment.nodes().insertBeforeThis(created);
			created.nodes().append(textFragment);
			LocalDom.flush();
			Selection selection = Document.get().getSelection();
			Text text = (Text) splits.contents.w3cNode();
			selection.collapse(text, text.getLength());
			return created;
		}

		protected abstract SR toStringRepresentable(WT wrappedType);

		public boolean isTriggerSequence(String potentialTrigger) {
			return potentialTrigger.startsWith(triggerSequence());
		}

		public String getTriggerSequence(RelativeSelection relativeSelection) {
			String potentialTrigger = relativeSelection
					.getTriggerableRangePrecedingFocus().text();
			if (triggerSequence().isEmpty()) {
				return potentialTrigger;
			} else {
				String pattern = Ax.format(".*(^|[ \\u200B({\\[])(%s.*)",
						triggerSequence());
				RegExp regExp = RegExp.compile(pattern);
				MatchResult matchResult = regExp.exec(potentialTrigger);
				return matchResult == null ? null : matchResult.getGroup(2);
			}
		}
	}

	@Directed(tag = "span", className = "cursor-target")
	public static class ZeroWidthCursorTarget extends FragmentNode {
		static final String ZWS_CONTENT = "\u200B";

		@Override
		public void onFragmentRegistration() {
			nodes().append(new TextNode(ZWS_CONTENT));
		}

		@Property.Not
		public TextNode getSoleTextNode() {
			if (provideChildNodes().size() != 1) {
				return null;
			}
			FragmentNode child = children().findFirst().get();
			return child instanceof TextNode ? (TextNode) child : null;
		}

		public void unwrapIfContainsNonZwsText() {
			TextNode soleTextNode = getSoleTextNode();
			if (soleTextNode == null
					|| !Objects.equals(soleTextNode.liveValue(), ZWS_CONTENT)) {
				List<TextNode> childTexts = (List) byType(TextNode.class)
						.collect(Collectors.toList());
				childTexts.forEach(text -> {
					String nodeValue = text.liveValue();
					String replaceValue = nodeValue.replace(ZWS_CONTENT, "");
					if (!Objects.equals(nodeValue, replaceValue)) {
						// this may move the selection cursor! so requires more
						// bubbling/event chaining, non-deferred
						text.setValue(replaceValue);
					}
				});
				// FIXME - strip before replace - due to LD issue
				strip();
			}
		}
	}

	@Binding(
		type = Type.PROPERTY,
		to = "contentEditable",
		transform = Binding.DisplayFalseTrueBidi.class)
	public boolean contentEditable = false;

	@Binding(type = Type.INNER_TEXT)
	public String content = "";

	protected SR stringRepresentable;

	public void setStringRepresentable(SR stringRepresentable) {
		set("stringRepresentable", this.stringRepresentable,
				stringRepresentable,
				() -> this.stringRepresentable = stringRepresentable);
	}

	public abstract Descriptor<WT, SR, ?> getDescriptor();

	@Binding(
		type = Type.PROPERTY,
		to = "uid",
		transform = RepresentableToStringTransform.class)
	public SR getStringRepresentable() {
		return this.stringRepresentable;
	}

	/**
	 * Transforms the referenced object to/from a string representation (which
	 * will populate the decorated node's <code>uid</code> field)
	 */
	public static class RepresentableToStringTransform<SR>
			implements Binding.Bidi<SR> {
		@Override
		public Function<SR, String> leftToRight() {
			return new Left();
		}

		@Override
		public Function<String, SR> rightToLeft() {
			return new Right();
		}

		public static interface ToStringRepresentation<SR>
				extends ContextSensitiveTransform<SR> {
		}

		public static interface FromStringRepresentation<SR>
				extends ContextSensitiveReverseTransform<SR> {
		}

		class Left extends Binding.AbstractContextSensitiveTransform<SR> {
			@Override
			public String apply(SR t) {
				if (t == null) {
					return null;
				} else {
					ToStringRepresentation<SR> impl = Registry
							.impl(ToStringRepresentation.class, t.getClass());
					impl.withContextNode(node);
					return impl.apply(t);
				}
			}
		}

		class Right
				extends Binding.AbstractContextSensitiveReverseTransform<SR> {
			@Override
			public SR apply(String t) {
				if (t == null) {
					return null;
				} else {
					Object contextModel = node.getModel();
					FromStringRepresentation<SR> impl = Registry
							.impl(FromStringRepresentation.class, t.getClass());
					impl.withContextNode(node);
					return (SR) impl.apply(t);
				}
			}
		}

		@Registration({ ToStringRepresentation.class, String.class })
		public static class PassthroughTransformLeft
				extends Binding.AbstractContextSensitiveTransform<String>
				implements ToStringRepresentation<String> {
			@Override
			public String apply(String t) {
				return t;
			}
		}

		@Registration({ FromStringRepresentation.class, String.class })
		public static class PassthroughTransformRight
				extends Binding.AbstractContextSensitiveReverseTransform<String>
				implements FromStringRepresentation<String> {
			@Override
			public String apply(String t) {
				return t;
			}
		}
	}

	public void putReferenced(WT wrappedType) {
		setStringRepresentable(
				getDescriptor().toStringRepresentable(wrappedType));
		String text = getDescriptor().triggerSequence()
				+ ((Function) getDescriptor().itemRenderer())
						.apply(wrappedType);
		setContent(text);
	}

	public void setContent(String content) {
		set("content", this.content, content, () -> this.content = content);
	}

	public void setContentEditable(boolean contentEditable) {
		if (!contentEditable && this.contentEditable) {
			int debug = 3;
		}
		set("contentEditable", this.contentEditable, contentEditable,
				() -> this.contentEditable = contentEditable);
	}

	boolean isValid() {
		// FIXME - DN server shd validate entity on update. and other
		// validations (e.g. not contained in a decorator)
		return stringRepresentable != null;
	}

	void positionCursorPostReferencedSelection() {
		LocalDom.flush();
		LocalDom.flushLocalMutations();
		FragmentNode.TextNode textNode = (TextNode) children().findFirst()
				.get();
		// TODO - position cursor at the end of the mention, then allow the
		// 'cursor validator' to move it to a correct location
		// try positioning cursor immediately after the decorator
		// guaranteed non-null (due to zws insertion)
		FragmentNode.TextNode cursorTarget = textNode.fragmentTree()
				.nextTextNode(true).get();
		Node cursorNode = cursorTarget.domNode().gwtNode();
		Selection selection = Document.get().getSelection();
		selection.collapse(cursorNode, 1);// after zws
	}

	void stripIfInvalid() {
		if (!isValid()) {
			strip();
			/*
			 * FIXME - dn - tree change listener (FragmentModel?) should
			 * probably merge adjacent FragmentNode.Text children if any result
			 * from the strip
			 */
		}
	}

	public void toNonEditable() {
		setContentEditable(false);
	}

	void ensureSpacers() {
		if (!isEditableTextNodeOrSpace(nodes().previousSibling())) {
			nodes().insertBeforeThis(new ZeroWidthCursorTarget());
		}
		if (!isEditableTextNodeOrSpace(nodes().nextSibling())) {
			nodes().insertAfterThis(new ZeroWidthCursorTarget());
		}
	}

	boolean isEditableTextNodeOrSpace(FragmentNode sibling) {
		if (sibling == null) {
			return false;
		}
		return sibling instanceof ZeroWidthCursorTarget
				|| sibling instanceof TextNode;
	}
}
