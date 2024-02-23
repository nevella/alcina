package cc.alcina.framework.gwt.client.dirndl.event;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Event.NativePreviewEvent;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.cmp.AppSuggestorCommand;
import cc.alcina.framework.gwt.client.dirndl.cmp.AppSuggestorEvent;
import cc.alcina.framework.gwt.client.dirndl.cmp.AppSuggestorRequest;
import cc.alcina.framework.gwt.client.util.GlobalKeyboardShortcuts;

/**
 * FIXME - recompute on code module load (registry modification/listener)
 */
public class KeybindingsHandler implements GlobalKeyboardShortcuts.Handler {
	List<KeyBinding.MatchData> boundEvents;

	EventDispatcher eventDispatcher;

	@FunctionalInterface
	public interface EventDispatcher {
		void dispatch(Class<? extends ModelEvent> eventType);
	}

	public KeybindingsHandler(EventDispatcher eventDispatcher) {
		this.eventDispatcher = eventDispatcher;
		AppSuggestorRequest suggestorRequest = new AppSuggestorRequest();
		Set<Class<? extends CommandContext>> appExcludes = suggestorRequest
				.appExcludes();
		Predicate<Class<? extends ModelEvent>> visible = clazz -> {
			AppSuggestorCommand command = Reflections.at(clazz)
					.annotation(AppSuggestorCommand.class);
			if (command != null) {
				Ax.out(clazz.getName());
				Set<Class<? extends CommandContext>> commandContexts = Set
						.of(AppSuggestorCommand.Support.contexts(command));
				boolean excludedContext = CommonUtils
						.hasIntersection(appExcludes, commandContexts);
				if (excludedContext) {
					return false;
				}
			}
			return true;
		};
		boundEvents = Registry.query(NodeEvent.class).registrations()
				.filter(type -> Reflections.at(type).has(KeyBinding.class))
				.map(clazz -> (Class<? extends ModelEvent>) clazz)
				.filter(visible).map(KeyBinding.MatchData::new)
				.collect(Collectors.toList());
	}

	@Override
	public void checkShortcut(NativePreviewEvent event, NativeEvent nativeEvent,
			String type, boolean altKey, boolean shiftKey, int keyCode) {
		if (nativeEvent != null && type.equals("keydown")) {
			Optional<Class<? extends ModelEvent>> match = boundEvents.stream()
					.filter(eventType -> KeyBinding.Support.matches(eventType,
							nativeEvent))
					.findFirst().map(KeyBinding.MatchData::getEventType);
			if (match.isPresent()) {
				Class<? extends ModelEvent> eventType = match.get();
				AppSuggestorCommand command = Reflections.at(eventType)
						.annotation(AppSuggestorCommand.class);
				if (command != null) {
					Class<? extends AppSuggestorEvent> omniType = (Class<? extends AppSuggestorEvent>) eventType;
					if (!AppSuggestorCommand.Support.testFilter(omniType,
							command)) {
						return;
					}
				}
				nativeEvent.stopPropagation();
				nativeEvent.preventDefault();
				eventDispatcher.dispatch(eventType);
				// JadexLayout.get().dispatchTopEvent(eventType, null);
			}
		}
	}
}