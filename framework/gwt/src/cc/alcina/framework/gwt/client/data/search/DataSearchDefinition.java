package cc.alcina.framework.gwt.client.data.search;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlTransient;

import cc.alcina.framework.common.client.domain.search.SearchOrders;
import cc.alcina.framework.common.client.domain.search.SearchOrders.SpecificIdOrder;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.TxtCriterion;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.data.entity.DataDomainBase;
import cc.alcina.framework.gwt.client.objecttree.search.packs.SearchUtils;

/*
 * I'd call this CrudSearch...but that'd be mean
 */
public abstract class DataSearchDefinition extends SearchDefinition {
    private GroupingParameters groupingParameters;

    private SearchOrders searchOrders = new SearchOrders<>();

    private int pageNumber;// 0-based

    public void addIdsCriterion(Collection<Long> ids) {
        addCriterionToSoleCriteriaGroup(new TxtCriterion(
                Ax.format("ids: %s", ids.stream().map(String::valueOf)
                        .collect(Collectors.joining(", ")))));
    }

    public DataCriteriaGroup demeterCriteriaGroup() {
        return (DataCriteriaGroup) getCriteriaGroups().iterator().next();
    }

    @XmlTransient
    public GroupingParameters getGroupingParameters() {
        return groupingParameters;
    }

    public int getPageNumber() {
        return this.pageNumber;
    }

    public SearchOrders getSearchOrders() {
        return this.searchOrders;
    }

    public boolean provideHasNoCriteria() {
        assert getCriteriaGroups().size() == 1;
        return getCriteriaGroups().iterator().next().provideIsEmpty();
    }

    public Optional<SearchOrders> provideIdSearchOrder() {
        TxtCriterion tx = firstCriterion(TxtCriterion.class);
        if (tx != null && Ax.blankToEmpty(tx.getValue())
                .matches(SearchUtils.IDS_REGEX)) {
            SearchOrders result = new SearchOrders<>();
            result.addOrder(new SpecificIdOrder(
                    SearchUtils.idsTextToSet(tx.getValue())), true);
            return Optional.of(result);
        } else {
            return Optional.empty();
        }
    }

    // not quite correct...but...
    public boolean provideIsSimpleTextSearch() {
        return provideSimpleTextSearchCriterion() != null;
    }

    public TxtCriterion provideSimpleTextSearchCriterion() {
        TxtCriterion tx = firstCriterion(TxtCriterion.class);
        if (tx != null && Ax.notBlank(tx.getValue())) {
            return tx;
        } else {
            return null;
        }
    }

    public abstract <C extends DataDomainBase> Class<C> resultClass();

    public void setGroupingParameters(GroupingParameters groupingParameters) {
        this.groupingParameters = groupingParameters;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public void setSearchOrders(SearchOrders searchOrders) {
        this.searchOrders = searchOrders;
    }

    @Override
    public String toString() {
        return new FormatBuilder().separator(" :: ").appendIfNotBlank(
                super.toString(), searchOrders, groupingParameters).toString();
    }

    public void toTextSearch(String text) {
        SearchUtils.toTextSearch(this, text);
    }

    public <G extends GroupingParameters> G typedGroupingParameters() {
        return (G) groupingParameters;
    }

    protected void init() {
        getCriteriaGroups().add(new DataCriteriaGroup());
        setResultsPerPage(50);
    }

    @ClientInstantiable
    public static class DataNullSearchDefinition extends DataSearchDefinition {
        @Override
        public boolean provideHasNoCriteria() {
            return true;
        }

        @Override
        public <C extends DataDomainBase> Class<C> resultClass() {
            throw new UnsupportedOperationException();
        }
    }
}
