package cc.alcina.framework.gwt.client.dirndl.cmp.sequence;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.domain.search.BindableSearchDefinition;
import cc.alcina.framework.common.client.logic.reflection.Registration;

@Registration(SequenceSearchDefinition.class)
public abstract class SequenceSearchDefinition
		extends BindableSearchDefinition {
	public abstract Class<? extends Sequence> sequenceClass();

	// for serialization
	public static class Noop extends SequenceSearchDefinition {
		@Override
		public Class<? extends Bindable> queriedBindableClass() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Class<? extends Sequence> sequenceClass() {
			/*
			 * will never match a concrete sequence class
			 */
			return Sequence.class;
		}
	}
}
