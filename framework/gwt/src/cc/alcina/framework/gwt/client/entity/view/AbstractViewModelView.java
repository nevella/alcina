package cc.alcina.framework.gwt.client.entity.view;

import java.beans.PropertyChangeEvent;
import java.util.List;
import java.util.function.Function;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.HasData;

import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.VersionableEntity;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.entity.EntityAction;
import cc.alcina.framework.gwt.client.entity.HasEntityAction;
import cc.alcina.framework.gwt.client.entity.view.ViewModel.ViewModelWithDataProvider;
import cc.alcina.framework.gwt.client.ide.ContentViewSections;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.logic.MessageManager;
import cc.alcina.framework.gwt.client.place.SubPlace;
import cc.alcina.framework.gwt.client.util.AsyncCallbackStd;
import cc.alcina.framework.gwt.client.util.RelativePopupPositioning;
import cc.alcina.framework.gwt.client.util.RelativePopupPositioning.OtherPositioningStrategy;
import cc.alcina.framework.gwt.client.util.RelativePopupPositioning.RelativePopupPositioningParams;
import cc.alcina.framework.gwt.client.widget.Link;
import cc.alcina.framework.gwt.client.widget.dialog.DecoratedRelativePopupPanel;
import cc.alcina.framework.gwt.client.widget.dialog.NonCancellableRemoteDialog;

public abstract class AbstractViewModelView<VM extends ViewModel>
        extends Composite implements ViewModelView<VM> {
    protected VM model;

    protected PermissibleActionListener createListener = new PermissibleActionListener() {
        @Override
        public void vetoableAction(PermissibleActionEvent evt) {
            NonCancellableRemoteDialog crd = new NonCancellableRemoteDialog(
                    "Saving...");
            crd.show();
            Registry.impl(CommitToStorageTransformListener.class)
                    .flushWithOneoffCallback(new AsyncCallbackStd() {
                        @Override
                        public void onSuccess(Object result) {
                            List<DomainTransformEvent> synth = Registry.impl(
                                    CommitToStorageTransformListener.class)
                                    .getSynthesisedEvents();
                            DomainTransformEvent dte = synth.stream()
                                    .filter(o -> o.getObjectId() != 0)
                                    .findFirst().get();
                            Class clazz = dte.getObjectClass();
                            MessageManager.get().showMessage("%s #%s created",
                                    clazz.getSimpleName(), dte.getObjectId());
                            AppController.get().doViewObject(clazz,
                                    dte.getObjectId());
                            crd.hide();
                        }
                    });
        }
    };

    private DecoratedRelativePopupPanel advancedDropdownPanel;

    @Override
    public VM getModel() {
        return model;
    }

    public void handleRowClicked(int index) {
        if (!isEditing()) {
            if (model instanceof ViewModelWithDataProvider) {
                ViewModelWithDataProvider castModel = (ViewModelWithDataProvider) model;
                List allResults = castModel.dataProvider.getAllResults();
                if (allResults.size() > index) {
                    VersionableEntity object = (VersionableEntity) allResults
                            .get(index);
                    AppController.get().doView(object);
                }
            }
        }
    }

    @Override
    public void setModel(VM model) {
        if (this.model != null) {
            this.model.removePropertyChangeListener(this);
        }
        this.model = model;
        model.addPropertyChangeListener(this);
    }

    public void updateToolbar() {
        throw new UnsupportedOperationException();
    }

    protected <C extends VersionableEntity> void asyncSelect(Class<C> clazz,
            String id, AbstractCellTable<C> table) {
        if (table == null || table.getSelectionModel() == null) {
            return;
        }
        // Domain.async(clazz, id, o -> {
        // table.getSelectionModel().setSelected(o, true);
        // });
        // disabled, doesn't work that nicely
    }

    protected Widget createDetailCaption(SubPlace place, Entity entity) {
        return createDetailCaption(place, entity, "");
    }

    protected Widget createDetailCaption(SubPlace place, Entity entity,
            String detailSuffix) {
        FlowPanel caption = new FlowPanel();
        caption.setStyleName("tab-caption-3");
        Link link = Link.createHrefNoUnderline(
                CommonUtils.titleCase(
                        CommonUtils.friendlyConstant(place.getSub())),
                "#" + place.toTokenString());
        caption.add(link);
        caption.add(new InlineLabel(" > " + entity.getId() + " " + detailSuffix));
        return caption;
    }

    protected Widget getAdvancedActionsPanel(DecoratedRelativePopupPanel rpp) {
        return null;
    }

    protected ContentViewSections getBuilder(HasEntityAction place) {
        ContentViewSections builder = new ContentViewSections();
        EntityAction action = place.getAction();
        boolean editable = action != null && action.isEditable();
        if (editable) {
            builder.editable();
        }
        boolean create = action == EntityAction.CREATE;
        builder.setAutoSave(!create);
        if (create) {
            builder.addCreateListener(createListener);
        }
        return builder;
    }

    protected void handleDataProviderDisplay(PropertyChangeEvent evt,
            HasData table) {
        if (model instanceof ViewModelWithDataProvider) {
            if ("place".equals(evt.getPropertyName())) {
                ViewModelWithDataProvider castModel = (ViewModelWithDataProvider) model;
                castModel.deltaDataProviderConnection(model.isActive(), table);
            }
        }
    }

    protected void hideAdvancedDropdownPanel() {
        if (advancedDropdownPanel != null) {
            advancedDropdownPanel.hide();
        }
    }

    protected boolean isEditing() {
        return model != null && model.getAction() == EntityAction.EDIT;
    }

    protected void refresh() {
        ClientFactory.refreshCurrentPlace();
    }

    protected void showAdvancedActionMenu(ClickEvent c) {
        advancedDropdownPanel = showDropdownPanel(c,
                drpp -> getAdvancedActionsPanel(drpp));
    }

    protected DecoratedRelativePopupPanel showDropdownPanel(ClickEvent c,
            Function<DecoratedRelativePopupPanel, Widget> innerPanelProvider) {
        Widget source = (Widget) c.getSource();
        DecoratedRelativePopupPanel popupPanel = new DecoratedRelativePopupPanel(
                true);
        popupPanel.setStyleName("tools-popup dropdown-popup");
        popupPanel.setAutoHideOnHistoryEventsEnabled(true);
        popupPanel.arrowCenterUp();
        RelativePopupPositioningParams params = new RelativePopupPositioningParams();
        params.relativeToElement = source.getElement();
        params.boundingWidget = RootPanel.get();
        params.relativeContainer = RootPanel.get();
        Widget panel = innerPanelProvider.apply(popupPanel);
        params.widgetToShow = panel;
        params.addRelativeWidgetHeight = true;
        OtherPositioningStrategy strategy = OtherPositioningStrategy.BELOW_CENTER;
        params.positioningStrategy = strategy;
        params.shiftX = -4;
        RelativePopupPositioning.showPopup(params, popupPanel);
        return popupPanel;
    }
}
