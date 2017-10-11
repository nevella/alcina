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
import com.google.gwt.dom.client.Text;
import com.google.gwt.user.client.DOM;

import rocket.selection.client.Selection;
import rocket.selection.client.SelectionEndPoint;
import rocket.util.client.Checker;

/**
 * A specialised SelectionSupport class that is adapted to handle
 * InternetExplorer differences from the standard implementation.
 * 
 * @author Miroslav Pokorny (mP) heavily rewritten by Nick
 */
public class InternetExplorerSelectionSupport extends SelectionSupport {
	final static String PARENT_NODE = "parentNode";

	public boolean ignoreEmptySelections = true;

	@Override
	native public Selection getSelection(JavaScriptObject window)/*-{
        return window.document.selection;
	}-*/;

	@Override
	native public void clear(final Selection selection)/*-{
        selection.empty();
	}-*/;

	@Override
	native public boolean isEmpty(final Selection selection)/*-{
        return selection.type == "None";
	}-*/;

	@Override
	native public void delete(final Selection selection) /*-{
        selection.clear();
	}-*/;

	@Override
	native public void clearAnySelectedText(final Selection selection)/*-{
        selection.empty();
	}-*/;

	@Override
	public SelectionEndPoint getStart(final Selection selection) {
		Checker.notNull("parameter:selection", selection);
		try {
			return this.getStart0(selection, ignoreEmptySelections);
		} catch (Exception e) {
			// cheap squelch
			return null;
		}
	}

	native protected SelectionEndPoint getStart0(final Selection selection,
			boolean ignoreEmptySelections) /*-{
        var selectionRange = selection.createRange();
        if (ignoreEmptySelections) {
            if (selectionRange.compareEndPoints("StartToEnd", selectionRange) == 0) {
                return null;
            }
        }

        return this.@rocket.selection.client.support.InternetExplorerSelectionSupport::getStart1(Lrocket/selection/client/Selection;Z)(selection,false);
	}-*/;

	native protected SelectionEndPoint getStart1(final Selection selection,
			boolean toEndPoint)/*-{
        var endPoint = null;

        if (!selection.createRange) {
            alert("selection.createRange" + selection.createRange);
        }

        var selectionRange = selection.createRange();
        if (selectionRange.text.length == 0) {
            return null;
        }
        selectionRange.collapse(!toEndPoint);

        var range = selectionRange.duplicate();
        var parent = range.parentElement();
        //the point we're after is in a text node
        //either that node is after a child of parentElement, or not
        //find elements around the node (binary)       
        var childNodes = parent.childNodes;
        if (childNodes.length == 0) {
            var text = this.@rocket.selection.client.support.SelectionSupport::getFirstTextDepthFirstWithParent(Lcom/google/gwt/dom/client/ElementRemote;I)(parent,toEndPoint?-1:1);
            endPoint = @rocket.selection.client.SelectionEndPoint::new(Lcom/google/gwt/dom/client/Node;I)(text,toEndPoint?text.length:0);
            return endPoint;
        }
        var rangeMin = 0;
        var rangeMax = childNodes.length - 1;
        var arrayPos = 0;
        var ltElt = parent;
        var gtElt = parent;
        var foundNeighbouringElements = false;
        //binary in elements
        var lastMatchingElement = null;
        while (!foundNeighbouringElements) {
            arrayPos = Math.floor((rangeMax - rangeMin) / 2) + rangeMin;
            //walk up
            var i = 0;
            var direction = 1;
            while (true) {
                var node = childNodes[arrayPos + i];
                var nodeType = node.nodeType;

                if (1 == nodeType && node != lastMatchingElement) {
                    lastMatchingElement = node;
                    range.moveToElementText(node);
                    range.collapse(true);
                    var side = selectionRange.compareEndPoints("StartToStart",
                            range);
                    if (side == -1) {
                        rangeMax = arrayPos + i;
                        gtElt = node;
                    } else {
                        rangeMin = arrayPos + i;
                        ltElt = node;
                    }
                    break;
                }
                i += direction;
                var pos = arrayPos + i;
                if (pos == rangeMax && gtElt != parent || pos > rangeMax) {
                    //come too far
                    i--;
                    direction = -1;
                }
                if (pos == rangeMin && ltElt != parent || pos < rangeMin) {
                    foundNeighbouringElements = true;
                    break;
                }
            }

        }
        if (rangeMax - rangeMin <= 1 && childNodes[rangeMax].nodeType == 1
                && childNodes[rangeMin].nodeType == 1) {
            //between elements
            var text = toEndPoint ? this.@rocket.selection.client.support.SelectionSupport::getFirstTextDepthFirst(Lcom/google/gwt/dom/client/ElementRemote;II)(parent,rangeMin,-1)
                    : this.@rocket.selection.client.support.SelectionSupport::getFirstTextDepthFirst(Lcom/google/gwt/dom/client/ElementRemote;II)(parent,rangeMin,+1);
            endPoint = @rocket.selection.client.SelectionEndPoint::new(Lcom/google/gwt/dom/client/Node;I)(text,toEndPoint?text.length:0);
            return endPoint;
        }
        range.moveToElementText(ltElt);
        range.collapse(ltElt == parent);
        var i = ltElt == parent ? 0 : rangeMin;
        var node = null;
        //linear in text (should only be a few)
        for (; i < childNodes.length; i++) {

            node = childNodes[i];
            if (1 == node.nodeType) {
                continue;
            }
            var text = node.data;
            if (selectionRange.compareEndPoints("StartToEnd", range) == 0) {
                endPoint = @rocket.selection.client.SelectionEndPoint::new(Lcom/google/gwt/dom/client/Node;I)(node,0);
                return endPoint;
                //return new sp(node,0);
            }
            range.move("character", text.length);
            if (selectionRange.compareEndPoints("StartToEnd", range) == -1) {
                range.move("character", -text.length);
                break;
            }
            if (selectionRange.compareEndPoints("StartToEnd", range) == 0) {
                endPoint = @rocket.selection.client.SelectionEndPoint::new(Lcom/google/gwt/dom/client/Node;I)(node,text.length);
                return endPoint;
                //return new sp(node,text.length);
            }
            if (selectionRange.compareEndPoints("StartToEnd", range) == 1
                    && i == childNodes.length - 1) {
                //ie weirdness
                endPoint = @rocket.selection.client.SelectionEndPoint::new(Lcom/google/gwt/dom/client/Node;I)(node,text.length);
                return endPoint;
                //return new sp(node,text.length);
            }

        }
        //binary in chars
        var rangeMin = 0;
        var rangeMax = node.data.length;
        var offset = 0;
        //binary in elements

        while (true) {
            var lastOffset = offset;
            offset = Math.floor((rangeMax - rangeMin) / 2) + rangeMin;
            range.move("character", offset - lastOffset);
            var cmp = selectionRange.compareEndPoints("StartToEnd", range);
            if (cmp == 0) {
                endPoint = @rocket.selection.client.SelectionEndPoint::new(Lcom/google/gwt/dom/client/Node;I)(node,offset);
                return endPoint;
                //return new sp(node,offset);
            }
            if (cmp == 1) {
                if (offset == 0) {
                    endPoint = @rocket.selection.client.SelectionEndPoint::new(Lcom/google/gwt/dom/client/Node;I)(node,offset);
                    return endPoint;
                    //return new sp(node,offset);
                }
                rangeMin = offset + 1;
            } else {
                rangeMax = offset;
            }

        }
	}-*/;

