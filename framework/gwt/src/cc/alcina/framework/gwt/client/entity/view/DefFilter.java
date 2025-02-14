package cc.alcina.framework.gwt.client.entity.view;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.domain.HasValue;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.TextCriterion;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.gwt.client.dirndl.RenderContext;
import cc.alcina.framework.gwt.client.objecttree.ObjectTreeGridRenderer;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchDefinitionEditor;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchRow;
import cc.alcina.framework.gwt.client.objecttree.search.SearchOperator;
import cc.alcina.framework.gwt.client.widget.FilterWidget;

public class DefFilter extends Composite implements
		HasValueChangeHandlers<SearchDefinition>, PropertyChangeListener {
	private FlowPanel fp;

	private SearchDefinition searchDefinition;

	protected FilterWidget simpleFilter;

	private SearchDefinition attachedDef = null;

	private FlatSearchDefinitionEditor flatEditor = null;

	private TextCriterion ignoreCriterion;

	TopicListener<Boolean> toggleListener = b -> setVisible(!isVisible());

	public DefFilter() {
		this.fp = new FlowPanel();
		initWidget(fp);
		setStyleName("def-filter");
		addAttachHandler(evt -> attachPce(evt.isAttached()));
	}

	@Override
	public HandlerRegistration addValueChangeHandler(
			ValueChangeHandler<SearchDefinition> handler) {
		return addHandler(handler, ValueChangeEvent.getType());
	}

	private void attachPce(boolean attached) {
		if (attached && attachedDef != searchDefinition
				&& searchDefinition != null) {
			searchDefinition.globalPropertyChangeListener(this, attached);
			attachedDef = searchDefinition;
		}
		if (!attached && searchDefinition != null) {
			searchDefinition.globalPropertyChangeListener(this, attached);
			attachedDef = null;
		}
		EntityClientUtils.topicToggleFilter.delta(toggleListener, attached);
	}

	public FlatSearchDefinitionEditor getFlatEditor() {
		return this.flatEditor;
	}

	public SearchDefinition getSearchDefinition() {
		return this.searchDefinition;
	}

	public FilterWidget getSimpleFilter() {
		return this.simpleFilter;
	}

	public void ignoreCriterion(TextCriterion ignoreCriterion) {
		this.ignoreCriterion = ignoreCriterion;
	}

	public void makeVisibleIfNonTextSearchDef() {
		if (flatEditor != null && flatEditor.isNotTextOnly(searchDefinition)) {
			setVisible(true);
		}
	}

	private void maybeRender() {
		if (isVisible() && searchDefinition != null) {
			render();
		}
	}

	protected void populateFilterFromSearch() {
		TextCriterion txtCriterion = searchDefinition
				.firstCriterion(TextCriterion.class);
		if (txtCriterion != null
				&& !txtCriterion.equivalentTo(ignoreCriterion)) {
			simpleFilter.setValue(txtCriterion.getValue());
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (!LooseContext.is(FlatSearchRow.CONTEXT_CHANGING_SEARCHABLE)) {
			// if changing operator of a row with no filter value, ignore
			if (evt.getNewValue() instanceof SearchOperator) {
				Object source = evt.getSource();
				if (source instanceof SearchCriterion
						&& source instanceof HasValue) {
					//
					Object value = ((HasValue) source).getValue();
					if (value == null || value.toString().isEmpty()
							|| (value instanceof Collection
									&& ((Collection) value).isEmpty())) {
						return;
					}
				}
			}
			ValueChangeEvent.fire(DefFilter.this, searchDefinition);
		}
	}

	private void render() {
		if (flatEditor != null) {
			fp.clear();
			fp.add(flatEditor);
			flatEditor.setModel(searchDefinition);
		} else {
			try {
				RenderContext renderContext = RenderContext.branch();
				ObjectTreeGridRenderer treeRenderer = new ObjectTreeGridRenderer();
				ComplexPanel beanView = treeRenderer.render(searchDefinition,
						renderContext);
				fp.clear();
				fp.add(beanView);
			} finally {
				RenderContext.merge();
			}
		}
	}

	public void setFlatEditor(FlatSearchDefinitionEditor flatEditor) {
		this.flatEditor = flatEditor;
	}

	public void setSearchDefinition(SearchDefinition def) {
		if (def != null) {
			attachPce(false);
		}
		this.searchDefinition = null;
		if (def != null) {
			try {
				this.searchDefinition = def.cloneObject();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		if (def != null && simpleFilter != null) {
			populateFilterFromSearch();
			Scheduler.get().scheduleDeferred(() -> {
				if (simpleFilter.isAttached()) {
					simpleFilter.getTextBox().setFocus(true);
				} else {
					new Timer() {
						@Override
						public void run() {
							if (simpleFilter.isAttached()) {
								simpleFilter.getTextBox().setFocus(true);
							}
						}
					}.schedule(500);
				}
			});
		}
		attachPce(true);
		maybeRender();
	}

	public void setSimpleFilter(FilterWidget simpleFilter) {
		this.simpleFilter = simpleFilter;
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			maybeRender();
		}
	}
}
