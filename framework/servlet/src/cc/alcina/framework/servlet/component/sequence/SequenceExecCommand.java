package cc.alcina.framework.servlet.component.sequence;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.util.Csv;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.CopyToClipboard;
import cc.alcina.framework.gwt.client.dirndl.model.NotificationObservable;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay;
import cc.alcina.framework.servlet.component.sequence.SequenceBrowser.Ui;
import cc.alcina.framework.servlet.component.shared.ExecCommand;
import cc.alcina.framework.servlet.component.shared.ExecCommandsArea;

@Registration.Self
public interface SequenceExecCommand<T> extends ExecCommand<T> {
	public static class ListCommands implements SequenceExecCommand {
		@Override
		public String name() {
			return "l";
		}

		@Override
		public void execCommand(ModelEvent event, List filteredElements) {
			Support.showAvailableCommands();
		}
	}

	public static class ExportToCsv implements SequenceExecCommand {
		@Override
		public String name() {
			return "csv";
		}

		@Override
		public void execCommand(ModelEvent event, List filteredElements) {
			String csvText = Csv.fromCollection(filteredElements)
					.toOutputString();
			event.reemitAs(event.getContext().node.getModel(),
					CopyToClipboard.class, csvText);
			String path = "/tmp/sequence.csv";
			Io.write().string(csvText).toPath(path);
			NotificationObservable
					.of("CSV copied to clipboard and written to %s", path)
					.publish();
		}
	}

	static class Support {
		static void showAvailableCommands() {
			Stream<SequenceExecCommand> commands = Registry
					.query(SequenceExecCommand.class).implementations();
			Overlay.attributes().withContents(new ExecCommandsArea(commands))
					.withLogicalParent(Ui.get().page).positionViewportCentered()
					.withRemoveOnMouseDownOutside(true).create().open();
		}

		static void execCommand(ModelEvent event, List filteredSequenceElements,
				String commandString) {
			Optional<SequenceExecCommand> exec = Registry
					.query(SequenceExecCommand.class).implementations()
					.filter(i -> i.name().equals(commandString)).findFirst();
			if (exec.isPresent()) {
				exec.get().execCommand(event, filteredSequenceElements);
			} else {
				NotificationObservable
						.of("No command '%s' found", commandString).publish();
			}
		}
	}
}