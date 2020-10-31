package cc.alcina.framework.gwt.client.entity.search;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlTransient;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.domain.search.SearchOrders;
import cc.alcina.framework.common.client.domain.search.SearchOrders.SpecificIdOrder;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.TxtCriterion;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.objecttree.search.packs.SearchUtils;
import cc.alcina.framework.gwt.client.place.BindablePlace;

public abstract class BindableSearchDefinition extends SearchDefinition {
	private GroupingParameters groupingParameters;

	private SearchOrders searchOrders = new SearchOrders<>();

	// zero based
	private int pageNumber;

	public BindableSearchDefinition() {
		super();
	}

	public void addIdsCriterion(Collection<Long> ids) {
		addCriterionToSoleCriteriaGroup(new TxtCriterion(
				Ax.format("ids: %s", ids.stream().map(String::valueOf)
						.collect(Collectors.joining(", ")))));
	}

	public EntityCriteriaGroup dataCriteriaGroup() {
		return (EntityCriteriaGroup) getCriteriaGroups().iterator().next();
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
		getCriteriaGroups().add(new EntityCriteriaGroup());
		setResultsPerPage(50);
	}

	@ClientInstantiable
	public static class DataNullSearchDefinition
			extends EntitySearchDefinition {
		@Override
		public boolean provideHasNoCriteria() {
			return true;
		}

		@Override
		public <C extends Entity> Class<C> queriedEntityClass() {
			throw new UnsupportedOperationException();
		}
	}

	public abstract Class<? extends Bindable> queriedBindableClass();

	public BindablePlace toPlace() {
		BindablePlace place = BindablePlace.forClass(queriedBindableClass());
		place.def = this;
		return place;
	}
}