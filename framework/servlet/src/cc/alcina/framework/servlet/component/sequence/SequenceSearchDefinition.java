package cc.alcina.framework.servlet.component.sequence;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.domain.search.BindableSearchDefinition;
import cc.alcina.framework.common.client.logic.reflection.Registration;

@Registration(SequenceSearchDefinition.class)
public abstract class SequenceSearchDefinition
		extends BindableSearchDefinition {
	// for serialization
	public static class Noop extends SequenceSearchDefinition {
		@Override
		public Class<? extends Bindable> queriedBindableClass() {
			throw new UnsupportedOperationException();
		}
	}
}
