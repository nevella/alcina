package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.annotations.IsSafeHtml;
import com.google.gwt.user.client.ui.impl.TextBoxImpl;

import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.StringMap;

public class ElementJso extends NodeJso implements ElementRemote {
	/*
	 * Non-private for access from bytecode generated (ElementJso$)
	 */
	static RemoteCache cache = new RemoteCache();

	/**
	 * Assert that the given {@link Node} is an {@link Element} and
	 * automatically typecast it.
	 */
	public static Element as(JavaScriptObject o) {
		assert is(o);
		return nodeFor(o);
	}

	/**
	 * Assert that the given {@link Node} is an {@link Element} and
	 * automatically typecast it.
	 */
	public static Element as(Node node) {
		assert is((JavaScriptObject) node.remote());
		return (Element) node;
	}

	/**
	 * Assert that the given {@link Node} is an {@link Element} and
	 * automatically typecast it.
	 */
	static ElementJso asRemote(JavaScriptObject o) {
		assert isRemote(o);
		return o.cast();
	}

	static List<Boolean> commaSeparatedBoolsToList(String string) {
		if (string.isEmpty()) {
			return Collections.emptyList();
		}
		return Arrays.asList(string.split(",")).stream()
				.map(Boolean::parseBoolean).collect(Collectors.toList());
	}

	static List<Integer> commaSeparatedIntsToList(String string) {
		if (string.isEmpty()) {
			return Collections.emptyList();
		}
		return Arrays.asList(string.split(",")).stream().map(Integer::parseInt)
				.collect(Collectors.toList());
	}

	static int indexOfName(String nameList, String name) {
		return ClientDomElement.indexOfName(nameList, name);
	}

	public static boolean is(JavaScriptObject o) {
		if (cache.lastIs == o) {
			return cache.lastIsResult;
		}
		boolean is0 = isRemote(o);
		cache.lastIs = o;
		cache.lastIsResult = is0;
		return is0;
	}

	/**
	 * Determine whether the given {@link Node} can be cast to an
	 * {@link Element}. A <code>null</code> node will cause this method to
	 * return <code>false</code>.
	 */
	public static boolean is(Node node) {
		return (node != null) && (node.getNodeType() == Node.ELEMENT_NODE);
	}

	private static boolean isRemote(JavaScriptObject o) {
		if (NodeJso.is(o)) {
			return ((NodeJso) o).getNodeType() == Node.ELEMENT_NODE;
		}
		return false;
	}

	/**
	 * Fast helper method to convert small doubles to 32-bit int.
	 *
	 * <p>
	 * Note: you should be aware that this uses JavaScript rounding and thus
	 * does NOT provide the same semantics as
	 * <code>int b = (int) someDouble;</code>. In particular, if x is outside
	 * the range [-2^31,2^31), then toInt32(x) would return a value equivalent
	 * to x modulo 2^32, whereas (int) x would evaluate to either MIN_INT or
	 * MAX_INT.
	 */
	private static native int toInt32(double val) /*-{
    return val | 0;
	}-*/;

	static String trimClassName(String className) {
		return ClientDomElement.trimClassName(className);
	}

	protected ElementJso() {
	}

	@Override
	public final boolean addClassName(String className) {
		return ClientDomElementStatic.addClassName(this, className);
	}

	/**
	 * Removes keyboard focus from this element.
	 */
	@Override
	public final native void blur() /*-{
    this.blur();
	}-*/;

