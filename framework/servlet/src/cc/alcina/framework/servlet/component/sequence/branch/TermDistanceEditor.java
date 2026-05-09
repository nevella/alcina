package cc.alcina.framework.servlet.component.sequence.branch;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.ValueChange;
import cc.alcina.framework.gwt.client.dirndl.model.Choices;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.edit.StringInput;
import cc.alcina.framework.servlet.component.sequence.branch.BranchingParserNodeCriterion.Depth;
import cc.alcina.framework.servlet.component.sequence.branch.BranchingParserNodeCriterion.TermDistance;

@Registration({ Model.Value.class, FormModel.Editor.class, TermDistance.class })
@Bean(PropertySource.FIELDS)
@TypedProperties
public class TermDistanceEditor extends Model.Value<TermDistance>
		implements ValueChange.Container {
	PackageProperties._TermDistanceEditor.InstanceProperties properties() {
		return PackageProperties.termDistanceEditor.instance(this);
	}

	@Directed.Transform(value = StringInput.To.class, transformsNull = true)
	String text;

	@Directed.Transform(value = Choices.Select.To.class, transformsNull = true)
	@Choices.EnumValues(Depth.class)
	Depth distance;

	TermDistance value;

	TermDistanceEditor() {
		from(properties().text()).withSetOnInitialise(false)
				.signal(this::onPropertiesUpdated);
		from(properties().distance()).withSetOnInitialise(false)
				.signal(this::onPropertiesUpdated);
	}

	/*
	 * standard deep property editor pattern. FIXME - dirndl - might be
	 * improvable with deep prop listener
	 */
	void onPropertiesUpdated() {
		TermDistance newValue = new TermDistance();
		newValue.distance = distance;
		newValue.text = text;
		setValue(newValue);
	}

	@Override
	public TermDistance getValue() {
		return value;
	}

	@Override
	public void setValue(TermDistance value) {
		this.distance = value.distance;
		this.text = value.text;
		set("value", this.value, value, () -> this.value = value);
	}
}