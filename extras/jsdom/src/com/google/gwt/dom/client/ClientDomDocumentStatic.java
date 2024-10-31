package com.google.gwt.dom.client;

import java.util.List;

import cc.alcina.framework.common.client.util.RunnableSupplier;

public class ClientDomDocumentStatic {
	/**
	 * Creates an &lt;a&gt; element.
	 * 
	 * @return the newly created element
	 */
	static AnchorElement createAnchorElement(ClientDomDocument domDocument) {
		return (AnchorElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), AnchorElement.TAG);
	}

	/**
	 * Creates an &lt;area&gt; element.
	 * 
	 * @return the newly created element
	 */
	static AreaElement createAreaElement(ClientDomDocument domDocument) {
		return (AreaElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), AreaElement.TAG);
	}

	/**
	 * Creates an &lt;audio&gt; element.
	 * 
	 * @return the newly created element
	 */
	static AudioElement createAudioElement(ClientDomDocument domDocument) {
		return (AudioElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), AudioElement.TAG);
	}

	/**
	 * Creates a &lt;base&gt; element.
	 * 
	 * @return the newly created element
	 */
	static BaseElement createBaseElement(ClientDomDocument domDocument) {
		return (BaseElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), BaseElement.TAG);
	}

	/**
	 * Creates a &lt;blockquote&gt; element.
	 * 
	 * @return the newly created element
	 */
	static QuoteElement createBlockQuoteElement(ClientDomDocument domDocument) {
		return (QuoteElement) DOMImpl.impl.createElement(
				domDocument.documentFor(), QuoteElement.TAG_BLOCKQUOTE);
	}

	/**
	 * Creates a 'blur' event.
	 */
	static NativeEvent createBlurEvent(ClientDomDocument domDocument) {
		return domDocument.createHtmlEvent(BrowserEvents.BLUR, false, false);
	}

	/**
	 * Creates a &lt;br&gt; element.
	 * 
	 * @return the newly created element
	 */
	static BRElement createBRElement(ClientDomDocument domDocument) {
		return (BRElement) DOMImpl.impl.createElement(domDocument.documentFor(),
				BRElement.TAG);
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
	static ButtonElement createButtonElement(ClientDomDocument domDocument) {
		return (ButtonElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), ButtonElement.TAG);
	}

	/**
	 * Creates an &lt;input type='button'&gt; element.
	 * 
	 * @return the newly created element
	 */
	static InputElement
			createButtonInputElement(ClientDomDocument domDocument) {
		return DOMImpl.impl.createInputElement(domDocument.documentFor(),
				"button");
	}

	/**
	 * Creates a &lt;canvas&gt; element.
	 * 
	 * @return the newly created element
	 */
	static CanvasElement createCanvasElement(ClientDomDocument domDocument) {
		return (CanvasElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), CanvasElement.TAG);
	}

	/**
	 * Creates a &lt;caption&gt; element.
	 * 
	 * @return the newly created element
	 */
	static TableCaptionElement
			createCaptionElement(ClientDomDocument domDocument) {
		return (TableCaptionElement) DOMImpl.impl.createElement(
				domDocument.documentFor(), TableCaptionElement.TAG);
	}

	/**
	 * Creates a 'change' event.
	 */
	static NativeEvent createChangeEvent(ClientDomDocument domDocument) {
		return domDocument.createHtmlEvent(BrowserEvents.CHANGE, false, true);
	}

	/**
	 * Creates an &lt;input type='checkbox'&gt; element.
	 * 
	 * @return the newly created element
	 */
	static InputElement createCheckInputElement(ClientDomDocument domDocument) {
		return DOMImpl.impl.createCheckInputElement(domDocument.documentFor());
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
	static NativeEvent createClickEvent(ClientDomDocument domDocument,
			int detail, int screenX, int screenY, int clientX, int clientY,
			boolean ctrlKey, boolean altKey, boolean shiftKey,
			boolean metaKey) {
		// We disallow setting the button here, because IE doesn't provide the
		// button property for click events.
		return domDocument.createMouseEvent(BrowserEvents.CLICK, true, true,
				detail, screenX, screenY, clientX, clientY, ctrlKey, altKey,
				shiftKey, metaKey, NativeEvent.BUTTON_LEFT, null);
	}

	/**
	 * Creates a &lt;col&gt; element.
	 * 
	 * @return the newly created element
	 */
	static TableColElement createColElement(ClientDomDocument domDocument) {
		return (TableColElement) DOMImpl.impl.createElement(
				domDocument.documentFor(), TableColElement.TAG_COL);
	}

	/**
	 * Creates a &lt;colgroup&gt; element.
	 * 
	 * @return the newly created element
	 */
	static TableColElement
			createColGroupElement(ClientDomDocument domDocument) {
		return (TableColElement) DOMImpl.impl.createElement(
				domDocument.documentFor(), TableColElement.TAG_COLGROUP);
	}

	/**
	 * Creates a 'contextmenu' event.
	 * 
	 * Note: Contextmenu events will not dispatch properly on Firefox 2 and
	 * earlier.
	 * 
	 * @return the event object
	 */
	static NativeEvent createContextMenuEvent(ClientDomDocument domDocument) {
		return domDocument.createHtmlEvent(BrowserEvents.CONTEXTMENU, true,
				true);
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
	static NativeEvent createDblClickEvent(ClientDomDocument domDocument,
			int detail, int screenX, int screenY, int clientX, int clientY,
			boolean ctrlKey, boolean altKey, boolean shiftKey,
			boolean metaKey) {
		// We disallow setting the button here, because IE doesn't provide the
		// button property for click events.
		return domDocument.createMouseEvent(BrowserEvents.DBLCLICK, true, true,
				detail, screenX, screenY, clientX, clientY, ctrlKey, altKey,
				shiftKey, metaKey, NativeEvent.BUTTON_LEFT, null);
	}

	/**
	 * Creates a &lt;del&gt; element.
	 * 
	 * @return the newly created element
	 */
	static ModElement createDelElement(ClientDomDocument domDocument) {
		return (ModElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), ModElement.TAG_DEL);
	}

	/**
	 * Creates a &lt;div&gt; element.
	 * 
	 * @return the newly created element
	 */
	static DivElement createDivElement(ClientDomDocument domDocument) {
		return (DivElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), DivElement.TAG);
	}

	/**
	 * Creates a &lt;dl&gt; element.
	 * 
	 * @return the newly created element
	 */
	static DListElement createDLElement(ClientDomDocument domDocument) {
		return (DListElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), DListElement.TAG);
	}

	/**
	 * Creates a new element.
	 * 
	 * @param tagName
	 *            the tag name of the element to be created
	 * @return the newly created element
	 */
	static Element createElement(ClientDomDocument domDocument,
			String tagName) {
		return DOMImpl.impl.createElement(domDocument.documentFor(), tagName);
	}

	/**
	 * Creates an 'error' event.
	 * 
	 * @return the event object
	 */
	static NativeEvent createErrorEvent(ClientDomDocument domDocument) {
		return domDocument.createHtmlEvent(BrowserEvents.ERROR, false, false);
	}

	/**
	 * Creates a &lt;fieldset&gt; element.
	 * 
	 * @return the newly created element
	 */
	static FieldSetElement
			createFieldSetElement(ClientDomDocument domDocument) {
		return (FieldSetElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), FieldSetElement.TAG);
	}

	/**
	 * Creates an &lt;input type='file'&gt; element.
	 * 
	 * @return the newly created element
	 */
	static InputElement createFileInputElement(ClientDomDocument domDocument) {
		return DOMImpl.impl.createInputElement(domDocument.documentFor(),
				"file");
	}

	/**
	 * Creates a 'focus' event.
	 * 
	 * @return the event object
	 */
	static NativeEvent createFocusEvent(ClientDomDocument domDocument) {
		return domDocument.createHtmlEvent(BrowserEvents.FOCUS, false, false);
	}

	/**
	 * Creates a &lt;form&gt; element.
	 * 
	 * @return the newly created element
	 */
	static FormElement createFormElement(ClientDomDocument domDocument) {
		return (FormElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), FormElement.TAG);
	}

	/**
	 * Creates a &lt;frame&gt; element.
	 * 
	 * @return the newly created element
	 */
	static FrameElement createFrameElement(ClientDomDocument domDocument) {
		return (FrameElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), FrameElement.TAG);
	}

	/**
	 * Creates a &lt;frameset&gt; element.
	 * 
	 * @return the newly created element
	 */
	static FrameSetElement
			createFrameSetElement(ClientDomDocument domDocument) {
		return (FrameSetElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), FrameSetElement.TAG);
	}

	/**
	 * Creates a &lt;head&gt; element.
	 * 
	 * @return the newly created element
	 */
	static HeadElement createHeadElement(ClientDomDocument domDocument) {
		return (HeadElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), HeadElement.TAG);
	}

	/**
	 * Creates an &lt;h(n)&gt; element.
	 * 
	 * @param n
	 *            the type of heading, from 1 to 6 inclusive
	 * @return the newly created element
	 */
	static HeadingElement createHElement(ClientDomDocument domDocument, int n) {
		assert (n >= 1) && (n <= 6);
		return (HeadingElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), "h" + n);
	}

	/**
	 * Creates an &lt;input type='hidden'&gt; element.
	 * 
	 * @return the newly created element
	 */
	static InputElement
			createHiddenInputElement(ClientDomDocument domDocument) {
		return DOMImpl.impl.createInputElement(domDocument.documentFor(),
				"hidden");
	}

	/**
	 * Creates an &lt;hr&gt; element.
	 * 
	 * @return the newly created element
	 */
	static HRElement createHRElement(ClientDomDocument domDocument) {
		return (HRElement) DOMImpl.impl.createElement(domDocument.documentFor(),
				HRElement.TAG);
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
	static NativeEvent createHtmlEvent(ClientDomDocument domDocument,
			String type, boolean canBubble, boolean cancelable) {
		return DOMImpl.impl.createHtmlEvent(domDocument.documentFor(), type,
				canBubble, cancelable);
	}

	/**
	 * Creates an &lt;iframe&gt; element.
	 * 
	 * @return the newly created element
	 */
	static IFrameElement createIFrameElement(ClientDomDocument domDocument) {
		return (IFrameElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), IFrameElement.TAG);
	}

	/**
	 * Creates an &lt;img&gt; element.
	 * 
	 * @return the newly created element
	 */
	static ImageElement createImageElement(ClientDomDocument domDocument) {
		return (ImageElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), ImageElement.TAG);
	}

	/**
	 * Creates an &lt;input type='image'&gt; element.
	 * 
	 * @return the newly created element
	 */
	static InputElement createImageInputElement(ClientDomDocument domDocument) {
		return DOMImpl.impl.createInputElement(domDocument.documentFor(),
				"image");
	}

	/**
	 * Creates an 'input' event.
	 */
	static NativeEvent createInputEvent(ClientDomDocument domDocument) {
		return domDocument.createHtmlEvent(BrowserEvents.INPUT, true, false);
	}

	/**
	 * Creates an &lt;ins&gt; element.
	 * 
	 * @return the newly created element
	 */
	static ModElement createInsElement(ClientDomDocument domDocument) {
		return (ModElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), ModElement.TAG_INS);
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
	static NativeEvent createKeyCodeEvent(ClientDomDocument domDocument,
			String type, boolean ctrlKey, boolean altKey, boolean shiftKey,
			boolean metaKey, int keyCode) {
		return DOMImpl.impl.createKeyCodeEvent(domDocument.documentFor(), type,
				ctrlKey, altKey, shiftKey, metaKey, keyCode);
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
	static NativeEvent createKeyDownEvent(ClientDomDocument domDocument,
			boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey,
			int keyCode) {
		return domDocument.createKeyCodeEvent(BrowserEvents.KEYDOWN, ctrlKey,
				altKey, shiftKey, metaKey, keyCode);
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
	static NativeEvent createKeyDownEvent(ClientDomDocument domDocument,
			boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey,
			int keyCode, int charCode) {
		return domDocument.createKeyEvent(BrowserEvents.KEYDOWN, true, true,
				ctrlKey, altKey, shiftKey, metaKey, keyCode, charCode);
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
	static NativeEvent createKeyEvent(ClientDomDocument domDocument,
			String type, boolean canBubble, boolean cancelable, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int keyCode,
			int charCode) {
		return DOMImpl.impl.createKeyEvent(domDocument.documentFor(), type,
				canBubble, cancelable, ctrlKey, altKey, shiftKey, metaKey,
				keyCode, charCode);
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
	static NativeEvent createKeyPressEvent(ClientDomDocument domDocument,
			boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey,
			int charCode) {
		return DOMImpl.impl.createKeyPressEvent(domDocument.documentFor(),
				ctrlKey, altKey, shiftKey, metaKey, charCode);
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
	static NativeEvent createKeyPressEvent(ClientDomDocument domDocument,
			boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey,
			int keyCode, int charCode) {
		return domDocument.createKeyEvent(BrowserEvents.KEYPRESS, true, true,
				ctrlKey, altKey, shiftKey, metaKey, keyCode, charCode);
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
	static NativeEvent createKeyUpEvent(ClientDomDocument domDocument,
			boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey,
			int keyCode) {
		return domDocument.createKeyCodeEvent(BrowserEvents.KEYUP, ctrlKey,
				altKey, shiftKey, metaKey, keyCode);
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
	static NativeEvent createKeyUpEvent(ClientDomDocument domDocument,
			boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey,
			int keyCode, int charCode) {
		return domDocument.createKeyEvent(BrowserEvents.KEYUP, true, true,
				ctrlKey, altKey, shiftKey, metaKey, keyCode, charCode);
	}

	/**
	 * Creates a &lt;label&gt; element.
	 * 
	 * @return the newly created element
	 */
	static LabelElement createLabelElement(ClientDomDocument domDocument) {
		return (LabelElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), LabelElement.TAG);
	}

	/**
	 * Creates a &lt;legend&gt; element.
	 * 
	 * @return the newly created element
	 */
	static LegendElement createLegendElement(ClientDomDocument domDocument) {
		return (LegendElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), LegendElement.TAG);
	}

	/**
	 * Creates a &lt;li&gt; element.
	 * 
	 * @return the newly created element
	 */
	static LIElement createLIElement(ClientDomDocument domDocument) {
		return (LIElement) DOMImpl.impl.createElement(domDocument.documentFor(),
				LIElement.TAG);
	}

	/**
	 * Creates a &lt;link&gt; element.
	 * 
	 * @return the newly created element
	 */
	static LinkElement createLinkElement(ClientDomDocument domDocument) {
		return (LinkElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), LinkElement.TAG);
	}

	/**
	 * Creates a 'load' event.
	 * 
	 * @return the event object
	 */
	static NativeEvent createLoadEvent(ClientDomDocument domDocument) {
		return domDocument.createHtmlEvent(BrowserEvents.LOAD, false, false);
	}

	/**
	 * Creates a &lt;map&gt; element.
	 * 
	 * @return the newly created element
	 */
	static MapElement createMapElement(ClientDomDocument domDocument) {
		return (MapElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), MapElement.TAG);
	}

	/**
	 * Creates a &lt;meta&gt; element.
	 * 
	 * @return the newly created element
	 */
	static MetaElement createMetaElement(ClientDomDocument domDocument) {
		return (MetaElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), MetaElement.TAG);
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
	static NativeEvent createMouseDownEvent(ClientDomDocument domDocument,
			int detail, int screenX, int screenY, int clientX, int clientY,
			boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey,
			int button) {
		return domDocument.createMouseEvent(BrowserEvents.MOUSEDOWN, true, true,
				detail, screenX, screenY, clientX, clientY, ctrlKey, altKey,
				shiftKey, metaKey, button, null);
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
	static NativeEvent createMouseEvent(ClientDomDocument domDocument,
			String type, boolean canBubble, boolean cancelable, int detail,
			int screenX, int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return DOMImpl.impl.createMouseEvent(domDocument.documentFor(), type,
				canBubble, cancelable, detail, screenX, screenY, clientX,
				clientY, ctrlKey, altKey, shiftKey, metaKey, button,
				relatedTarget);
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
	static NativeEvent createMouseMoveEvent(ClientDomDocument domDocument,
			int detail, int screenX, int screenY, int clientX, int clientY,
			boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey,
			int button) {
		return domDocument.createMouseEvent(BrowserEvents.MOUSEMOVE, true, true,
				detail, screenX, screenY, clientX, clientY, ctrlKey, altKey,
				shiftKey, metaKey, button, null);
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
	static NativeEvent createMouseOutEvent(ClientDomDocument domDocument,
			int detail, int screenX, int screenY, int clientX, int clientY,
			boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey,
			int button, Element relatedTarget) {
		return domDocument.createMouseEvent(BrowserEvents.MOUSEOUT, true, true,
				detail, screenX, screenY, clientX, clientY, ctrlKey, altKey,
				shiftKey, metaKey, button, relatedTarget);
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
	static NativeEvent createMouseOverEvent(ClientDomDocument domDocument,
			int detail, int screenX, int screenY, int clientX, int clientY,
			boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey,
			int button, Element relatedTarget) {
		return domDocument.createMouseEvent(BrowserEvents.MOUSEOVER, true, true,
				detail, screenX, screenY, clientX, clientY, ctrlKey, altKey,
				shiftKey, metaKey, button, relatedTarget);
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
	static NativeEvent createMouseUpEvent(ClientDomDocument domDocument,
			int detail, int screenX, int screenY, int clientX, int clientY,
			boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey,
			int button) {
		return domDocument.createMouseEvent(BrowserEvents.MOUSEUP, true, true,
				detail, screenX, screenY, clientX, clientY, ctrlKey, altKey,
				shiftKey, metaKey, button, null);
	}

	/**
	 * Creates a &lt;object&gt; element.
	 * 
	 * @return the newly created element
	 */
	static ObjectElement createObjectElement(ClientDomDocument domDocument) {
		return (ObjectElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), ObjectElement.TAG);
	}

	/**
	 * Creates an &lt;ol&gt; element.
	 * 
	 * @return the newly created element
	 */
	static OListElement createOLElement(ClientDomDocument domDocument) {
		return (OListElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), OListElement.TAG);
	}

	/**
	 * Creates an &lt;optgroup&gt; element.
	 * 
	 * @return the newly created element
	 */
	static OptGroupElement
			createOptGroupElement(ClientDomDocument domDocument) {
		return (OptGroupElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), OptGroupElement.TAG);
	}

	/**
	 * Creates an &lt;option&gt; element.
	 * 
	 * @return the newly created element
	 */
	static OptionElement createOptionElement(ClientDomDocument domDocument) {
		return (OptionElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), OptionElement.TAG);
	}

	/**
	 * Creates a &lt;param&gt; element.
	 * 
	 * @return the newly created element
	 */
	static ParamElement createParamElement(ClientDomDocument domDocument) {
		return (ParamElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), ParamElement.TAG);
	}

	/**
	 * Creates an &lt;input type='password'&gt; element.
	 * 
	 * @return the newly created element
	 */
	static InputElement
			createPasswordInputElement(ClientDomDocument domDocument) {
		return DOMImpl.impl.createInputElement(domDocument.documentFor(),
				"password");
	}

	/**
	 * Creates a &lt;p&gt; element.
	 * 
	 * @return the newly created element
	 */
	static ParagraphElement createPElement(ClientDomDocument domDocument) {
		return (ParagraphElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), ParagraphElement.TAG);
	}

	/**
	 * Creates a &lt;pre&gt; element.
	 * 
	 * @return the newly created element
	 */
	static PreElement createPreElement(ClientDomDocument domDocument) {
		return (PreElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), PreElement.TAG);
	}

	/**
	 * Creates a &lt;button type='button'&gt; element.
	 * 
	 * @return the newly created element
	 */
	static ButtonElement
			createPushButtonElement(ClientDomDocument domDocument) {
		return DOMImpl.impl.createButtonElement(domDocument.documentFor(),
				"button");
	}

	/**
	 * Creates a &lt;q&gt; element.
	 * 
	 * @return the newly created element
	 */
	static QuoteElement createQElement(ClientDomDocument domDocument) {
		return (QuoteElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), QuoteElement.TAG_Q);
	}

	/**
	 * Creates an &lt;input type='radio'&gt; element.
	 * 
	 * @param name
	 *            the name of the radio input (used for grouping)
	 * @return the newly created element
	 */
	static InputElement createRadioInputElement(ClientDomDocument domDocument,
			String name) {
		return DOMImpl.impl.createInputRadioElement(domDocument.documentFor(),
				name);
	}

	/**
	 * Creates a &lt;button type='reset'&gt; element.
	 * 
	 * @return the newly created element
	 */
	static ButtonElement
			createResetButtonElement(ClientDomDocument domDocument) {
		return DOMImpl.impl.createButtonElement(domDocument.documentFor(),
				"reset");
	}

	/**
	 * Creates an &lt;input type='reset'&gt; element.
	 * 
	 * @return the newly created element
	 */
	static InputElement createResetInputElement(ClientDomDocument domDocument) {
		return DOMImpl.impl.createInputElement(domDocument.documentFor(),
				"reset");
	}

	/**
	 * Creates a &lt;script&gt; element.
	 * 
	 * @return the newly created element
	 */
	static ScriptElement createScriptElement(ClientDomDocument domDocument) {
		return (ScriptElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), ScriptElement.TAG);
	}

	/**
	 * Creates a &lt;script&gt; element.
	 * 
	 * @param source
	 *            the source code to set inside the element
	 * @return the newly created element
	 */
	static ScriptElement createScriptElement(ClientDomDocument domDocument,
			String source) {
		return DOMImpl.impl.createScriptElement(domDocument.documentFor(),
				source);
	}

	/**
	 * Creates a 'scroll' event.
	 * 
	 * Note: Contextmenu events will not dispatch properly on Firefox 2 and
	 * earlier.
	 * 
	 * @return the event object
	 */
	static NativeEvent createScrollEvent(ClientDomDocument domDocument) {
		return domDocument.createHtmlEvent(BrowserEvents.SCROLL, false, false);
	}

	/**
	 * Creates a &lt;select&gt; element.
	 *
	 * @return the newly created element
	 */
	static SelectElement createSelectElement(ClientDomDocument domDocument) {
		return (SelectElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), SelectElement.TAG);
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
	static SelectElement createSelectElement(ClientDomDocument domDocument,
			boolean multiple) {
		SelectElement el = domDocument.createSelectElement();
		el.setMultiple(multiple);
		return el;
	}

	/**
	 * Creates an &lt;source&gt; element.
	 * 
	 * @return the newly created element
	 */
	static SourceElement createSourceElement(ClientDomDocument domDocument) {
		return (SourceElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), SourceElement.TAG);
	}

	/**
	 * Creates a &lt;span&gt; element.
	 * 
	 * @return the newly created element
	 */
	static SpanElement createSpanElement(ClientDomDocument domDocument) {
		return (SpanElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), SpanElement.TAG);
	}

	/**
	 * Creates a &lt;style&gt; element.
	 * 
	 * @return the newly created element
	 */
	static StyleElement createStyleElement(ClientDomDocument domDocument) {
		return (StyleElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), StyleElement.TAG);
	}

	/**
	 * Creates a &lt;button type='submit'&gt; element.
	 * 
	 * @return the newly created element
	 */
	static ButtonElement
			createSubmitButtonElement(ClientDomDocument domDocument) {
		return DOMImpl.impl.createButtonElement(domDocument.documentFor(),
				"submit");
	}

	/**
	 * Creates an &lt;input type='submit'&gt; element.
	 * 
	 * @return the newly created element
	 */
	static InputElement
			createSubmitInputElement(ClientDomDocument domDocument) {
		return DOMImpl.impl.createInputElement(domDocument.documentFor(),
				"submit");
	}

	/**
	 * Creates a &lt;table&gt; element.
	 * 
	 * @return the newly created element
	 */
	static TableElement createTableElement(ClientDomDocument domDocument) {
		return (TableElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), TableElement.TAG);
	}

	/**
	 * Creates a &lt;tbody&gt; element.
	 * 
	 * @return the newly created element
	 */
	static TableSectionElement
			createTBodyElement(ClientDomDocument domDocument) {
		return (TableSectionElement) DOMImpl.impl.createElement(
				domDocument.documentFor(), TableSectionElement.TAG_TBODY);
	}

	/**
	 * Creates a &lt;td&gt; element.
	 * 
	 * @return the newly created element
	 */
	static TableCellElement createTDElement(ClientDomDocument domDocument) {
		return (TableCellElement) DOMImpl.impl.createElement(
				domDocument.documentFor(), TableCellElement.TAG_TD);
	}

	/**
	 * Creates a &lt;textarea&gt; element.
	 * 
	 * @return the newly created element
	 */
	static TextAreaElement
			createTextAreaElement(ClientDomDocument domDocument) {
		return (TextAreaElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), TextAreaElement.TAG);
	}

	/**
	 * Creates an &lt;input type='text'&gt; element.
	 * 
	 * @return the newly created element
	 */
	static InputElement createTextInputElement(ClientDomDocument domDocument) {
		return DOMImpl.impl.createInputElement(domDocument.documentFor(),
				"text");
	}

	/**
	 * Creates a &lt;tfoot&gt; element.
	 * 
	 * @return the newly created element
	 */
	static TableSectionElement
			createTFootElement(ClientDomDocument domDocument) {
		return (TableSectionElement) DOMImpl.impl.createElement(
				domDocument.documentFor(), TableSectionElement.TAG_TFOOT);
	}

	/**
	 * Creates a &lt;thead&gt; element.
	 * 
	 * @return the newly created element
	 */
	static TableSectionElement
			createTHeadElement(ClientDomDocument domDocument) {
		return (TableSectionElement) DOMImpl.impl.createElement(
				domDocument.documentFor(), TableSectionElement.TAG_THEAD);
	}

	/**
	 * Creates a &lt;th&gt; element.
	 * 
	 * @return the newly created element
	 */
	static TableCellElement createTHElement(ClientDomDocument domDocument) {
		return (TableCellElement) DOMImpl.impl.createElement(
				domDocument.documentFor(), TableCellElement.TAG_TH);
	}

	/**
	 * Creates a &lt;title&gt; element.
	 * 
	 * @return the newly created element
	 */
	static TitleElement createTitleElement(ClientDomDocument domDocument) {
		return (TitleElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), TitleElement.TAG);
	}

	/**
	 * Creates a &lt;tr&gt; element.
	 * 
	 * @return the newly created element
	 */
	static TableRowElement createTRElement(ClientDomDocument domDocument) {
		return (TableRowElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), TableRowElement.TAG);
	}

	/**
	 * Creates a &lt;ul&gt; element.
	 * 
	 * @return the newly created element
	 */
	static UListElement createULElement(ClientDomDocument domDocument) {
		return (UListElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), UListElement.TAG);
	}

	/**
	 * Creates a &lt;video&gt; element.
	 * 
	 * @return the newly created element
	 */
	static VideoElement createVideoElement(ClientDomDocument domDocument) {
		return (VideoElement) DOMImpl.impl
				.createElement(domDocument.documentFor(), VideoElement.TAG);
	}

	static void enableScrolling(ClientDomDocument domDocument, boolean enable) {
		throw new UnsupportedOperationException();
	}

	static int getBodyOffsetLeft(ClientDomDocument domDocument) {
		throw new UnsupportedOperationException();
	}

	static int getBodyOffsetTop(ClientDomDocument domDocument) {
		throw new UnsupportedOperationException();
	}

	static int getClientHeight(ClientDomDocument domDocument) {
		throw new UnsupportedOperationException();
	}

	static int getClientWidth(ClientDomDocument domDocument) {
		throw new UnsupportedOperationException();
	}

	static String getCompatMode(ClientDomDocument domDocument) {
		throw new UnsupportedOperationException();
	}

	static Element getDocumentElement(ClientDomDocument domDocument) {
		throw new UnsupportedOperationException();
	}

	static String getDomain(ClientDomDocument domDocument) {
		throw new UnsupportedOperationException();
	}

	static Element getElementById(ClientDomDocument domDocument,
			String elementId) {
		throw new UnsupportedOperationException();
	}

	static NodeList<Element> getElementsByTagName(ClientDomDocument domDocument,
			String tagName) {
		throw new UnsupportedOperationException();
	}

	static HeadElement getHead(ClientDomDocument domDocument) {
		throw new UnsupportedOperationException();
	}

	static String getNodeName(ClientDomDocument domDocument) {
		return "#document";
	}

	static short getNodeType(ClientDomDocument domDocument) {
		return Node.DOCUMENT_NODE;
	}

	static String getNodeValue(ClientDomDocument domDocument) {
		return null;
	}

	static String getReferrer(ClientDomDocument domDocument) {
		throw new UnsupportedOperationException();
	}

	static int getScrollHeight(ClientDomDocument domDocument) {
		throw new UnsupportedOperationException();
	}

	static int getScrollLeft(ClientDomDocument domDocument) {
		throw new UnsupportedOperationException();
	}

	static int getScrollTop(ClientDomDocument domDocument) {
		throw new UnsupportedOperationException();
	}

	static int getScrollWidth(ClientDomDocument domDocument) {
		throw new UnsupportedOperationException();
	}

	static String getTitle(ClientDomDocument domDocument) {
		throw new UnsupportedOperationException();
	}

	static String getURL(ClientDomDocument domDocument) {
		throw new UnsupportedOperationException();
	}

	static Element getViewportElement(ClientDomDocument domDocument) {
		throw new UnsupportedOperationException();
	}

	static void importNode(ClientDomDocument domDocument, Node node,
			boolean deep) {
		throw new UnsupportedOperationException();
	}

	static boolean isCSS1Compat(ClientDomDocument domDocument) {
		throw new UnsupportedOperationException();
	}

	static void setNodeValue(ClientDomDocument domDocument, String nodeValue) {
		throw new UnsupportedOperationException();
	}

	static void setScrollLeft(ClientDomDocument domDocument, int left) {
		throw new UnsupportedOperationException();
	}

	static void setScrollTop(ClientDomDocument domDocument, int top) {
		throw new UnsupportedOperationException();
	}

	static void setTitle(ClientDomDocument domDocument, String title) {
		throw new UnsupportedOperationException();
	}

	static void invoke(ClientDomDocument domDocument, Runnable runnable,
			Class clazz, String methodName, List<Class> argumentTypes,
			List<?> arguments, boolean sync) {
		if (argumentTypes == null) {
			argumentTypes = List.of();
		}
		if (arguments == null) {
			arguments = List.of();
		}
		domDocument.invoke(RunnableSupplier.of(runnable), clazz, methodName,
				argumentTypes, arguments, sync);
	}
}
