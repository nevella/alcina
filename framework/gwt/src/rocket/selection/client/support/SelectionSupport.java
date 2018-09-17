/*
 * Copyright Miroslav Pokorny
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rocket.selection.client.support;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ElementRemote;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeRemote;
import com.google.gwt.dom.client.Text;
import com.google.gwt.dom.client.TextRemote;

import rocket.selection.client.Selection;
import rocket.selection.client.SelectionEndPoint;
import rocket.util.client.Checker;
import rocket.util.client.JavaScript;

/**
 * This class provides the standard implementation of
 * 
 * @author Miroslav Pokorny (mP)
 * 
 *
 */
// FIXME - localdom2 - fix the element vs text logic hidden formerly by jso
// cross casting
abstract public class SelectionSupport {
	native static TextRemote remote(Text text)/*-{
    var implAccess = text.@com.google.gwt.dom.client.Text::implAccess()();
    var remote = implAccess.@com.google.gwt.dom.client.Text.TextImplAccess::typedRemote()();
    return remote;
	}-*/;

	native public void clear(final Selection selection)/*-{
    selection.removeAllRanges();
	}-*/;

	native public void clearAnySelectedText(final Selection selection)/*-{
    selection.removeAllRanges();
	}-*/;

	/**
	 * Deletes or removes the selected dom objects from the object.
	 * 
	 * @param selection
	 */
	public void delete(final Selection selection) {
		if (this.isEmpty(selection)) {
			throw new IllegalStateException(
					"No selection exists unable to perform delete");
		}
		this.delete0(selection);
		this.clear(selection);
	}

	/**
	 * Extracts any selection and makes it a child of an element not attached to
	 * the DOM.
	 * 
	 * @param element
	 */
	public Element extract(final Selection selection) {
		throw new UnsupportedOperationException();
		// return this.extract0(selection);
	}

	public SelectionEndPoint getEnd(final Selection selection) {
		final SelectionEndPoint end = new SelectionEndPoint();
		NodeRemote nodeRemote = JavaScript
				.getObject(selection, Constants.FOCUS_NODE).cast();
		Node endNode = LocalDom.nodeFor(nodeRemote);
		if (endNode.getNodeType() == Node.ELEMENT_NODE) {
			// this occurs when selecting, say, the next LI in a list as the
			// endpoint. Hence different to IE impl
			ElementRemote elementRemote = (ElementRemote) ((Element) end
					.getNode()).implAccess().typedRemote();
			TextRemote remote = getFirstTextDepthFirstWithParent(elementRemote,
					1);
			end.setTextNode(LocalDom.nodeFor(remote));
			end.setOffset(0);
		} else {
			end.setTextNode((Text) end.getNode().cast());
			end.setOffset(
					JavaScript.getInteger(selection, Constants.FOCUS_OFFSET));
		}
		return end;
	}

	abstract public Selection getSelection(JavaScriptObject window);

	public SelectionEndPoint getStart(final Selection selection) {
		final SelectionEndPoint start = new SelectionEndPoint();
		NodeRemote nodeRemote = JavaScript
				.getObject(selection, Constants.ANCHOR_NODE).cast();
		Text startNode = LocalDom.nodeFor(nodeRemote);
		if (start.getTextNode().getNodeType() == Node.ELEMENT_NODE) {
			ElementRemote elementRemote = (ElementRemote) ((Element) start
					.getNode()).implAccess().typedRemote();
			TextRemote remote = getFirstTextDepthFirstWithParent(elementRemote,
					1);
			start.setTextNode(LocalDom.nodeFor(remote));
			start.setOffset(0);
		} else {
			start.setTextNode((Text) start.getNode().cast());
			start.setOffset(
					JavaScript.getInteger(selection, Constants.ANCHOR_OFFSET));
		}
		return start;
	}

	public boolean isEmpty(final Selection selection) {
		return JavaScript.getBoolean(selection, Constants.IS_COLLAPSED);
	}

	public void setEnd(final Selection selection, final SelectionEndPoint end) {
		Checker.notNull("parameter:selection", selection);
		Checker.notNull("parameter:end", end);
		setEnd0(selection, end.getTextNode(), end.getOffset());
	}