	@Override
	public void setStart(final Selection selection,
			final SelectionEndPoint start) {
		Checker.notNull("parameter:selection", selection);
		Checker.notNull("parameter:start", start);
		final Text textNode = start.getTextNode();
		final int offset = start.getOffset();
		this.setStart0(selection, textNode, offset);
	}

	native private void setStart0(final Selection selection,
			final Text textNode, final int offset)/*-{
        var rangeOffset = offset;
        var moveToElement = null;

        // try an element before $textNode counting the number of characters one has moved backwards...
        var node = textNode.previousSibling;

        while (node) {
            // if a textNode is try its previous sibling...
            if (node.nodeType == 3) {
                rangeOffset = rangeOffset + node.data.length;
                continue;
            }

            // found an element stop searching...
            if (node.nodeType == 1) {
                moveToElement = node;
                rangeOffset = rangeOffset + node.innerText.toString().length;
                break;
            }

            // ignore other types...
            node = node.previousSibling;
        }

        // if moveToElement is null use textNode's parent.
        if (!moveToElement) {
            moveToElement = textNode.parentNode;
        }

        // update the start of selection range...
        var range = selection.createRange();
        range.moveToElementText(moveToElement);
        range.moveStart("character", rangeOffset);
        range.select();
	}-*/;

	@Override
	public void setEnd(final Selection selection, final SelectionEndPoint end) {
		Checker.notNull("parameter:selection", selection);
		Checker.notNull("parameter:end", end);
		final Text textNode = end.getTextNode();
		final int offset = end.getOffset();
		this.setEnd0(selection, textNode, offset);
	}

	native private void setEnd0(final Selection selection, final Text textNode,
			final int offset)/*-{
        var rangeOffset = offset;
        var moveToElement = null;

        // try an element before $textNode counting the number of characters one has moved backwards...
        var node = textNode.previousSibling;

        while (node) {
            // if textNode is try its previous sibling...
            if (node.nodeType == 3) {
                rangeOffset = rangeOffset + node.data.length;
                continue;
            }

            // found an element stop searching...
            if (node.nodeType == 1) {
                moveToElement = node;
                rangeOffset = rangeOffset + node.innerText.toString().length;
                break;
            }

            // ignore other types...
            node = node.previousSibling;
        }

        // if moveToElement is null use textNode's parent.
        if (!moveToElement) {
            moveToElement = textNode.parentNode;
        }

        // update the end of selection range...
        var range = selection.createRange();
        range.moveToElementText(moveToElement);
        range.moveStart("character", rangeOffset);
        range.collapse();

        var selectionRange = selection.createRange();
        selectionRange.setEndPoint("EndToStart", range);
        selectionRange.select();
	}-*/;

