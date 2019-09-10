package com.google.gwt.dom.client;

/**
 * all methods should actually call through to domdocument_static ... when i get
 * the time
 * 
 * @author nick@alcina.cc
 *
 */
public interface DomDocument extends DomNode {
	/**
	 * Creates an &lt;a&gt; element.
	 * 
	 * @return the newly created element
	 */
	AnchorElement createAnchorElement();

	/**
	 * Creates an &lt;area&gt; element.
	 * 
	 * @return the newly created element
	 */
	AreaElement createAreaElement();

	/**
	 * Creates an &lt;audio&gt; element.
	 * 
	 * @return the newly created element
	 */
	AudioElement createAudioElement();

	/**
	 * Creates a &lt;base&gt; element.
	 * 
	 * @return the newly created element
	 */
	BaseElement createBaseElement();

	/**
	 * Creates a &lt;blockquote&gt; element.
	 * 
	 * @return the newly created element
	 */
	QuoteElement createBlockQuoteElement();

	/**
	 * Creates a 'blur' event.
	 */
	NativeEvent createBlurEvent();

	/**
	 * Creates a &lt;br&gt; element.
	 * 
	 * @return the newly created element
	 */
	BRElement createBRElement();

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
	ButtonElement createButtonElement();

	/**
	 * Creates an &lt;input type='button'&gt; element.
	 * 
	 * @return the newly created element
	 */
	InputElement createButtonInputElement();

	/**
	 * Creates a &lt;canvas&gt; element.
	 * 
	 * @return the newly created element
	 */
	CanvasElement createCanvasElement();

	/**
	 * Creates a &lt;caption&gt; element.
	 * 
	 * @return the newly created element
	 */
	TableCaptionElement createCaptionElement();

	/**
	 * Creates a 'change' event.
	 */
	NativeEvent createChangeEvent();