	/**
	 * Explicitly build html by traversing with javascript (cos we can't trust
	 * IE11, although we can webkit and probably FF)
	 *
	 * FIXME dirndl 1x3 - probably remove
	 */
	final native String buildOuterHtml()/*-{

    function escapeHtml(str, buffer) {
      var node = document.createTextNode(str);
      buffer.div.appendChild(node);
      var result = buffer.div.innerHTML;
      buffer.div.removeChild(node);
      return result;
    }
    function addNodeToBuiltHtml(node, buffer, depth) {
      var buf = buffer.buf;
      //fixme - test
      switch (node.nodeType) {
      //TEXT_NODE
      case 3:
        buf += escapeHtml(node.data, buffer);
        break;
      //PROCESSING_INSTRUCTION_NODE
      case 7:
        buf += '<?';
        buf += node.name;
        buf += ' ';
        buf += escapeHtml(node.data, buffer);
        buf += '?>';
        //COMMENT_NODE
      case 8:
        buf += '<!--';
        buf += escapeHtml(node.data, buffer);
        buf += '-->';
        break;
      //ELEMENT_NODE
      case 1:
        buf += '<';
        buf += node.tagName;
        if (node.attributes.length > 0) {
          for (var idx = 0; idx < node.attributes.length; idx++) {
            buf += ' ';
            buf += node.attributes[idx].name;
            buf += '="';
            buf += escapeHtml(node.attributes[idx].value, buffer).split("\"")
                .join("&quot;");
            buf += '"';
          }
        }
        buf += '>';
        var idx = 0;
        var size = node.childNodes.length;
        buffer.buf = buf;
        for (; idx < size; idx++) {
          var child = node.childNodes.item(idx);
          addNodeToBuiltHtml(child, buffer, depth + 1);
        }
        buf = buffer.buf;
        var re = /^(?:area|base|br|col|command|embed|hr|img|input|keygen|link|meta|param|source|track|wbr)$/i;
        if (node.tagName.match(re)) {

        } else {
          buf += '</';
          buf += node.tagName;
          buf += '>';
        }

        break;
      default:
        throw "node not handled:" + node;
      }
      buffer.buf = buf;

    }
    var buffer = {
      buf : '',
      div : $doc.createElement('div')
    }
    addNodeToBuiltHtml(this, buffer, 0);
    return buffer.buf;
	}-*/;

	/**
	 * Dispatched the given event with this element as its target. The event
	 * will go through all phases of the browser's normal event dispatch
	 * mechanism.
	 *
	 * Note: Because the browser's normal dispatch mechanism is used, exceptions
	 * thrown from within handlers triggered by this method cannot be caught by
	 * wrapping this method in a try/catch block. Such exceptions will be caught
	 * by the
	 * {@link com.google.gwt.core.client.GWT#setUncaughtExceptionHandler(com.google.gwt.core.client.GWT.UncaughtExceptionHandler)
	 * uncaught exception handler} as usual.
	 *
	 * @param evt
	 *            the event to be dispatched
	 */
	@Override
	public final void dispatchEvent(NativeEvent evt) {
		DOMImpl.impl.dispatchEvent(elementFor(), evt.jso);
	}

	@Override
	public final Element elementFor() {
		return LocalDom.nodeFor(this);
	}

	@Override
	public final void ensureId() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Gives keyboard focus to this element.
	 */
	@Override
	public final native void focus() /*-{
    this.focus();
	}-*/;

	/**
	 * Gets an element's absolute bottom coordinate in the document's coordinate
	 * system.
	 */
	@Override
	public final int getAbsoluteBottom() {
		return getAbsoluteTop() + getOffsetHeight();
	}

	/**
	 * Gets an element's absolute left coordinate in the document's coordinate
	 * system.
	 */
	@Override
	public final int getAbsoluteLeft() {
		return DOMImpl.impl.getAbsoluteLeft(elementFor());
	}

	/**
	 * Gets an element's absolute right coordinate in the document's coordinate
	 * system.
	 */
	@Override
	public final int getAbsoluteRight() {
		return getAbsoluteLeft() + getOffsetWidth();
	}

	/**
	 * Gets an element's absolute top coordinate in the document's coordinate
	 * system.
	 */
	@Override
	public final int getAbsoluteTop() {
		return DOMImpl.impl.getAbsoluteTop(elementFor());
	}

	/**
	 * Retrieves an attribute value by name. Attribute support can be
	 * inconsistent across various browsers. Consider using the accessors in
	 * {@link Element} and its specific subclasses to retrieve attributes and
	 * properties.
	 *
	 * @param name
	 *            The name of the attribute to retrieve
	 * @return The Attr value as a string, or the empty string if that attribute
	 *         does not have a specified or default value
	 */
	@Override
	public final String getAttribute(String name) {
		return DOMImpl.impl.getAttribute(elementFor(), name);
	}