	@Override
	public SelectionEndPoint getEnd(final Selection selection) {
		try {
			return this.getEnd0(selection, ignoreEmptySelections);
		} catch (Exception e) {
			// cheap squelch
			return null;
		}
	}

	protected native SelectionEndPoint getEnd0(final Selection selection,
			boolean ignoreEmptySelections) /*-{
        var selectionRange = selection.createRange();
        if (ignoreEmptySelections) {
            if (selectionRange.compareEndPoints("StartToEnd", selectionRange) == 0) {
                return null;
            }
        }

        return this.@rocket.selection.client.support.InternetExplorerSelectionSupport::getStart1(Lrocket/selection/client/Selection;Z)(selection,true);
	}-*/;

	protected native SelectionEndPoint getEnd1(final Selection selection,
			final Element element)/*-{
        var endPoint = null;

        var selectionRange = selection.createRange();

        var range = selectionRange.duplicate();
        range.moveToElementText(element);
        range.collapse(true);

        // loop thru all the childNodes belonging to element.
        var childNodes = element.childNodes;
        for (var i = 0; i < childNodes.length; i++) {
            var node = childNodes[i];
            var nodeType = node.nodeType;

            // found an element check its child nodes...
            if (1 == nodeType) {
                endPoint = this.@rocket.selection.client.support.InternetExplorerSelectionSupport::getEnd1(Lrocket/selection/client/Selection;Lcom/google/gwt/dom/client/Element;)(selection,node);

                if (null == endPoint) {
                    range.move("character", node.innerText.toString().length);
                    continue;
                }
                // endPoint found stop searching....
                break;
            }

            // found a textNode...
            if (3 == nodeType) {
                var text = node.data;
                for (var j = 0; j < text.length; j++) {
                    // found selection end stop searching!
                    if (selectionRange.compareEndPoints("EndToStart", range) == 0) {
                        endPoint = @rocket.selection.client.SelectionEndPoint::new(Lcom/google/gwt/dom/client/Node;I)(node,j);
                        break;
                    }
                    range.move("character", 1);
                }
                // did the above for loop find the end ? if so stop escape!
                if (null != endPoint) {
                    break;
                }
            }
        }

        return endPoint;
	}-*/;

	@Override
	public Element extract(final Selection selection) {
		final Element element = DOM.createSpan();
		if (false == this.isEmpty(selection)) {
			this.extract0(selection, element);
			this.delete(selection);
		}
		return element;
	}

	/**
	 * Extracts the html that represents the selected area and sets the
	 * innerHTML attribute of the given element.
	 * 
	 * @param selection
	 * @param element
	 */
	native private void extract0(final Selection selection,
			final Element element)/*-{
        // extract the selected area and assign to $html.
        var html = selection.createRange().htmlText;

        // set the innerHTML of $element to $html
        element.innerHTML = html;
	}-*/;

	@Override
	protected void surround0(final Selection selection, final Element element) {
		// get child index of selection start textNode from its parent.
		final SelectionEndPoint selectionStart = this.getStart(selection);
		final Text textNode = selectionStart.getTextNode();
		final Element parentOfTextNode = textNode.getParentElement().cast();
		int insertIndex = 0;
		if (selectionStart.getOffset() > 0) {
			insertIndex = this.getChildIndexOfTextNode(textNode) + 1;
		}
		// extract selection to become a child of element.
		this.extract0(selection, element);
		// delete the
		this.delete(selection);
		// insert $element just after the original start of selection textNode
		this.insertChild(parentOfTextNode, element, insertIndex);
	}

	native private int getChildIndexOfTextNode(final Text textNode)/*-{
        var i = 0;

        var children = textNode.parentNode.childNodes;
        var count = children.length;
        for (var j = 0; j < count; j++) {
            if (textNode === children[j]) {
                i = j;
                break;
            }
        }

        return i;
	}-*/;

	/**
	 * This method is necessary because GWT's DOM.insertChild( Element parent,
	 * Element element, int index ) fails because it skip text nodes when
	 * inserting.
	 * 
	 * @param textNode
	 * @return
	 */
	native private void insertChild(final Element parent, final Element element,
			final int index)/*-{
        var childNodes = parent.childNodes;
        if (index >= childNodes.length) {
            parent.appendChild(element);
        } else {
            parent.insertBefore(element, childNodes[index]);
        }
	}-*/;

	

	
}