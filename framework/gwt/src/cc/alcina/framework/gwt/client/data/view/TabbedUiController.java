package cc.alcina.framework.gwt.client.data.view;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;

public class TabbedUiController extends UiController {
    private Map<Class<? extends Place>, PlaceTab> placeTabs = new LinkedHashMap<>();

    @Override
    public IsWidget getView(AcceptsOneWidget panel, ViewModel viewModel,
            Place place) {
        PlaceTab tab = getPlaceTab(place.getClass());
        IsWidget isWidget = getWidgetForViewModel(viewModel);
        ((AcceptsOneWidget) tab).setWidget(isWidget);
        return tab;
    }

    public void registerTab(PlaceTab tab) {
        placeTabs.put(tab.getPlaceBaseClass(), tab);
    }

    PlaceTab getPlaceTab(Class<?> clazz) {
        while (clazz != null) {
            if (placeTabs.containsKey(clazz)) {
                return placeTabs.get(clazz);
            }
            clazz = clazz.getSuperclass();
        }
        throw new UnsupportedOperationException();
    }
}
