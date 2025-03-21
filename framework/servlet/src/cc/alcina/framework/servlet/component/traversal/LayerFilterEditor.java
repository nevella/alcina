package cc.alcina.framework.servlet.component.traversal;

import java.util.List;

import com.google.gwt.user.client.History;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestionEntry;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestor;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestor.AnswerImpl;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestor.SuggestionSelected;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.SelectionChanged;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel.TextTitle;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition.Position;
import cc.alcina.framework.gwt.client.util.WidgetUtils;
import cc.alcina.framework.servlet.component.traversal.StandardLayerAttributes.Filter;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowser.Ui;

class LayerFilterEditor extends Model.All
		implements ModelEvents.Filter.Handler, SuggestionSelected.Handler {
	interface Host {
		Filter getLayerFilterAttribute();

		Layer getLayer();

		void setFilterEditorOpen(boolean open);

		default String getDefaultEditorText() {
			return null;
		}
	}

	// TODO - dirndl - maybe a lightweight singleton Action? Although
	// this works well enough
	Link button;

	@Directed(
		tag = "existing",
		reemits = { DomEvents.Click.class, ModelEvents.Filter.class })
	LeafModel.TextTitle existing;

	private Overlay overlay;

	private Host host;

	LayerFilterEditor(LayerFilterEditor.Host host) {
		this.host = host;
		button = Link.of(ModelEvents.Filter.class).withoutHref(true)
				.withClassName("filter").withText("");
		StandardLayerAttributes.Filter attr = host.getLayerFilterAttribute();
		if (attr != null) {
			this.existing = new TextTitle(attr.toString());
		}
	}

	@Directed.Delegating
	@DirectedContextResolver(AppSuggestor.Resolver.class)
	static class FilterSuggestor extends Model.All
			implements ModelEvents.SelectionChanged.Handler {
		Suggestor suggestor;

		@Property.Not
		Host host;

		FilterSuggestor(LayerFilterEditor.Host host) {
			this.host = host;
			StandardLayerAttributes.Filter attr = host
					.getLayerFilterAttribute();
			Suggestor.Attributes attributes = Suggestor.attributes();
			attributes.withFocusOnBind(true);
			attributes.withSuggestionXAlign(Position.CENTER);
			attributes.withLogicalAncestors(List.of(FilterSuggestor.class));
			boolean hasExistingFilter = attr != null;
			/*
			 * note this routes via appsuggestor because it essentially does the
			 * same work as appsuggestor (changing the global Place)
			 */
			attributes.withAnswer(new AnswerImpl(Ui.get().createAnswerSupplier(
					host.getLayer().index, hasExistingFilter)));
			attributes.withNonOverlaySuggestionResults(true);
			attributes.withInputPrompt("Filter layer");
			attributes.withInputExpandable(true);
			attributes.withInputText(hasExistingFilter ? attr.toString()
					: host.getDefaultEditorText());
			attributes.withSelectAllOnFocus(hasExistingFilter);
			suggestor = attributes.create();
		}

		// copied from appsuggestor - that's ok, these really *are*
		// contextualised app suggestions
		@Override
		public void onSelectionChanged(SelectionChanged event) {
			AppSuggestionEntry suggestion = (AppSuggestionEntry) suggestor
					.provideSelectedValue();
			// emit cleanup
			suggestor.closeSuggestions();
			suggestor.setValue(null);
			event.reemitAs(this, SuggestionSelected.class);
			// *then* global state change
			if (suggestion.url() != null) {
				History.newItem(suggestion.url());
			} else {
				event.reemitAs(this, suggestion.modelEvent(),
						suggestion.eventData());
			}
		}
	}

	@Override
	public void onFilter(ModelEvents.Filter event) {
		WidgetUtils.squelchCurrentEvent();
		FilterSuggestor suggestor = new FilterSuggestor(host);
		overlay = Overlay.attributes().dropdown(Position.START,
				provideElement().getBoundingClientRect(), this, suggestor)
				.create();
		host.setFilterEditorOpen(true);
		overlay.open();
	}

	@Override
	public void onSuggestionSelected(SuggestionSelected event) {
		overlay.close(null, false);
		event.bubble();
	}
}