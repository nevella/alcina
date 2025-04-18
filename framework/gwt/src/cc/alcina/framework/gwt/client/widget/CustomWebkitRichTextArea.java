/**
 *
 */
package cc.alcina.framework.gwt.client.widget;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.impl.RichTextAreaImplSafari;

/**
 * TODO remove me when GWT RichTextArea is fixed. See #4279 (vaadin trac)
 * (readded cos looks like gwt trunk as of jan 2012 has same probs)
 *
 */
public class CustomWebkitRichTextArea extends RichTextAreaImplSafari {
	public CustomWebkitRichTextArea() {
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {
			@Override
			public void execute() {
				hookBlur(getElement());
			}
		});
	}

	private native void hookBlur(Element iframe)
	/*-{
	//can happen if immediately detached
	if (!(iframe && iframe.contentDocument && iframe.contentDocument.documentElement)) {
	  return;
	}
	iframe.contentDocument.documentElement.onblur = function(evtJso) {
	  if (iframe.__listener) {
	    var evt = @com.google.gwt.user.client.Event::new(Lcom/google/gwt/dom/client/NativeEventJso;)(evtJso);
	    iframe.__listener.@com.google.gwt.user.client.ui.Widget::onBrowserEvent(Lcom/google/gwt/user/client/Event;)(evt);
	  }
	};
	}-*/;

	// guard against instantly-detached elts
	@Override
	protected native void initElement0() /*-{
    // Most browsers don't like setting designMode until slightly _after_
    // the iframe becomes attached to the DOM. Any non-zero timeout will do
    // just fine.
    var _this = this;
    _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::initializing = true;
    setTimeout(
        $entry(function() {
          // Turn on design mode.
          var elem = _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem;
          if (elem.contentWindow && elem.contentWindow.document) {
            _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.document.designMode = 'On';

            // Send notification that the iframe has reached design mode.
            _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::onElementInitialized()();
          }
        }), 1);
	}-*/;
}