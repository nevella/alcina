package cc.alcina.framework.gwt.client.data.view;

import com.google.gwt.place.shared.Place;
import com.google.gwt.view.client.HasData;

import cc.alcina.framework.common.client.csobjects.BaseBindable;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.gwt.client.data.DataAction;
import cc.alcina.framework.gwt.client.data.HasDataAction;

@Bean
public abstract class ViewModel<P extends Place> extends BaseBindable {
	private boolean active;

	private boolean updated;

	private P place;

	private boolean loading;

	public void fireUpdated() {
		updated = false;
		setUpdated(true);
	}

	public DataAction getAction() {
		return ((HasDataAction) place).getAction();
	}

	public P getPlace() {
		return this.place;
	}

	public boolean isActive() {
		return this.active;
	}

	public boolean isLoading() {
		return this.loading;
	}

	public boolean isUpdated() {
		return this.updated;
	}

	public void setActive(boolean active) {
		boolean old_active = this.active;
		this.active = active;
		propertyChangeSupport().firePropertyChange("active", old_active,
				active);
	}

	public void setLoading(boolean loading) {
		boolean old_loading = this.loading;
		this.loading = loading;
		propertyChangeSupport().firePropertyChange("loading", old_loading,
				loading);
	}

	public void setPlace(P place) {
		P old_place = this.place;
		this.place = place;
		propertyChangeSupport().firePropertyChange("place", old_place, place);
	}

	public void setUpdated(boolean updated) {
		boolean old_updated = this.updated;
		this.updated = updated;
		propertyChangeSupport().firePropertyChange("updated", old_updated,
				updated);
	}

	public abstract static class DetailViewModel<P extends Place, T extends HasIdAndLocalId>
			extends ViewModelWithAction<P> {
		private T modelObject;

		public T getModelObject() {
			return this.modelObject;
		}

		public void setModelObject(T modelObject) {
			T old_modelObject = this.modelObject;
			this.modelObject = modelObject;
			propertyChangeSupport().firePropertyChange("modelObject",
					old_modelObject, modelObject);
		}
	}

	public static abstract class ViewModelWithAction<P extends Place>
			extends ViewModel<P> {
		public DataAction action;

		@Override
		public DataAction getAction() {
			return this.action;
		}

		public void setAction(DataAction action) {
			DataAction old_action = this.action;
			this.action = action;
			propertyChangeSupport().firePropertyChange("action", old_action,
					action);
		}
	}

	public static abstract class ViewModelWithDataProvider<P extends Place, T extends HasIdAndLocalId>
			extends ViewModel<P> {
		public DomainStoreDataProvider<T> dataProvider;

		public void deltaDataProviderConnection(boolean active,
				HasData hasData) {
			if (dataProvider == null || hasData == null) {
				return;
			}
			if (dataProvider.getDataDisplays().contains(hasData)) {
				dataProvider.removeDataDisplay(hasData);
			}
			if (active) {
				dataProvider.setSearchDefinition(null);
				dataProvider.addDataDisplay(hasData);
			}
		}
	}

	public static abstract class ViewModelWithDataProviderAndAction<P extends Place, T extends HasIdAndLocalId>
			extends ViewModelWithDataProvider<P, T> {
		public DataAction action;

		@Override
		public DataAction getAction() {
			return this.action;
		}

		public void setAction(DataAction action) {
			DataAction old_action = this.action;
			this.action = action;
			propertyChangeSupport().firePropertyChange("action", old_action,
					action);
		}
	}
}
