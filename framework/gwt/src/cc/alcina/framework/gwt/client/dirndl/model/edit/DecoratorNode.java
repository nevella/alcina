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
import cc.alcina.framework.gwt.client.dirndl.model.dom.RelativeInputModel;

class DecoratorNode {
	SplitResult splits;

	DomNode node;

	RelativeInputModel relativeInput;

	ContentDecorator decorator;

	DecoratorNode(ContentDecorator decorator, DomNode node) {
		this.decorator = decorator;
		this.node = node;
	}

	DecoratorNode(ContentDecorator decorator,
			RelativeInputModel relativeInput) {
		this.decorator = decorator;
		this.relativeInput = relativeInput;
	}

	boolean isValid() {
		return node.has("_entity");
	}

	void setEntity(Entity entity, String render) {
		DomNode textNode = node.children.firstNode();
		textNode.setText(decorator.triggerSequence
				+ ((Function) decorator.itemRenderer).apply(entity));
		node.setAttr("_entity",
				entity.toLocator().toRecoverableNumericString());
		LocalDom.flush();
		//
		// TODO - position cursor at the end of the mention, then allow the
		// 'cursor validator' to move it to a correct location
		// try positioning cursor immediately after the decorator
		DomNode cursorTarget = textNode.tree().nextTextNode(true).orElse(null);
		if (cursorTarget != null) {
			// ensure next text node is within the root CR
			if (!cursorTarget.ancestors().has(
					decorator.logicalParent.provideElement().asDomNode())) {
				cursorTarget = null;
				// and that not in a decorator
			} else if (decorator.hasDecorator(cursorTarget)) {
				cursorTarget = null;
			}
		}
		if (cursorTarget == null) {
			/*
			 * Add a text node containing a zero-width space - this gets us out
			 * of the decorator tag.
			 */
			cursorTarget = node.builder().text("\u200B").insertAfterThis();
		}
		LocalDom.flush();
		Node cursorNode = cursorTarget.gwtNode();
		SelectionJso selection = Document.get().jsoRemote().getSelection();
		cursorNode.implAccess().ensureRemote();
		NodeJso remote = cursorNode.implAccess().jsoRemote();
		NodeJso rr1 = remote.getParentNodeJso();
		selection.collapse(remote, 1);
	}

	void setModel(Object model, String render) {
		setEntity((Entity) model, render);
	}

	void splitAndWrap() {
		splits = relativeInput.splitAt(-1, 0);
		this.node = splits.contents.builder().tag(decorator.tag)
				.attr("contentEditable", "false").wrap();
		LocalDom.flush();
		SelectionJso selection = Document.get().jsoRemote().getSelection();
		Text text = (Text) splits.contents.w3cNode();
		selection.collapse(text.implAccess().ensureRemote(), text.getLength());
	}

	void stripIfInvalid() {
		if (!isValid()) {
			DomNode firstNode = node.children.firstNode();
			node.strip();
			if (firstNode.isText()) {
				// undo split
				firstNode.text().mergeWithAdjacentTexts();
			}
		}
	}

	void validateSelection() {
		if (relativeInput.containsPartialAncestorFocusTag(decorator.tag)) {
			relativeInput.selectWholeAncestorFocusTag(decorator.tag);
		}
	}
}