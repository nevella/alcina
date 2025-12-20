package cc.alcina.framework.servlet.component.sequence;

import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.HasFilteredSequenceElements;
import cc.alcina.framework.servlet.component.shared.ExecCommand;

public interface SequenceBehaviorsServer extends
		ExecCommand.PerformCommand.Handler, HasFilteredSequenceElements {
	@Override
	default void onPerformCommand(ExecCommand.PerformCommand event) {
		SequenceExecCommand.Support.execCommand(event,
				provideFiltereedSequenceElements(), event.getModel());
	}
}