	private final native JsArrayString getAttributeList()/*-{
    var result = [];
    var attrs = this.attributes;
    for (var i = 0; i < attrs.length; i++) {
      result.push(attrs[i].name);
      result.push(attrs[i].value);
    }
    return result;
	}-*/;

	@Override
	public final Map<String, String> getAttributeMap() {
		StringMap result = new StringMap();
		JsArrayString arr = getAttributeList();
		for (int idx = 0; idx < arr.length(); idx += 2) {
			result.put(arr.get(idx), arr.get(idx + 1));
		}
		return result;
	}

	public final native DomRect getBoundingClientRect()/*-{
		var rect = this.getBoundingClientRect();
		return @com.google.gwt.dom.client.DomRect::new(Lcom/google/gwt/dom/client/DomRectJso;)(rect);
	}-*/;

	/**
	 * The class attribute of the element. This attribute has been renamed due
	 * to conflicts with the "class" keyword exposed by many languages.
	 *
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/global.html#adef-class">
	 *      W3C HTML Specification</a>
	 */
	@Override
	public final native String getClassName() /*-{
    return this.className || "";
	}-*/;

	/**
	 * Returns the inner height of an element in pixels, including padding but
	 * not the horizontal scrollbar height, border, or margin.
	 *
	 * @return the element's client height
	 */
	@Override
	public final int getClientHeight() {
		return toInt32(getSubPixelClientHeight());
	}

	/**
	 * Returns the inner width of an element in pixels, including padding but
	 * not the vertical scrollbar width, border, or margin.
	 *
	 * @return the element's client width
	 */
	@Override
	public final int getClientWidth() {
		return toInt32(getSubPixelClientWidth());
	}

	public final native StyleRemote getComputedStyle() /*-{
    return $wnd.getComputedStyle(this);
	}-*/;

	final List<ContiguousTextNodes> getContiguousTextContainers() {
		List<ContiguousTextNodes> result = new ArrayList<>();
		Stack<ElementJso> stack = new Stack<>();
		stack.push(this);
		while (!stack.isEmpty()) {
			// breadth first is important here, because later application
			// requires correct parent indicies (at time of application)
			ElementJso elem = stack.remove(0);
			List<NodeJso> list = new ArrayList<>();
			NodeListJso<Node> childNodes = elem.getChildNodes0();
			int length = childNodes.getLength();
			int lastNodeType = -1;
			NodeJso lastNode = null;
			for (int idx = 0; idx < length; idx++) {
				NodeJso node = childNodes.getItem0(idx);
				int nodeType = node.getNodeType();
				switch (nodeType) {
				case Node.ELEMENT_NODE:
					ElementJso elemChild = (ElementJso) node;
					switch (elemChild.getNodeName().toLowerCase()) {
					case "script":
					case "style":
						// not visual - and sometimes weird ("br" in script(!!))
						// - don't descend
						break;
					default:
						stack.push(elemChild);
						break;
					}
					break;
				case Node.TEXT_NODE:
					if (lastNodeType == Node.TEXT_NODE) {
						result.add(
								new ContiguousTextNodes(idx, lastNode, node));
					}
					break;
				case Node.COMMENT_NODE:
					if (lastNodeType == Node.COMMENT_NODE) {
						result.add(
								new ContiguousTextNodes(idx, lastNode, node));
					}
					break;
				default:
					throw new UnsupportedOperationException();
				}
				lastNodeType = nodeType;
				lastNode = node;
			}
		}
		return result;
	}

	/**
	 * Specifies the base direction of directionally neutral text and the
	 * directionality of tables.
	 */
	@Override
	public final native String getDir() /*-{
    return this.dir;
	}-*/;

	/**
	 * Returns the draggable attribute of this element.
	 *
	 * @return one of {@link #DRAGGABLE_AUTO}, {@link #DRAGGABLE_FALSE}, or
	 *         {@link #DRAGGABLE_TRUE}
	 */
	@Override
	public final native String getDraggable() /*-{
    return this.draggable || null;
	}-*/;

	@Override
	public final NodeList<Element> getElementsByTagName(String tagName) {
		return new NodeList(getElementsByTagName0(tagName));
	}

