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
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Commit;
import cc.alcina.framework.gwt.client.dirndl.layout.FragmentNode;
import cc.alcina.framework.gwt.client.dirndl.model.dom.EditSelection;
import cc.alcina.framework.gwt.client.dirndl.model.edit.StringRepresentable.RepresentableToStringTransform;
import cc.alcina.framework.gwt.client.dirndl.model.edit.StringRepresentable.RepresentableToStringTransform.HasStringRepresentableType;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentModel;

/**
 * 
 */
public abstract class DecoratorNode<WT, SR> extends FragmentNode
		implements HasStringRepresentableType<SR> {
	@Override
	public Class<SR> stringRepresentableType() {
		return Reflections.at(this).getGenericBounds().bounds.get(1);
	}

	/**
	 * Models the characteristics of the content decorator, such as the key
	 * sequence which triggers its creation, the class reference modelled, etc
	 *
	 */
	public static abstract class Descriptor<WT, SR, DN extends DecoratorNode>
			implements ModelEvents.Commit.Handler {
		public abstract DN createNode();

		public abstract Function<WT, String> itemRenderer();

		@Override
		public abstract void onCommit(Commit event);

		public abstract String triggerSequence();

		DN splitAndWrap(EditSelection editSelection,
				FragmentModel fragmentModel) {
			String triggerSequence = getTriggerSequence(editSelection);
			SplitResult splits = editSelection
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

		public String getTriggerSequence(EditSelection editSelection) {
			String potentialTrigger = editSelection
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
		public static final String ZWS_CONTENT = "\u200B";

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
					if (!Objects.equals(nodeValue, replaceValue) &&
					// localdom doesn't like 0-length text nodes
							replaceValue.length() > 0) {
						// this may move the selection cursor! so requires more
						// bubbling/event chaining, non-deferred
						text.setValue(replaceValue);
						// this is the non-bubbling, quick hack - FIXME FN
						Document.get().getSelection().validate();
						text.domNode().asLocation().locationContext
								.invalidate();
					}
				});
				nodes().strip();
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
		if (provideIsUnbound()) {
			return;// removed
		}
		FragmentNode.TextNode textNode = (TextNode) children().findFirst()
				.get();
		// TODO - position cursor at the end of the mention, then allow the
		// 'cursor validator' to move it to a correct location
		// try positioning cursor immediately after the decorator
		// guaranteed non-null (due to zws insertion)
		FragmentNode.TextNode cursorTarget = textNode.fragmentTree()
				.nextTextNode(true).orElse(null);
		/*
		 * well - what's the dispatch model for ZWS insertion? Maybe it is
		 * null...maybe we've lost focus...
		 */
		if (cursorTarget == null) {
			int debug = 3;
			// Client.eventBus().queued()
			// .lambda(this::positionCursorPostReferencedSelection)
			// .deferred().dispatch();
			return;
		}
		Node cursorNode = cursorTarget.domNode().gwtNode();
		Selection selection = Document.get().getSelection();
		selection.collapse(cursorNode, 1);// after zws
	}

	void stripIfInvalid() {
		if (!isValid()) {
			nodes().strip();
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
