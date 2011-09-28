package cc.alcina.framework.gwt.client.objecttree.basic;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.search.CriteriaGroup;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSelectorMinimal;

/**
 * 
 * @author nreddel@barnet.com.au
 * 
 */
public abstract class CriteriaGroupSelectorCustomiser<C extends CriteriaGroup, SC extends SearchCriterion, O>
		extends BoundSelectorMinimal {
	protected C criteriaGroup;

	private PropertyChangeListener pcl = new PropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent evt) {
			SC templateCriterion = newCriterion(null);
			Set toReplace = new LinkedHashSet();
			for (Object sc : criteriaGroup.getCriteria()) {
				if (sc.getClass() != templateCriterion.getClass()) {
					toReplace.add(sc);
				}
			}
			Set newValue = (Set) evt.getNewValue();
			if (newValue == null) {
				return;
			}
			for (Object obj : newValue) {
				SC tc = newCriterion((O) obj);
				toReplace.add(tc);
			}
			criteriaGroup.setCriteria(toReplace);
		}
	};

	/**
	 * note must allow obj==null
	 */
	protected abstract SC newCriterion(O obj);

	public CriteriaGroupSelectorCustomiser(Class selectionObjectClass,
			CollectionFilter filter) {
		super();
		this.selectionObjectClass = selectionObjectClass;
		this.filter = filter;
	}

	@Override
	protected void customiseLeftWidget() {
		super.customiseLeftWidget();
		search.setSortGroups(true);
		search.setSortGroupContents(true);
	}

	protected void customiseRightWidget() {
		super.customiseRightWidget();
		results.setItemsHaveLinefeeds(true);
		results.setSortGroups(true);
		results.setSortGroupContents(true);
		
	}

	@Override
	protected abstract Map createObjectMap();

	protected abstract O getSearchCriterionDisplayObject(SC searchCriterion);

	@SuppressWarnings("unchecked")
	public void setModel(Object model) {
		this.criteriaGroup = (C) model;
		addPropertyChangeListener("value", pcl);
		Set values = new HashSet();
		SC templateCriterion = newCriterion(null);
		for (Object sc : criteriaGroup.getCriteria()) {
			// allows for potentially multiple criteria types in the cg
			if (sc.getClass() == templateCriterion.getClass()) {
				values.add(getSearchCriterionDisplayObject((SC) sc));
			}
		}
		renderSelects();
		setValue(values);
		redrawGrid();
	}
}
