package cc.alcina.framework.gwt.client.dirndl.activity;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.remote.CommonRemoteServiceAsync;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.entity.search.EntitySearchDefinition;
import cc.alcina.framework.gwt.client.entity.search.ModelSearchResults;
import cc.alcina.framework.gwt.client.util.AsyncCallbackStd;

@RegistryLocation(registryPoint = DirectedEntitySearchActivity.class, implementationType = ImplementationType.INSTANCE)
@ClientInstantiable
public class DirectedEntitySearchActivity<EP extends EntityPlace, B extends Bindable>
		extends DirectedActivity<EP> {
	private transient ModelSearchResults<B> searchResults;

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
		Registry.impl(CommonRemoteServiceAsync.class).searchModel((EntitySearchDefinition) place.def,
				AsyncCallbackStd.<ModelSearchResults> consumerForm(results -> {
					setSearchResults(results);
					fireUpdated();
				}));
		super.start(panel, eventBus);
	}
}
