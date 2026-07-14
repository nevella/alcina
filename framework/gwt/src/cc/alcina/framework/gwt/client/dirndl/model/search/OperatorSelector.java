package cc.alcina.framework.gwt.client.dirndl.model.search;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.NodeContext;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ValueChange;
import cc.alcina.framework.gwt.client.dirndl.model.Choices;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.ValueTransformer;
import cc.alcina.framework.gwt.client.dirndl.model.edit.ChoiceEditor;
import cc.alcina.framework.gwt.client.dirndl.model.edit.ChoicesEditorSingle;
import cc.alcina.framework.gwt.client.dirndl.model.edit.FocusOnBindMarker;
import cc.alcina.framework.gwt.client.dirndl.model.search.Searchable.AvailableOperators;
import cc.alcina.framework.gwt.client.dirndl.model.search.Searchable.ChoiceRenderer;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

/*
@formatter:off

Notes on model/overlays when editing the operator

Clicking on the dropdown opens the and overlay with contents OperatorSelector, which contains a 
contenteditable [EditArea]. OnBind, that editor displays a choice-suggestor - which does not have an input
(input comes from the EditArea), but does contain the results


@formatter:on
*/
@TypedProperties
public class OperatorSelector extends Model.Fields
		implements ValueChange.Container {
	@Directed.Transform(ChoicesEditorSingle.SingleSuggestions.To.class)
	@FocusOnBindMarker
	@Choices.Values(AvailableOperators.class)
	@ValueTransformer(ChoiceRenderer.To.class)
	@ChoiceEditor.WidthConstrained
	StandardSearchOperator operator;

	@Property.Not
	InstanceProperty<SearchCriterion, StandardSearchOperator> operatorProperty;

	public OperatorSelector(
			InstanceProperty<SearchCriterion, StandardSearchOperator> operatorProperty) {
		this.operatorProperty = operatorProperty;
	}

	PackageProperties._OperatorSelector.InstanceProperties properties() {
		return PackageProperties.operatorSelector.instance(this);
	}

	@Override
	public void onNodeContext(NodeContext event) {
		from(operatorProperty).to(properties().operator()).bidi();
		from(properties().operator()).signal(() -> {
			if (provideIsBound()) {
				emitEvent(ModelEvents.Close.class);
			}
		});
	}
}