package cc.alcina.framework.gwt.client.dirndl.event;

import java.util.List;

public interface CommandContext {
	public interface Provider {
		List<Class<? extends CommandContext>> getContexts();
	}
}
