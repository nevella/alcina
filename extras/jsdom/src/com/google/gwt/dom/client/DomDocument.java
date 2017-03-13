package com.google.gwt.dom.client;

import com.google.gwt.core.client.SingleJsoImpl;

/**
 * all default methods should actually call through to domdocument_static ... when i get the time
 * @author nick@alcina.cc
 *
 */
public interface DomDocument extends DomNode {
	@Override
	default String getNodeName() {
		return "#document";
	}

	@Override
	default short getNodeType() {
		return Node.DOCUMENT_NODE;
	}

	@Override
	default String getNodeValue() {
		return null;
	}

	@Override
	default void setNodeValue(String nodeValue) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Creates an &lt;a&gt; element.
	 * 
	 * @return the newly created element
	 */
	default AnchorElement createAnchorElement() {
		return (AnchorElement) DOMImpl.impl.createElement(documentFor(),
				AnchorElement.TAG);
	}

	/**
	 * Creates an &lt;area&gt; element.
	 * 
	 * @return the newly created element
	 */
	default AreaElement createAreaElement() {
		return (AreaElement) DOMImpl.impl.createElement(documentFor(),
				AreaElement.TAG);
	}

	/**
	 * Creates an &lt;audio&gt; element.
	 * 
	 * @return the newly created element
	 */
	default AudioElement createAudioElement() {
		return (AudioElement) DOMImpl.impl.createElement(documentFor(),
				AudioElement.TAG);
	}

	/**
	 * Creates a &lt;base&gt; element.
	 * 
	 * @return the newly created element
	 */
	default BaseElement createBaseElement() {
		return (BaseElement) DOMImpl.impl.createElement(documentFor(),
				BaseElement.TAG);
	}

	/**
	 * Creates a &lt;blockquote&gt; element.
	 * 
	 * @return the newly created element
	 */
	default QuoteElement createBlockQuoteElement() {
		return (QuoteElement) DOMImpl.impl.createElement(documentFor(),
				QuoteElement.TAG_BLOCKQUOTE);
	}

	/**
	 * Creates a 'blur' event.
	 */
	default NativeEvent createBlurEvent() {
		return createHtmlEvent(BrowserEvents.BLUR, false, false);
	}

	/**
	 * Creates a &lt;br&gt; element.
	 * 
	 * @return the newly created element
	 */
	default BRElement createBRElement() {
		return (BRElement) DOMImpl.impl.createElement(documentFor(), BRElement.TAG);
	}

	/**
	 * Creates a &lt;button&gt; element.
	 * <p>
	 * <b>Warning!</b> The button type is actually implementation-dependent and
	 * is read-only.
	 * 
	 * @return the newly created element
	 * @deprecated use {@link #createPushButtonElement()},
	 *             {@link #createResetButtonElement()} or
	 *             {@link #createSubmitButtonElement()} instead.
	 */
	@Deprecated
	default ButtonElement createButtonElement() {
		return (ButtonElement) DOMImpl.impl.createElement(documentFor(),
				ButtonElement.TAG);
	}

	/**
	 * Creates an &lt;input type='button'&gt; element.
	 * 
	 * @return the newly created element
	 */
	default InputElement createButtonInputElement() {
		return DOMImpl.impl.createInputElement(documentFor(), "button");
	}

	/**
	 * Creates a &lt;canvas&gt; element.
	 * 
	 * @return the newly created element
	 */
	default CanvasElement createCanvasElement() {
		return (CanvasElement) DOMImpl.impl.createElement(documentFor(),
				CanvasElement.TAG);
	}

	/**
	 * Creates a &lt;caption&gt; element.
	 * 
	 * @return the newly created element
	 */
	default TableCaptionElement createCaptionElement() {
		return (TableCaptionElement) DOMImpl.impl.createElement(documentFor(),
				TableCaptionElement.TAG);
	}

	/**
	 * Creates a 'change' event.
	 */
	default NativeEvent createChangeEvent() {
		return createHtmlEvent(BrowserEvents.CHANGE, false, true);
	}

	/**
	 * Creates an &lt;input type='checkbox'&gt; element.
	 * 
	 * @return the newly created element
	 */
	default InputElement createCheckInputElement() {
		return DOMImpl.impl.createCheckInputElement(documentFor());
	}

