package cc.alcina.framework.gwt.client.data.view;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.HasKeyboardPagingPolicy.KeyboardPagingPolicy;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;

import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicSupport;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.data.view.res.DataClientResources;

public class DataClientUtils {
    public static final String TOPIC_TOGGLE_FILTER = DataClientUtils.class
            .getName() + "." + "TOPIC_TOGGLE_FILTER";

    public static void clearSelection(AbstractCellTable table) {
        if (table == null) {
            return;
        }
        SelectionModel selectionModel = table.getSelectionModel();
        if (selectionModel instanceof SingleSelectionModel) {
            ((SingleSelectionModel) selectionModel).clear();
        } else if (selectionModel instanceof MultiSelectionModel) {
            ((MultiSelectionModel) selectionModel).clear();
        }
    }

    public static Image createLoadingImage(int sizePx) {
        ImageResource loadingImg = DataClientResources.INSTANCE.transparent();
        Image image = new Image(loadingImg);
        image.setStyleName("dg-loading-image");
        image.setPixelSize(sizePx, sizePx);
        return image;
    }

    public static boolean isIdList(String str) {
        return TransformManager.idListToLongs(str).size() > 0;
    }

    public static boolean isLoggedIn() {
        return PermissionsManager.get().isLoggedIn();
    }

    public static boolean isTestServer() {
        return Window.Location.getHref().contains("28080");
    }

    public static void notImplemented() {
        ClientNotifications.get().showMessage("Not implemented yet");
    }

    public static void setupKeyboardPoliciesAndStyles(AbstractCellTable table) {
        table.setKeyboardPagingPolicy(KeyboardPagingPolicy.INCREASE_RANGE);
        table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.ENABLED);
        table.setStyleName("data-grid");
    }

    public static TopicSupport<Boolean> topicToggleFilter() {
        return new TopicSupport<>(TOPIC_TOGGLE_FILTER);
    }
}
