package cc.alcina.framework.servlet.component.sequence;

import com.google.gwt.activity.shared.PlaceUpdateable;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.service.InstanceQuery;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.activity.DirectedActivity;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.cmp.help.HelpPlace;
import cc.alcina.framework.gwt.client.dirndl.cmp.status.StatusModule;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.ApplicationHelp;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.component.KeyboardShortcutsArea;
import cc.alcina.framework.servlet.component.sequence.SequenceArea.Service;
import cc.alcina.framework.servlet.component.sequence.SequenceBrowser.Ui;
import cc.alcina.framework.servlet.component.sequence.SequenceBrowserCommand.ClearFilter;
import cc.alcina.framework.servlet.component.sequence.SequenceBrowserCommand.ColumnSetCycle;
import cc.alcina.framework.servlet.component.sequence.SequenceBrowserCommand.DetailDisplayCycle;
import cc.alcina.framework.servlet.component.sequence.SequenceBrowserCommand.FocusSearch;
import cc.alcina.framework.servlet.component.sequence.SequenceBrowserCommand.ShowKeyboardShortcuts;
import cc.alcina.framework.servlet.component.sequence.SequenceBrowserCommand.ToggleHelp;
import cc.alcina.framework.servlet.component.sequence.SequenceEvents.LoadSequence;
import cc.alcina.framework.servlet.component.sequence.SequenceEvents.NavigateToNewSequencePlace;
import cc.alcina.framework.servlet.component.sequence.SequenceSettings.ColumnSet;
import cc.alcina.framework.servlet.component.sequence.SequenceSettings.DetailDisplayMode;

/*
 */
@TypedProperties
@DirectedContextResolver
class Page extends Model.Fields
		implements SequenceBrowserCommand.ClearFilter.Handler,
		SequenceBrowserCommand.FocusSearch.Handler,
		SequenceBrowserCommand.ShowKeyboardShortcuts.Handler,
		ModelEvents.ApplicationHelp.Handler,
		SequenceEvents.LoadSequence.Handler,
		SequenceBrowserCommand.ToggleHelp.Handler, Binding.TabIndexZero,
		SequenceArea.Service.Provider,
		SequenceBrowserCommand.DetailDisplayCycle.Handler,
		SequenceBrowserCommand.ColumnSetCycle.Handler,
		SequenceEvents.NavigateToNewSequencePlace.Handler {
	/**
	 * This activity hooks the Page up to the RootArea (the general routing
	 * contract)
	 */
	@Directed.Delegating
	@Bean(PropertySource.FIELDS)
	@Registration({ DirectedActivity.class, SequencePlace.class })
	static class ActivityRoute extends DirectedActivity
			// register in spite of non-public access
			implements Registration.AllSubtypes, PlaceUpdateable,
			ModelEvent.DelegatesDispatch {
		@Directed
		Page page;

		@Override
		public void onBeforeRender(BeforeRender event) {
			page = new Page();
			super.onBeforeRender(event);
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

	PackageProperties._Page.InstanceProperties properties() {
		return PackageProperties.page.instance(this);
	}

	@Directed
	SequenceArea sequenceArea = new SequenceArea();

	Ui ui;

	class SequenceAreaServiceImpl implements SequenceArea.Service {
		Header header;

		SequenceAreaServiceImpl() {
			header = new Header(Page.this);
		}

		@Override
		public Model getSequenceDefinitionHeader() {
			return header;
		}

		@Override
		public InstanceProperty<?, SequencePlace> getPlaceProperty() {
			return ui.subtypeProperties().place();
		}

		@Override
		public InstanceQuery getInstanceQuery() {
			InstanceQuery instanceQuery = new Sequence.Blank.LoaderImpl()
					.getQuery();
			String sequenceKey = ui.settings.sequenceKey;
			sequenceKey = Ax.blankToEmpty(sequenceKey);
			Sequence.Loader loader = Sequence.Loader.getLoader(sequenceKey);
			return loader.getQuery();
		}

		@Override
		public SequenceSettings getSettings() {
			return ui.settings;
		}

		@Override
		public long getElementLimit() {
			return ui.elementLimit();
		}
	}

	SequenceAreaServiceImpl serviceImpl;

	Page() {
		this.ui = Ui.get();
		this.ui.page = this;
		this.serviceImpl = new SequenceAreaServiceImpl();
		bindings().addBindHandler(ui::bindKeyboardShortcuts);
		from(ui.settings.properties().sequenceKey())
				.dispatchDistinct(sequenceArea.reloadSequenceLambda);
	}

	@Override
	public void onClearFilter(ClearFilter event) {
		serviceImpl.header.mid.suggestor.clear();
		new SequencePlace().go();
	}

	@Override
	public void onFocusSearch(FocusSearch event) {
		serviceImpl.header.mid.suggestor.focus();
	}

	@Override
	public void onShowKeyboardShortcuts(ShowKeyboardShortcuts event) {
		KeyboardShortcutsArea.show(ui.getKeybindingsHandler());
	}

	@Override
	public void onApplicationHelp(ApplicationHelp event) {
		HelpPlace.toggleRoot(Ui.place().copy()).go();
	}

	@Override
	public void onToggleHelp(ToggleHelp event) {
		event.reemitAs(this, ApplicationHelp.class);
	}

	@Override
	public Service getSequenceAreaService() {
		return serviceImpl;
	}

	@Override
	public void onLoadSequence(LoadSequence event) {
		ui.settings.properties().sequenceKey().set(event.getModel());
	}

	@Override
	public void onColumnSetCycle(ColumnSetCycle event) {
		SequenceSettings settings = getSequenceAreaService().getSettings();
		ColumnSet next = settings.nextColumnSet();
		StatusModule.get()
				.showMessageTransitional(Ax.format("Column set -> %s", next));
	}

	@Override
	public void onPropertyDisplayCycle(DetailDisplayCycle event) {
		SequenceSettings settings = getSequenceAreaService().getSettings();
		DetailDisplayMode next = settings.nextDetailDisplayMode();
		StatusModule.get().showMessageTransitional(
				Ax.format("Detail display mode -> %s", next));
	}

	@Override
	public void onNavigateToNewSequencePlace(NavigateToNewSequencePlace event) {
		event.getModel().go();
	}
}
