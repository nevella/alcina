package cc.alcina.framework.gwt.client.objecttree.search;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.validator.Validator;

import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.gwt.client.gwittir.BasicBindingAction;
import cc.alcina.framework.gwt.client.gwittir.renderer.FriendlyEnumRenderer;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSelectorMinimal;
import cc.alcina.framework.gwt.client.widget.RelativePopupValidationFeedback;
import cc.alcina.framework.gwt.client.widget.SpanPanel;

public class FlatSearchRow extends AbstractBoundWidget<SearchCriterion>
		implements ClickHandler {
	public static final String CONTEXT_CHANGING_SEARCHABLE = FlatSearchRow.class
			.getName() + ".CONTEXT_CHANGING_SEARCHABLE";

	private FlowPanel fp;

	private FlatSearchDefinitionEditor controller;

	private AddRemoveButtons addRemoveButtons;

	AbstractBoundWidget valueEditor;

	BoundSelectorMinimal searchableSelector;

	BoundSelectorMinimal operatorSelector;

	private SearchCriterion value;

	private FlatSearchable searchable;

	private SearchOperator operator;

	private FlatSearchRowAction rowAction;

	public FlatSearchRow() {
		// just for reflection
	}

	public FlatSearchRow(FlatSearchDefinitionEditor controller) {
		this.controller = controller;
		this.fp = new FlowPanel();
		initWidget(fp);
		rowAction = new FlatSearchRowAction();
		setAction(rowAction);
	}

	public void disableMinus(boolean b) {
		addRemoveButtons.minus.setEnabled(!b);
	}

	public SearchOperator getOperator() {
		return this.operator;
	}

	public FlatSearchable getSearchable() {
		return this.searchable;
	}

	public SearchCriterion getValue() {
		return this.value;
	}

	@Override
	public void onClick(ClickEvent event) {
		if (event.getSource() == addRemoveButtons.minus) {
			controller.removeRow(this, searchable.hasValue(value));
		}
		if (event.getSource() == addRemoveButtons.plus) {
			controller.addRow(searchable, null);
		}
	}

	public void setOperator(SearchOperator operator) {
		SearchOperator old_operator = this.operator;
		this.operator = operator;
		if (old_operator == null && operator == null) {
			return;
		}
		changes.firePropertyChange("operator", old_operator, operator);
		renderAndBind();
	}

	public void setSearchable(FlatSearchable searchable) {
		FlatSearchable old_searchable = this.searchable;
		this.searchable = searchable;
		changes.firePropertyChange("searchable", old_searchable, searchable);
		if (value != null) {
			rowAction.unbind(this);
			LooseContext.runWithBoolean(
					FlatSearchRow.CONTEXT_CHANGING_SEARCHABLE, () -> {
						controller.setupForNewCriterion(this,
								old_searchable.hasValue(value));
						return null;
					});
			renderAndBind();
		} else {
			operator = null;
			renderAndBind();
		}
	}

	public void setValue(SearchCriterion value) {
		SearchCriterion old_value = this.value;
		this.value = value;
		changes.firePropertyChange("value", old_value, value);
		renderAndBind();
	}

	private void renderAndBind() {
		if (searchable != null && value != null) {
			if (operator == null) {
				// manually set from criterion (if null in criterion, use
				// searchable default)
				if (value.getOperator() == null || !searchable.listOperators()
						.contains(value.getOperator())) {
					SearchOperator newOperator = searchable.getOperator(value);
					LooseContext.runWithBoolean(
							FlatSearchRow.CONTEXT_CHANGING_SEARCHABLE, () -> {
								value.setOperator(
										(StandardSearchOperator) newOperator);
								return null;
							});
					setOperator(newOperator);
				} else {
					setOperator(value.getOperator());
				}
			} else {
				renderAndBind0();
			}
		}
	}

	private void renderAndBind0() {
		fp.clear();
		fp.setStyleName("flat-search-row");
		searchableSelector = new FlatSearchSelector(FlatSearchable.class, 1,
				null, () -> controller.searchables);
		operatorSelector = new FlatSearchSelector(SearchOperator.class, 1,
				FriendlyEnumRenderer.INSTANCE,
				() -> searchable.listOperators());
		operatorSelector.addStyleName("operator");
		valueEditor = searchable.createEditor();
		valueEditor.addStyleName("editor");
		fp.add(searchableSelector);
		fp.add(operatorSelector);
		fp.add(valueEditor);
		this.addRemoveButtons = new AddRemoveButtons();
		fp.add(addRemoveButtons);
		rowAction.refreshBindings();
	}

	private class AddRemoveButtons extends Composite {
		private Button minus;

		private Button plus;

		public AddRemoveButtons() {
			SpanPanel buttonsPanel = new SpanPanel();
			buttonsPanel.getElement().getStyle().setPaddingLeft(12, Unit.PX);
			initWidget(buttonsPanel);
			this.minus = new Button(" - ", FlatSearchRow.this);
			minus.setStyleName("alcina-Button");
			buttonsPanel.add(minus);
			this.plus = new Button(" + ", FlatSearchRow.this);
			plus.setStyleName("alcina-Button");
			buttonsPanel.add(plus);
		}
	}

	class FlatSearchRowAction extends BasicBindingAction {
		@Override
		protected void set0(BoundWidget widget) {
			refreshBindings();
		}

		void refreshBindings() {
			boolean wasBound = binding.isBound();
			if (binding.isBound()) {
				binding.unbind();
			}
			wasSet = false;
			binding.getChildren().clear();
			Binding child = new Binding(valueEditor, "value", value,
					searchable.getCriterionPropertyName());
			Validator validator = searchable.getValidator();
			if (validator != null) {
				child.getLeft().validator = validator;
				child.getLeft().feedback = new RelativePopupValidationFeedback(
						RelativePopupValidationFeedback.BOTTOM);
			}
			binding.getChildren().add(child);
			binding.getChildren().add(new Binding(searchableSelector, "value",
					FlatSearchRow.this, "searchable"));
			if (searchable.getOperatorPropertyName().isPresent()) {
				binding.getChildren().add(new Binding(operatorSelector, "value",
						value,
						(String) searchable.getOperatorPropertyName().get()));
			}
			binding.getChildren().add(new Binding(operatorSelector, "value",
					FlatSearchRow.this, "operator"));
			binding.setLeft();
			if (wasBound) {
				binding.bind();
			}
		}
	}

	public void bind() {
		rowAction.bind(this);
	}
}