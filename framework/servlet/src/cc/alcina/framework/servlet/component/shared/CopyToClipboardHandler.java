package cc.alcina.framework.servlet.component.shared;

import cc.alcina.framework.entity.ConsoleUtil;
import cc.alcina.framework.gwt.client.dirndl.cmp.status.StatusModule;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.CopyToClipboard;

public interface CopyToClipboardHandler
		extends ModelEvents.CopyToClipboard.Handler {
	@Override
	default void onCopyToClipboard(CopyToClipboard event) {
		ConsoleUtil.copyToClipboard(event.getModel());
		StatusModule.get().showMessageTransitional("Copied to clipboard");
	}
}
