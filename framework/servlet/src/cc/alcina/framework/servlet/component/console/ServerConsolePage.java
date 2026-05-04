package cc.alcina.framework.servlet.component.console;

import com.google.gwt.activity.shared.PlaceUpdateable;

import cc.alcina.framework.servlet.component.console.ServerConsoleBrowser.Ui;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.activity.DirectedActivity;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.help.HelpPlace;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.NodeContext;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.ApplicationHelp;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.component.KeyboardShortcutsArea;
import cc.alcina.framework.gwt.client.place.BasePlace;

/*
 * TODO - look at an approach to prevent double-fires of say reloadServerConsole
 * - the thing is that two things can trigger that, _both_ will be true on
 * startup
 * 
 * <p>The bindings are quite complex - to try and minimise the redraws - but
 * even so, there is the issue above. Possibly the above, add filters to (say)
 * reloadServerConsole keyed off some sort of 'context original event'
 */
@TypedProperties
@Directed(tag = "page")
class ServerConsolePage extends Model.Fields
		implements ServerConsoleBrowserCommand.ClearFilter.Handler,
		ServerConsoleBrowserCommand.ShowKeyboardShortcuts.Handler,
		ModelEvents.ApplicationHelp.Handler,
		ServerConsoleBrowserCommand.ToggleHelp.Handler, Binding.TabIndexZero,
		ModelEvents.PlaceChanged.Emitter {
	/**
	 * This activity hooks the Page up to the RootArea (the general routing
	 * contract)
	 */
	@Directed.Delegating
	@Bean(PropertySource.FIELDS)
	@Registration({ DirectedActivity.class, ServerConsolePlace.class })
	static class ActivityRoute extends DirectedActivity
			// register in spite of non-public access
			implements Registration.AllSubtypes, PlaceUpdateable,
			ModelEvent.DelegatesDispatch {
		@Directed
		ServerConsolePage page;

		@Override
		public void onNodeContext(NodeContext event) {
			page = new ServerConsolePage();
		}

		@Override
		public boolean canUpdate(PlaceUpdateable otherActivity) {
			/*
			 * All place updates are handled by the Page
			 */
			return true;
		}

		@Override
		public Model provideDispatchDelegate() {
			return page;
		}
	}

	@Directed
	Header header;

	@Directed
	ServerConsoleArea reportArea;

	Ui ui;

	ServerConsolePage() {
		this.ui = Ui.get();
		this.ui.page = this;
		header = new Header(this);
		reportArea = new ServerConsoleArea(this);
		bindings().addBindHandler(ui::bindKeyboardShortcuts);
		from(ui.subtypeProperties().place())
				.emitStreamElement(ModelEvents.PlaceChanged.class);
	}

	@Override
	public void onClearFilter(ServerConsoleBrowserCommand.ClearFilter event) {
		header.mid.suggestor.clear();
		BasePlace clearedPlace = (BasePlace) Reflections
				.newInstance(Client.currentPlace().getClass());
		clearedPlace.go();
	}

	@Override
	public void onShowKeyboardShortcuts(
			ServerConsoleBrowserCommand.ShowKeyboardShortcuts event) {
		KeyboardShortcutsArea
				.show(ServerConsoleBrowser.Ui.get().getKeybindingsHandler());
	}

	@Override
	public void onApplicationHelp(ApplicationHelp event) {
		HelpPlace.toggleRoot(Ui.place().copy()).go();
	}

	@Override
	public void onToggleHelp(ServerConsoleBrowserCommand.ToggleHelp event) {
		event.reemitAs(this, ApplicationHelp.class);
	}
}
