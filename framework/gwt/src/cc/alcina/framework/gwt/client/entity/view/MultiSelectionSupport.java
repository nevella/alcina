package cc.alcina.framework.gwt.client.entity.view;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.CellPreviewEvent.Handler;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.DefaultSelectionEventManager.EventTranslator;
import com.google.gwt.view.client.DefaultSelectionEventManager.SelectAction;
import com.google.gwt.view.client.OrderedMultiSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.gwt.client.entity.EntityAction;
import cc.alcina.framework.gwt.client.entity.place.EntitySubPlace;
import cc.alcina.framework.gwt.client.widget.Link;

public abstract class MultiSelectionSupport<T extends HasId> {
    public boolean selecting;

    public OrderedMultiSelectionModel<T> multipleSelectionModel;

    private AbstractViewModelView view;

    private CellTableView<T> cellTableView;

    protected SingleSelectionModel<T> singleSelectionModel;

    public MultiSelectionSupport(AbstractViewModelView view,
            CellTableView<T> cellTableView) {
        this.view = view;
        this.cellTableView = cellTableView;
        this.singleSelectionModel = createSingleSelectionModel();
        this.multipleSelectionModel = createOrderedSelectionModel();
    }

    public void addToLinksPanel(FlowPanel linksPanel) {
        if (selecting) {
            linksPanel.add(new Link("Stop selecting", c -> toggleSelecting()));
        } else {
            linksPanel.add(new Link("Select", c -> toggleSelecting()));
        }
    }

    public AbstractCellTable<T> getTable() {
        return cellTableView.table();
    }

    public Set<Long> provideSelectedIds() {
        return selecting
                ? multipleSelectionModel.getSelectedSet().stream()
                        .map(HasId::getId).collect(Collectors.toSet())
                : new LinkedHashSet<>();
    }

    public void toggleSelecting() {
        selecting = !selecting;
        multipleSelectionModel.clear();
        singleSelectionModel.clear();
        updateKeyboardSelectionMode(false);
        view.updateToolbar();
    }

    public void updateKeyboardSelectionMode(boolean edit) {
        if (edit) {
            return;
        }
        if (selecting) {
            getTable().setSelectionModel(multipleSelectionModel,
                    createSelectionEventManager());
            getTable().setKeyboardSelectionPolicy(
                    KeyboardSelectionPolicy.ENABLED);
            // no change handler
        } else {
            getTable().setKeyboardSelectionPolicy(
                    KeyboardSelectionPolicy.ENABLED);
            getTable().setSelectionModel(singleSelectionModel,
                    createSelectionEventManager());
            singleSelectionModel.addSelectionChangeHandler(
                    new SelectionChangeEvent.Handler() {
                        @Override
                        public void onSelectionChange(
                                SelectionChangeEvent event) {
                            handleSingleSelectionChange();
                        }
                    });
        }
    }

    private Handler<T> createSelectionEventManager() {
        return DefaultSelectionEventManager.createCustomManager(
                new SuppressHyperlinkAndHandleNewTabModClickEventTranslator());
    }

    protected abstract OrderedMultiSelectionModel<T> createOrderedSelectionModel();

    protected abstract SingleSelectionModel<T> createSingleSelectionModel();

    protected abstract void handleSingleSelectionChange();

    protected boolean isEditing() {
        return ((EntitySubPlace) view.getModel().getPlace())
                .getAction() == EntityAction.EDIT;
    }

    public class SuppressHyperlinkAndHandleNewTabModClickEventTranslator<E>
            implements EventTranslator<E> {
        @Override
        public boolean clearCurrentSelection(CellPreviewEvent<E> event) {
            return false;
        }

        @Override
        public SelectAction translateSelectionEvent(CellPreviewEvent<E> event) {
            if (Element.is(event.getNativeEvent().getEventTarget())) {
                if (Element.as(event.getNativeEvent().getEventTarget())
                        .getTagName().equalsIgnoreCase("A")) {
                    return SelectAction.IGNORE;
                }
            }
            if (!selecting && BrowserEvents.CLICK
                    .equals(event.getNativeEvent().getType())) {
                view.handleRowClicked(event.getIndex());
                return SelectAction.IGNORE;
            } else {
                return SelectAction.DEFAULT;
            }
        }
    }
}
