package cc.alcina.framework.gwt.client.dirndl.model.component;

import java.util.List;
import java.util.stream.Collectors;

import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeyBinding;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeybindingsHandler;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeyBinding.MatchData;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay;

/**
 * Models a view of all keyboard shortcuts
 */
public class KeyboardShortcutsArea extends Model.All {
	public List<Shortcut> shortcuts;

	public KeyboardShortcutsArea(KeybindingsHandler keybindingsHandler) {
		shortcuts = keybindingsHandler.getContextMatches().map(Shortcut::new)
				.collect(Collectors.toList());
	}

	class Shortcut extends Model.All {
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
	}

	public static void show(KeybindingsHandler keybindingsHandler) {
		Overlay.builder()
				.withContents(new KeyboardShortcutsArea(keybindingsHandler))
				.positionViewportCentered().withRemoveOnMouseDownOutside(true)
				.build().open();
	}
}