	/**
	 * Returns a NodeList of all descendant Elements with a given tag name, in
	 * the order in which they are encountered in a preorder traversal of this
	 * Element tree.
	 *
	 * @param name
	 *            The name of the tag to match on. The special value "*" matches
	 *            all tags
	 * @return A list of matching Element nodes
	 */
	private final native NodeListJso<Element> getElementsByTagName0(String name) /*-{
    return this.getElementsByTagName(name);
	}-*/;

	@Override
	public final Element getFirstChildElement() {
		return ClientDomElementStatic.getFirstChildElement(this);
	}

	/**
	 * The element's identifier.
	 *
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/global.html#adef-id">
	 *      W3C HTML Specification</a>
	 */
	@Override
	public final native String getId() /*-{
    return this.id;
	}-*/;

	@Override
	public final String getInnerHTML() {
		return ClientDomElementStatic.getInnerHTML(this);
	}

	final native String getInnerHTML0()/*-{
    return this.innerHTML;
	}-*/;

	@Override
	public final String getInnerText() {
		return ClientDomElementStatic.getInnerText(this);
	}

	/**
	 * Language code defined in RFC 1766.
	 */
	@Override
	public final native String getLang() /*-{
    return this.lang;
	}-*/;

	@Override
	public final Element getNextSiblingElement() {
		return ClientDomElementStatic.getNextSiblingElement(this);
	}

	/**
	 * The height of an element relative to the layout.
	 */
	@Override
	public final int getOffsetHeight() {
		return toInt32(getSubPixelOffsetHeight());
	}

	/**
	 * The number of pixels that the upper left corner of the current element is
	 * offset to the left within the offsetParent node.
	 */
	@Override
	public final int getOffsetLeft() {
		return toInt32(getSubPixelOffsetLeft());
	}

	/**
	 * Returns a reference to the object which is the closest (nearest in the
	 * containment hierarchy) positioned containing element.
	 */
	@Override
	public final native Element getOffsetParent() /*-{
    var elem = this.offsetParent ? @com.google.gwt.dom.client.LocalDom::nodeFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.offsetParent)
        : null;
    return elem;
	}-*/;

	/**
	 * The number of pixels that the upper top corner of the current element is
	 * offset to the top within the offsetParent node.
	 */
	@Override
	public final int getOffsetTop() {
		return toInt32(getSubPixelOffsetTop());
	}

	/**
	 * The width of an element relative to the layout.
	 */
	@Override
	public final int getOffsetWidth() {
		return toInt32(getSubPixelOffsetWidth());
	}

	@Override
	public final native String getOuterHtml()/*-{
    return this.outerHTML;
	}-*/;

	@Override
	public final Element getPreviousSiblingElement() {
		return ClientDomElementStatic.getPreviousSiblingElement(this);
	}

	/**
	 * Gets a boolean property from this element.
	 *
	 * @param name
	 *            the name of the property to be retrieved
	 * @return the property value
	 */
	@Override
	public final native boolean getPropertyBoolean(String name) /*-{
    return !!this[name];
	}-*/;

	/**
	 * Gets a double property from this element.
	 *
	 * @param name
	 *            the name of the property to be retrieved
	 * @return the property value
	 */
	@Override
	public final native double getPropertyDouble(String name) /*-{
    return parseFloat(this[name]) || 0.0;
	}-*/;

	/**
	 * Gets an integer property from this element.
	 *
	 * @param name
	 *            the name of the property to be retrieved
	 * @return the property value
	 */
	@Override
	public final native int getPropertyInt(String name) /*-{
    return parseInt(this[name]) | 0;
	}-*/;

	/**
	 * Gets a JSO property from this element.
	 *
	 * @param name
	 *            the name of the property to be retrieved
	 * @return the property value
	 */
	@Override
	public final native JavaScriptObject getPropertyJSO(String name) /*-{
    return this[name] || null;
	}-*/;

	/**
	 * Gets an object property from this element.
	 *
	 * @param name
	 *            the name of the property to be retrieved
	 * @return the property value
	 */
	@Override
	public final native Object getPropertyObject(String name) /*-{
    return this[name];
	}-*/;

	@Override
	public final String getPropertyString(String name) {
		return getPropertyString0(jsoPropertyName(name));
	}

