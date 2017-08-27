package cc.alcina.framework.gwt.client.objecttree.basic;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collection;

import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.Renderer;

import cc.alcina.framework.common.client.logic.FilterCombinator;
import cc.alcina.framework.common.client.search.CriteriaGroup;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSelectorMinimal;
import cc.alcina.framework.gwt.client.gwittir.widget.RadioButtonList;
import cc.alcina.framework.gwt.client.widget.layout.LayoutEvents;

/**
 * We're not obeying all bound widget stuff here...
 * 
 * @author nreddel@barnet.com.au
 * 
 */
public class CriteriaGroupSelectorCustomiserWrapper<C extends CriteriaGroup> extends AbstractBoundWidget<CriteriaGroup>{
	protected C criteriaGroup;
	protected BoundSelectorMinimal customiser;
	protected RadioButtonList<FilterCombinator> filterRbl;
	protected Renderer<FilterCombinator, String> fcRend = new Renderer<FilterCombinator, String>() {
			public String render(FilterCombinator o) {
				if (o == null) {
					return "";
				}
				switch (o) {
				case AND:
					return "Match all selected ('and')";
				case OR:
					return "Match any selected ('or')";
				}
				return "";
			}
		};


	protected ComplexPanel container;


	public CriteriaGroupSelectorCustomiserWrapper() {
		this(null, "");
	}


	public CriteriaGroupSelectorCustomiserWrapper(BoundSelectorMinimal customiser,
			String groupName) {
		this.customiser = customiser;
		filterRbl = new RadioButtonList<FilterCombinator>(
				"FilterCombinator_" + groupName, Arrays
						.asList(FilterCombinator.values()), fcRend);
		filterRbl.setColumnCount(2);
		createContainer();
		container.add(customiser);
		container.add(filterRbl);
		initWidget(container);
	}


	protected void createContainer() {
		this.container = new FlowPanel();
	}
	public CriteriaGroup getValue() {
		// noop
		return null;
	}

	@Override
	public void setModel(Object model) {
		super.setModel(model);
		this.criteriaGroup = (C) model;
		this.filterRbl.setValue(criteriaGroup.getCombinator());
		this.customiser.setModel(model);
		// sadly, above won't fire a property change for an empty set of
		// tags
		updateRblVisibility();
		this.filterRbl.addPropertyChangeListener("value",
				new PropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent evt) {
						criteriaGroup.setCombinator(filterRbl.singleValue());
					}
				});
		this.customiser.addPropertyChangeListener("value",
				new PropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent evt) {
						updateRblVisibility();
					}
				});
	}

	public void setValue(CriteriaGroup value) {
		// noop
	}


	protected void updateRblVisibility() {
		Collection values = (Collection) customiser.getValue();
		boolean newVis = values != null && values.size() > 1;
		boolean oldVis = filterRbl.isVisible();
		filterRbl.setVisible(newVis);
		if (oldVis!=newVis){
			LayoutEvents.get().fireRequiresGlobalRelayout();
		}
	}
}
