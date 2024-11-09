package cc.alcina.framework.gwt.client.dirndl.model.component;

import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeyBinding;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeyBinding.MatchData;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeybindingsHandler;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.FilterContentsFilterable;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.edit.StringInput;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay;
import cc.alcina.framework.gwt.client.objecttree.search.packs.SearchUtils;

/**
 * Models a view of all keyboard shortcuts
 */
@Directed(
	reemits = { ModelEvents.Filter.class, ModelEvents.FilterContents.class })
public class KeyboardShortcutsArea extends Model.All
		implements ModelEvents.FilterContents.Emitter {
	public Heading heading = new Heading("Keyboard Shortcuts");

	@StringInput.FocusOnBind
	@Directed(tag = "contents-filter")
	@Directed(reemits = { ModelEvents.Input.class, ModelEvents.Filter.class })
	public StringInput filter;

	@Directed.Wrap("shortcuts")
	public List<Shortcut> shortcuts;

	public KeyboardShortcutsArea(KeybindingsHandler keybindingsHandler) {
		filter = new StringInput();
		filter.setPlaceholder("Filter metadata");
		shortcuts = keybindingsHandler.getContextMatches().map(Shortcut::new)
				.sorted().collect(Collectors.toList());
	}

	class Shortcut extends FilterContentsFilterable.Abstract
			implements Comparable<Shortcut> {
		@Directed.Wrap("modifier-cell")
		String modifier;

		String key;

		String commandText;

		Shortcut(KeyBinding.MatchData matchData) {
			MatchData.Entry entry = matchData.entries.get(0);
			modifier = entry.modifiers.stream().map(Object::toString)
					.collect(Collectors.joining(", "));
			key = entry.binding.key();
			commandText = NestedName.get(matchData.eventType);
		}

		@Override
		public boolean matchesFilter(String filterString) {
			return SearchUtils.containsIgnoreCase(filterString, commandText,
					key);
		}

		@Override
		public int compareTo(Shortcut o) {
			return commandText.compareTo(o.commandText);
		}
	}

	public static void show(KeybindingsHandler keybindingsHandler) {
		Overlay.builder()
				.withContents(new KeyboardShortcutsArea(keybindingsHandler))
				.positionViewportCentered().withRemoveOnMouseDownOutside(true)
				.build().open();
	}
}
