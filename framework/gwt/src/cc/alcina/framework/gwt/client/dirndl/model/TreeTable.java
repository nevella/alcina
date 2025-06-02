package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.PropertyOrder;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;
import cc.alcina.framework.gwt.client.dirndl.layout.Tables.ColumnWidth;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.BindableClassTransformer;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.TableColumn;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.TableRow;
import cc.alcina.framework.gwt.client.dirndl.model.Tree.TreeNode;
import cc.alcina.framework.gwt.client.dirndl.model.TreeTable.AbstractNode.DisplayOrder;

/**
 * <p>
 * Or should it be TableTree? Well - this implementation is tree-centric (unlike
 * Swing), so - question answered.
 * 
 * <p>
 * Note that this doesn't have a table model so much as a tree-shaped set of row
 * models
 */
public class TreeTable extends Model.Fields {
	/*
	 * The resolver is applied here (rather than on the TreeTable) so that the
	 * TreeTable property can have an app-specific resolver (for custom tablerow
	 * rendering)
	 */
	@DirectedContextResolver(Resolver.class)
	@Directed(
		bindings = { @Binding(
			// unused, but required
			from = "root",
			type = Type.STYLE_ATTRIBUTE,
			to = "gridTemplateColumns",
			transform = GridTemplateColumnsTransform.class) })
	Tree tree;

	static class GridTemplateColumnsTransform
			extends Binding.AbstractContextSensitiveTransform<Object> {
		@Override
		public String apply(Object t) {
			Resolver resolver = (Resolver) node.getResolver();
			List<TableColumn> columns = resolver.treeTable().tableModel
					.getHeader().getColumns();
			/*
			 * see
			 * dirndl.layout.Tables.Multiple.IntermediateModel.IntermediateModel
			 * for example of how to get default col width
			 */
			String gridColumnWidth = "minmax(min-content, 8rem)";
			String cellColumns = columns.stream()
					.map(col -> col.getField().getProperty()).map(p -> {
						ColumnWidth columnWidth = p
								.annotation(ColumnWidth.class);
						return columnWidth != null ? columnWidth.value()
								: gridColumnWidth;
					}).collect(Collectors.joining(" "));
			String nodeLabelWidth = "200px";
			return Ax.format("%s %s", nodeLabelWidth, cellColumns);
		}
	}

	Class<? extends Bindable> bindableClass;

	TableModel tableModel;

	public TreeTable(Tree tree, Class<? extends Bindable> bindableClass) {
		this.tree = tree;
		this.bindableClass = bindableClass;
	}

	@Override
	public void onBeforeRender(BeforeRender event) {
		BindableClassTransformer transformer = new TableModel.BindableClassTransformer();
		transformer.withContextNode(event.node);
		tableModel = transformer.apply(bindableClass);
		tableModel.init(event.node);
		super.onBeforeRender(event);
	}

	/**
	 * <p>
	 * Because this class inserts a @Directed in the superclass property order,
	 * it needs a custom ordering
	 */
	@ReflectiveSerializer.Checks(ignore = true)
	@PropertyOrder(custom = DisplayOrder.class)
	@TypedProperties
	public static class AbstractNode<NM extends AbstractNode, B extends Bindable>
			extends TreeNode<NM> {
		public static PackageProperties._TreeTable_AbstractNode properties = PackageProperties.treeTable_abstractNode;

		static class DisplayOrder extends PropertyOrder.Custom.Defined {
			DisplayOrder() {
				super(properties.label, properties.contents,
						properties.children);
			}
		}

		public AbstractNode() {
		}

		public AbstractNode(NM parent, String label) {
			super(parent, label);
		}

		private B contents;

		@Directed
		public B getContents() {
			return contents;
		}

		public void setContents(B contents) {
			this.contents = contents;
		}
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
				if (Reflections.isAssignableFrom(AbstractNode.class,
						location.property.getDeclaringType())
						&& Objects.equals(location.property.getName(),
								AbstractNode.properties.contents.name())) {
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
}
