package cc.alcina.framework.gwt.client.dirndl.cmp.command;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.behavior.InterceptKeys;
import com.google.gwt.user.client.Event.NativePreviewEvent;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorCommand;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorEvent;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeyBinding.MatchData;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.event.ReflectedEvents;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
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
	@FunctionalInterface
	public interface EventDispatcher {
		void dispatch(Class<? extends ModelEvent> eventType);
	}

	public static class ShortcutBehaviorChanged
			extends ModelEvent<InterceptKeys, ShortcutBehaviorChanged.Handler> {
		public interface Handler extends NodeEvent.Handler {
			void onShortcutBehaviorChanged(ShortcutBehaviorChanged event);
		}

		public interface Binding extends Handler {
			@Override
			default void
					onShortcutBehaviorChanged(ShortcutBehaviorChanged event) {
				((Model) this).bindings().onNodeEvent(event);
			}
		}

		@Override
		public void dispatch(ShortcutBehaviorChanged.Handler handler) {
			handler.onShortcutBehaviorChanged(this);
		}
	}

	List<MatchData> boundEvents;

	EventDispatcher eventDispatcher;

	CommandContext.Provider commandContextProvider;

	InterceptKeys lastBehavior;

	Model topModel;

	public KeybindingsHandler(EventDispatcher eventDispatcher,
			CommandContext.Provider commandContextProvider) {
		this.eventDispatcher = eventDispatcher;
		this.commandContextProvider = commandContextProvider;
		boundEvents = Registry.query(NodeEvent.class).registrations()
				.filter(type -> Reflections.at(type).has(KeyBinding.class))
				.map(clazz -> (Class<? extends ModelEvent>) clazz)
				.map(MatchData::new).collect(Collectors.toList());
		List<String> invalid = boundEvents.stream().map(MatchData::checkInvalid)
				.filter(Objects::nonNull).collect(Collectors.toList());
		if (invalid.size() > 0) {
			Ax.err(invalid);
			throw new IllegalStateException();
		}
	}

	public Stream<MatchData> getContextMatches() {
		Set<Class<? extends CommandContext>> contexts = commandContextProvider
				.getContexts();
		return boundEvents.stream().filter(eventType -> KeyBinding.Support
				.matches(contexts, eventType, null));
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
				{
					AppSuggestorCommand command = Reflections.at(eventType)
							.annotation(AppSuggestorCommand.class);
					if (command != null) {
						Class<? extends AppSuggestorEvent> suggestorEventType = (Class<? extends AppSuggestorEvent>) eventType;
						if (!AppSuggestorCommand.Support
								.testFilter(suggestorEventType, command)) {
							return;
						}
					}
				}
				{
					KeyBinding keyBinding = Reflections.at(eventType)
							.annotation(KeyBinding.class);
					if (keyBinding != null) {
						if (!KeyBinding.Support.testFilter(eventType,
								keyBinding)) {
							return;
						}
					}
				}
				nativeEvent.stopPropagation();
				nativeEvent.preventDefault();
				eventDispatcher.dispatch(eventType);
			}
		}
	}

	public void registerTopModel(Model topModel) {
		this.lastBehavior = null;
		this.topModel = topModel;
		this.topModel.on(ReflectedEvents.PlaceChanged.class)
				.signal(this::emitBehavior);
	}

	public void emitBehavior() {
		InterceptKeys behavior = new InterceptKeys();
		getContextMatches().flatMap(m -> m.entries.stream())
				.forEach(e -> behavior.addIntercept(e.binding.key(),
						e.binding.keyCode(), e.modifiers));
		if (!Objects.equals(behavior, lastBehavior)) {
			this.topModel.emitEvent(ShortcutBehaviorChanged.class, behavior);
			lastBehavior = behavior;
		}
	}
}