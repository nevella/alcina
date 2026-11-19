/**
 * <h2>WIP - see dirndl-events.html as well
 * <p>
 * Currently, overlaycontainer/overlay is fairly simplistic - the container is a
 * glass in modal mode, and intercepts events that way (rather than via
 * nativepreview, such as in unused
 * cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents.EventRelativeBinding.handleConsumed()
 *
 */
// Note - event preview handling was essentially copied-and-truncated from
// GWT PopupPanel - it may be a little excessive
//
// Note - should observe source resize events and close if non-modal
//
// FIXME - dirndl 1x1j - check focus() of outside elt if popup is modal
//
// 20230120 - 1x1j - partial, need to document role of container, overlay and
// the outside events. overlaycontainer may be more like 'glass if modal, unused
// if not' - which may mean no need for the preview *cancellation* handling
// since the glass will intercept. In many ways glass is nicer - thoughts...?
//
// Note that current 'glass' doesn't prevent 'outside' handlers underneath from
// firing - but I think the real solution is to generalise *something* - say
// visibile event scope - and rework the eventbus, rather than continue hacking
// on NativePreviewEvent consumed/canceled
//
/*
 * More notes (for docs)(basically describing logic of popups and 'logical parent'):
 * @formatter:off
 *

overlays and events
* close on click outside. a popup should close on click outside unless:
    * it's a descendant popup
* events in popups should
    * bubble to something in the contents of parent overlay (well, the rerouteparentevnts)
    * bubble to originating model
* popup (not modal) overlays are essentially
    * logically owned by their originating model or popup parent model
	* so things like 'ContextResolver' come from the logicalparent (if the world were just, 
	  an overlay would be a dom child of the parent - as it is, its *logical* dirndl aspects are 
	  resolved up the parent chain, or at least intended to be.

Viewport-relative positioning constraints - a story:



Area A is rendered with Overlay.position
- Is it too tall/wide? (That's a css constraint - if it can't fit on the screen or whatever req'd fraction)
- Am I offscreen x? Fix is first align offscreen coord to viewport
- Am I offscreen y? (note, y in a text area or contenteditable should be one line away from click, not rel to 
  container elt.)
- 1. Try to scroll the container (with an animation!) Note for romcom this will require:
  - rect/scroll data of scrollable ancestor
  - rect/scroll data of ancestor chain 
  - only send event rects if they change
  - optionally extend the scroll container (with css height) - not default
- 2. If not possible, position above
- 3. If that wouldn't be 100% visible, do nothing
[the above should be a default 'position onscreen' strategy, which can be disabled]
 * @formatter:on
 *
 */
package cc.alcina.framework.gwt.client.dirndl.overlay;
