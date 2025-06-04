package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;
import cc.alcina.framework.gwt.client.dirndl.layout.Tables.ColumnWidth;
import cc.alcina.framework.gwt.client.dirndl.model.TableEvents.CellClicked;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.BindableClassTransformer;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.TableColumn;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.TableRow;

/**
 * <p>
 * Or should it be TableTree? Well - this implementation is tree-centric (unlike
 * Swing), so - question answered.
 * 
 * <p>
 * Note that this doesn't have a table model so much as a tree-shaped set of row
 * models
 */
public class TreeTable extends Model.Fields
		implements TableEvents.CellClicked.Handler {
	public String nodeLabelWidth = "200px";

	@Binding(type = Type.STYLE_ATTRIBUTE)
	String gridTemplateColumns;

	@Directed.Wrap("columns")
	List<TableModel.TableColumn> columns;

	/*
	 * The resolver is applied here (rather than on the TreeTable) so that the
	 * TreeTable property can have an app-specific resolver (for custom tablerow
	 * rendering)
	 * 
	 * The model may be a tree or a model containing a tree, the sass
	 * association is slightly different in each case
	 */
	@DirectedContextResolver(Resolver.class)
	@Directed
	Model tree;

	Class<? extends Bindable> bindableClass;

	TableModel tableModel;

	public TreeTable(Model tree, Class<? extends Bindable> bindableClass) {
		this.tree = tree;
		this.bindableClass = bindableClass;
	}

	@Override
	public void onBeforeRender(BeforeRender event) {
		BindableClassTransformer transformer = new TableModel.BindableClassTransformer();
		transformer.withContextNode(event.node);
		tableModel = transformer.apply(bindableClass);
		tableModel.init(event.node);
		columns = tableModel.header.getColumns().stream()
				.collect(Collectors.toList());
		columns.add(0, new TableColumn(""));
		populateGridTemplateColumns();
		super.onBeforeRender(event);
	}

	void populateGridTemplateColumns() {
		List<TableColumn> columns = tableModel.getHeader().getColumns();
		/*
		 * see dirndl.layout.Tables.Multiple.IntermediateModel.IntermediateModel
		 * for example of how to get default col width
		 */
		String gridColumnWidth = "minmax(min-content, 8rem)";
		String cellColumns = columns.stream()
				.map(col -> col.getField().getProperty()).map(p -> {
					ColumnWidth columnWidth = p.annotation(ColumnWidth.class);
					return columnWidth != null ? columnWidth.value()
							: gridColumnWidth;
				}).collect(Collectors.joining(" "));
		gridTemplateColumns = Ax.format("%s %s", nodeLabelWidth, cellColumns);
	}

	public static class Resolver extends ContextResolver
			implements NodeEditorContext.Has {
		AnnotationLocation contentsLocation;

		private TreeTable treeTable;

		Resolver() {
			resolveDirectedPropertyAscends = true;
			resolveAnnotationsAscends = true;
		}

		TreeTable treeTable() {
			if (treeTable == null) {
				treeTable = parent().getRootModel();
			}
			return treeTable;
		}

		@Override
		public NodeEditorContext getNodeEditorContext() {
			return treeTable.tableModel;
		}

		@Override
		protected Object resolveModel(AnnotationLocation location,
				Object model) {
			if (contentsLocation == null && location.property != null) {
				if (Reflections.isAssignableFrom(Tree.TreeNode.class,
						location.property.getDeclaringType())
						&& Objects.equals(location.property.getName(),
								Tree.TreeNode.properties.contents.name())) {
					contentsLocation = location;
				}
			}
			if (Objects.equals(location, contentsLocation)) {
				TableRow tableRow = new TableModel.TableRow(
						treeTable().tableModel, model);
				model = tableRow;
			}
			return super.resolveModel(location, model);
		}
	}

	@Override
	public void onCellClicked(CellClicked event) {
		tableModel.onCellClicked(event);
	}
}
