package cc.alcina.framework.gwt.client.widget.complex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.ui.FlowPanel;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;

import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.common.client.actions.instances.CreateAction;
import cc.alcina.framework.common.client.actions.instances.DeleteAction;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.customiser.MultilineWidget;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundTableExt;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory.PaneWrapperWithObjects;
import cc.alcina.framework.gwt.client.ide.widget.Toolbar;
import cc.alcina.framework.gwt.client.widget.UsefulWidgetFactory;

public abstract class MultilineRowEditor<H extends Entity>
		extends AbstractBoundWidget<Set<H>> implements MultilineWidget {
	private FlowPanel holder;

	private PermissibleActionListener toolbarListener = new PermissibleActionListener() {
		@Override
		public void vetoableAction(PermissibleActionEvent evt) {
			if (evt.getAction() instanceof CreateAction) {
				doCreateRow();
				renderTable();
			} else if (evt.getAction() instanceof DeleteAction) {
				doDeleteRows();
				renderTable();
			} else {
				if (handleCustomAction(MultilineRowEditor.this,
						evt.getAction())) {
					renderTable();
				}
			}
		}
	};

	private Toolbar toolbar;;

	BoundTableExt table;

	private Set<H> value;

	private boolean editable;

	public MultilineRowEditor() {
		holder = UsefulWidgetFactory.styledPanel("multi-line-editor");
		initWidget(holder);
	}

	public Set<H> getValue() {
		return this.value;
	}

	public boolean isEditable() {
		return this.editable;
	}

	@Override
	public boolean isMultiline() {
		return true;
	}

	public void redraw() {
		renderTable();
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	public void setValue(Set<H> value) {
		this.value = value;
		renderTable();
		setStyleName("empty", CommonUtils.nonNullSet(value).isEmpty());
	}

	private void renderTable() {
		holder.clear();
		List<PermissibleAction> actions = new ArrayList<>(
				Arrays.asList(new PermissibleAction[] { new CreateAction(),
						new DeleteAction() }));
		customiseActions(actions);
		FlowPanel tableToolbarHolder = UsefulWidgetFactory
				.styledPanel("table-toolbar-holder");
		toolbar = new ContentViewFactory().createToolbar(actions, false);
		toolbar.removeStyleName("alcina-ToolbarSmall");
		if (editable) {
			tableToolbarHolder.add(toolbar);
		}
		int tableMask = BoundTableExt.HEADER_MASK
				| BoundTableExt.NO_NAV_ROW_MASK
				| BoundTableExt.NO_SELECT_CELL_MASK;
		if (editable) {
			tableMask |= BoundTableExt.ROW_HANDLE_MASK
					| BoundTableExt.HANDLES_AS_CHECKBOXES;
		}
		List<H> values = new ArrayList<>(CommonUtils.nonNullSet(getValue()));
		values = filterVisibleValues(values);
		sortValues(values);
		values.forEach(v -> TransformManager.get().registerDomainObject(v));
		ContentViewFactory contentViewFactory = new ContentViewFactory()
				.noCaption().setBeanClass(getItemClass()).editable(editable)
				.autoSave(true).doNotClone(true).setTableMask(tableMask);
		customiseContentViewFactory(contentViewFactory, getModel());
		PaneWrapperWithObjects view = contentViewFactory
				.createMultipleBeanView(values);
		table = (BoundTableExt) view.getBoundWidget();
		table.setNoContentMessage("0 items");
		table.setModel(getModel());
		view.addStyleName("alcina-grid");
		tableToolbarHolder.add(view);
		holder.add(tableToolbarHolder);
		toolbar.addVetoableActionListener(toolbarListener);
	}

	protected void customiseActions(List<PermissibleAction> actions) {
	}

	protected void customiseContentViewFactory(
			ContentViewFactory contentViewFactory, Object model) {
	}

	protected abstract void doCreateRow();

	protected abstract void doDeleteRows();

	protected List<H> filterVisibleValues(List<H> values) {
		return values;
	}

	protected abstract Class<H> getItemClass();

	/**
	 * @param multilineRowEditor
	 * @return true if the table should be refreshed
	 */
	protected boolean handleCustomAction(MultilineRowEditor multilineRowEditor,
			PermissibleAction action) {
		return false;
	}

	protected abstract void sortValues(List<H> values);
}