	public void setStart(final Selection selection,
			final SelectionEndPoint start) {
		Checker.notNull("parameter:selection", selection);
		Checker.notNull("parameter:start", start);
		setStart0(selection, start.getTextNode(), start.getOffset());
	}

	final public void surround(final Selection selection,
			final Element element) {
		if (this.isEmpty(selection)) {
			throw new IllegalStateException(
					"No selection exists unable to perform surround");
		}
		this.surround0(selection, element);
	}

	native private void delete0(final Selection selection)/*-{
    var range = selection.getRangeAt(0);
    range.deleteContents();
	}-*/;

	native private Element extract0(final Selection selection)/*-{
    var element = selection.anchorNode.ownerDocument.createElement("span");

    var range = selection.getRangeAt(0);
    if (range) {
      range.surroundContents(element);
    }
    return element;
	}-*/;

	native private void setEnd0(final Selection selection, final Text text,
			final int offset)/*-{
    var textNode = @rocket.selection.client.support.SelectionSupport::remote(Lcom/google/gwt/dom/client/Text;)(text);
    // if an existing selection exists use that otherwise set the start to the new end.
    var startNode = selection.anchorNode;
    var startOffset = selection.anchorOffset;
    if (!startNode) {
      startNode = textNode;
      startOffset = offset;
    }

    // create a new range that will join the old end and the new start...
    var range = textNode.ownerDocument.createRange();
    range.setStart(startNode, startOffset);

    range.setEnd(textNode, offset);

    // delete all ranges then recreate...
    selection.removeAllRanges();
    selection.addRange(range);
	}-*/;

	native private void setStart0(final Selection selection, final Text text,
			final int offset)/*-{
    var textNode = @rocket.selection.client.support.SelectionSupport::remote(Lcom/google/gwt/dom/client/Text;)(text);
    // if an existing end exists use that otherwise set the end to the new start
    var endNode = selection.focusNode;
    var endOffset = selection.focusOffset;
    if (!endNode) {
      endNode = textNode;
      endOffset = offset;
    }

    var range = textNode.ownerDocument.createRange();
    range.setStart(textNode, offset);

    range.setEnd(endNode, endOffset);

    // delete all ranges then recreate...
    selection.removeAllRanges();
    selection.addRange(range);
	}-*/;

	native protected Text getFirstTextDepthFirst(final ElementRemote parent,
			int childIndex, int direction)/*-{
    var childNodes = parent.childNodes;
    var i = childIndex;
    for (; i >= 0 && i < childNodes.length; i += direction) {
      var node = childNodes[i];
      var nodeType = node.nodeType;
      if (3 == nodeType) {
        return node;
      }
      if (1 == nodeType && node.childNodes.length != 0) {
        var result = this.@rocket.selection.client.support.SelectionSupport::getFirstTextDepthFirst(Lcom/google/gwt/dom/client/ElementRemote;II)(node,direction==-1?node.childNodes.length-1:0,direction);
        if (result != null) {
          return result;
        }
      }
    }
    return null;

	}-*/;

	native protected TextRemote getFirstTextDepthFirstWithParent(
			final ElementRemote element, int direction)/*-{
    var childNodes = element.parentNode.childNodes;
    var i = direction == 1 ? 0 : childNodes.length - 1;
    var found = false;
    for (; i >= 0 && i < childNodes.length; i += direction) {
      if (element == childNodes[i]) {
        found = true;
      }
      if (found) {
        var node = childNodes[i];
        var nodeType = node.nodeType;
        if (3 == nodeType) {
          return node;
        }
        var result = this.@rocket.selection.client.support.SelectionSupport::getFirstTextDepthFirst(Lcom/google/gwt/dom/client/ElementRemote;II)(node,direction==-1?node.childNodes.length-1:0,direction);
        if (result != null) {
          return result;
        }
      }
    }
    return this.@rocket.selection.client.support.SelectionSupport::getFirstTextDepthFirstWithParent(Lcom/google/gwt/dom/client/ElementRemote;I)(element.parentNode,direction);
	}-*/;

	native protected void surround0(final Selection selection,
			final Element element)/*-{
    var range = selection.getRangeAt(0);
    range.surroundContents(element);
	}-*/;
}
