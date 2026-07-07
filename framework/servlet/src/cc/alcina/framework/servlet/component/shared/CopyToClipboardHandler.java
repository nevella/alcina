package cc.alcina.framework.servlet.component.shared;

import com.google.gwt.dom.client.Document;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ConsoleUtil;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.CopyToClipboard;
import cc.alcina.framework.gwt.client.dirndl.model.NotificationObservable;

public interface CopyToClipboardHandler
		extends ModelEvents.CopyToClipboard.Handler {
	@Override
	default void onCopyToClipboard(CopyToClipboard event) {
		if (Ax.isTest()) {
			ConsoleUtil.copyToClipboard(event.getModel());
		} else {
			Document.get().writeClipboardText(event.getModel());
		}
		NotificationObservable.of("Copied to clipboard").publish();
		;
	}
}
