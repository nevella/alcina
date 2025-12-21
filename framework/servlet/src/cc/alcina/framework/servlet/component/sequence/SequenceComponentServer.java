package cc.alcina.framework.servlet.component.sequence;

import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceComponent;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequencePlace;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

@Directed.Delegating
public class SequenceComponentServer extends Model.Fields
		implements SequenceBehaviorsServer {
	@Directed
	SequenceComponent component;

	public SequenceComponentServer(Model header,
			InstanceProperty<?, SequencePlace> sequencePlaceProperty) {
		this.component = new SequenceComponent(header, sequencePlaceProperty);
	}

	@Override
	public List<?> provideFiltereedSequenceElements() {
		return component.provideFiltereedSequenceElements();
	}
}
