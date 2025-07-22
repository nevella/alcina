package cc.alcina.framework.gwt.client.dirndl.activity;

import java.util.stream.Stream;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.csobjects.SearchResult;
import cc.alcina.framework.common.client.domain.search.ModelSearchResults;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.remote.SearchRemoteServiceAsync;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.place.BindablePlace;
import cc.alcina.framework.gwt.client.util.AsyncCallbackStd;

@Registration(DirectedBindableSearchActivity.class)
public class DirectedBindableSearchActivity<BP extends BindablePlace, B extends Bindable & SearchResult>
		extends DirectedActivity<BP> {
	private transient ModelSearchResults<B> searchResults;

	public Stream<Class<? extends ModelEvent>> getActions() {
		return Stream.empty();
	}

	@AlcinaTransient
	public ModelSearchResults<B> getSearchResults() {
		return this.searchResults;
	}

	public void setSearchResults(ModelSearchResults searchResults) {
		ModelSearchResults old_searchResults = this.searchResults;
		this.searchResults = searchResults;
		propertyChangeSupport().firePropertyChange("searchResults",
				old_searchResults, searchResults);
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		Registry.impl(SearchRemoteServiceAsync.class).searchModel(place.def,
				AsyncCallbackStd.<ModelSearchResults> consumerForm(results -> {
					setSearchResults(results);
					if (provideIsBound()) {
						// search result rendering is normally tree-shaped
						Node node = provideNode();
						node.dispatch(ModelEvents.SearchResultsReturned.class,
								results);
						// note this may cause node to become unbound
						node.dispatch(ModelEvents.TransformSourceModified.class,
								results);
					}
				}));
		super.start(panel, eventBus);
	}

	@Registration({ DirectedBindableSearchActivity.class, EntityPlace.class })
	public static class DirectedBindableSearchActivity_Entity<E extends Entity & SearchResult>
			extends DirectedBindableSearchActivity<EntityPlace, E> {
		@Override
		public Stream<Class<? extends ModelEvent>> getActions() {
			Class<? extends Entity> entityClass = ((EntityPlace) getPlace())
					.provideEntityClass();
			Stream<Class<? extends ModelEvent>> superStream = super.getActions();
			if (Permissions.isPermitted(
					Permissions.getObjectPermissions(entityClass).create())) {
				return Stream.concat(superStream,
						Stream.of(ModelEvents.Create.class));
			} else {
				return superStream;
			}
		}
	}
}
