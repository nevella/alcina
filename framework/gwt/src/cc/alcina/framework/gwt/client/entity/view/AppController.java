package cc.alcina.framework.gwt.client.entity.view;

import java.util.List;
import java.util.Set;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.VersionableEntity;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.gwt.client.entity.EntityAction;
import cc.alcina.framework.gwt.client.entity.HasEntityAction;
import cc.alcina.framework.gwt.client.entity.export.RowExportContentDefinition;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.entity.search.EntitySearchDefinition;
import cc.alcina.framework.gwt.client.entity.search.GroupingParameters;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.logic.MessageManager;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BindablePlace;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

@RegistryLocation(registryPoint = AppController.class, implementationType = ImplementationType.SINGLETON)
@ClientInstantiable
public class AppController {
	public static AppController get() {
		return Registry.impl(AppController.class);
	}

	public AppController() {
		super();
	}

	public void deleteMultiple(List<? extends VersionableEntity> list) {
		MessageManager.get().showMessage(Ax.format("Deleting %s",
				CommonUtils.pluralise("record", list.size(), true)));
		for (VersionableEntity ds : list) {
			if (!validateDelete(ds)) {
				break;
			}
			ds.delete();
		}
		flushPostDelete(CommonUtils.last(list));
	}

	public void doCreate(Class<? extends Entity> clazz) {
		EntityPlace target = (EntityPlace) RegistryHistoryMapper.get()
				.getPlaceByModelClass(clazz);
		target.action = EntityAction.CREATE;
		ClientFactory.goTo(target);
	}

	public boolean doDelete(VersionableEntity object) {
		if (!validateDelete(object)) {
			return false;
		}
		delete0(object);
		return true;
	}

	public void doEdit(VersionableEntity object) {
		if (object == null) {
			return;
		}
		EntityPlace target = (EntityPlace) RegistryHistoryMapper.get()
				.getPlaceByModelClass(object.getClass());
		target.action = EntityAction.EDIT;
		target.id = object.getId();
		ClientFactory.goTo(target);
	}

	public void doSearch(Class<? extends Entity> clazz, String text) {
		EntityPlace target = (EntityPlace) RegistryHistoryMapper.get()
				.getPlaceByModelClass(clazz);
		target.getSearchDefinition().toTextSearch(text);
		target.action = EntityAction.VIEW;
		ClientFactory.goTo(target);
	}

	public void doSearch(EntitySearchDefinition def) {
		if (def == null) {
			return;
		}
		EntityPlace target = (EntityPlace) RegistryHistoryMapper.get()
				.getPlaceByModelClass(def.entityResultClass());
		target.action = EntityAction.VIEW;
		target.def = def;
		ClientFactory.goTo(target);
	}

	public void doView(VersionableEntity object) {
		if (object == null) {
			return;
		}
		EntityPlace target = getViewPlace(object);
		if (Event.getCurrentEvent() != null && WidgetUtils.isNewTabModifier()) {
			Window.open(target.toAbsoluteHrefString(), "_blank", "");
		} else {
			ClientFactory.goTo(target);
		}
	}

	public void doViewObject(Class clazz, long objectId) {
		EntityPlace target = (EntityPlace) RegistryHistoryMapper.get()
				.getPlaceByModelClass(clazz);
		target.action = EntityAction.VIEW;
		target.id = objectId;
		ClientFactory.goTo(target);
	}

	public void export(DomainStoreDataProvider dataProvider,
			GroupingParameters exportDefinition,
			RowExportContentDefinition exportContentDefinition,
			Set<Long> selectedIds) {
		throw new UnsupportedOperationException();
	}

	public String getPlaceSubToken(Enum value) {
		BasePlace place = RegistryHistoryMapper.get().getPlaceBySubPlace(value);
		return place.toTokenString();
	}

	public String getPlaceToken(VersionableEntity o) {
		return o == null ? null : getViewPlace(o).toTokenString();
	}

	public EntityPlace getViewPlace(VersionableEntity object) {
		if (object == null) {
			return null;
		}
		EntityPlace target = (EntityPlace) RegistryHistoryMapper.get()
				.getPlaceByModelClass(object.getClass());
		if (target == null) {
			return null;
		}
		target.action = EntityAction.VIEW;
		target.id = object.getId();
		return target;
	}

	public void goToCurrentPlace() {
		Place place = ClientFactory.get().getPlaceController().getWhere();
		if (place != null) {
			String token = RegistryHistoryMapper.get().getToken(place);
			place = RegistryHistoryMapper.get().getPlace(token);
			goToPlace(place);
		}
	}

	public void goToPlace(Place place) {
		goToPlace(place, true);
	}

	public void goToPlace(Place place, boolean fireHistoryEvent) {
		try {
			LooseContext.pushWithBoolean(
					PlaceHistoryHandler.CONTEXT_IGNORE_NEXT_TOKEN,
					!fireHistoryEvent);
			ClientFactory.goTo(place);
		} finally {
			LooseContext.pop();
		}
	}

	public void goToPlaceReplaceCurrent(Place place) {
		try {
			LooseContext.pushWithTrue(
					PlaceHistoryHandler.CONTEXT_REPLACE_CURRENT_TOKEN);
			ClientFactory.goTo(place);
		} finally {
			LooseContext.pop();
		}
	}

	public void toggleCurrentPlaceEditing(boolean edit) {
		Place current = ClientFactory.currentPlace();
		Place copy = RegistryHistoryMapper.get().copyPlace(current);
		((HasEntityAction) copy)
				.setAction(edit ? EntityAction.EDIT : EntityAction.VIEW);
		ClientFactory.goTo(copy);
	}

	private void afterDeleteSuccess(VersionableEntity object) {
		MessageManager.get().showMessage(Ax.format("Deleted %s %s",
				object.getClass().getSimpleName(), object.getId()));
		AppViewModel.get().resetProviderFor(object.getClass());
		ClientFactory.refreshCurrentPlace();
	}

	protected void addObjectCriterion(Object model, EntityPlace place) {
		// for subclasses
	}

	protected void delete0(VersionableEntity object) {
		object.delete();
		flushPostDelete(object);
	}

	protected void flushPostDelete(VersionableEntity object) {
		CommitToStorageTransformListener
				.flushAndRun(() -> afterDeleteSuccess(object));
	}

	protected void maybeSetId(BindablePlace place,
			Set<? extends VersionableEntity> collection) {
		if (collection.size() == 1) {
			VersionableEntity next = collection.iterator().next();
			if (next != null) {
				place.id = next.getId();
				place.def.clearAllCriteria();
			}
		}
	}

	protected boolean validateDelete(VersionableEntity object) {
		return true;
	}
}