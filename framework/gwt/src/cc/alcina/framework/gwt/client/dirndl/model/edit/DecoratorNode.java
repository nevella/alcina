package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.function.Function;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeJso;
import com.google.gwt.dom.client.SelectionJso;
import com.google.gwt.dom.client.Text;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNode.DomNodeText.SplitResult;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.FragmentNode;
import cc.alcina.framework.gwt.client.dirndl.model.dom.RelativeInputModel;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentModel;

/**
 * A node which is decorated by user activity (such as a # tag populated from a
 * dropdown)
 *
 * @param <E>
 *            the type this decorator selects
 */
public abstract class DecoratorNode<E extends Entity> extends FragmentNode {
	@Binding(
		type = Type.PROPERTY,
		to = "contentEditable",
		transform = Binding.DisplayFalseTrueBidi.class)
	public boolean contentEditable = true;

	@Binding(type = Type.PROPERTY, transform = ContextLocatorTransform.class)
	public EntityLocator entity;

	public abstract Descriptor<E> getDescriptor();

	public void setContentEditable(boolean contentEditable) {
		set("contentEditable", this.contentEditable, contentEditable,
				() -> this.contentEditable = contentEditable);
	}

	public void setEntity(EntityLocator entity) {
		set("entity", this.entity, entity, () -> this.entity = entity);
	}

	public String tag() {
		// TODO Auto-generated method stub
		return null;
	}

	public void toNonEditable() {
		setContentEditable(false);
		/*
		 * FIXME - dn - don't insert if unnecessary. 'necessary' test is: ensure
		 * editable text before. tree iterate before, find first text. if it
		 * doesn't exist, is in a CE or a different block, insert a
		 * cursor-target span
		 */
		nodes().insertBeforeThis(new ZeroWidthCursorTarget());
		nodes().insertAfterThis(new ZeroWidthCursorTarget());
	}

	/**
	 * This is only called when a chooser is *not* showing, so isValid is
	 * logically correct
	 */
	public void validate() {
		if (!isValid()) {
			DomNode firstNode = domNode().children.firstNode();
			domNode().strip();
			if (firstNode.isText()) {
				// undo split
				firstNode.text().mergeWithAdjacentTexts();
			}
		}
	}

	protected Class<E> entityClass() {
		return Reflections.at(this).getGenericBounds().bounds.get(0);
	}

	boolean isValid() {
		// FIXME - DN server shd validate entity on update. and other
		// validations (e.g. not contained in a decorator)
		return entity != null;
	}

	void putEntity(Entity entity) {
		setEntity(entity.toLocator());
		FragmentNode.TextNode textNode = (TextNode) children().findFirst()
				.get();
		String text = getDescriptor().triggerSequence()
				+ ((Function) getDescriptor().itemRenderer()).apply(entity);
		textNode.setValue(text);
		LocalDom.flush();
		// TODO - position cursor at the end of the mention, then allow the
		// 'cursor validator' to move it to a correct location
		// try positioning cursor immediately after the decorator
		// guaranteed non-null (due to zws insertion)
		FragmentNode.TextNode cursorTarget = fragmentTree().nextTextNode(true)
				.get();
		Node cursorNode = cursorTarget.domNode().gwtNode();
		SelectionJso selection = Document.get().jsoRemote().getSelection();
		cursorNode.implAccess().ensureRemote();
		NodeJso remote = cursorNode.implAccess().jsoRemote();
		NodeJso rr1 = remote.getParentNodeJso();
		selection.collapse(remote, 1);// after zws
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

	void validateSelection() {
		// if (relativeInput.containsPartialAncestorFocusTag(decorator.tag)) {
		// relativeInput.selectWholeAncestorFocusTag(decorator.tag);
		// }
		// should be a static method on decorator node
		throw new UnsupportedOperationException();
	}

	public static class ContextLocatorTransform
			implements Binding.Bidi<EntityLocator> {
		@Override
		public Function<EntityLocator, String> leftToRight() {
			return new ContextLocatorTransformLeft();
		}

		@Override
		public Function<String, EntityLocator> rightToLeft() {
			return new ContextLocatorTransformRight();
		}
	}

	public static class ContextLocatorTransformLeft
			extends Binding.AbstractContextSensitiveTransform<EntityLocator> {
		@Override
		public String apply(EntityLocator t) {
			return t == null ? null : t.toRecoverableNumericString();
		}
	}

	public static class ContextLocatorTransformRight extends
			Binding.AbstractContextSensitiveReverseTransform<EntityLocator> {
		@Override
		public EntityLocator apply(String t) {
			DecoratorNode contextNode = node.getModel();
			return t == null ? null
					: EntityLocator.parse(contextNode.entityClass(), t);
		}
	}

	/**
	 * The characteristics of the decorator, such as the key sequence which
	 * triggers its creation
	 *
	 */
	public static abstract class Descriptor<E extends Entity> {
		public abstract DecoratorNode createNode();

		public abstract Function<E, String> itemRenderer();

		public abstract String triggerSequence();

		DecoratorNode splitAndWrap(RelativeInputModel relativeInput,
				FragmentModel fragmentModel) {
			SplitResult splits = relativeInput.splitAt(-1, 0);
			DomNode splitContents = splits.contents;
			// may need to flush
			LocalDom.flush();
			FragmentNode textFragment = fragmentModel
					.getFragmentNode(splitContents);
			DecoratorNode created = createNode();
			textFragment.nodes().insertBeforeThis(created);
			created.nodes().append(textFragment);
			LocalDom.flush();
			SelectionJso selection = Document.get().jsoRemote().getSelection();
			Text text = (Text) splits.contents.w3cNode();
			selection.collapse(text.implAccess().ensureRemote(),
					text.getLength());
			return created;
		}
	}

	@Directed(tag = "span", className = "cursor-target")
	public static class ZeroWidthCursorTarget extends FragmentNode {
		@Binding(type = Type.INNER_TEXT)
		public String text = "\u200B";
	}
}