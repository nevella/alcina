package cc.alcina.framework.gwt.client.dirndl.model.search;

import java.util.List;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.domain.search.BindableSearchDefinition;
import cc.alcina.framework.common.client.domain.search.criterion.PropertyCriterion;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.ValueChange;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.model.Choices;
import cc.alcina.framework.gwt.client.dirndl.model.Choices.Values;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.edit.StringInput;

@Registration({ Model.Value.class, FormModel.Editor.class,
		PropertyCriterion.Filter.class })
@Bean(PropertySource.FIELDS)
@TypedProperties
public class FilterEditor extends Model.Value<PropertyCriterion.Filter>
		implements ValueChange.Container {
	PackageProperties._FilterEditor.InstanceProperties properties() {
		return PackageProperties.filterEditor.instance(this);
	}

	@Directed.Transform(value = Choices.Select.To.class, transformsNull = true)
	@Choices.Values(ValuesImpl.class)
	String propertyName;

	static class ValuesImpl
			implements Choices.Values.ValueSupplier.ContextSensitive {
		@Override
		public List<?> apply(Node contextNode, Values t) {
			BindableSearchDefinition def = (BindableSearchDefinition) contextNode
					.service(SearchDefinitionEditor.Service.class)
					.getSearchDefinition();
			Class<? extends Bindable> type = def.queriedBindableClass();
			return Reflections.at(type).properties().stream()
					.map(p -> p.getName()).sorted().toList();
		}
	}

	@Directed.Transform(value = StringInput.To.class, transformsNull = true)
	String filterValue;

	PropertyCriterion.Filter value;

	FilterEditor() {
		from(properties().propertyName()).withSetOnInitialise(false)
				.signal(this::onPropertiesUpdated);
		from(properties().filterValue()).withSetOnInitialise(false)
				.signal(this::onPropertiesUpdated);
	}

	/*
	 * standard deep property editor pattern. FIXME - dirndl - might be
	 * improvable with deep prop listener
	 */
	void onPropertiesUpdated() {
		PropertyCriterion.Filter newValue = new PropertyCriterion.Filter();
		newValue.propertyName = propertyName;
		newValue.filterValue = filterValue;
		setValue(newValue);
	}

	@Override
	public PropertyCriterion.Filter getValue() {
		return value;
	}

	@Override
	public void setValue(PropertyCriterion.Filter value) {
		properties().propertyName().set(value.propertyName);
		properties().filterValue().set(value.filterValue);
		set("value", this.value, value, () -> this.value = value);
	}
}