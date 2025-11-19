package cc.alcina.framework.servlet.component.shared;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.event.Filterable;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.component.KeyboardShortcutsArea;
import cc.alcina.framework.gwt.client.dirndl.model.edit.StringInput;
import cc.alcina.framework.gwt.client.objecttree.search.packs.SearchUtils;

/**
 * <p>
 * Models a view of all exec commands
 * 
 * @see KeyboardShortcutsArea
 */
public class ExecCommandsArea extends Model.All
		implements ModelEvents.Filter.Emitter {
	public Heading heading = new Heading("Exec commands");

	@StringInput.FocusOnBind
	@Directed(tag = "contents-filter")
	@Directed(reemits = { ModelEvents.Input.class, ModelEvents.Filter.class })
	public StringInput filter;

	@Directed.Wrap("commands")
	public List<CommandArea> commandAreas;

	public ExecCommandsArea(Stream<? extends ExecCommand> commands) {
		filter = new StringInput();
		filter.setPlaceholder("Filter metadata");
		commandAreas = commands.map(CommandArea::new).sorted()
				.collect(Collectors.toList());
	}

	@Directed(tag = "command-area")
	class CommandArea extends Filterable.FilterFilterable.Abstract
			implements Comparable<CommandArea>, DomEvents.Click.Handler {
		String name;

		String description;

		CommandArea(ExecCommand command) {
			name = command.name();
			description = command.description();
		}

		@Override
		public boolean matchesFilter(String filterString) {
			return SearchUtils.containsIgnoreCase(filterString, name,
					description);
		}

		@Override
		public int compareTo(CommandArea o) {
			return name.compareTo(o.name);
		}

		@Override
		public void onClick(Click event) {
			event.reemitAs(this, ExecCommand.PerformCommand.class, name);
		}
	}
}
