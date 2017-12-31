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
package rocket.selection.client;

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;

import rocket.util.client.Checker;

/**
 * An end point uses a combination of a textNode and offset to mark the
 * start/end of a selection
 * 
 * @author Miroslav Pokorny (mP)
 */
public class SelectionEndPoint {
	private Text textNode;

	private Node node;

	/**
	 * The number of characters starting from the beginning of the textNode
	 * where the selection begins/ends.
	 */
	public int offset;

	public SelectionEndPoint() {
		super();
	}

	public SelectionEndPoint(Node node, final int offset) {
		super();
		this.setNode(node);
		this.setOffset(offset);
	}

	public Node getNode() {
		return this.node;
	}

	public int getOffset() {
		return offset;
	}

	public Text getTextNode() {
		Checker.notNull("field:textNode", textNode);
		return textNode;
	}

	public void setNode(Node node) {
		this.node = node;
		if (node.getNodeType() == Node.TEXT_NODE) {
			this.textNode = (Text) node;
		}
	}

	public void setOffset(final int offset) {
		this.offset = offset;
	}

	public void setTextNode(final Text textNode) {
		Checker.notNull("parameter:textNode", textNode);
		this.textNode = textNode;
		this.node = textNode;
	}

	public String toString() {
		return super.toString() + ", offset: " + offset + ", textNode\""
				+ this.textNode + "\"";
	}
}
