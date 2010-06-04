/**
 * 
 */
package cc.alcina.framework.gwt.client.widget;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.impl.RichTextAreaImplSafari;

/**
 * TODO remove me when GWT RichTextArea is fixed. See #4279 (vaadin trac)
 * 
 */
class CustomWebkitRichTextArea extends RichTextAreaImplSafari {
	public CustomWebkitRichTextArea() {
		DeferredCommand.addCommand(new Command() {
			public void execute() {
				hookBlur(getElement());
			}
		});
	}

	private native void hookBlur(Element iframe)
	/*-{
		//can happen if immediately detached
		if (!(iframe && iframe.contentDocument&&iframe.contentDocument.documentElement)){
			return;
		}
	    iframe.contentDocument.documentElement.onblur = function(evt) {
	      if (iframe.__listener) {
	        iframe.__listener.@com.google.gwt.user.client.ui.Widget::onBrowserEvent(Lcom/google/gwt/user/client/Event;)(evt);
	      }
	    };
	    
	    
	}-*/;
}