	/**
	 * Gets a property from this element.
	 *
	 * @param name
	 *            the name of the property to be retrieved
	 * @return the property value
	 */
	private final native String getPropertyString0(String name) /*-{
    return (this[name] == null) ? null : String(this[name]);
	}-*/;

	/**
	 * The height of the scroll view of an element.
	 */
	@Override
	public final int getScrollHeight() {
		return toInt32(getSubPixelScrollHeight());
	}

	@Override
	public final int getScrollLeft() {
		return ClientDomElementStatic.getScrollLeft(this);
	}

	/**
	 * The number of pixels that an element's content is scrolled from the top.
	 */
	@Override
	public final int getScrollTop() {
		return toInt32(getSubPixelScrollTop());
	}

	/**
	 * The width of the scroll view of an element.
	 */
	@Override
	public final int getScrollWidth() {
		return toInt32(getSubPixelScrollWidth());
	}

	@Override
	public final String getString() {
		return ClientDomElementStatic.getString(this);
	}

	/**
	 * Gets this element's {@link Style} object.
	 */
	@Override
	public final Style getStyle() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Gets this element's {@link Style} object.
	 */
	final native StyleRemote getStyle0() /*-{
    return this.style;
	}-*/;

	public final StyleRemote getStyleRemote() {
		return getStyle0();
	}

	private final native double getSubPixelClientHeight() /*-{
    return this.clientHeight;
	}-*/;

	private final native double getSubPixelClientWidth() /*-{
    return this.clientWidth;
	}-*/;

	private final native double getSubPixelOffsetHeight() /*-{
    return this.offsetHeight || 0;
	}-*/;

	private final native double getSubPixelOffsetLeft() /*-{
    return this.offsetLeft || 0;
	}-*/;

	private final native double getSubPixelOffsetTop() /*-{
    return this.offsetTop || 0;
	}-*/;

	private final native double getSubPixelOffsetWidth() /*-{
    return this.offsetWidth || 0;
	}-*/;

	private final native double getSubPixelScrollHeight() /*-{
    return this.scrollHeight || 0;
	}-*/;

	private final native double getSubPixelScrollTop() /*-{
    return this.scrollTop || 0;
	}-*/;

	private final native double getSubPixelScrollWidth() /*-{
    return this.scrollWidth || 0;
	}-*/;

	@Override
	public final int getTabIndex() {
		return ClientDomElementStatic.getTabIndex(this);
	}

	@Override
	public final String getTagName() {
		return ClientDomElementStatic.getTagName(this);
	}

	final native String getTagNameRemote()/*-{
    return this.tagName;
	}-*/;

	/**
	 * The element's advisory title.
	 */
	@Override
	public final native String getTitle() /*-{
    return this.title;
	}-*/;

	@Override
	public final boolean hasAttribute(String name) {
		return ClientDomElementStatic.hasAttribute(this, name);
	}

	@Override
	public final boolean hasClassName(String className) {
		return ClientDomElementStatic.hasClassName(this, className);
	}

	@Override
	public final boolean hasTagName(String tagName) {
		return ClientDomElementStatic.hasTagName(this, tagName);
	}

	final boolean hasTagNameInternal(String tag) {
		return getTagNameRemote().equals(tag);
	}

	final String jsoPropertyName(String name) {
		return Objects.equals(name, "class") ? "className" : name;
	}

	public final String provideRemoteDomTree() {
		return provideRemoteDomTree0();
	}

	final native String provideRemoteDomTree0()/*-{
    function addNode(node, buffer, depth) {
      var buf = buffer.buf;
      for (var idx = 0; idx < depth; idx++) {
        buf += ' ';
      }
      buf += node.nodeType;
      buf += ': ';
      switch (node.nodeType) {
      case 3:
      case 8:
        buf += '[';
        buf += node.data.split('\n').join('\\n').split('\t').join('\\t').split(
            '\r').join('\\r');
        buf += ']';
        break;
      case 1:
        buf += node.tagName;
        buf += ' : ';
        break;
      }
      buf += '\n';
      buffer.buf = buf;
      if (node.nodeType == 1) {
        var idx = 0;
        var size = node.childNodes.length;
        for (; idx < size; idx++) {
          var child = node.childNodes.item(idx);
          addNode(child, buffer, depth + 1);
        }
      }
    }
    var buffer = {
      buf : ''
    };
    addNode(this, buffer, 0);
    return buffer.buf;
	}-*/;

