package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.function.Function;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeJso;
import com.google.gwt.dom.client.SelectionJso;

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

public abstract class DecoratorNode<E extends Entity> extends FragmentNode {
	@Binding(type = Type.PROPERTY)
	public boolean contentEditable;

	@Binding(type = Type.PROPERTY, transform = ContextLocatorTransform.class)
	public EntityLocator entity;

	public void setContentEditable(boolean contentEditable) {
		set("contentEditable", this.contentEditable, contentEditable,
				() -> this.contentEditable = contentEditable);
	}

	public void setEntity(EntityLocator entity) {
		set("entity", this.entity, entity, () -> this.entity = entity);
	}

	public void toNonEditable() {
		setContentEditable(false);
		nodes().insertBeforeThis(new ZeroWidthCursorTarget());
		nodes().insertAfterThis(new ZeroWidthCursorTarget());
		/*
		 * FIXME - dn - don't insert if unnecessary. 'necessary' test is: ensure
		 * editable text before. tree iterate before, find first text. if it
		 * doesn't exist, is in a CE or a different block, insert a
		 * cursor-target span
		 */
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

	protected abstract Function<E, String> itemRenderer();

	protected abstract String triggerSequence();

	boolean isValid() {
		// TODO - model the entity as an entitylocator field (of this class)
		return domNode().has("_entity");
	}

	void putEntity(Entity entity) {
		setEntity(entity.toLocator());
		FragmentNode.Text textNode = (Text) children().findFirst().get();
		String text = triggerSequence()
				+ ((Function) itemRenderer()).apply(entity);
		textNode.setValue(text);
		LocalDom.flush();
		// TODO - position cursor at the end of the mention, then allow the
		// 'cursor validator' to move it to a correct location
		// try positioning cursor immediately after the decorator
		FragmentNode.Text cursorTarget = textNode.tree().nextTextNode(true)
				.orElse(null);
		LocalDom.flush();
		Node cursorNode = cursorTarget.domNode().gwtNode();
		SelectionJso selection = Document.get().jsoRemote().getSelection();
		cursorNode.implAccess().ensureRemote();
		NodeJso remote = cursorNode.implAccess().jsoRemote();
		NodeJso rr1 = remote.getParentNodeJso();
		selection.collapse(remote, 1);// after zws
	}

	void splitAndWrap(RelativeInputModel relativeInput) {
		SplitResult splits = relativeInput.splitAt(-1, 0);
		/*
		 *
		 */
		// this.node = splits.contents.builder().tag(decorator.tag).wrap();
		// LocalDom.flush();
		// SelectionJso selection = Document.get().jsoRemote().getSelection();
		// Text text = (Text) splits.contents.w3cNode();
		// selection.collapse(text.implAccess().ensureRemote(),
		// text.getLength());
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
		// should be a static method on decorator node
		throw new UnsupportedOperationException();
	}

	public static class ContextLocatorTransform
			extends Binding.AbstractContextSensitiveTransform<EntityLocator> {
		@Override
		public String apply(EntityLocator t) {
			return t == null ? null : t.toRecoverableNumericString();
		}
	}

	@Directed(tag = "span", cssClass = "cursor-target")
	public static class ZeroWidthCursorTarget extends FragmentNode {
		@Binding(type = Type.INNER_TEXT)
		public String text = "\u200B";
	}
}