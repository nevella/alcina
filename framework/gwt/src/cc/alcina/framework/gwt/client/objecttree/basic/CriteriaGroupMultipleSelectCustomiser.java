package cc.alcina.framework.gwt.client.objecttree.basic;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.search.CriteriaGroup;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.gwt.client.gwittir.widget.SetBasedListBox;

/**
 * 
 * @author nick@alcina.cc
 * 
 */
public abstract class CriteriaGroupMultipleSelectCustomiser<C extends CriteriaGroup, SC extends SearchCriterion, O>
		extends SetBasedListBox {
	protected C criteriaGroup;

	private PropertyChangeListener pcl = new PropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent evt) {
			SC templateCriterion = newCriterion(null);
			Set saved = new HashSet();
			for (Object sc : criteriaGroup.getCriteria()) {
				if (sc.getClass() != templateCriterion.getClass()) {
					saved.add(sc);
				}
			}
			Set newCriterionSet = new HashSet();
			newCriterionSet.addAll(saved);
			Object obj = evt.getNewValue();
			Set newValue = null;
			if (!(obj == null || obj instanceof Set)) {
				newValue = Collections.singleton(evt.getNewValue());
			} else {
				newValue = (Set) evt.getNewValue();
			}
			if (newValue == null) {
			} else {
				for (Object member : newValue) {
					SC tc = newCriterion((O) member);
					newCriterionSet.add(tc);
				}
			}
			criteriaGroup.setCriteria(newCriterionSet);
		}
	};

	protected Class selectionObjectClass;

	protected CollectionFilter filter;

	private PropertyChangeListener cgListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			// note - widget code doesn't change the set - just the members -
			// hence this is only called by other code and we assume will
			// deserve a full refresh
			updateValues();
		}
	};

	public CriteriaGroupMultipleSelectCustomiser(Class selectionObjectClass,
			CollectionFilter filter) {
		super();
		this.selectionObjectClass = selectionObjectClass;
		this.filter = filter;
		setMultipleSelect(true);
	}

	public void setModel(Object model) {
		this.criteriaGroup = (C) model;
		addPropertyChangeListener("value", pcl);
		setOptions();
		updateValues();
	}

	protected abstract O getSearchCriterionDisplayObject(SC searchCriterion);

	/**
	 * note must allow obj==null
	 */
	protected abstract SC newCriterion(O obj);

	@Override
	protected void onLoad() {
		super.onLoad();
		criteriaGroup.addPropertyChangeListener("criteria", cgListener);
	}

	@Override
	protected void onUnload() {
		criteriaGroup.removePropertyChangeListener("criteria", cgListener);
		super.onUnload();
	}

	protected void setOptions() {
		setOptions(Arrays.asList(selectionObjectClass.getEnumConstants()));
	}

	protected void updateValues() {
		Set values = new HashSet();
		SC templateCriterion = newCriterion(null);
		for (Object sc : criteriaGroup.getCriteria()) {
			// allows for potentially multiple criteria types in the cg
			if (sc.getClass() == templateCriterion.getClass()) {
				values.add(getSearchCriterionDisplayObject((SC) sc));
			}
		}
		setValue(values);
	}
}
