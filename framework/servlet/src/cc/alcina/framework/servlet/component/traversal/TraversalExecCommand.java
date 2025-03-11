package cc.alcina.framework.servlet.component.traversal;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.util.Csv;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.CopyToClipboard;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel.PreText;
import cc.alcina.framework.gwt.client.dirndl.model.NotificationObservable;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay;

@Registration.Self
public interface TraversalExecCommand<T> {
	default String name() {
		return getClass().getSimpleName();
	}

	void execCommand(ModelEvent event, List<T> filteredElements);

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
			String message = Registry.query(TraversalExecCommand.class)
					.implementations()
					.map(qe -> Ax.format("%s - %s", qe.name(),
							NestedName.get(qe)))
					.collect(Collectors.joining("\n"));
			Overlay.attributes().withContents(new PreText(message))
					.positionViewportCentered()
					.withRemoveOnMouseDownOutside(true).create().open();
		}

		static void execCommand(ModelEvent event, List filteredSequenceElements,
				String commandString) {
			Optional<TraversalExecCommand> exec = Registry
					.query(TraversalExecCommand.class).implementations()
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
