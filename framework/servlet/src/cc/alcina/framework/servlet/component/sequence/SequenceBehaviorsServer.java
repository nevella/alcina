package cc.alcina.framework.servlet.component.sequence;

import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.HasFilteredSequenceElements;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceEvents;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceEvents.SequenceGenerationExceptionEvent;
import cc.alcina.framework.servlet.component.shared.CopyToClipboardHandler;
import cc.alcina.framework.servlet.component.shared.ExecCommand;
import cc.alcina.framework.servlet.environment.RemoteUi;

public interface SequenceBehaviorsServer
		extends ExecCommand.PerformCommand.Handler, CopyToClipboardHandler,
		SequenceEvents.SequenceGenerationExceptionEvent.Handler,
		HasFilteredSequenceElements {
	@Override
	default void onPerformCommand(ExecCommand.PerformCommand event) {
		SequenceExecCommand.Support.execCommand(event,
				provideFiltereedSequenceElements(), event.getModel());
	}

	@Override
	default void onSequenceGenerationExceptionEvent(
			SequenceGenerationExceptionEvent event) {
		RemoteUi.Invoke.exceptionNotifier().accept(event.getModel());
	}
}
