package cc.alcina.framework.servlet.component.shared;

import com.google.gwt.dom.client.Document;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ConsoleUtil;
import cc.alcina.framework.gwt.client.dirndl.cmp.status.StatusModule;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.CopyToClipboard;

public interface CopyToClipboardHandler
		extends ModelEvents.CopyToClipboard.Handler {
	@Override
	default void onCopyToClipboard(CopyToClipboard event) {
		if (Ax.isTest() && false) {
			ConsoleUtil.copyToClipboard(event.getModel());
		} else {
			Document.get().writeClipboardText(event.getModel());
		}
		StatusModule.get().showMessageTransitional("Copied to clipboard");
	}
}
