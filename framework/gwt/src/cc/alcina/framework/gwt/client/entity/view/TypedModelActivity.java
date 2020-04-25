package cc.alcina.framework.gwt.client.entity.view;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.gwt.client.entity.EntityAction;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.entity.view.ViewModel.DetailViewModel;
import cc.alcina.framework.gwt.client.place.TypedActivity;

public abstract class TypedModelActivity<P extends Place, VM extends ViewModel>
        extends TypedActivity<P> {
    protected VM model;

    public TypedModelActivity(P place) {
        super(place);
    }

    @Override
    public void onStop() {
        model.setActive(false);
        super.onStop();
    }

    @Override
    public void start(AcceptsOneWidget panel, EventBus eventBus) {
        model = ClientFactory.get().getUiController()
                .getViewModel(getModelClass());
        IsWidget view = ClientFactory.get().getUiController().getView(panel,
                model, place);
        updateModel();
        panel.setWidget(view);
    }

    protected abstract Class<VM> getModelClass();

    protected void updateModel() {
        model.setActive(true);
        model.setPlace(place);
        model.fireUpdated();
    }

    public static abstract class TypedDetailModelActivity<P extends EntityPlace, VM extends DetailViewModel, T extends Entity>
            extends TypedModelActivity<P, VM> {
        public TypedDetailModelActivity(P place) {
            super(place);
        }

        protected abstract Class<T> getDomainClass();

        protected void onCreate(T modelObject) {
        }

        @Override
        protected void updateModel() {
            super.updateModel();
            boolean create = place.action == EntityAction.CREATE;
            model.setModelObject(null);
            model.setAction(place.action);
            Domain.async(getDomainClass(), place.id, create, modelObject -> {
                if (create) {
                    onCreate(modelObject);
                }
                model.setModelObject(modelObject);
            });
        }
    }
}