	// FIXME - dirndl 1x1e - should also check tagname for really warped
	// dom. unlikely to be an issue though. Also result should be a json form
	// (JSO)
	final native ElementJsoIndex provideRemoteIndex(boolean debug)/*-{
    var result = {
      hasNode : null,
      root : null,
      indicies : [],
      ancestors : [],
      sizes : [],
      debugData : [],
      remoteDefined : [],
      debugLog : '',
    };
    var cursor = this;
    while (true) {
      var hasNode = @com.google.gwt.dom.client.LocalDom::hasNode(Lcom/google/gwt/core/client/JavaScriptObject;)(cursor);
      if (hasNode) {
        result.hasNode = cursor;
        break;
      }
      var parent = cursor.parentElement;
      if (parent == null) {
        result.root = cursor;
        break;
      }
      var idx = 0;
      var size = parent.childNodes.length;
      for (; idx < size; idx++) {
        var node = parent.childNodes.item(idx);
        if (debug) {
          result.debugLog += "Checking node - depth: " + result.indicies.length;
          result.debugLog += " - idx: " + idx;
          result.debugLog += " - Node type: " + node.nodeType;
          result.debugLog += " - Node name: " + node.nodeName;
          result.debugLog += " - Cursor type: " + node.nodeType;
          result.debugLog += " - Cursor name: " + node.nodeName;
          result.debugLog += "\n";
        }
        if (node == cursor) {
          result.indicies.push(idx);
          result.ancestors.push(cursor);
          var className = cursor.className;
          if (!className.indexOf && typeof className.baseVal == 'string') {
            className = className.baseVal;
          }
          result.remoteDefined
              .push(className.indexOf(@Element::REMOTE_DEFINED) != -1);
          break;
        }
      }
      result.sizes.push(size);
      if (debug) {
        var buf = '';
        var idx = 0;
        for (; idx < size; idx++) {
          var node = parent.childNodes.item(idx);
          buf += node.nodeType;
          buf += ': ';
          switch (node.nodeType) {
          case 3:
          case 8:
            buf += '[';
            buf += node.data.split('\n').join('\\n').split('\t').join('\\t');
            buf += ']';
            break;
          case 1:
            buf += node.tagName;
            buf += ' : ';
            break;
          }
          buf += "\n";
        }
        result.debugData.push(buf);
      }
      cursor = parent;
    }
    return result;

	}-*/;

	/** only allow if telling all local nodes that they're detached **/
	final Node removeAllChildren0() {
		LocalDom.verifyMutatingState();
		setInnerHTML("");
		return node();
	}

	/**
	 * Removes an attribute by name.
	 */
	@Override
	public final native void removeAttribute(String name) /*-{
    @com.google.gwt.dom.client.LocalDom::verifyMutatingState();
    this.removeAttribute(name);
	}-*/;

	@Override
	public final boolean removeClassName(String className) {
		return ClientDomElementStatic.removeClassName(this, className);
	}

	final native void removeFromParent0()/*-{
    @com.google.gwt.dom.client.LocalDom::verifyMutatingState();
    this.parentElement.removeChild(this);
	}-*/;

	@Override
	public final void replaceClassName(String oldClassName,
			String newClassName) {
		ClientDomElementStatic.replaceClassName(this, oldClassName,
				newClassName);
	}

	public final void replaceWith(ElementJso replacement) {
		getParentElementJso().insertBefore0(replacement, this);
		removeFromParent0();
	}

	@Override
	public final void scrollIntoView() {
		ClientDomElementStatic.scrollIntoView(this);
	}

	/**
	 * Adds a new attribute. If an attribute with that name is already present
	 * in the element, its value is changed to be that of the value parameter.
	 *
	 * @param name
	 *            The name of the attribute to create or alter
	 * @param value
	 *            Value to set in string form
	 */
	@Override
	public final native void setAttribute(String name, String value) /*-{
    @com.google.gwt.dom.client.LocalDom::verifyMutatingState();
    this.setAttribute(name, value);
	}-*/;

