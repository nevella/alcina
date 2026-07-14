package cc.alcina.framework.gwt.client.dirndl.model;

import com.google.gwt.dom.client.Element;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestor.SuggestionSelected;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.NodeContext;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Closed;
import cc.alcina.framework.gwt.client.dirndl.event.ReflectedEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ReflectedEvents.FocusEditor;
import cc.alcina.framework.gwt.client.dirndl.event.ValueChange;
import cc.alcina.framework.gwt.client.dirndl.model.TableColumnsMetadata.ColumnMetadata;
import cc.alcina.framework.gwt.client.dirndl.model.TableColumnsMetadata.EditFilter;
import cc.alcina.framework.gwt.client.dirndl.model.TableView.TableContainer;
import cc.alcina.framework.gwt.client.dirndl.model.edit.ChoiceEditor;
import cc.alcina.framework.gwt.client.dirndl.model.edit.StringInput;
import cc.alcina.framework.gwt.client.dirndl.model.search.ValueEditor;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition.Position;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

/**
 * Support for filtering beans by a property
 */
class PropertyFilter {
	TableContainer tableContainer;

	EditFilter event;

	TableModel.FilterService filterService;

	PropertyFilter(TableContainer tableContainer, EditFilter event,
			TableModel.FilterService filterService) {
		this.tableContainer = tableContainer;
		this.event = event;
		this.filterService = filterService;
	}

	void editFilter() {
		FilterCoordinator filterCoordinator = new FilterCoordinator(event);
		filterCoordinator.overlay.open();
	}

	@TypedProperties
	class FilterCoordinator extends Bindable.Fields
			implements ModelEvents.Closed.Handler, SuggestionSelected.Handler {
		class FilterEditor extends Model.All implements ValueChange.Container,
				ReflectedEvents.FocusEditor.Reflector {
			@Directed.Transform(Choices.Select.To.class)
			@Choices.EnumValues(StandardSearchOperator.class)
			@ChoiceEditor.WidthConstrained
			StandardSearchOperator operator;

			@Directed
			ValueEditor valueEditor;

			class StringInputServiceImpl implements StringInput.Service {
				@Override
				public boolean isCommitOnEnter() {
					return true;
				}
			}

			public void onNodeContext(NodeContext event) {
				node.getResolver().registerService(StringInput.Service.class,
						new StringInputServiceImpl());
				valueEditor = new ValueEditor(editableInstanceProperty,
						tableContainer.node.getResolver());
				this.operator = FilterCoordinator.this.operator;
				exec(() -> emitEvent(FocusEditor.class)).dispatch();
			}
		}

		Property property;

		Element relativeTo;

		Overlay overlay;

		StandardSearchOperator operator;

		FilterEditor editor;

		InstanceProperty editableInstanceProperty;

		FilterCoordinator(EditFilter event) {
			this.property = event.getModel().provideProperty();
			this.relativeTo = ((Model) event.getModel()).provideElement();
			ColumnMetadata columnMetadata = filterService.getMetadata()
					.getColumnMetadata(property);
			editableInstanceProperty = property
					.createTemplateInstanceProperty();
			editor = new FilterEditor();
			editor.operator = columnMetadata.filterOperator;
			overlay = Overlay.attributes().dropdown(Position.START,
					relativeTo.getBoundingClientRect(), tableContainer, editor)
					.withClosedHandler(this).create();
		}

		void open() {
			overlay.open();
		}

		@Override
		public void onClosed(Closed event) {
			onFilterClosed(event);
		}

		@Override
		public void onSuggestionSelected(SuggestionSelected event) {
			overlay.close(null, false);
			onFilterClosed(event);
		}

		void onFilterClosed(ModelEvent event) {
			event.reemitAs(tableContainer, TableEvents.FilterModified.class,
					new TableEvents.FilterModified.Data(property,
							editableInstanceProperty.get(), editor.operator));
		}
	}
}
