/* 
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

package cc.alcina.framework.gwt.client.util;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;

/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class GWTDOMUtils {
	public static final String TEXT_NODE_TAG = "#text";

	public static final String XPATH = "__xpath";

	public static Element getPrecedingElementBreathFirst(Node n,
			Element lastChildElementOf) {
		Element parentOrLastChild = (Element) ((n == null) ? lastChildElementOf
				: n.getParentNode());
		NodeList<Node> nl = parentOrLastChild.getChildNodes();
		boolean foundNode = n == null;
		for (int i = nl.getLength() - 1; i >= 0; i--) {
			Node n2 = nl.getItem(i);
			if (n != null && n2 == n) {
				foundNode = true;
			}
			if (foundNode && n2.getNodeType() == Node.ELEMENT_NODE) {
				return getPrecedingElementBreathFirst(null, (Element) n2);
			}
		}
		return parentOrLastChild;
	}

	public static void insertAfter(Element el, Node newNode, Node insertAfter) {
		NodeList<Node> nl = el.getChildNodes();
		boolean foundAfter = false;
		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.getItem(i);
			if (foundAfter) {
				el.insertBefore(newNode, n);
				return;
			}
			if (n == insertAfter) {
				foundAfter = true;
			}
		}
		el.appendChild(newNode);
	}

	public static void unwrap(Element el) {
		Element parent = el.getParentElement();
		NodeList<Node> nl = el.getChildNodes();
		boolean foundAfter = false;
		Node[] tmp = new Node[nl.getLength()];
		for (int i = 0; i < nl.getLength(); i++) {
			tmp[i]=nl.getItem(i);
		}
		for (int i = 0; i < tmp.length; i++) {
			Node n = tmp[i];
			el.removeChild(n);
			parent.insertBefore(n, el);
		}
		parent.removeChild(el);
	}

//	private static String lastXpath;
//
//	private static Element lastXpathParent;

	public static void wrap(Element wrapper, Node toWrap) {
		Element parent = (Element) toWrap.getParentNode();
		parent.insertBefore(wrapper, toWrap);
		parent.removeChild(toWrap);
		wrapper.appendChild(toWrap);
	}

	public static String xpath(Node n, StringBuffer b, boolean justThisNode) {
		if (b==null){
			b = new StringBuffer();
		}
		if (n.getNodeType() == Node.DOCUMENT_NODE) {
//			lastXpath = b.toString();
			return b.toString();
		}
		// child string
		Element p = (Element) n.getParentNode();
		String tagName = n.getNodeType() == Node.TEXT_NODE ? TEXT_NODE_TAG
				: ((Element) n).getTagName();
		NodeList<Node> nl = p.getChildNodes();
		int ctr = 0;
		for (int i = nl.getLength() - 1; i >= 0; i--) {
			Node n2 = nl.getItem(i);
			if (n2 == n) {
				break;
			}
			String tagName2 = n2.getNodeType() == Node.TEXT_NODE ? TEXT_NODE_TAG
					: ((Element) n2).getTagName();
			if (tagName.equals(tagName2)) {
				ctr++;
			}
		}
		boolean firstNode = b.length()==0;
		if (!firstNode) {
			b.insert(0, "/");
		} 
		b.insert(0, tagName + "[" + ctr + "]");
		if (justThisNode||p.getParentElement()==null) {
			return b.toString();
		} else {
			if (firstNode){
//				if (lastXpathParent==p){
//					
//					return lastXpath.
//				}
			}
			return xpath(p, b, justThisNode);
		}
	}
}
