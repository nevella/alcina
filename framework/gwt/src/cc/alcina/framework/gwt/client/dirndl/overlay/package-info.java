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
// TODO - 1x1j - check focus() of outside elt if popup is modal
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
package cc.alcina.framework.gwt.client.dirndl.overlay;
