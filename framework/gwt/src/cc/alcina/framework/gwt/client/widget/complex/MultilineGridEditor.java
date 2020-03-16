package cc.alcina.framework.gwt.client.widget.complex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.ui.FlowPanel;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;

import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.common.client.actions.instances.CreateAction;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.gwt.client.gwittir.customiser.MultilineWidget;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundTableExt;
import cc.alcina.framework.gwt.client.gwittir.widget.GridForm;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory.PaneWrapperWithObjects;
import cc.alcina.framework.gwt.client.ide.widget.Toolbar;
import cc.alcina.framework.gwt.client.widget.Link;
import cc.alcina.framework.gwt.client.widget.UsefulWidgetFactory;

public abstract class MultilineGridEditor<H extends Entity>
		extends AbstractBoundWidget<Set<H>> implements MultilineWidget {
	private FlowPanel holder;

	private PermissibleActionListener toolbarListener = new PermissibleActionListener() {
		@Override
		public void vetoableAction(PermissibleActionEvent evt) {
			if (evt.getAction() instanceof CreateAction) {
				doCreateRow();
				renderTable();
			} else {
				if (handleCustomAction(MultilineGridEditor.this,
						evt.getAction())) {
					renderTable();
				}
			}
		}
	};

	private Toolbar toolbar;

	BoundTableExt table;;

	private Set<H> value;

	private boolean editable;

	List<GridForm> grids;

	public MultilineGridEditor() {
		holder = UsefulWidgetFactory.styledPanel("multi-line-editor grid");
		initWidget(holder);
	}

	public abstract String getCreateActionDisplayName();

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
		setStyleName("empty", value.isEmpty());
	}

	private void createPerObjectActions(FlowPanel gridAndActions, H rowValue) {
		List<Link> links = createPerRowEditActions(rowValue);
		FlowPanel panel = UsefulWidgetFactory.styledPanel("per-object-links");
		links.stream().forEach(link -> panel.add(link));
		gridAndActions.add(panel);
	}

	private void renderTable() {
		holder.clear();
		List<PermissibleAction> actions = new ArrayList<>(
				Arrays.asList(new PermissibleAction[] {
						new CreateGridAction(getCreateActionDisplayName()) }));
		customiseActions(actions);
		FlowPanel tableToolbarHolder = UsefulWidgetFactory
				.styledPanel("table-toolbar-holder");
		toolbar = new ContentViewFactory().createToolbar(actions, false);
		toolbar.removeStyleName("alcina-ToolbarSmall");
		if (editable) {
			tableToolbarHolder.add(toolbar);
		}
		List<H> values = new ArrayList<>(getValue());
		values = filterVisibleValues(values);
		sortValues(values);
		values.forEach(v -> TransformManager.get().registerDomainObjectIfNonProvisional(v));
		grids = new ArrayList<>();
		for (H value : values) {
		    ContentViewFactory contentViewFactory = new ContentViewFactory()
	                .noCaption().setBeanClass(value.getClass()).editable(editable)
	                .autoSave(ContentViewFactory.autoSaveFromContentViewAncestor(this)).doNotClone(true);
	        customiseContentViewFactory(contentViewFactory, getModel());
	        PaneWrapperWithObjects view = contentViewFactory
	                .createBeanView(value);
			GridForm grid = (GridForm) view.getBoundWidget();
			grid.setModel(value);
			grid.setValue(value);
			FlowPanel gridAndActions = new FlowPanel();
			gridAndActions.setStyleName("grid-and-actions");
			grids.add(grid);
			gridAndActions.add(grid);
			if (editable) {
				createPerObjectActions(gridAndActions, value);
			}
			tableToolbarHolder.add(gridAndActions);
		}
		holder.add(tableToolbarHolder);
		toolbar.addVetoableActionListener(toolbarListener);
	}
	protected void customiseContentViewFactory(
            ContentViewFactory contentViewFactory, Object model) {
    }

	protected List<Link> createPerRowEditActions(H rowValue) {
		Link link = Link.createNoUnderline("Delete", evt -> {
			doDeleteRow(rowValue);
		});
		return Collections.singletonList(link);
	}

	protected void customiseActions(List<PermissibleAction> actions) {
	}

	protected abstract void doCreateRow();

	protected abstract void doDeleteRow(H item);

	protected List<H> filterVisibleValues(List<H> values) {
		return values;
	}

	/**
	 * @param multilineRowEditor
	 * @return true if the table should be refreshed
	 */
	protected boolean handleCustomAction(MultilineGridEditor multilineRowEditor,
			PermissibleAction action) {
		return false;
	}

	public static class CreateGridAction extends CreateAction {
		private String name;

		public CreateGridAction() {
		}

		public CreateGridAction(String name) {
			this.name = name;
		}

		@Override
		public String getDisplayName() {
			return name;
		}
	}
	protected abstract void sortValues(List<H> values);
}