	/**
	 * The class attribute of the element. This attribute has been renamed due
	 * to conflicts with the "class" keyword exposed by many languages.
	 *
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/global.html#adef-class">
	 *      W3C HTML Specification</a>
	 */
	@Override
	public final native void setClassName(String className) /*-{
    @com.google.gwt.dom.client.LocalDom::verifyMutatingState();
    this.className = className || "";
	}-*/;

	/**
	 * Specifies the base direction of directionally neutral text and the
	 * directionality of tables.
	 */
	@Override
	public final native void setDir(String dir) /*-{
    this.dir = dir;
	}-*/;

	@Override
	public final void setDraggable(String draggable) {
		ClientDomElementStatic.setDraggable(this, draggable);
	}

	/**
	 * The element's identifier.
	 *
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/global.html#adef-id">
	 *      W3C HTML Specification</a>
	 */
	@Override
	public final native void setId(String id) /*-{
    this.id = id;
	}-*/;

	/**
	 * All of the markup and content within a given element.
	 */
	@Override
	public final native void setInnerHTML(@IsSafeHtml
	String html) /*-{
    @com.google.gwt.dom.client.LocalDom::verifyMutatingState();
    this.innerHTML = html || '';
	}-*/;

	@Override
	public final void setInnerSafeHtml(SafeHtml html) {
		ClientDomElementStatic.setInnerSafeHtml(this, html);
	}

	@Override
	public final void setInnerText(String text) {
		ClientDomElementStatic.setInnerText(this, text);
	}

	/**
	 * Language code defined in RFC 1766.
	 */
	@Override
	public final native void setLang(String lang) /*-{
    this.lang = lang;
	}-*/;

	/**
	 * Sets a boolean property on this element.
	 *
	 * @param name
	 *            the name of the property to be set
	 * @param value
	 *            the new property value
	 */
	@Override
	public final native void setPropertyBoolean(String name, boolean value) /*-{
    @com.google.gwt.dom.client.LocalDom::verifyMutatingState();
    this[name] = value;
	}-*/;

	/**
	 * Sets a double property on this element.
	 *
	 * @param name
	 *            the name of the property to be set
	 * @param value
	 *            the new property value
	 */
	@Override
	public final native void setPropertyDouble(String name, double value) /*-{
    @com.google.gwt.dom.client.LocalDom::verifyMutatingState();
    this[name] = value;
	}-*/;

	/**
	 * Sets an integer property on this element.
	 *
	 * @param name
	 *            the name of the property to be set
	 * @param value
	 *            the new property value
	 */
	@Override
	public final native void setPropertyInt(String name, int value) /*-{
    @com.google.gwt.dom.client.LocalDom::verifyMutatingState();
    this[name] = value;
	}-*/;

	/**
	 * Sets a JSO property on this element.
	 *
	 * @param name
	 *            the name of the property to be set
	 * @param value
	 *            the new property value
	 */
	@Override
	public final native void setPropertyJSO(String name, JavaScriptObject value) /*-{
    @com.google.gwt.dom.client.LocalDom::verifyMutatingState();
    this[name] = value;
	}-*/;

	/**
	 * Sets an object property on this element.
	 *
	 * @param name
	 *            the name of the property to be set
	 * @param value
	 *            the new property value
	 */
	@Override
	public final native void setPropertyObject(String name, Object value) /*-{
    @com.google.gwt.dom.client.LocalDom::verifyMutatingState();
    this[name] = value;
	}-*/;

	/**
	 * Sets a property on this element.
	 *
	 * @param name
	 *            the name of the property to be set
	 * @param value
	 *            the new property value
	 */
	@Override
	public final void setPropertyString(String name, String value) {
		setPropertyString0(jsoPropertyName(name), value);
	}

	/*
	 * This is a hack - rather than relying on knowledge of
	 * "is this a property or an attribute", set both
	 * 
	 * but attr first
	 */
	final native void setPropertyString0(String name, String value) /*-{
    @com.google.gwt.dom.client.LocalDom::verifyMutatingState();
	this.setAttribute(name,value);
    this[name] = value;
	}-*/;

	@Override
	public final void setScrollLeft(int scrollLeft) {
		ClientDomElementStatic.setScrollLeft(this, scrollLeft);
	}