	/**
	 * Creates an &lt;input type='checkbox'&gt; element.
	 * 
	 * @return the newly created element
	 */
	InputElement createCheckInputElement();

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
	NativeEvent createClickEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey);

	/**
	 * Creates a &lt;col&gt; element.
	 * 
	 * @return the newly created element
	 */
	TableColElement createColElement();

	/**
	 * Creates a &lt;colgroup&gt; element.
	 * 
	 * @return the newly created element
	 */
	TableColElement createColGroupElement();

	/**
	 * Creates a 'contextmenu' event.
	 * 
	 * Note: Contextmenu events will not dispatch properly on Firefox 2 and
	 * earlier.
	 * 
	 * @return the event object
	 */
	NativeEvent createContextMenuEvent();

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
	NativeEvent createDblClickEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey);

	/**
	 * Creates a &lt;del&gt; element.
	 * 
	 * @return the newly created element
	 */
	ModElement createDelElement();

	/**
	 * Creates a &lt;div&gt; element.
	 * 
	 * @return the newly created element
	 */
	DivElement createDivElement();

	/**
	 * Creates a &lt;dl&gt; element.
	 * 
	 * @return the newly created element
	 */
	DListElement createDLElement();

	/**
	 * Creates a new element.
	 * 
	 * @param tagName
	 *            the tag name of the element to be created
	 * @return the newly created element
	 */
	Element createElement(String tagName);

	/**
	 * Creates an 'error' event.
	 * 
	 * @return the event object
	 */
	NativeEvent createErrorEvent();

	/**
	 * Creates a &lt;fieldset&gt; element.
	 * 
	 * @return the newly created element
	 */
	FieldSetElement createFieldSetElement();

	/**
	 * Creates an &lt;input type='file'&gt; element.
	 * 
	 * @return the newly created element
	 */
	InputElement createFileInputElement();

	/**
	 * Creates a 'focus' event.
	 * 
	 * @return the event object
	 */
	NativeEvent createFocusEvent();

	/**
	 * Creates a &lt;form&gt; element.
	 * 
	 * @return the newly created element
	 */
	FormElement createFormElement();

	/**
	 * Creates a &lt;frame&gt; element.
	 * 
	 * @return the newly created element
	 */
	FrameElement createFrameElement();

	/**
	 * Creates a &lt;frameset&gt; element.
	 * 
	 * @return the newly created element
	 */
	FrameSetElement createFrameSetElement();

	/**
	 * Creates a &lt;head&gt; element.
	 * 
	 * @return the newly created element
	 */
	HeadElement createHeadElement();

	/**
	 * Creates an &lt;h(n)&gt; element.
	 * 
	 * @param n
	 *            the type of heading, from 1 to 6 inclusive
	 * @return the newly created element
	 */
	HeadingElement createHElement(int n);

	/**
	 * Creates an &lt;input type='hidden'&gt; element.
	 * 
	 * @return the newly created element
	 */
	InputElement createHiddenInputElement();

	/**
	 * Creates an &lt;hr&gt; element.
	 * 
	 * @return the newly created element
	 */
	HRElement createHRElement();

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
	NativeEvent createHtmlEvent(String type, boolean canBubble,
			boolean cancelable);

	/**
	 * Creates an &lt;iframe&gt; element.
	 * 
	 * @return the newly created element
	 */
	IFrameElement createIFrameElement();

	/**
	 * Creates an &lt;img&gt; element.
	 * 
	 * @return the newly created element
	 */
	ImageElement createImageElement();

	/**
	 * Creates an &lt;input type='image'&gt; element.
	 * 
	 * @return the newly created element
	 */
	InputElement createImageInputElement();

	/**
	 * Creates an 'input' event.
	 */
	NativeEvent createInputEvent();

	/**
	 * Creates an &lt;ins&gt; element.
	 * 
	 * @return the newly created element
	 */
	ModElement createInsElement();

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
	NativeEvent createKeyCodeEvent(String type, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode);

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
	NativeEvent createKeyDownEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode);

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
	NativeEvent createKeyDownEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode);

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
	NativeEvent createKeyEvent(String type, boolean canBubble,
			boolean cancelable, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode);

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
	NativeEvent createKeyPressEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int charCode);

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
	NativeEvent createKeyPressEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode);

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
	NativeEvent createKeyUpEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode);

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
	NativeEvent createKeyUpEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode);

	/**
	 * Creates a &lt;label&gt; element.
	 * 
	 * @return the newly created element
	 */
	LabelElement createLabelElement();

	/**
	 * Creates a &lt;legend&gt; element.
	 * 
	 * @return the newly created element
	 */
	LegendElement createLegendElement();

	/**
	 * Creates a &lt;li&gt; element.
	 * 
	 * @return the newly created element
	 */
	LIElement createLIElement();

	/**
	 * Creates a &lt;link&gt; element.
	 * 
	 * @return the newly created element
	 */
	LinkElement createLinkElement();

	/**
	 * Creates a 'load' event.
	 * 
	 * @return the event object
	 */
	NativeEvent createLoadEvent();

	/**
	 * Creates a &lt;map&gt; element.
	 * 
	 * @return the newly created element
	 */
	MapElement createMapElement();

	/**
	 * Creates a &lt;meta&gt; element.
	 * 
	 * @return the newly created element
	 */
	MetaElement createMetaElement();

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
	NativeEvent createMouseDownEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button);

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
	NativeEvent createMouseEvent(String type, boolean canBubble,
			boolean cancelable, int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget);

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
	NativeEvent createMouseMoveEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button);

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
	NativeEvent createMouseOutEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget);

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
	NativeEvent createMouseOverEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget);

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
	NativeEvent createMouseUpEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button);

	/**
	 * Creates a &lt;object&gt; element.
	 * 
	 * @return the newly created element
	 */
	ObjectElement createObjectElement();

	/**
	 * Creates an &lt;ol&gt; element.
	 * 
	 * @return the newly created element
	 */
	OListElement createOLElement();

	/**
	 * Creates an &lt;optgroup&gt; element.
	 * 
	 * @return the newly created element
	 */
	OptGroupElement createOptGroupElement();

	/**
	 * Creates an &lt;option&gt; element.
	 * 
	 * @return the newly created element
	 */
	OptionElement createOptionElement();

	/**
	 * Creates a &lt;param&gt; element.
	 * 
	 * @return the newly created element
	 */
	ParamElement createParamElement();

	/**
	 * Creates an &lt;input type='password'&gt; element.
	 * 
	 * @return the newly created element
	 */
	InputElement createPasswordInputElement();

	/**
	 * Creates a &lt;p&gt; element.
	 * 
	 * @return the newly created element
	 */
	ParagraphElement createPElement();

	/**
	 * Creates a &lt;pre&gt; element.
	 * 
	 * @return the newly created element
	 */
	PreElement createPreElement();

	/**
	 * Creates a &lt;button type='button'&gt; element.
	 * 
	 * @return the newly created element
	 */
	ButtonElement createPushButtonElement();

	/**
	 * Creates a &lt;q&gt; element.
	 * 
	 * @return the newly created element
	 */
	QuoteElement createQElement();

	/**
	 * Creates an &lt;input type='radio'&gt; element.
	 * 
	 * @param name
	 *            the name of the radio input (used for grouping)
	 * @return the newly created element
	 */
	InputElement createRadioInputElement(String name);

	/**
	 * Creates a &lt;button type='reset'&gt; element.
	 * 
	 * @return the newly created element
	 */
	ButtonElement createResetButtonElement();

	/**
	 * Creates an &lt;input type='reset'&gt; element.
	 * 
	 * @return the newly created element
	 */
	InputElement createResetInputElement();

	/**
	 * Creates a &lt;script&gt; element.
	 * 
	 * @return the newly created element
	 */
	ScriptElement createScriptElement();

	/**
	 * Creates a &lt;script&gt; element.
	 * 
	 * @param source
	 *            the source code to set inside the element
	 * @return the newly created element
	 */
	ScriptElement createScriptElement(String source);

	/**
	 * Creates a 'scroll' event.
	 * 
	 * Note: Contextmenu events will not dispatch properly on Firefox 2 and
	 * earlier.
	 * 
	 * @return the event object
	 */
	NativeEvent createScrollEvent();

	/**
	 * Creates a &lt;select&gt; element.
	 *
	 * @return the newly created element
	 */
	SelectElement createSelectElement();

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
	SelectElement createSelectElement(boolean multiple);

	/**
	 * Creates an &lt;source&gt; element.
	 * 
	 * @return the newly created element
	 */
	SourceElement createSourceElement();

	/**
	 * Creates a &lt;span&gt; element.
	 * 
	 * @return the newly created element
	 */
	SpanElement createSpanElement();

	/**
	 * Creates a &lt;style&gt; element.
	 * 
	 * @return the newly created element
	 */
	StyleElement createStyleElement();

	/**
	 * Creates a &lt;button type='submit'&gt; element.
	 * 
	 * @return the newly created element
	 */
	ButtonElement createSubmitButtonElement();

	/**
	 * Creates an &lt;input type='submit'&gt; element.
	 * 
	 * @return the newly created element
	 */
	InputElement createSubmitInputElement();

	/**
	 * Creates a &lt;table&gt; element.
	 * 
	 * @return the newly created element
	 */
	TableElement createTableElement();

	/**
	 * Creates a &lt;tbody&gt; element.
	 * 
	 * @return the newly created element
	 */
	TableSectionElement createTBodyElement();

	/**
	 * Creates a &lt;td&gt; element.
	 * 
	 * @return the newly created element
	 */
	TableCellElement createTDElement();

	/**
	 * Creates a &lt;textarea&gt; element.
	 * 
	 * @return the newly created element
	 */
	TextAreaElement createTextAreaElement();

	/**
	 * Creates an &lt;input type='text'&gt; element.
	 * 
	 * @return the newly created element
	 */
	InputElement createTextInputElement();

	Text createTextNode(String data);

	/**
	 * Creates a &lt;tfoot&gt; element.
	 * 
	 * @return the newly created element
	 */
	TableSectionElement createTFootElement();

	/**
	 * Creates a &lt;thead&gt; element.
	 * 
	 * @return the newly created element
	 */
	TableSectionElement createTHeadElement();

	/**
	 * Creates a &lt;th&gt; element.
	 * 
	 * @return the newly created element
	 */
	TableCellElement createTHElement();

	/**
	 * Creates a &lt;title&gt; element.
	 * 
	 * @return the newly created element
	 */
	TitleElement createTitleElement();

	/**
	 * Creates a &lt;tr&gt; element.
	 * 
	 * @return the newly created element
	 */
	TableRowElement createTRElement();

	/**
	 * Creates a &lt;ul&gt; element.
	 * 
	 * @return the newly created element
	 */
	UListElement createULElement();

	String createUniqueId();

	/**
	 * Creates a &lt;video&gt; element.
	 * 
	 * @return the newly created element
	 */
	VideoElement createVideoElement();

	Document documentFor();

	void enableScrolling(boolean enable);

	BodyElement getBody();

	int getBodyOffsetLeft();

	int getBodyOffsetTop();

	int getClientHeight();

	int getClientWidth();

	String getCompatMode();

	Element getDocumentElement();

	String getDomain();

	Element getElementById(String elementId);

	NodeList<Element> getElementsByTagName(String tagName);

	HeadElement getHead();

	@Override
	String getNodeName();

	@Override
	short getNodeType();

	@Override
	String getNodeValue();

	String getReferrer();

	int getScrollHeight();

	int getScrollLeft();

	int getScrollTop();

	int getScrollWidth();

	String getTitle();

	String getURL();

	Element getViewportElement();

	void importNode(Node node, boolean deep);

	boolean isCSS1Compat();

	@Override
	void setNodeValue(String nodeValue);

	void setScrollLeft(int left);

	void setScrollTop(int top);

	void setTitle(String title);
}
