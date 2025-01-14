package cc.alcina.framework.servlet.component.gallery;

import com.google.gwt.activity.shared.PlaceUpdateable;

import cc.alcina.framework.servlet.component.gallery.GalleryBrowser.Ui;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.activity.DirectedActivity;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.help.HelpPlace;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.ApplicationHelp;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.component.KeyboardShortcutsArea;
import cc.alcina.framework.gwt.client.place.BasePlace;

/*
 * TODO - look at an approach to prevent double-fires of say reloadGallery - the
 * thing is that two things can trigger that, _both_ will be true on startup
 * 
 * <p>The bindings are quite complex - to try and minimise the redraws - but
 * even so, there is the issue above. Possibly the above, add filters to (say)
 * reloadGallery keyed off some sort of 'context original event'
 */
@TypedProperties
@Directed(
	tag = "page",
	bindings = @Binding(to = "tabIndex", literal = "0", type = Type.PROPERTY))
class GalleryPage extends Model.Fields
		implements GalleryBrowserCommand.ClearFilter.Handler,
		GalleryBrowserCommand.ShowKeyboardShortcuts.Handler,
		ModelEvents.ApplicationHelp.Handler,
		GalleryBrowserCommand.ToggleHelp.Handler {
	static PackageProperties._GalleryPage properties = PackageProperties.galleryPage;

	/**
	 * This activity hooks the Page up to the RootArea (the general routing
	 * contract)
	 */
	@Directed.Delegating
	@Bean(PropertySource.FIELDS)
	@Registration({ DirectedActivity.class, GalleryPlace.class })
	static class ActivityRoute extends DirectedActivity
			// register in spite of non-public access
			implements Registration.AllSubtypes, PlaceUpdateable,
			ModelEvent.DelegatesDispatch {
		@Directed
		GalleryPage page;

		@Override
		public void onBeforeRender(BeforeRender event) {
			page = new GalleryPage();
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

	@Directed
	Header header;

	@Directed
	GalleryArea galleryArea;

	Ui ui;

	GalleryPage() {
		this.ui = Ui.get();
		this.ui.page = this;
		header = new Header(this);
		galleryArea = new GalleryArea(this);
		bindings().addBindHandler(ui::bindKeyboardShortcuts);
	}

	@Override
	public void onClearFilter(GalleryBrowserCommand.ClearFilter event) {
		header.mid.suggestor.clear();
		BasePlace clearedPlace = (BasePlace) Reflections
				.newInstance(Client.currentPlace().getClass());
		clearedPlace.go();
	}

	@Override
	public void onShowKeyboardShortcuts(
			GalleryBrowserCommand.ShowKeyboardShortcuts event) {
		KeyboardShortcutsArea
				.show(GalleryBrowser.Ui.get().getKeybindingsHandler());
	}

	@Override
	public void onApplicationHelp(ApplicationHelp event) {
		HelpPlace.toggleRoot(Ui.place().copy()).go();
	}

	@Override
	public void onToggleHelp(GalleryBrowserCommand.ToggleHelp event) {
		event.reemitAs(this, ApplicationHelp.class);
	}
}