	/**
	 * The number of pixels that an element's content is scrolled to the top.
	 */
	@Override
	public final native void setScrollTop(int scrollTop) /*-{
    @com.google.gwt.dom.client.LocalDom::verifyMutatingState();
    this.scrollTop = scrollTop;
	}-*/;

	/**
	 * The index that represents the element's position in the tabbing order.
	 *
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-tabindex">
	 *      W3C HTML Specification</a>
	 */
	@Override
	public final native void setTabIndex(int tabIndex) /*-{
    @com.google.gwt.dom.client.LocalDom::verifyMutatingState();
    this.tabIndex = tabIndex;
	}-*/;

	/**
	 * The element's advisory title.
	 */
	@Override
	public final native void setTitle(String title) /*-{
    // Setting the title to null results in the string "null" being displayed
    // on some browsers.
    @com.google.gwt.dom.client.LocalDom::verifyMutatingState();
    this.title = title || '';
	}-*/;

	// event handlers should (currently) be added pre-dom-attach
	@Override
	public final void sinkBitlessEvent(String eventTypeName) {
		throw new UnsupportedOperationException();
	}

	// event handlers should (currently) be added pre-dom-attach
	@Override
	public final void sinkEvents(int eventBits) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void toggleClassName(String className) {
		ClientDomElementStatic.toggleClassName(this, className);
	}

	static class ContiguousTextNodes {
		NodeJso previous;

		NodeJso node;

		int idx;

		ContiguousTextNodes(int idx, NodeJso previous, NodeJso node) {
			this.idx = idx;
			this.previous = previous;
			this.node = node;
		}

		void applyToLocal() {
		}
	}

	static class ElementJsoIndex extends JavaScriptObject {
		protected ElementJsoIndex() {
		}

		final native JsArray ancestors()/*-{
      return this.ancestors;
		}-*/;

		final native String debugData()/*-{
      return this.debugData.join("\n\n");
		}-*/;

		final native String debugLog()/*-{
      return this.debugLog;
		}-*/;

		public final String getString() {
			FormatBuilder fb = new FormatBuilder();
			fb.line("Element remote:\n===========");
			fb.line("Indicies (lowest first):\n%s", stringIndicies());
			fb.line("Ancestors (lowest first):\n%s", ancestors());
			fb.line("Root:\n%s",
					root() != null ? root().getTagNameRemote() : "null");
			fb.line("Debug data:\n%s", debugData());
			fb.line("\nDebug log:\n%s", debugLog());
			return fb.toString();
		}

		final native ElementJso hasNode()/*-{
      return this.hasNode;
		}-*/;

		final boolean hasRemoteDefined() {
			for (Boolean value : remoteDefined()) {
				if (value) {
					return true;
				}
			}
			return false;
		}

		final List<Integer> indicies() {
			return commaSeparatedIntsToList(stringIndicies());
		}

		final native JsArrayInteger jsIndicies()/*-{
      return this.indicies;
		}-*/;

		final native JsArrayInteger jsSizes()/*-{
      return this.sizes;
		}-*/;

		final List<Boolean> remoteDefined() {
			return commaSeparatedBoolsToList(stringRemoteDefined());
		}

		final native ElementJso root()/*-{
      return this.root;
		}-*/;

		final List<Integer> sizes() {
			return commaSeparatedIntsToList(stringSizes());
		}

		final native String stringIndicies()/*-{
      return this.indicies.join(",");
		}-*/;

		final native String stringRemoteDefined()/*-{
      return this.remoteDefined.join(",");
		}-*/;

		final native String stringSizes()/*-{
      return this.sizes.join(",");
		}-*/;
	}

	/**
	 * Determines whether the given {@link JavaScriptObject} can be cast to an
	 * {@link Element}. A <code>null</code> object will cause this method to
	 * return <code>false</code>.
	 *
	 *
	 */
	static class RemoteCache {
		boolean lastIsResult;

		JavaScriptObject lastIs;
	}

	@Override
	final public void setSelectionRange(int pos, int length) {
		((TextBoxImpl) GWT.create(TextBoxImpl.class))
				.setSelectionRange((Element) node(), pos, length);
	}
}
