package cc.alcina.framework.gwt.client.dirndl.model.component;

import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeyBinding;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeyBinding.MatchData;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeybindingsHandler;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.FilterContentsFilterable;
import cc.alcina.framework.gwt.client.dirndl.layout.Feature_Dirndl_Documentation;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.edit.StringInput;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay;
import cc.alcina.framework.gwt.client.objecttree.search.packs.SearchUtils;

/**
 * <p>
 * Models a view of all keyboard shortcuts
 */
/**
 * <p>
 * Design decision - I was very tempted to create a Dialog class for this.
 * <p>
 * But Dialog classes always end up expanding and expanding...and what's really
 * generalisable is the UI pattern - there's no benefit in say having an
 * abstract with `heading`, `body`, `footer` - in fact it's a loss.
 * <p>
 * So the solution is a few sass mixins (1-header-elt panel,
 * 2-header-elt-panel)...and we're good. Note the requirement for a spacer elt
 * if you use a footer (possibly it could be done without with better grid
 * thinking)
 */
@Feature.Ref(Feature_Dirndl_Documentation.DesignNotes.class)
public class KeyboardShortcutsArea extends Model.All
		implements ModelEvents.FilterContents.ReflectFiltering {
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
		Overlay.attributes()
				.withContents(new KeyboardShortcutsArea(keybindingsHandler))
				.positionViewportCentered().withCloseOnMouseDownOutside(true)
				.create().open();
	}
}
