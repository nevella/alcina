package cc.alcina.framework.gwt.client.dirndl.model.search;

import java.util.List;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.domain.search.BindableSearchDefinition;
import cc.alcina.framework.common.client.domain.search.criterion.PropertyOrderCriterion.Order;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.util.ClassUtil;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.ValueChange;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.model.Choices;
import cc.alcina.framework.gwt.client.dirndl.model.Choices.Values;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

@Registration({ Model.Value.class, FormModel.Editor.class, Order.class })
@Bean(PropertySource.FIELDS)
@TypedProperties
public class OrderEditor extends Model.Value<Order>
		implements ValueChange.Container {
	PackageProperties._OrderEditor.InstanceProperties properties() {
		return PackageProperties.orderEditor.instance(this);
	}

	@Directed.Transform(value = Choices.Select.To.class)
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
					.filter(p -> Reflections.isAssignableFrom(Comparable.class,
							ClassUtil.getWrapperType(p.getType())))
					.map(p -> p.getName()).sorted().toList();
		}
	}

	@Directed.Transform(value = Choices.Select.To.class, transformsNull = true)
	@Choices.EnumValues(SearchCriterion.Direction.class)
	SearchCriterion.Direction direction;

	Order value;

	OrderEditor() {
		from(properties().propertyName()).withSetOnInitialise(false)
				.signal(this::onPropertiesUpdated);
		from(properties().direction()).withSetOnInitialise(false)
				.signal(this::onPropertiesUpdated);
	}

	/*
	 * standard deep property editor pattern. FIXME - dirndl - might be
	 * improvable with deep prop listener
	 */
	void onPropertiesUpdated() {
		Order newValue = new Order();
		newValue.propertyName = propertyName;
		newValue.direction = direction;
		setValue(newValue);
	}

	@Override
	public Order getValue() {
		return value;
	}

	@Override
	public void setValue(Order value) {
		properties().propertyName().set(value.propertyName);
		properties().direction().set(value.direction);
		set("value", this.value, value, () -> this.value = value);
	}
}