package cc.alcina.framework.gwt.client.dirndl.cmp.help;

import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class Help {
	@Directed.Delegating
	public static class HeaderButton extends Model.All {
		@Directed(
			reemits = { DomEvents.Click.class,
					ModelEvents.ApplicationHelp.class })
		String helpButton = "?";
	}
}