	/**
	 * Creates a 'click' event.
	 * 
	 * <p>
	 * Note that this method does not allow the event's 'button' field to be
	 * specified, because not all browsers support it reliably for click events.
	 * </p>
	 * 
	 * @param detail
	 *            the event's detail property
	 * @param screenX
	 *            the event's screen-relative x-position
	 * @param screenY
	 *            the event's screen-relative y-position
	 * @param clientX
	 *            the event's client-relative x-position
	 * @param clientY
	 *            the event's client-relative y-position
	 * @param ctrlKey
	 *            <code>true</code> if the ctrl key is depressed
	 * @param altKey
	 *            <code>true</code> if the alt key is depressed
	 * @param shiftKey
	 *            <code>true</code> if the shift key is depressed
	 * @param metaKey
	 *            <code>true</code> if the meta key is depressed
	 * @return the event object
	 */
	default NativeEvent createClickEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey) {
		// We disallow setting the button here, because IE doesn't provide the
		// button property for click events.
		return createMouseEvent(BrowserEvents.CLICK, true, true, detail,
				screenX, screenY, clientX, clientY, ctrlKey, altKey, shiftKey,
				metaKey, NativeEvent.BUTTON_LEFT, null);
	}

	/**
	 * Creates a &lt;col&gt; element.
	 * 
	 * @return the newly created element
	 */
	default TableColElement createColElement() {
		return (TableColElement) DOMImpl.impl.createElement(documentFor(),
				TableColElement.TAG_COL);
	}

	/**
	 * Creates a &lt;colgroup&gt; element.
	 * 
	 * @return the newly created element
	 */
	default TableColElement createColGroupElement() {
		return (TableColElement) DOMImpl.impl.createElement(documentFor(),
				TableColElement.TAG_COLGROUP);
	}

	/**
	 * Creates a 'contextmenu' event.
	 * 
	 * Note: Contextmenu events will not dispatch properly on Firefox 2 and
	 * earlier.
	 * 
	 * @return the event object
	 */
	default NativeEvent createContextMenuEvent() {
		return createHtmlEvent(BrowserEvents.CONTEXTMENU, true, true);
	}

	/**
	 * Creates a 'dblclick' event.
	 * 
	 * <p>
	 * Note that this method does not allow the event's 'button' field to be
	 * specified, because not all browsers support it reliably for click events.
	 * </p>
	 * 
	 * <p>
	 * Note that on some browsers, this may cause 'click' events to be
	 * synthesized as well.
	 * </p>
	 * 
	 * @param detail
	 *            the event's detail property
	 * @param screenX
	 *            the event's screen-relative x-position
	 * @param screenY
	 *            the event's screen-relative y-position
	 * @param clientX
	 *            the event's client-relative x-position
	 * @param clientY
	 *            the event's client-relative y-position
	 * @param ctrlKey
	 *            <code>true</code> if the ctrl key is depressed
	 * @param altKey
	 *            <code>true</code> if the alt key is depressed
	 * @param shiftKey
	 *            <code>true</code> if the shift key is depressed
	 * @param metaKey
	 *            <code>true</code> if the meta key is depressed
	 * @return the event object
	 */
	default NativeEvent createDblClickEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey) {
		// We disallow setting the button here, because IE doesn't provide the
		// button property for click events.
		return createMouseEvent(BrowserEvents.DBLCLICK, true, true, detail,
				screenX, screenY, clientX, clientY, ctrlKey, altKey, shiftKey,
				metaKey, NativeEvent.BUTTON_LEFT, null);
	}

	/**
	 * Creates a &lt;del&gt; element.
	 * 
	 * @return the newly created element
	 */
	default ModElement createDelElement() {
		return (ModElement) DOMImpl.impl.createElement(documentFor(),
				ModElement.TAG_DEL);
	}

	/**
	 * Creates a &lt;div&gt; element.
	 * 
	 * @return the newly created element
	 */
	default DivElement createDivElement() {
		return (DivElement) DOMImpl.impl.createElement(documentFor(),
				DivElement.TAG);
	}

	/**
	 * Creates a &lt;dl&gt; element.
	 * 
	 * @return the newly created element
	 */
	default DListElement createDLElement() {
		return (DListElement) DOMImpl.impl.createElement(documentFor(),
				DListElement.TAG);
	}

	/**
	 * Creates a new element.
	 * 
	 * @param tagName
	 *            the tag name of the element to be created
	 * @return the newly created element
	 */
	default Element createElement(String tagName) {
		return DOMImpl.impl.createElement(documentFor(), tagName);
	}

	/**
	 * Creates an 'error' event.
	 * 
	 * @return the event object
	 */
	default NativeEvent createErrorEvent() {
		return createHtmlEvent(BrowserEvents.ERROR, false, false);
	}

	/**
	 * Creates a &lt;fieldset&gt; element.
	 * 
	 * @return the newly created element
	 */
	default FieldSetElement createFieldSetElement() {
		return (FieldSetElement) DOMImpl.impl.createElement(documentFor(),
				FieldSetElement.TAG);
	}

	/**
	 * Creates an &lt;input type='file'&gt; element.
	 * 
	 * @return the newly created element
	 */
	default InputElement createFileInputElement() {
		return DOMImpl.impl.createInputElement(documentFor(), "file");
	}

	/**
	 * Creates a 'focus' event.
	 * 
	 * @return the event object
	 */
	default NativeEvent createFocusEvent() {
		return createHtmlEvent(BrowserEvents.FOCUS, false, false);
	}

	/**
	 * Creates a &lt;form&gt; element.
	 * 
	 * @return the newly created element
	 */
	default FormElement createFormElement() {
		return (FormElement) DOMImpl.impl.createElement(documentFor(),
				FormElement.TAG);
	}

	/**
	 * Creates a &lt;frame&gt; element.
	 * 
	 * @return the newly created element
	 */
	default FrameElement createFrameElement() {
		return (FrameElement) DOMImpl.impl.createElement(documentFor(),
				FrameElement.TAG);
	}

	/**
	 * Creates a &lt;frameset&gt; element.
	 * 
	 * @return the newly created element
	 */
	default FrameSetElement createFrameSetElement() {
		return (FrameSetElement) DOMImpl.impl.createElement(documentFor(),
				FrameSetElement.TAG);
	}

	/**
	 * Creates a &lt;head&gt; element.
	 * 
	 * @return the newly created element
	 */
	default HeadElement createHeadElement() {
		return (HeadElement) DOMImpl.impl.createElement(documentFor(),
				HeadElement.TAG);
	}

	/**
	 * Creates an &lt;h(n)&gt; element.
	 * 
	 * @param n
	 *            the type of heading, from 1 to 6 inclusive
	 * @return the newly created element
	 */
	default HeadingElement createHElement(int n) {
		assert (n >= 1) && (n <= 6);
		return (HeadingElement) DOMImpl.impl.createElement(documentFor(), "h" + n);
	}

	/**
	 * Creates an &lt;input type='hidden'&gt; element.
	 * 
	 * @return the newly created element
	 */
	default InputElement createHiddenInputElement() {
		return DOMImpl.impl.createInputElement(documentFor(), "hidden");
	}

	/**
	 * Creates an &lt;hr&gt; element.
	 * 
	 * @return the newly created element
	 */
	default HRElement createHRElement() {
		return (HRElement) DOMImpl.impl.createElement(documentFor(), HRElement.TAG);
	}

	/**
	 * Creates an event.
	 * 
	 * <p>
	 * While this method may be used to create events directly, it is generally
	 * preferable to use existing helper methods such as
	 * {@link #createFocusEvent()}.
	 * </p>
	 * 
	 * <p>
	 * Also, note that on Internet Explorer the 'canBubble' and 'cancelable'
	 * arguments will be ignored (the event's behavior is inferred by the
	 * browser based upon its type).
	 * </p>
	 * 
	 * @param type
	 *            the type of event (e.g., BrowserEvents.FOCUS,
	 *            BrowserEvents.LOAD, etc)
	 * @param canBubble
	 *            <code>true</code> if the event should bubble
	 * @param cancelable
	 *            <code>true</code> if the event should be cancelable
	 * @return the event object
	 */
	default NativeEvent createHtmlEvent(String type, boolean canBubble,
			boolean cancelable) {
		return DOMImpl.impl.createHtmlEvent(documentFor(), type, canBubble,
				cancelable);
	}

	/**
	 * Creates an &lt;iframe&gt; element.
	 * 
	 * @return the newly created element
	 */
	default IFrameElement createIFrameElement() {
		return (IFrameElement) DOMImpl.impl.createElement(documentFor(),
				IFrameElement.TAG);
	}

	/**
	 * Creates an &lt;img&gt; element.
	 * 
	 * @return the newly created element
	 */
	default ImageElement createImageElement() {
		return (ImageElement) DOMImpl.impl.createElement(documentFor(),
				ImageElement.TAG);
	}

	/**
	 * Creates an &lt;input type='image'&gt; element.
	 * 
	 * @return the newly created element
	 */
	default InputElement createImageInputElement() {
		return DOMImpl.impl.createInputElement(documentFor(), "image");
	}

	/**
	 * Creates an 'input' event.
	 */
	default NativeEvent createInputEvent() {
		return createHtmlEvent(BrowserEvents.INPUT, true, false);
	}

	/**
	 * Creates an &lt;ins&gt; element.
	 * 
	 * @return the newly created element
	 */
	default ModElement createInsElement() {
		return (ModElement) DOMImpl.impl.createElement(documentFor(),
				ModElement.TAG_INS);
	}

	/**
	 * Creates a key-code event ('keydown' or 'keyup').
	 * 
	 * <p>
	 * While this method may be used to create events directly, it is generally
	 * preferable to use existing helper methods such as
	 * {@link #createKeyDownEvent(boolean, boolean, boolean, boolean, int)} or
	 * {@link #createKeyUpEvent(boolean, boolean, boolean, boolean, int)}.
	 * </p>
	 * 
	 * @param type
	 *            the type of event (e.g., BrowserEvents.KEYDOWN,
	 *            BrowserEvents.KEYPRESS, etc)
	 * @param ctrlKey
	 *            <code>true</code> if the ctrl key is depressed
	 * @param altKey
	 *            <code>true</code> if the alt key is depressed
	 * @param shiftKey
	 *            <code>true</code> if the shift key is depressed
	 * @param metaKey
	 *            <code>true</code> if the meta key is depressed
	 * @param keyCode
	 *            the key-code to be set on the event
	 * @return the event object
	 */
	default NativeEvent createKeyCodeEvent(String type, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int keyCode) {
		return DOMImpl.impl.createKeyCodeEvent(documentFor(), type, ctrlKey, altKey,
				shiftKey, metaKey, keyCode);
	}

	/**
	 * Creates a 'keydown' event.
	 * 
	 * @param ctrlKey
	 *            <code>true</code> if the ctrl key is depressed
	 * @param altKey
	 *            <code>true</code> if the alt key is depressed
	 * @param shiftKey
	 *            <code>true</code> if the shift key is depressed
	 * @param metaKey
	 *            <code>true</code> if the meta key is depressed
	 * @param keyCode
	 *            the key-code to be set on the event
	 * @return the event object
	 */
	default NativeEvent createKeyDownEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode) {
		return createKeyCodeEvent(BrowserEvents.KEYDOWN, ctrlKey, altKey,
				shiftKey, metaKey, keyCode);
	}

	/**
	 * Creates a 'keydown' event.
	 * 
	 * @param ctrlKey
	 *            <code>true</code> if the ctrl key is depressed
	 * @param altKey
	 *            <code>true</code> if the alt key is depressed
	 * @param shiftKey
	 *            <code>true</code> if the shift key is depressed
	 * @param metaKey
	 *            <code>true</code> if the meta key is depressed
	 * @param keyCode
	 *            the key-code to be set on the event
	 * @param charCode
	 *            the char-code to be set on the event
	 * @return the event object
	 * 
	 * @deprecated as of GWT2.1 (keydown events don't have a charCode), use
	 *             {@link #createKeyDownEvent(boolean, boolean, boolean, boolean, int)}
	 */
	@Deprecated
	default NativeEvent createKeyDownEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return createKeyEvent(BrowserEvents.KEYDOWN, true, true, ctrlKey,
				altKey, shiftKey, metaKey, keyCode, charCode);
	}

	/**
	 * Creates a key event.
	 * 
	 * <p>
	 * While this method may be used to create events directly, it is generally
	 * preferable to use existing helper methods such as
	 * {@link #createKeyPressEvent(boolean, boolean, boolean, boolean, int, int)}
	 * .
	 * </p>
	 * 
	 * <p>
	 * Also, note that on Internet Explorer the 'canBubble' and 'cancelable'
	 * arguments will be ignored (the event's behavior is inferred by the
	 * browser based upon its type).
	 * </p>
	 * 
	 * @param type
	 *            the type of event (e.g., BrowserEvents.KEYDOWN,
	 *            BrowserEvents.KEYPRESS, etc)
	 * @param canBubble
	 *            <code>true</code> if the event should bubble
	 * @param cancelable
	 *            <code>true</code> if the event should be cancelable
	 * @param ctrlKey
	 *            <code>true</code> if the ctrl key is depressed
	 * @param altKey
	 *            <code>true</code> if the alt key is depressed
	 * @param shiftKey
	 *            <code>true</code> if the shift key is depressed
	 * @param metaKey
	 *            <code>true</code> if the meta key is depressed
	 * @param keyCode
	 *            the key-code to be set on the event
	 * @param charCode
	 *            the char-code to be set on the event
	 * @return the event object
	 * 
	 * @deprecated use
	 *             {@link #createKeyCodeEvent(String, boolean, boolean, boolean, boolean, int)}
	 *             or
	 *             {@link #createKeyPressEvent(boolean, boolean, boolean, boolean, int)}
	 */
	@Deprecated
	default NativeEvent createKeyEvent(String type, boolean canBubble,
			boolean cancelable, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return DOMImpl.impl.createKeyEvent(documentFor(), type, canBubble,
				cancelable, ctrlKey, altKey, shiftKey, metaKey, keyCode,
				charCode);
	}

	/**
	 * Creates a 'keypress' event.
	 * 
	 * @param ctrlKey
	 *            <code>true</code> if the ctrl key is depressed
	 * @param altKey
	 *            <code>true</code> if the alt key is depressed
	 * @param shiftKey
	 *            <code>true</code> if the shift key is depressed
	 * @param metaKey
	 *            <code>true</code> if the meta key is depressed
	 * @param charCode
	 *            the char-code to be set on the event
	 * @return the event object
	 */
	default NativeEvent createKeyPressEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int charCode) {
		return DOMImpl.impl.createKeyPressEvent(documentFor(), ctrlKey, altKey,
				shiftKey, metaKey, charCode);
	}

	/**
	 * Creates a 'keypress' event.
	 * 
	 * @param ctrlKey
	 *            <code>true</code> if the ctrl key is depressed
	 * @param altKey
	 *            <code>true</code> if the alt key is depressed
	 * @param shiftKey
	 *            <code>true</code> if the shift key is depressed
	 * @param metaKey
	 *            <code>true</code> if the meta key is depressed
	 * @param keyCode
	 *            the key-code to be set on the event
	 * @param charCode
	 *            the char-code to be set on the event
	 * @return the event object
	 * 
	 * @deprecated as of GWT 2.1 (keypress events don't have a keyCode), use
	 *             {@link #createKeyPressEvent(boolean, boolean, boolean, boolean, int)}
	 */
	@Deprecated
	default NativeEvent createKeyPressEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return createKeyEvent(BrowserEvents.KEYPRESS, true, true, ctrlKey,
				altKey, shiftKey, metaKey, keyCode, charCode);
	}

	/**
	 * Creates a 'keyup' event.
	 * 
	 * @param ctrlKey
	 *            <code>true</code> if the ctrl key is depressed
	 * @param altKey
	 *            <code>true</code> if the alt key is depressed
	 * @param shiftKey
	 *            <code>true</code> if the shift key is depressed
	 * @param metaKey
	 *            <code>true</code> if the meta key is depressed
	 * @param keyCode
	 *            the key-code to be set on the event
	 * @return the event object
	 */
	default NativeEvent createKeyUpEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode) {
		return createKeyCodeEvent(BrowserEvents.KEYUP, ctrlKey, altKey,
				shiftKey, metaKey, keyCode);
	}

	/**
	 * Creates a 'keyup' event.
	 * 
	 * @param ctrlKey
	 *            <code>true</code> if the ctrl key is depressed
	 * @param altKey
	 *            <code>true</code> if the alt key is depressed
	 * @param shiftKey
	 *            <code>true</code> if the shift key is depressed
	 * @param metaKey
	 *            <code>true</code> if the meta key is depressed
	 * @param keyCode
	 *            the key-code to be set on the event
	 * @param charCode
	 *            the char-code to be set on the event
	 * @return the event object
	 * 
	 * @deprecated as of GWT 2.1 (keyup events don't have a charCode), use
	 *             {@link #createKeyUpEvent(boolean, boolean, boolean, boolean, int)}
	 */
	@Deprecated
	default NativeEvent createKeyUpEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return createKeyEvent(BrowserEvents.KEYUP, true, true, ctrlKey, altKey,
				shiftKey, metaKey, keyCode, charCode);
	}

	/**
	 * Creates a &lt;label&gt; element.
	 * 
	 * @return the newly created element
	 */
	default LabelElement createLabelElement() {
		return (LabelElement) DOMImpl.impl.createElement(documentFor(),
				LabelElement.TAG);
	}

	/**
	 * Creates a &lt;legend&gt; element.
	 * 
	 * @return the newly created element
	 */
	default LegendElement createLegendElement() {
		return (LegendElement) DOMImpl.impl.createElement(documentFor(),
				LegendElement.TAG);
	}

	/**
	 * Creates a &lt;li&gt; element.
	 * 
	 * @return the newly created element
	 */
	default LIElement createLIElement() {
		return (LIElement) DOMImpl.impl.createElement(documentFor(), LIElement.TAG);
	}

	/**
	 * Creates a &lt;link&gt; element.
	 * 
	 * @return the newly created element
	 */
	default LinkElement createLinkElement() {
		return (LinkElement) DOMImpl.impl.createElement(documentFor(),
				LinkElement.TAG);
	}

	/**
	 * Creates a 'load' event.
	 * 
	 * @return the event object
	 */
	default NativeEvent createLoadEvent() {
		return createHtmlEvent(BrowserEvents.LOAD, false, false);
	}

	/**
	 * Creates a &lt;map&gt; element.
	 * 
	 * @return the newly created element
	 */
	default MapElement createMapElement() {
		return (MapElement) DOMImpl.impl.createElement(documentFor(),
				MapElement.TAG);
	}

	/**
	 * Creates a &lt;meta&gt; element.
	 * 
	 * @return the newly created element
	 */
	default MetaElement createMetaElement() {
		return (MetaElement) DOMImpl.impl.createElement(documentFor(),
				MetaElement.TAG);
	}

	/**
	 * Creates a 'mousedown' event.
	 * 
	 * @param detail
	 *            the event's detail property
	 * @param screenX
	 *            the event's screen-relative x-position
	 * @param screenY
	 *            the event's screen-relative y-position
	 * @param clientX
	 *            the event's client-relative x-position
	 * @param clientY
	 *            the event's client-relative y-position
	 * @param ctrlKey
	 *            <code>true</code> if the ctrl key is depressed
	 * @param altKey
	 *            <code>true</code> if the alt key is depressed
	 * @param shiftKey
	 *            <code>true</code> if the shift key is depressed
	 * @param metaKey
	 *            <code>true</code> if the meta key is depressed
	 * @param button
	 *            the event's button property (values from
	 *            {@link NativeEvent#BUTTON_LEFT} et al)
	 * @return the event object
	 */
	default NativeEvent createMouseDownEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button) {
		return createMouseEvent(BrowserEvents.MOUSEDOWN, true, true, detail,
				screenX, screenY, clientX, clientY, ctrlKey, altKey, shiftKey,
				metaKey, button, null);
	}

	/**
	 * Creates an mouse event.
	 * 
	 * <p>
	 * While this method may be used to create events directly, it is generally
	 * preferable to use existing helper methods such as
	 * {@link #createClickEvent(int, int, int, int, int, boolean, boolean, boolean, boolean)}
	 * .
	 * </p>
	 * 
	 * <p>
	 * Also, note that on Internet Explorer the 'canBubble' and 'cancelable'
	 * arguments will be ignored (the event's behavior is inferred by the
	 * browser based upon its type).
	 * </p>
	 * 
	 * @param type
	 *            the type of event (e.g., BrowserEvents.FOCUS,
	 *            BrowserEvents.LOAD, etc)
	 * @param canBubble
	 *            <code>true</code> if the event should bubble
	 * @param cancelable
	 *            <code>true</code> if the event should be cancelable
	 * @param detail
	 *            the event's detail property
	 * @param screenX
	 *            the event's screen-relative x-position
	 * @param screenY
	 *            the event's screen-relative y-position
	 * @param clientX
	 *            the event's client-relative x-position
	 * @param clientY
	 *            the event's client-relative y-position
	 * @param ctrlKey
	 *            <code>true</code> if the ctrl key is depressed
	 * @param altKey
	 *            <code>true</code> if the alt key is depressed
	 * @param shiftKey
	 *            <code>true</code> if the shift key is depressed
	 * @param metaKey
	 *            <code>true</code> if the meta key is depressed
	 * @param button
	 *            the event's button property (values from
	 *            {@link NativeEvent#BUTTON_LEFT} et al)
	 * @param relatedTarget
	 *            the event's related target (only relevant for mouseover and
	 *            mouseout events)
	 * @return the event object
	 */
	default NativeEvent createMouseEvent(String type, boolean canBubble,
			boolean cancelable, int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return DOMImpl.impl.createMouseEvent(documentFor(), type, canBubble,
				cancelable, detail, screenX, screenY, clientX, clientY, ctrlKey,
				altKey, shiftKey, metaKey, button, relatedTarget);
	}

	/**
	 * Creates a 'mousemove' event.
	 * 
	 * @param detail
	 *            the event's detail property
	 * @param screenX
	 *            the event's screen-relative x-position
	 * @param screenY
	 *            the event's screen-relative y-position
	 * @param clientX
	 *            the event's client-relative x-position
	 * @param clientY
	 *            the event's client-relative y-position
	 * @param ctrlKey
	 *            <code>true</code> if the ctrl key is depressed
	 * @param altKey
	 *            <code>true</code> if the alt key is depressed
	 * @param shiftKey
	 *            <code>true</code> if the shift key is depressed
	 * @param metaKey
	 *            <code>true</code> if the meta key is depressed
	 * @param button
	 *            the event's button property (values from
	 *            {@link NativeEvent#BUTTON_LEFT} et al)
	 * @return the event object
	 */
	default NativeEvent createMouseMoveEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button) {
		return createMouseEvent(BrowserEvents.MOUSEMOVE, true, true, detail,
				screenX, screenY, clientX, clientY, ctrlKey, altKey, shiftKey,
				metaKey, button, null);
	}

	/**
	 * Creates a 'mouseout' event.
	 * 
	 * Note: The 'relatedTarget' parameter will be ignored on Firefox 2 and
	 * earlier.
	 * 
	 * @param detail
	 *            the event's detail property
	 * @param screenX
	 *            the event's screen-relative x-position
	 * @param screenY
	 *            the event's screen-relative y-position
	 * @param clientX
	 *            the event's client-relative x-position
	 * @param clientY
	 *            the event's client-relative y-position
	 * @param ctrlKey
	 *            <code>true</code> if the ctrl key is depressed
	 * @param altKey
	 *            <code>true</code> if the alt key is depressed
	 * @param shiftKey
	 *            <code>true</code> if the shift key is depressed
	 * @param metaKey
	 *            <code>true</code> if the meta key is depressed
	 * @param button
	 *            the event's button property (values from
	 *            {@link NativeEvent#BUTTON_LEFT} et al)
	 * @param relatedTarget
	 *            the event's related target
	 * @return the event object
	 */
	default NativeEvent createMouseOutEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return createMouseEvent(BrowserEvents.MOUSEOUT, true, true, detail,
				screenX, screenY, clientX, clientY, ctrlKey, altKey, shiftKey,
				metaKey, button, relatedTarget);
	}

	/**
	 * Creates a 'mouseover' event.
	 * 
	 * Note: The 'relatedTarget' parameter will be ignored on Firefox 2 and
	 * earlier.
	 * 
	 * @param detail
	 *            the event's detail property
	 * @param screenX
	 *            the event's screen-relative x-position
	 * @param screenY
	 *            the event's screen-relative y-position
	 * @param clientX
	 *            the event's client-relative x-position
	 * @param clientY
	 *            the event's client-relative y-position
	 * @param ctrlKey
	 *            <code>true</code> if the ctrl key is depressed
	 * @param altKey
	 *            <code>true</code> if the alt key is depressed
	 * @param shiftKey
	 *            <code>true</code> if the shift key is depressed
	 * @param metaKey
	 *            <code>true</code> if the meta key is depressed
	 * @param button
	 *            the event's button property (values from
	 *            {@link NativeEvent#BUTTON_LEFT} et al)
	 * @param relatedTarget
	 *            the event's related target
	 * @return the event object
	 */
	default NativeEvent createMouseOverEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return createMouseEvent(BrowserEvents.MOUSEOVER, true, true, detail,
				screenX, screenY, clientX, clientY, ctrlKey, altKey, shiftKey,
				metaKey, button, relatedTarget);
	}

	/**
	 * Creates a 'mouseup' event.
	 * 
	 * @param detail
	 *            the event's detail property
	 * @param screenX
	 *            the event's screen-relative x-position
	 * @param screenY
	 *            the event's screen-relative y-position
	 * @param clientX
	 *            the event's client-relative x-position
	 * @param clientY
	 *            the event's client-relative y-position
	 * @param ctrlKey
	 *            <code>true</code> if the ctrl key is depressed
	 * @param altKey
	 *            <code>true</code> if the alt key is depressed
	 * @param shiftKey
	 *            <code>true</code> if the shift key is depressed
	 * @param metaKey
	 *            <code>true</code> if the meta key is depressed
	 * @param button
	 *            the event's button property (values from
	 *            {@link NativeEvent#BUTTON_LEFT} et al)
	 * @return the event object
	 */
	default NativeEvent createMouseUpEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button) {
		return createMouseEvent(BrowserEvents.MOUSEUP, true, true, detail,
				screenX, screenY, clientX, clientY, ctrlKey, altKey, shiftKey,
				metaKey, button, null);
	}

	/**
	 * Creates a &lt;object&gt; element.
	 * 
	 * @return the newly created element
	 */
	default ObjectElement createObjectElement() {
		return (ObjectElement) DOMImpl.impl.createElement(documentFor(),
				ObjectElement.TAG);
	}

	/**
	 * Creates an &lt;ol&gt; element.
	 * 
	 * @return the newly created element
	 */
	default OListElement createOLElement() {
		return (OListElement) DOMImpl.impl.createElement(documentFor(),
				OListElement.TAG);
	}

	/**
	 * Creates an &lt;optgroup&gt; element.
	 * 
	 * @return the newly created element
	 */
	default OptGroupElement createOptGroupElement() {
		return (OptGroupElement) DOMImpl.impl.createElement(documentFor(),
				OptGroupElement.TAG);
	}

	/**
	 * Creates an &lt;option&gt; element.
	 * 
	 * @return the newly created element
	 */
	default OptionElement createOptionElement() {
		return (OptionElement) DOMImpl.impl.createElement(documentFor(),
				OptionElement.TAG);
	}

	/**
	 * Creates a &lt;param&gt; element.
	 * 
	 * @return the newly created element
	 */
	default ParamElement createParamElement() {
		return (ParamElement) DOMImpl.impl.createElement(documentFor(),
				ParamElement.TAG);
	}

	/**
	 * Creates an &lt;input type='password'&gt; element.
	 * 
	 * @return the newly created element
	 */
	default InputElement createPasswordInputElement() {
		return DOMImpl.impl.createInputElement(documentFor(), "password");
	}

	/**
	 * Creates a &lt;p&gt; element.
	 * 
	 * @return the newly created element
	 */
	default ParagraphElement createPElement() {
		return (ParagraphElement) DOMImpl.impl.createElement(documentFor(),
				ParagraphElement.TAG);
	}

	/**
	 * Creates a &lt;pre&gt; element.
	 * 
	 * @return the newly created element
	 */
	default PreElement createPreElement() {
		return (PreElement) DOMImpl.impl.createElement(documentFor(),
				PreElement.TAG);
	}

	/**
	 * Creates a &lt;button type='button'&gt; element.
	 * 
	 * @return the newly created element
	 */
	default ButtonElement createPushButtonElement() {
		return DOMImpl.impl.createButtonElement(documentFor(), "button");
	}

	/**
	 * Creates a &lt;q&gt; element.
	 * 
	 * @return the newly created element
	 */
	default QuoteElement createQElement() {
		return (QuoteElement) DOMImpl.impl.createElement(documentFor(),
				QuoteElement.TAG_Q);
	}

	/**
	 * Creates an &lt;input type='radio'&gt; element.
	 * 
	 * @param name
	 *            the name of the radio input (used for grouping)
	 * @return the newly created element
	 */
	default InputElement createRadioInputElement(String name) {
		return DOMImpl.impl.createInputRadioElement(documentFor(), name);
	}

	/**
	 * Creates a &lt;button type='reset'&gt; element.
	 * 
	 * @return the newly created element
	 */
	default ButtonElement createResetButtonElement() {
		return DOMImpl.impl.createButtonElement(documentFor(), "reset");
	}

	/**
	 * Creates an &lt;input type='reset'&gt; element.
	 * 
	 * @return the newly created element
	 */
	default InputElement createResetInputElement() {
		return DOMImpl.impl.createInputElement(documentFor(), "reset");
	}

	/**
	 * Creates a &lt;script&gt; element.
	 * 
	 * @return the newly created element
	 */
	default ScriptElement createScriptElement() {
		return (ScriptElement) DOMImpl.impl.createElement(documentFor(),
				ScriptElement.TAG);
	}

	/**
	 * Creates a &lt;script&gt; element.
	 * 
	 * @param source
	 *            the source code to set inside the element
	 * @return the newly created element
	 */
	default ScriptElement createScriptElement(String source) {
		return DOMImpl.impl.createScriptElement(documentFor(), source);
	}

	/**
	 * Creates a 'scroll' event.
	 * 
	 * Note: Contextmenu events will not dispatch properly on Firefox 2 and
	 * earlier.
	 * 
	 * @return the event object
	 */
	default NativeEvent createScrollEvent() {
		return createHtmlEvent(BrowserEvents.SCROLL, false, false);
	}

	/**
	 * Creates a &lt;select&gt; element.
	 *
	 * @return the newly created element
	 */
	default SelectElement createSelectElement() {
		return (SelectElement) DOMImpl.impl.createElement(documentFor(),
				SelectElement.TAG);
	}

	/**
	 * Creates a &lt;select&gt; element.
	 *
	 * @param multiple
	 *            <code>true</code> to allow multiple-selection
	 * @return the newly created element
	 *
	 * @deprecatred use {@link #createSelectElement()} and call
	 *              {@link SelectElement#setMultiple(boolean)} to configure
	 *              multiple-selection.
	 */
	@Deprecated
	default SelectElement createSelectElement(boolean multiple) {
		SelectElement el = createSelectElement();
		el.setMultiple(multiple);
		return el;
	}

	/**
	 * Creates an &lt;source&gt; element.
	 * 
	 * @return the newly created element
	 */
	default SourceElement createSourceElement() {
		return (SourceElement) DOMImpl.impl.createElement(documentFor(),
				SourceElement.TAG);
	}

	/**
	 * Creates a &lt;span&gt; element.
	 * 
	 * @return the newly created element
	 */
	default SpanElement createSpanElement() {
		return (SpanElement) DOMImpl.impl.createElement(documentFor(),
				SpanElement.TAG);
	}

	/**
	 * Creates a &lt;style&gt; element.
	 * 
	 * @return the newly created element
	 */
	default StyleElement createStyleElement() {
		return (StyleElement) DOMImpl.impl.createElement(documentFor(),
				StyleElement.TAG);
	}

	/**
	 * Creates a &lt;button type='submit'&gt; element.
	 * 
	 * @return the newly created element
	 */
	default ButtonElement createSubmitButtonElement() {
		return DOMImpl.impl.createButtonElement(documentFor(), "submit");
	}

	/**
	 * Creates an &lt;input type='submit'&gt; element.
	 * 
	 * @return the newly created element
	 */
	default InputElement createSubmitInputElement() {
		return DOMImpl.impl.createInputElement(documentFor(), "submit");
	}

	/**
	 * Creates a &lt;table&gt; element.
	 * 
	 * @return the newly created element
	 */
	default TableElement createTableElement() {
		return (TableElement) DOMImpl.impl.createElement(documentFor(),
				TableElement.TAG);
	}

	/**
	 * Creates a &lt;tbody&gt; element.
	 * 
	 * @return the newly created element
	 */
	default TableSectionElement createTBodyElement() {
		return (TableSectionElement) DOMImpl.impl.createElement(documentFor(),
				TableSectionElement.TAG_TBODY);
	}

	/**
	 * Creates a &lt;td&gt; element.
	 * 
	 * @return the newly created element
	 */
	default TableCellElement createTDElement() {
		return (TableCellElement) DOMImpl.impl.createElement(documentFor(),
				TableCellElement.TAG_TD);
	}

	/**
	 * Creates a &lt;textarea&gt; element.
	 * 
	 * @return the newly created element
	 */
	default TextAreaElement createTextAreaElement() {
		return (TextAreaElement) DOMImpl.impl.createElement(documentFor(),
				TextAreaElement.TAG);
	}

	/**
	 * Creates an &lt;input type='text'&gt; element.
	 * 
	 * @return the newly created element
	 */
	default InputElement createTextInputElement() {
		return DOMImpl.impl.createInputElement(documentFor(), "text");
	}

	Text createTextNode(String data);

	/**
	 * Creates a &lt;tfoot&gt; element.
	 * 
	 * @return the newly created element
	 */
	default TableSectionElement createTFootElement() {
		return (TableSectionElement) DOMImpl.impl.createElement(documentFor(),
				TableSectionElement.TAG_TFOOT);
	}

	/**
	 * Creates a &lt;thead&gt; element.
	 * 
	 * @return the newly created element
	 */
	default TableSectionElement createTHeadElement() {
		return (TableSectionElement) DOMImpl.impl.createElement(documentFor(),
				TableSectionElement.TAG_THEAD);
	}

	/**
	 * Creates a &lt;th&gt; element.
	 * 
	 * @return the newly created element
	 */
	default TableCellElement createTHElement() {
		return (TableCellElement) DOMImpl.impl.createElement(documentFor(),
				TableCellElement.TAG_TH);
	}

	/**
	 * Creates a &lt;title&gt; element.
	 * 
	 * @return the newly created element
	 */
	default TitleElement createTitleElement() {
		return (TitleElement) DOMImpl.impl.createElement(documentFor(),
				TitleElement.TAG);
	}

	/**
	 * Creates a &lt;tr&gt; element.
	 * 
	 * @return the newly created element
	 */
	default TableRowElement createTRElement() {
		return (TableRowElement) DOMImpl.impl.createElement(documentFor(),
				TableRowElement.TAG);
	}

	/**
	 * Creates a &lt;ul&gt; element.
	 * 
	 * @return the newly created element
	 */
	default UListElement createULElement() {
		return (UListElement) DOMImpl.impl.createElement(documentFor(),
				UListElement.TAG);
	}

	Document documentFor();

	/**
	 * Creates a &lt;video&gt; element.
	 * 
	 * @return the newly created element
	 */
	default VideoElement createVideoElement() {
		return (VideoElement) DOMImpl.impl.createElement(documentFor(),
				VideoElement.TAG);
	}

	String createUniqueId();

	default void enableScrolling(boolean enable) {
		throw new UnsupportedOperationException();
	}

	BodyElement getBody();

	default int getBodyOffsetLeft() {
		throw new UnsupportedOperationException();
	}

	default int getBodyOffsetTop() {
		throw new UnsupportedOperationException();
	}

	default int getClientHeight() {
		throw new UnsupportedOperationException();
	}

	default int getClientWidth() {
		throw new UnsupportedOperationException();
	}

	default String getCompatMode() {
		throw new UnsupportedOperationException();
	}

	default Element getDocumentElement() {
		throw new UnsupportedOperationException();
	}

	default String getDomain() {
		throw new UnsupportedOperationException();
	}

	default Element getElementById(String elementId) {
		throw new UnsupportedOperationException();
	}

	default NodeList<Element> getElementsByTagName(String tagName) {
		throw new UnsupportedOperationException();
	}

	default HeadElement getHead() {
		throw new UnsupportedOperationException();
	}

	default String getReferrer() {
		throw new UnsupportedOperationException();
	}

	default int getScrollHeight() {
		throw new UnsupportedOperationException();
	}

	default int getScrollLeft() {
		throw new UnsupportedOperationException();
	}

	default int getScrollTop() {
		throw new UnsupportedOperationException();
	}

	default int getScrollWidth() {
		throw new UnsupportedOperationException();
	}

	default String getTitle() {
		throw new UnsupportedOperationException();
	}

	default String getURL() {
		throw new UnsupportedOperationException();
	}

	default void importNode(Node node, boolean deep) {
		throw new UnsupportedOperationException();
	}

	default boolean isCSS1Compat() {
		throw new UnsupportedOperationException();
	}

	default void setScrollLeft(int left) {
		throw new UnsupportedOperationException();
	}

	default void setScrollTop(int top) {
		throw new UnsupportedOperationException();
	}

	default void setTitle(String title) {
		throw new UnsupportedOperationException();
	}

	default Element getViewportElement() {
		throw new UnsupportedOperationException();
	}
}
