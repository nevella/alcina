package cc.alcina.framework.gwt.client.dirndl.event;

import java.util.List;
import java.util.Optional;

import com.google.gwt.event.shared.EventHandler;
import com.google.web.bindery.event.shared.SimpleEventBus;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registration.EnvironmentRegistration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.ClassSerialization;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.common.client.util.HasDisplayName.ClassDisplayName;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.TopLevelMissedEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.model.Choices;
import cc.alcina.framework.gwt.client.dirndl.model.HasNode;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 * <p>
 * A ModelEvent is a logical event in terms of the abstract UI model - a
 * correctly modelled Dirndl app will use dozens of different model event types
 * to reflect the meaning of user interactions, and to respond to those
 * meanings.
 * 
 * <p>
 * An example: a link with text "Add" will emit a DOM click event when clicked,
 * this should be translated to a ModelEvent.Add (via @Directed.reemits()) model
 * event and *that* model event should be logically handled - possibly several
 * layers higher in the UI stack. FIXME give a demo app example
 * 
 * <p>
 * The Dirndl event model heavily favours "translate DOM events to model events
 * early" - and handle _semantic_ (aka ModelEvent) events rather than direct DOM
 * events.
 * 
 * <h2>Gotchas</h2>
 * <p>
 * Event reemission: if a model reeemits an event (say it receives a Selected
 * event and, in consequence, emits a Selected event - see {@link Choices}) then
 * it must check for reemission to avoid an infinite loop with a call to
 * {@link ModelEvent#wasReemitted}. See {@link Choices.Single#onSelected}
 *
 * <p>
 * Naming note. Use present/imperative when the event is "a command UI gesture
 * was made" - e.g. clicking on the 'x' button somewhere commands 'Close', so
 * emit a {@code CloseEvent}. But when a UI <i>change</i> event occurs - such as
 * 'the popup closed', use the past simple - {@code ClosedEvent}.
 *
 * <p>
 * Naming note: 'dispatch' - emit/enqueue an event. 'Fire' call an event handler
 * with an event instance. There may be some lingering misuse...and there's
 * confusion all over the web. But if you "dispatch" a physical package, that's
 * really the beginning of a process - not a step - so I think this usage is
 * reasonable. Note that GWT event bus sort of has it the other way round - sad.
 *
 * <p>
 * usage note: there are many ways to reemit or emit an event from code -
 * preferred are the fluent ones ({@code DirectedLayout.Node.dispatch_, {@code
 * NodeEvent.reemitAs})
 * 
 * <p>
 * TODO - descendant event
 *
 *
 * @param <T>
 *
 * @param <H>
 */
@Registration(ClassSerialization.class)
public abstract class ModelEvent<T, H extends NodeEvent.Handler>
		extends NodeEvent<H> implements NodeEvent.WithoutDomBinding,
		Registration.Ensure, Registration.AllSubtypesClient {
	// Although this is the one 'dispatch' call, access it via context (since
	// context is always required)
	static void dispatch(Context context, Class<? extends ModelEvent> type,
			Object model) {
		if (context.node == null) {
			/*
			 * detached before fire
			 */
			return;
		}
		ModelEvent modelEvent = Reflections.newInstance(type);
		context.setNodeEvent(modelEvent);
		modelEvent.setModel(model);
		DirectedLayout.dispatchModelEvent(modelEvent);
		if (!modelEvent.handled) {
			Optional<TopLevelHandler> handler = Registry
					.optional(TopLevelHandler.class, type);
			if (handler.isPresent()) {
				((SimpleEventBus) Client.eventBus()).fireEventFromSource(
						modelEvent, context.node, List.of(handler.get()));
			} else {
				Optional<TopLevelCatchallHandler> catchallHandler = Registry
						.optional(TopLevelCatchallHandler.class);
				if (catchallHandler.isPresent()) {
					catchallHandler.get().handle(modelEvent);
				}
			}
		}
		modelEvent.onDispatchComplete();
	}

	public static String staticDisplayName(Class<? extends ModelEvent> clazz) {
		Optional<ClassDisplayName> classDisplayName = Registry
				.optional(HasDisplayName.ClassDisplayName.class, clazz);
		String value = null;
		if (classDisplayName.isPresent()) {
			value = classDisplayName.get().displayName();
		} else {
			value = CommonUtils
					.deInfix(clazz.getSimpleName().replaceFirst("Event$", ""));
		}
		return TextProvider.get().getUiObjectText(clazz,
				TextProvider.DISPLAY_NAME, value);
	}

	private boolean handled;

	public ModelEvent() {
	}

	public void bubble() {
		getContext().bubble();
	}

	public boolean checkReemitted(HasNode hasNode) {
		if (wasReemitted(hasNode.provideNode())) {
			bubble();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public Class<H> getHandlerClass() {
		return Reflections.at(getClass()).getGenericBounds().bounds.get(1);
	}

	public <T0 extends T> T0 getModel() {
		return (T0) this.model;
	}

	public String getName() {
		return staticDisplayName(getClass());
	}

	public boolean isHandled() {
		return handled;
	}

	protected void onDispatchComplete() {
	}

	public void setHandled(boolean handled) {
		this.handled = handled;
	}

	public void setTypedModel(T typedModel) {
		this.model = typedModel;
	}

	// normally call checkreemitted, since there's not much else to do
	public boolean wasReemitted(Node node) {
		return getContext().getPrevious() != null
				&& getContext().getPrevious().reemission == node;
	}

	public <M extends ModelEvent> M withTypedModel(T typedModel) {
		this.model = typedModel;
		return (M) this;
	}

	/**
	 * The event should be routed to descendant nodes, rather than ancestors
	 */
	public abstract static class DescendantEvent<T, H extends NodeEvent.Handler, E extends ModelEvent.Emitter>
			extends ModelEvent<T, H> {
		public Class<E> getEmitterClass() {
			return Reflections.at(getClass()).getGenericBounds().bounds.get(2);
		}
	}

	// Marker interface - for descendant events, the receiver (handler) will
	// bind to the nearest ancestor with this type
	public interface Emitter {
	}

	/**
	 * Marks a ModelEvent subclass as not requiring a handler - there are only a
	 * few (Input, Change) - in almost all other cases a model event without a
	 * handler is a dev issue.
	 *
	 *
	 *
	 */
	public interface NoHandlerRequired {
	}

	@EnvironmentRegistration
	public static interface TopLevelCatchallHandler {
		void handle(ModelEvent unhandledEvent);

		public static class MissedEventEmitter
				implements TopLevelCatchallHandler {
			private Emitter emittingModel;

			public <MEE extends MissedEventEmitter> MEE withEmittingModel(
					TopLevelMissedEvent.Emitter emittingModel) {
				this.emittingModel = emittingModel;
				return (MEE) this;
			}

			int depth = 0;

			@Override
			public void handle(ModelEvent unhandledEvent) {
				try {
					/*
					 * the depth check prevents infinite recursive loops
					 */
					if (depth > 0) {
						return;
					}
					depth++;
					((Model) emittingModel).emitEvent(TopLevelMissedEvent.class,
							unhandledEvent);
				} finally {
					depth--;
				}
			}
		}
	}

	/**
	 * <p>
	 * Marker for top-level handlers of unhandled ModelEvents (basically
	 * links/actions where the context doesn't matter, such as 'logout' or
	 * 'privacy')
	 *
	 * <p>
	 * The handlers are currently singletons (since EnvironmentRegistration only
	 * supports singletons)
	 *
	 *
	 */
	/*
	 * Nope, this leaks. Instead annotate subtypes as so:
	 @formatter:off

@Registration.EnvironmentOptionalRegistration(@Registration({
		TopLevelHandler.class, CreateDocument.class }))

	 @formatter:on
	 */
	// @Registration.NonGenericSubtypes(TopLevelHandler.class)
	@Reflected
	@EnvironmentRegistration
	public static interface TopLevelHandler<M extends ModelEvent>
			extends EventHandler, ModelEvent.Handler {
	}

	/**
	 * A useful interface for top-level general/framework models, allowing event
	 * receiver composition (normally the 'main' directed property will be the
	 * dispatch delegate)
	 */
	public interface DelegatesDispatch {
		Model provideDispatchDelegate();
	}
}