package cc.alcina.framework.gwt.client.data.view;

import java.util.List;
import java.util.Set;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.gwt.client.data.DataAction;
import cc.alcina.framework.gwt.client.data.HasDataAction;
import cc.alcina.framework.gwt.client.data.entity.DataDomainBase;
import cc.alcina.framework.gwt.client.data.export.RowExportContentDefinition;
import cc.alcina.framework.gwt.client.data.place.DataPlace;
import cc.alcina.framework.gwt.client.data.search.DataSearchDefinition;
import cc.alcina.framework.gwt.client.data.search.GroupingParameters;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.logic.MessageManager;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.GenericBasePlace;
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

    public void deleteMultiple(List<? extends DataDomainBase> list) {
        MessageManager.get().showMessage(CommonUtils.formatJ("Deleting %s",
                CommonUtils.pluralise("record", list.size(), true)));
        for (DataDomainBase ds : list) {
            if (!validateDelete(ds)) {
                break;
            }
            ds.delete();
        }
        flushPostDelete(CommonUtils.last(list));
    }

    public void doCreate(Class<? extends HasIdAndLocalId> clazz) {
        DataPlace target = (DataPlace) RegistryHistoryMapper.get()
                .getPlaceByModelClass(clazz);
        target.action = DataAction.CREATE;
        ClientFactory.goTo(target);
    }

    public boolean doDelete(DataDomainBase object) {
        if (!validateDelete(object)) {
            return false;
        }
        delete0(object);
        return true;
    }

    public void doEdit(DataDomainBase object) {
        if (object == null) {
            return;
        }
        DataPlace target = (DataPlace) RegistryHistoryMapper.get()
                .getPlaceByModelClass(object.getClass());
        target.action = DataAction.EDIT;
        target.id = object.getId();
        ClientFactory.goTo(target);
    }

    public void doSearch(Class<? extends HasIdAndLocalId> clazz, String text) {
        DataPlace target = (DataPlace) RegistryHistoryMapper.get()
                .getPlaceByModelClass(clazz);
        target.getSearchDefinition().toTextSearch(text);
        target.action = DataAction.VIEW;
        ClientFactory.goTo(target);
    }

    public void doSearch(DataSearchDefinition def) {
        if (def == null) {
            return;
        }
        DataPlace target = (DataPlace) RegistryHistoryMapper.get()
                .getPlaceByModelClass(def.resultClass());
        target.action = DataAction.VIEW;
        target.def = def;
        ClientFactory.goTo(target);
    }

    public void doView(DataDomainBase object) {
        if (object == null) {
            return;
        }
        DataPlace target = getViewPlace(object);
        if (Event.getCurrentEvent() != null && WidgetUtils.isNewTabModifier()) {
            Window.open(target.toAbsoluteHrefString(), "_blank", "");
        } else {
            ClientFactory.goTo(target);
        }
    }

    public void doViewObject(Class clazz, long objectId) {
        DataPlace target = (DataPlace) RegistryHistoryMapper.get()
                .getPlaceByModelClass(clazz);
        target.action = DataAction.VIEW;
        target.id = objectId;
        ClientFactory.goTo(target);
    }

    public void export(MemcacheDataProvider dataProvider,
            GroupingParameters exportDefinition,
            RowExportContentDefinition exportContentDefinition,
            Set<Long> selectedIds) {
        throw new UnsupportedOperationException();
    }

    public String getPlaceSubToken(Enum value) {
        BasePlace place = RegistryHistoryMapper.get().getPlaceBySubPlace(value);
        return place.toTokenString();
    }

    public String getPlaceToken(DataDomainBase o) {
        return o == null ? null : getViewPlace(o).toTokenString();
    }

    public DataPlace getViewPlace(DataDomainBase object) {
        if (object == null) {
            return null;
        }
        DataPlace target = (DataPlace) RegistryHistoryMapper.get()
                .getPlaceByModelClass(object.getClass());
        if (target == null) {
            return null;
        }
        target.action = DataAction.VIEW;
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

    public void toggleCurrentPlaceEditing(boolean edit) {
        Place current = ClientFactory.currentPlace();
        Place copy = RegistryHistoryMapper.get().copyPlace(current);
        ((HasDataAction) copy)
                .setAction(edit ? DataAction.EDIT : DataAction.VIEW);
        ClientFactory.goTo(copy);
    }

    private void afterDeleteSuccess(DataDomainBase object) {
        MessageManager.get().showMessage(CommonUtils.formatJ("Deleted %s %s",
                object.getClass().getSimpleName(), object.getId()));
        AppViewModel.get().resetProviderFor(object.getClass());
        ClientFactory.refreshCurrentPlace();
    }

    protected void addObjectCriterion(Object model, DataPlace place) {
        // for subclasses
    }

    protected void delete0(DataDomainBase object) {
        object.delete();
        flushPostDelete(object);
    }

    protected void flushPostDelete(DataDomainBase object) {
        CommitToStorageTransformListener
                .flushAndRun(() -> afterDeleteSuccess(object));
    }

    protected void maybeSetId(GenericBasePlace place,
            Set<? extends DataDomainBase> collection) {
        if (collection.size() == 1) {
            DataDomainBase next = collection.iterator().next();
            if (next != null) {
                place.id = next.getId();
                place.def.clearAllCriteria();
            }
        }
    }

    protected boolean validateDelete(DataDomainBase object) {
        return true;
    }
}