package cc.alcina.framework.gwt.client.objecttree.search;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gwt.user.client.ui.FlowPanel;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.BoundWidget;

import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.TxtCriterion;
import cc.alcina.framework.gwt.client.gwittir.BasicBindingAction;

public class FlatSearchDefinitionEditor extends AbstractBoundWidget {
    private FlowPanel fp;

    private Map<SearchCriterion, FlatSearchRow> rows = new LinkedHashMap<SearchCriterion, FlatSearchRow>();

    SearchDefinition def;

    List<FlatSearchable> searchables;

    private FlatSearchable lastDefEmptySearchable;

    public FlatSearchDefinitionEditor() {
        super();
        render();
    }

    public void checkDisableFirstRowRemove() {
        for (FlatSearchRow row : rows.values()) {
            row.disableMinus(rows.size() == 1);
        }
    }

    public List<FlatSearchable> getSearchables() {
        return this.searchables;
    }

    @Override
    public Object getValue() {
        return def;
    }

    public boolean isNotTextOnly(SearchDefinition def) {
        if (def == null) {
            return false;
        }
        for (SearchCriterion sc : def.allCriteria()) {
            if (sc.getClass() == TxtCriterion.class) {
                continue;
            }
            Optional<FlatSearchable> searchable = searchableForCriterion(sc);
            if (searchable.isPresent()
                    && searchable.get().isNonDefaultValue(sc)) {
                return true;
            }
        }
        return false;
    }

    public void refreshRows() {
        // if (!isAttached() || def == null) {
        // return;
        //
        // }
        if (def == null) {
            return;
        }
        fp.clear();
        rows.clear();
        // render criteria w values
        for (SearchCriterion sc : def.allCriteria()) {
            Optional<FlatSearchable> searchable = searchableForCriterion(sc);
            if (searchable.isPresent() && !rows.containsKey(sc)
                    && searchable.get().hasValue(sc)) {
                addRow(searchable.get(), sc);
            }
        }
        // use last if we can
        if (lastDefEmptySearchable != null) {
            Optional<FlatSearchable> searchable = searchables.stream().filter(
                    s -> s.getClass() == lastDefEmptySearchable.getClass())
                    .findFirst();
            if (searchable.isPresent()) {
                for (SearchCriterion sc : def.allCriteria()) {
                    Optional<FlatSearchable> searchableReCriterion = searchableForCriterion(
                            sc);
                    if (searchableReCriterion.isPresent()
                            && searchableReCriterion.get()
                                    .getClass() == searchable.get()
                                            .getClass()) {
                        sc.setOperator(lastDefEmptySearchable.getCriterion()
                                .getOperator());
                        addRow(searchable.get(), sc);
                        return;
                    }
                }
            }
        }
        if (rows.size() > 0) {
            return;
        }
        // or first searchable criteria w/out value
        for (SearchCriterion sc : def.allCriteria()) {
            Optional<FlatSearchable> searchable = searchableForCriterion(sc);
            if (searchable.isPresent()
                    && searchables.get(0) == searchable.get()) {
                addRow(searchable.get(), sc);
                return;
            }
        }
        // or create a criteria
        if (rows.size() == 0) {
            addRow(searchables.get(0), null);
        }
    }

    public Optional<FlatSearchable> searchableForCriterion(SearchCriterion sc) {
        return searchables.stream()
                .filter(s -> s.getCriterionClass() == sc.getClass())
                .findFirst();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setModel(Object model) {
        lastDefEmptySearchable = null;
        if (searchables != null && def != null) {
            for (SearchCriterion sc : def.allCriteria()) {
                Optional<FlatSearchable> searchable = searchableForCriterion(
                        sc);
                if (searchable.isPresent() && rows.containsKey(sc)
                        && !searchable.get().hasValue(sc)) {
                    lastDefEmptySearchable = searchable.get();
                    lastDefEmptySearchable.setCriterion(sc);
                    break;
                }
            }
        }
        this.def = (SearchDefinition) model;
        super.setModel(model);
        refreshRows();
    }

    public void setSearchables(List<FlatSearchable> searchables) {
        this.searchables = searchables;
    }

    public void setupForNewCriterion(FlatSearchRow row, boolean hadValue) {
        def.removeCriterion(row.getValue(), !hadValue);
        row.setValue(null);
        row.setOperator(null);
        SearchCriterion sc = row.getSearchable().createCriterionInstance();
        def.addCriterionToSoleCriteriaGroup(sc);
        row.setValue(sc);
        row.bind();
    }

    @Override
    public void setValue(Object value) {
    }

    private void render() {
        this.fp = new FlowPanel();
        setAction(new FlatSearchDefinitionEditorAction());
        initWidget(fp);
    }

    void addRow(FlatSearchable flatSearchable, SearchCriterion sc) {
        FlatSearchRow row = new FlatSearchRow(this);
        row.setSearchable(flatSearchable);
        if (sc == null) {
            sc = flatSearchable.createCriterionInstance();
            def.addCriterionToSoleCriteriaGroup(sc, true);
        }
        row.setValue(sc);
        row.setModel(def);
        fp.add(row);
        rows.put(sc, row);
        checkDisableFirstRowRemove();
    }

    void removeRow(FlatSearchRow row, boolean hadValue) {
        def.removeCriterion(row.getValue(), !hadValue);
        rows.remove(row.getValue());
        fp.remove(row);
        checkDisableFirstRowRemove();
    }

    class FlatSearchDefinitionEditorAction extends BasicBindingAction {
        @Override
        protected void set0(BoundWidget widget) {
            refreshRows();
        }
    }
}
