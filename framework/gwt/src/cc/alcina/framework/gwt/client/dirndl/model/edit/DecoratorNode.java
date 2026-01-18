package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Selection;
import com.google.gwt.dom.client.Text;
import com.google.gwt.dom.client.behavior.ElementBehavior;
import com.google.gwt.dom.client.behavior.HasElementBehaviors;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNode.DomNodeText.SplitResult;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Commit;
import cc.alcina.framework.gwt.client.dirndl.model.dom.EditSelection;
import cc.alcina.framework.gwt.client.dirndl.model.edit.StringRepresentable.RepresentableToStringTransform;
import cc.alcina.framework.gwt.client.dirndl.model.edit.StringRepresentable.RepresentableToStringTransform.HasStringRepresentableType;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentIsolate;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentModel;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.TextNode;

/**
 * The base class for a visual model of a decorated measure, such as a hashtag
 * or mention in a document, or a selected choice in a dropdown suggestor
 */
@Directed(className = "decorator-node")
@TypedProperties
public abstract class DecoratorNode<WT, SR> extends EditNode implements
		HasStringRepresentableType<SR>, FragmentIsolate, HasElementBehaviors {
	static boolean isNonEditable(FragmentNode node) {
		return node instanceof DecoratorNode
				&& !((DecoratorNode) node).contentEditable;
	}

	@Override
	public List<Class<? extends ElementBehavior>> getBehaviors() {
		return List.of(ElementBehavior.FragmentIsolateBehavior.class);
	}

	/**
	 * Models the characteristics of the content decorator, such as the key
	 * sequence which triggers its creation, the class reference modelled, etc
	 *
	 */
	public static abstract class Descriptor<WT, SR, DN extends DecoratorNode>
			implements ModelEvents.Commit.Handler {
		public abstract DN createNode();

		public abstract Function<WT, ?> itemRenderer();

		@Override
		public abstract void onCommit(Commit event);

		public abstract String triggerSequence();

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

		protected abstract SR toStringRepresentable(WT wrappedType);

		DN splitAndWrap(EditSelection editSelection,
				FragmentModel fragmentModel) {
			LocalDom.flushLocalMutations();
			String triggerSequence = getTriggerSequence(editSelection);
			SplitResult splits = editSelection
					.splitAtTriggerRange(triggerSequence);
			DomNode splitContents = splits.contents;
			// may need to flush (to populate FNs) - note for romcom, want to
			// not force remote
			LocalDom.flushLocalMutations();
			TextNode textFragment = (TextNode) fragmentModel
					.getFragmentNode(splitContents);
			FragmentNode parent = textFragment.parent();
			DN created = createNode();
			created.properties().contentEditable().set(true);
			textFragment.nodes().insertBeforeThis(created);
			created.nodes().append(textFragment);
			LocalDom.flush();
			Selection selection = Document.get().getSelection();
			Text text = (Text) splits.contents.w3cNode();
			selection.collapse(text, text.getLength());
			return created;
		}
	}

	protected PackageProperties._DecoratorNode.InstanceProperties properties() {
		return PackageProperties.decoratorNode.instance(this);
	}

	@Directed
	public Object content = null;

	/**
	 * The serialized form of the object this decorator represents (in
	 * combination with the decorator tagname)
	 */
	@Binding(
		type = Type.PROPERTY,
		to = "_data",
		transform = RepresentableToStringTransform.class)
	public SR stringRepresentable;

	public DecoratorNode() {
	}

	@Override
	public Class<SR> stringRepresentableType() {
		return Reflections.at(this).getGenericBounds().bounds.get(1);
	}

	/*
	 * for method refs
	 */
	public SR getStringRepresentable() {
		return stringRepresentable;
	}

	@Override
	public void onBind(Bind event) {
		super.onBind(event);
		emitEvent(DecoratorEvents.DecoratorBound.class);
	}

	public abstract Descriptor<WT, SR, ?> getDescriptor();

	public void putReferenced(WT referenced) {
		properties().stringRepresentable()
				.set(getDescriptor().toStringRepresentable(referenced));
		String triggerSequence = getDescriptor().triggerSequence();
		Object renderedReferenced = ((Function) getDescriptor().itemRenderer())
				.apply(referenced);
		if (renderedReferenced instanceof String) {
			String text = triggerSequence + renderedReferenced;
			properties().content().set(text);
		} else {
			properties().content().set(renderedReferenced);
		}
	}

	public boolean isValid() {
		// FIXME - DN server shd validate entity on update. and other
		// validations (e.g. not contained in a decorator)
		return stringRepresentable != null;
	}

	void positionCursorPostReferencedSelection() {
		LocalDom.flushLocalMutations();
		if (provideIsUnbound()) {
			return;// removed
		}
		TextNode cursorTarget = null;
		FragmentNode siblingCursor = nodes().nextSibling();
		while (siblingCursor != null) {
			Optional<TextNode> subsequentText = siblingCursor.tree().stream()
					.filter(n -> n instanceof TextNode).map(n -> (TextNode) n)
					.findFirst();
			if (subsequentText.isPresent()) {
				cursorTarget = subsequentText.get();
			}
			siblingCursor = siblingCursor.nodes().nextSibling();
		}
		if (cursorTarget == null) {
			cursorTarget = nodes().append(new TextNode(""));
		}
		Node cursorGwtNode = cursorTarget.domNode().gwtNode();
		Selection selection = Document.get().getSelection();
		selection.collapse(cursorGwtNode, 0);// after zws
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

	/*
	 * insert a blank text node if the subsequent is also a non-editable
	 * decorator
	 */
	void ensureInterNonEditableTarget() {
		if (contentEditable) {
			return;
		}
		/*
		 * Possibly review this - the treeSubsequentNodeNoDescent usage is
		 * intended to handle effectively adjacent decorators such as
		 * <container><decorator/></container><decorator>
		 */
		FragmentNode checkSubsequent = nodes().treeSubsequentNodeNoDescent();
		if (HasContentEditable.isUneditable(checkSubsequent)) {
			TextNode newCursorTarget = new TextNode();
			nodes().insertAfterThis(newCursorTarget);
		}
	}

	public boolean allowPartialSelection() {
		return content != null && content instanceof AlllowsPartialSelection;
	}

	/**
	 * Marker for complex decorator content which is itself
	 * editable/sub-selectable
	 */
	public interface AlllowsPartialSelection {
	}
}
