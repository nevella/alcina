package cc.alcina.framework.gwt.client.dirndl.cmp.command;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Event.NativePreviewEvent;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorCommand;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorEvent;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorRequest;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeyBinding.MatchData;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.util.KeyboardShortcuts;

/**
 * FIXME - recompute on code module load (registry modification/listener)
 * 
 * <p>
 * This class registers ModelEvent subtypes which have a {@link KeyBinding}
 * annotation (detailing the binding). The applicable keybindings will be
 * filtered at event time by the intersection of KeyBinding.context(s) and
 * CommandContext.Provider.getContexts()
 * 
 * <p>
 * The event must have non-empty contexts - either via its {@link KeyBinding}
 * annotation or by a {@link AppSuggestorCommand} annotation on the type (those
 * form a tree so are more ergonomic)
 * 
 * <p>
 * The app must register a {@link CommandContext.Provider} implementation
 */
public class KeybindingsHandler implements KeyboardShortcuts.Handler {
	List<MatchData> boundEvents;

	EventDispatcher eventDispatcher;

	CommandContext.Provider commandContextProvider;

	@FunctionalInterface
	public interface EventDispatcher {
		void dispatch(Class<? extends ModelEvent> eventType);
	}

	public KeybindingsHandler(EventDispatcher eventDispatcher,
			CommandContext.Provider commandContextProvider) {
		this.eventDispatcher = eventDispatcher;
		this.commandContextProvider = commandContextProvider;
		AppSuggestorRequest suggestorRequest = new AppSuggestorRequest();
		boundEvents = Registry.query(NodeEvent.class).registrations()
				.filter(type -> Reflections.at(type).has(KeyBinding.class))
				.map(clazz -> (Class<? extends ModelEvent>) clazz)
				.map(MatchData::new).collect(Collectors.toList());
		List<String> invalid = boundEvents.stream().map(MatchData::checkInvalid)
				.filter(Objects::nonNull).toList();
		if (invalid.size() > 0) {
			Ax.err(invalid);
			throw new IllegalStateException();
		}
	}

	@Override
	public void checkShortcut(NativePreviewEvent event, NativeEvent nativeEvent,
			String type, boolean altKey, boolean shiftKey, int keyCode) {
		if (nativeEvent != null && type.equals("keydown")) {
			Set<Class<? extends CommandContext>> contexts = commandContextProvider
					.getContexts();
			Optional<Class<? extends ModelEvent>> match = boundEvents.stream()
					.filter(eventType -> KeyBinding.Support.matches(contexts,
							eventType, nativeEvent))
					.findFirst().map(MatchData::getEventType);
			if (match.isPresent()) {
				Class<? extends ModelEvent> eventType = match.get();
				AppSuggestorCommand command = Reflections.at(eventType)
						.annotation(AppSuggestorCommand.class);
				if (command != null) {
					Class<? extends AppSuggestorEvent> suggestorEventType = (Class<? extends AppSuggestorEvent>) eventType;
					if (!AppSuggestorCommand.Support
							.testFilter(suggestorEventType, command)) {
						return;
					}
				}
				nativeEvent.stopPropagation();
				nativeEvent.preventDefault();
				eventDispatcher.dispatch(eventType);
			}
		}
	}
}