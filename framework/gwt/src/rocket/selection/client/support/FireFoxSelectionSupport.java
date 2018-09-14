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
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeRemote;
import com.google.gwt.dom.client.Text;
import com.google.gwt.dom.client.TextRemote;

import cc.alcina.framework.common.client.util.TopicPublisher.TopicSupport;
import rocket.selection.client.Selection;
import rocket.selection.client.SelectionEndPoint;
import rocket.util.client.JavaScript;

/**
 * A specialised SelectionSupport class that is adapted to handle FireFox
 * differences from the standard implementation.
 * 
 * @author Miroslav Pokorny (mP)
 */
public class FireFoxSelectionSupport extends SelectionSupport {
	private static final String TOPIC_SELECTION_DEBUG = FireFoxSelectionSupport.class
			.getName() + ".TOPIC_SELECTION_DEBUG";

	public static TopicSupport<Integer> selectionDebugTopic() {
		return new TopicSupport<>(TOPIC_SELECTION_DEBUG);
	}

	@Override
	public SelectionEndPoint getEnd(final Selection selection) {
		int debugInfo = 0;
		try {
			final SelectionEndPoint end = new SelectionEndPoint();
			NodeRemote nodeRemote = JavaScript
					.getObject(selection, Constants.FOCUS_NODE).cast();
			end.setNode(LocalDom.nodeFor(nodeRemote));
			end.setOffset(
					JavaScript.getInteger(selection, Constants.FOCUS_OFFSET));
			debugInfo = 1;
			if (end.getNode() == null) {
				return null;
			}
			if (end.getNode().getNodeType() == Node.ELEMENT_NODE) {
				debugInfo = 2;
				Element parent = (Element) end.getNode().cast();
				if (parent.getChildNodes().getLength() <= end.getOffset()) {
					return null;
				}
				debugInfo = 3;
				Node node = parent.getChildNodes().getItem(end.getOffset());
				debugInfo = 4;
				if (node.getNodeType() == Node.TEXT_NODE) {
					debugInfo = 5;
					end.setTextNode((Text) node);
					end.setOffset(0);
				} else {
					debugInfo = 6;
					TextRemote textRemote = getFirstTextDepthFirstWithParent(
							((Element) node).implAccess().typedRemote(), 1);
					Text text = LocalDom.nodeFor(textRemote);
					end.setTextNode(text);
					end.setOffset(0);
				}
			} else {
				debugInfo = 7;
				end.setTextNode((Text) end.getNode().cast());
			}
			return end;
		} catch (Exception e) {
			selectionDebugTopic().publish(debugInfo);
			return null;
		}
	}

	@Override
	native public Selection getSelection(final JavaScriptObject window)/*-{
    return window.getSelection();
	}-*/;

	@Override
	public SelectionEndPoint getStart(final Selection selection) {
		int debugInfo = -1;
		try {
			final SelectionEndPoint start = new SelectionEndPoint();
			NodeRemote nodeRemote = JavaScript
					.getObject(selection, Constants.ANCHOR_NODE).cast();
			start.setNode(LocalDom.nodeFor(nodeRemote));
			start.setOffset(
					JavaScript.getInteger(selection, Constants.ANCHOR_OFFSET));
			if (start.getNode().getNodeType() == Node.ELEMENT_NODE) {
				Element parent = (Element) start.getNode().cast();
				Node node = parent.getChildNodes().getItem(start.getOffset());
				if (node.getNodeType() == Node.TEXT_NODE) {
					start.setTextNode((Text) node);
					start.setOffset(0);
				} else {
					TextRemote textRemote = getFirstTextDepthFirstWithParent(
							((Element) node).implAccess().typedRemote(), 1);
					Text text = LocalDom.nodeFor(textRemote);
					start.setTextNode(text);
					start.setOffset(0);
				}
			} else {
				start.setTextNode((Text) start.getNode().cast());
			}
			return start;
		} catch (Exception e) {
			selectionDebugTopic().publish(debugInfo);
			return null;
		}
	}
}
