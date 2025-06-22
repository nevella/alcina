package cc.alcina.framework.servlet.component.traversal;

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
import cc.alcina.framework.servlet.component.shared.ExecCommand;
import cc.alcina.framework.servlet.component.shared.ExecCommandsArea;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowser.Ui;

@Registration.Self
public interface TraversalExecCommand<T> extends ExecCommand<T> {
	public static class ListCommands implements TraversalExecCommand {
		@Override
		public String name() {
			return "l";
		}

		@Override
		public void execCommand(ModelEvent event, List filteredElements) {
			Support.showAvailableCommands();
		}
	}

	public static class ExportToCsv implements TraversalExecCommand {
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
			String path = "/tmp/traversal.csv";
			Io.write().string(csvText).toPath(path);
			NotificationObservable
					.of("CSV copied to clipboard and written to %s", path)
					.publish();
		}
	}

	static class Support {
		public static void showAvailableCommands() {
			Stream<TraversalExecCommand> commands = Registry
					.query(TraversalExecCommand.class).implementations();
			Overlay.attributes().withContents(new ExecCommandsArea(commands))
					.withLogicalParent(Ui.get().page).positionViewportCentered()
					.withCloseOnMouseDownOutside(true).create().open();
		}

		static void execCommand(ModelEvent event, List list,
				String commandString) {
			Optional<TraversalExecCommand> exec = Registry
					.query(TraversalExecCommand.class).implementations()
					.filter(i -> i.name().equals(commandString)).findFirst();
			if (exec.isPresent()) {
				exec.get().execCommand(event, list);
			} else {
				NotificationObservable
						.of("No command '%s' found", commandString).publish();
			}
		}
	}
}
