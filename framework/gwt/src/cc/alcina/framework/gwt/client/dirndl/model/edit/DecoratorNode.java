package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.function.Function;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Selection;
import com.google.gwt.dom.client.Text;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNode.DomNodeText.SplitResult;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Commit;
import cc.alcina.framework.gwt.client.dirndl.layout.FragmentNode;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.dom.EditSelection;
import cc.alcina.framework.gwt.client.dirndl.model.edit.StringRepresentable.RepresentableToStringTransform;
import cc.alcina.framework.gwt.client.dirndl.model.edit.StringRepresentable.RepresentableToStringTransform.HasStringRepresentableType;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentIsolate;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentModel;

/**
 * The base class for a visual model of a decorated measure, such as a hashtag
 * or mention in a document, or a selected choice in a dropdown suggestor
 */
@Directed(className = "decorator-node")
@TypedProperties
public abstract class DecoratorNode<WT, SR> extends FragmentNode implements
		HasStringRepresentableType<SR>, FragmentIsolate, HasContentEditable {
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
			String triggerSequence = getTriggerSequence(editSelection);
			SplitResult splits = editSelection
					.splitAtTriggerRange(triggerSequence);
			DomNode splitContents = splits.contents;
			// may need to flush (to populate FNs) - note for romcom, want to
			// not force remote
			LocalDom.flush();
			FragmentNode textFragment = fragmentModel
					.getFragmentNode(splitContents);
			FragmentNode parent = textFragment.parent();
			/*
			 * key - but it'd be nice to handle more elegantly (?named
			 * behaviour?)
			 */
			if (parent instanceof ZeroWidthCursorTarget) {
				parent.nodes().strip();
			}
			DN created = createNode();
			DecoratorNode.properties.contentEditable.set(created, true);
			textFragment.nodes().insertBeforeThis(created);
			created.nodes().append(textFragment);
			LocalDom.flush();
			Selection selection = Document.get().getSelection();
			Text text = (Text) splits.contents.w3cNode();
			selection.collapse(text, text.getLength());
			return created;
		}
	}

	/*
	 * WIP - the internal model of the decorator. Normally this is just a simple
	 * text node
	 */
	class InternalModel extends FragmentModel {
		InternalModel(Model rootModel) {
			super(rootModel);
		}
	}

	public static PackageProperties._DecoratorNode properties = PackageProperties.decoratorNode;

	InternalModel internalModel;

	@Binding(
		type = Type.PROPERTY,
		to = "contentEditable",
		transform = Binding.DisplayFalseTrueBidi.class)
	public boolean contentEditable = false;

	@Binding(type = Type.INNER_TEXT)
	public String content = "";

	@Binding(
		type = Type.PROPERTY,
		to = "uid",
		transform = RepresentableToStringTransform.class)
	public SR stringRepresentable;

	public DecoratorNode() {
		bindings().from(this).on(properties.contentEditable)
				.accept(this::notifyContentEditableDelta);
	}

	@Override
	public void onBind(Bind event) {
		super.onBind(event);
		new DecoratorEvent()
				.withType(event.isBound() ? DecoratorEvent.Type.node_bound
						: DecoratorEvent.Type.node_unbound)
				.withSubtype(NestedName.get(this)).withMessage(content)
				.publish();
	}

	@Override
	public Class<SR> stringRepresentableType() {
		return Reflections.at(this).getGenericBounds().bounds.get(1);
	}

	@Override
	public FragmentModel getFragmentModel() {
		if (internalModel == null) {
			internalModel = new InternalModel(this);
		}
		return internalModel;
	}

	/*
	 * for method refs
	 */
	public SR getStringRepresentable() {
		return stringRepresentable;
	}

	public abstract Descriptor<WT, SR, ?> getDescriptor();

	public void putReferenced(WT wrappedType) {
		properties.stringRepresentable.set(this,
				getDescriptor().toStringRepresentable(wrappedType));
		String text = getDescriptor().triggerSequence()
				+ ((Function) getDescriptor().itemRenderer())
						.apply(wrappedType);
		properties.content.set(this, text);
	}

	public void toNonEditable() {
		properties.contentEditable.set(this, false);
	}

	@Override
	public boolean provideIsContentEditable() {
		return contentEditable;
	}

	void notifyContentEditableDelta(boolean contentEditable) {
		new DecoratorEvent().withType(DecoratorEvent.Type.editable_attr_changed)
				.withSubtype(NestedName.get(this))
				.withMessage(
						Ax.format("[-->%s] :: %s", contentEditable, content))
				.publish();
	}

	boolean isValid() {
		// FIXME - DN server shd validate entity on update. and other
		// validations (e.g. not contained in a decorator)
		return stringRepresentable != null;
	}

	void positionCursorPostReferencedSelection() {
		LocalDom.flushLocalMutations();
		if (provideIsUnbound()) {
			return;// removed
		}
		FragmentNode nextSibling = nodes().nextSibling();
		// FIXME - fragment.isolate - position cursor at the end of the mention,
		// then allow the
		// 'cursor validator' to move it to a correct location
		//
		// current:
		// try positioning cursor immediately after the decorator
		// guaranteed non-null (due to zws insertion)
		FragmentNode.TextNode cursorTarget = nextSibling instanceof FragmentNode.TextNode
				? (FragmentNode.TextNode) nextSibling
				: nextSibling.fragmentTree().nextTextNode(true).orElse(null);
		/*
		 * well - what's the dispatch model for ZWS insertion? Maybe it is
		 * null...maybe we've lost focus...
		 */
		if (cursorTarget == null) {
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

	ZeroWidthCursorTarget ensureInterNonEditableTarget() {
		if (contentEditable) {
			return null;
		}
		FragmentNode treeSubsequentNode = nodes().treeSubsequentNode();
		if (treeSubsequentNode instanceof ZeroWidthCursorTarget) {
			return (ZeroWidthCursorTarget) treeSubsequentNode;
		} else if (HasContentEditable.isUneditable(treeSubsequentNode)) {
			ZeroWidthCursorTarget newCursorTarget = new ZeroWidthCursorTarget();
			nodes().insertAfterThis(newCursorTarget);
			return newCursorTarget;
		} else {
			return null;
		}
	}
}
