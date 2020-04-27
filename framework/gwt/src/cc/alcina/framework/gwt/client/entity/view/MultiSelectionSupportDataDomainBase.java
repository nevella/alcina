package cc.alcina.framework.gwt.client.entity.view;

import com.google.gwt.view.client.OrderedMultiSelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;

import cc.alcina.framework.common.client.logic.domain.VersionableEntity;

public class MultiSelectionSupportDataDomainBase<T extends VersionableEntity>
        extends MultiSelectionSupport<T> {
    public MultiSelectionSupportDataDomainBase(AbstractViewModelView view,
            CellTableView<T> cellTableView) {
        super(view, cellTableView);
    }

    @Override
    protected OrderedMultiSelectionModel<T> createOrderedSelectionModel() {
        return new OrderedMultiSelectionModel<T>(t -> t.getId());
    }

    @Override
    protected SingleSelectionModel<T> createSingleSelectionModel() {
        return new SingleSelectionModel<T>(t -> t.getId());
    }

    @Override
    protected void handleSingleSelectionChange() {
        // want to handle this earlier, (it's not really selection, more just
        // 'clickin')
        if (singleSelectionModel.getSelectedObject() != null) {
            throw new UnsupportedOperationException();
        }
    }
}