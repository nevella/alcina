package cc.alcina.framework.servlet.component.sequence;

import java.lang.annotation.Annotation;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.HasAnnotations;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.impl.form.FmsContentCells.FmsCellsContextResolver;
import cc.alcina.framework.gwt.client.dirndl.impl.form.FmsContentCells.FmsCellsContextResolver.DisplayAllMixin;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.BeanViewModifiers;
import cc.alcina.framework.gwt.client.dirndl.model.HasClassNames;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.TableEvents;
import cc.alcina.framework.gwt.client.dirndl.model.TableEvents.RowClicked;
import cc.alcina.framework.gwt.client.dirndl.model.TableEvents.RowsModelAttached;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.RowsModel.RowMeta;
import cc.alcina.framework.gwt.client.dirndl.model.TableView;
import cc.alcina.framework.servlet.component.sequence.SequenceEvents.HighlightModelChanged;
import cc.alcina.framework.servlet.component.sequence.SequenceEvents.SelectedIndexChanged;
import cc.alcina.framework.servlet.component.sequence.SequenceSettings.ColumnSet;

@TypedProperties
// this is just to force the SequenceArea to be accessible from RowTransformer
// (via the resolver)
@Directed(tag = "sequence")
@DirectedContextResolver(FmsCellsContextResolver.class)
class SequenceTable extends Model.Fields
		implements TableEvents.RowsModelAttached.Handler,
		SequenceEvents.HighlightModelChanged.Handler,
		SequenceEvents.SelectedIndexChanged.Handler {
	static class ColumnResolver extends DisplayAllMixin {
		@Override
		public <A extends Annotation> A contextAnnotation(
				HasAnnotations reflector, Class<A> clazz,
				ResolutionContext resolutionContext) {
			if (clazz == Directed.Exclude.class) {
				if (reflector instanceof Property) {
					if (isExclude((Property) reflector)) {
						return (A) new Directed.Exclude.Impl();
					}
				}
			}
			return super.contextAnnotation(reflector, clazz, resolutionContext);
		}

		@Override
		protected <A extends Annotation> List<A> resolveAnnotations0(
				Class<A> annotationClass, AnnotationLocation location) {
			if (annotationClass == Directed.Exclude.class) {
				if (isExclude(location.property)) {
					return List.of((A) new Directed.Exclude.Impl());
				}
			}
			return super.resolveAnnotations0(annotationClass, location);
		}

		boolean isExclude(Property property) {
			if (SequenceSettings.get().columnSet == ColumnSet.DETAIL) {
				switch (property.getName()) {
				case "in":
				case "out":
				case "type":
					return true;
				}
			}
			return false;
		}
	}

	static class RowTransformer
			extends ModelTransform.AbstractContextSensitiveModelTransform {
		private ModelTransform sequenceRowTransform;

		@Override
		public AbstractContextSensitiveModelTransform
				withContextNode(Node node) {
			SequenceTable ctx = node.getResolver().parent().getRootModel();
			sequenceRowTransform = ctx.page.sequence.getRowTransform();
			return super.withContextNode(node);
		}

		@Override
		public Object apply(Object t) {
			return sequenceRowTransform.apply(t);
		}
	}

	class RowsModelSupport extends TableModel.RowsModel.Support {
		protected void updateRowDecoratorsAndScroll() {
			for (int idx = 0; idx < filteredElements.size(); idx++) {
				Object filteredElement = filteredElements.get(idx);
				boolean hasMatch = page.highlightModel
						.hasMatch(filteredElement);
				RowMeta rowMeta = rowsModel.meta.get(idx);
				rowMeta.setFlag("matches", hasMatch);
				if (filteredElement instanceof HasClassNames) {
					((HasClassNames) filteredElement).provideClassNames()
							.forEach(className -> rowMeta.setFlag(className,
									true));
				}
			}
			selectAndScroll(page.getPlace().selectedElementIdx,
					filteredElements);
		}

		protected void onSelectedRowsChanged() {
			IntPair selected = rowsModel.getSelectedRowsRange();
			if (selected != null) {
				if (selected.isPoint()) {
					page.getPlace().copy().withSelectedElementIdx(selected.i1)
							.go();
				} else {
					page.getPlace().copy().withSelectedElementIdx(selected.i1)
							.withSelectedRange(selected).go();
				}
			}
		}
	}

	@Directed
	Heading header;

	@Binding(type = Type.PROPERTY)
	ColumnSet columnSet;

	@Directed.Transform(TableView.class)
	@TableModel.RowTransformer(RowTransformer.class)
	@BeanViewModifiers(detached = true, nodeEditors = true, editable = false)
	@DirectedContextResolver(ColumnResolver.class)
	List<?> filteredElements;

	SequenceArea page;

	RowsModelSupport selectionSupport = new RowsModelSupport();

	SequenceTable(SequenceArea page) {
		this.page = page;
		filteredElements = page.filteredSequenceElements;
		header = new Heading(
				Ax.format("Sequence elements [%s]", filteredElements.size()));
		from(SequenceSettings.get().properties().columnSet())
				.to(properties().columnSet()).oneWay();
	}

	public void onRowClicked(RowClicked event) {
		Object rowModel = event.getModel().getOriginalRowModel();
		int index = page.sequence.getElements().indexOf(rowModel);
		if (event.getContext().getOriginatingNativeEvent().getShiftKey()) {
			if (page.getPlace().selectedElementIdx != -1) {
				IntPair absolutePair = IntPair
						.of(page.getPlace().selectedElementIdx, index)
						.toLowestFirst();
				page.getPlace().copy().withSelectedRange(absolutePair).go();
			}
		} else {
			page.getPlace().copy().withSelectedElementIdx(index).go();
		}
	}

	@Override
	public void onRowsModelAttached(RowsModelAttached event) {
		selectionSupport.onRowsModelAttached(event);
	}

	@Override
	public void onHighlightModelChanged(HighlightModelChanged event) {
		selectionSupport.updateRowDecoratorsAndScroll();
	}

	@Override
	public void onSelectedIndexChanged(SelectedIndexChanged event) {
		selectionSupport.updateRowDecoratorsAndScroll();
	}

	PackageProperties._SequenceTable.InstanceProperties properties() {
		return PackageProperties.sequenceTable.instance(this);
	}
}
