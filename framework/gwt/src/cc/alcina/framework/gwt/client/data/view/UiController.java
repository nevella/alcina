package cc.alcina.framework.gwt.client.data.view;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public class UiController {
    private Map<Class<? extends ViewModel>, ViewModelView> viewModelViews = new LinkedHashMap<>();

    private Map<Class<? extends ViewModel>, ViewModel> viewModels = new LinkedHashMap<>();

    private AcceptsOneWidget singleWidgetHolder;

    public UiController() {
        super();
    }

    public AcceptsOneWidget getSingleWidgetHolder() {
        return this.singleWidgetHolder;
    }

    public IsWidget getView(AcceptsOneWidget panel, ViewModel viewModel,
            Place place) {
        IsWidget isWidget = getWidgetForViewModel(viewModel);
        return isWidget;
    }

    public <VM extends ViewModel> VM getViewModel(Class<VM> clazz) {
        if (viewModels.get(clazz) == null) {
            viewModels.put(clazz, Reflections.classLookup().newInstance(clazz));
        }
        return (VM) viewModels.get(clazz);
    }

    public void setSingleWidgetHolder(AcceptsOneWidget singleWidgetHolder) {
        this.singleWidgetHolder = singleWidgetHolder;
    }

    protected IsWidget getWidgetForViewModel(ViewModel viewModel) {
        Class<? extends ViewModel> clazz = viewModel.getClass();
        if (viewModelViews.get(clazz) == null) {
            viewModelViews.put(clazz,
                    Registry.impl(ViewModelView.class, clazz));
        }
        ViewModelView view = viewModelViews.get(clazz);
        view.setModel(viewModel);
        return view;
    }
}