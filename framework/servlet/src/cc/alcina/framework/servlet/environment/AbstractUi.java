package cc.alcina.framework.servlet.environment;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.user.client.Event;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeybindingsHandler;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.model.NotificationObservable;
import cc.alcina.framework.gwt.client.util.KeyboardShortcuts;
import cc.alcina.framework.servlet.ServletLayerTopics;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.ProcessingException;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponent;
import cc.alcina.framework.servlet.servlet.AuthenticationTokenStore;

/**
 * <p>
 * Activity routing - all AbstractUi apps use the {@link Place} and
 * {@link Activity} system for modelling main UI state. Generally the toplevel
 * UI component will observe the app place property and remit
 * {@link ModelEvents.PlaceChanged} events
 */
@TypedProperties
@TypeSerialization(flatSerializable = false, reflectiveSerializable = false)
public abstract class AbstractUi<P extends Place> extends Bindable.Fields
		implements RemoteUi {
	public interface ClientExceptionNotificationPolicy {
		public static class No implements ClientExceptionNotificationPolicy {
			@Override
			public boolean isNotifyException(ProcessingException message) {
				return false;
			}
		}

		public static class Once implements ClientExceptionNotificationPolicy {
			boolean notified;

			@Override
			public boolean isNotifyException(ProcessingException message) {
				if (!notified) {
					notified = true;
					return true;
				} else {
					return false;
				}
			}
		}

		boolean isNotifyException(ProcessingException message);
	}

	public class CommandContextProviderImpl implements CommandContext.Provider {
		@Override
		public Set<Class<? extends CommandContext>> getContexts() {
			Set<Class<? extends CommandContext>> commandContexts = new LinkedHashSet<>();
			commandContexts.addAll(getAppCommandContexts());
			return commandContexts;
		}
	}

	DirectedLayout layout;

	/*
	 * In most cases Environment.get() will also give access to the environment
	 * - but in cases where (say) an off-thread event is being processed, this
	 * access to the environment must be used
	 * 
	 */
	@Property.Not
	Environment environment;

	boolean reloading;

	protected ClientExceptionNotificationPolicy clientExceptionNotificationPolicy = new ClientExceptionNotificationPolicy.No();

	List<ProcessingException> notifiedExceptions = new ArrayList<>();

	/**
	 * The current place, transformed from the browser url.
	 */
	public P place;

	private KeyboardShortcuts keyboardShortcuts;

	private KeybindingsHandler keybindingsHandler;

	RemoteComponent remoteComponent;

	public RemoteComponent getRemoteComponent() {
		return remoteComponent;
	}

	public PackageProperties._AbstractUi.InstanceProperties properties() {
		return PackageProperties.abstractUi.instance(this);
	}

	@Override
	public void reloadApp(String message) {
		/*
		 * edge-case - because restart will interfere with message passing (and
		 * cause multiple fires), ignore all except first call
		 */
		if (!reloading) {
			Preconditions.checkState(Ax.isTest());
			reloading = true;
			NotificationObservable.of(message).publish();
			RemoteUi.get().flush();
			ServletLayerTopics.topicRestartConsole.signal();
		}
	}

	@Override
	public boolean isNotifyException(ProcessingException message) {
		return clientExceptionNotificationPolicy.isNotifyException(message);
	}

	@Override
	@Property.Not
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	public KeybindingsHandler getKeybindingsHandler() {
		return keybindingsHandler;
	}

	public void bindKeyboardShortcuts(boolean bound) {
		if (bound) {
			keyboardShortcuts = new KeyboardShortcuts();
			Event.addNativePreviewHandler(keyboardShortcuts);
		}
		keyboardShortcuts.deltaHandler(keybindingsHandler, bound);
	}

	public abstract Set<Class<? extends CommandContext>>
			getAppCommandContexts();

	public CommandContext.Provider getCommandContextProvider() {
		return new CommandContextProviderImpl();
	}

	public void setPlace(P place) {
		set("place", this.place, place, () -> this.place = place);
	}

	@Override
	public String toString() {
		return Ax.format("%s::%s", NestedName.get(this),
				Environment.get().access().getConnectedClientUid());
	}

	@Override
	public final void render() {
		PlaceChangeEvent.Handler placeChangeHandler = evt -> {
			properties().place().set((P) evt.getNewPlace());
		};
		keybindingsHandler = new KeybindingsHandler(eventType -> {
			layout.layoutResult.getRoot().dispatch(eventType, null);
		}, getCommandContextProvider());
		Client.eventBus().addHandler(PlaceChangeEvent.TYPE, placeChangeHandler);
		Registry.register().singleton(ContextResolver.Default.class,
				new RemoteResolver.Default());
		layout = render0();
	}

	@Override
	public void end() {
		if (layout != null) {
			try {
				layout.remove();
				layout = null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	protected abstract DirectedLayout render0();

	protected RemoteComponentProtocol.Session getSession() {
		return environment.access().getSession();
	}

	public void invokeInEnvironmentContext(Runnable runnable) {
		environment.access().invoke(runnable);
	}

	protected AuthenticationTokenStore getAuthenticationTokenStore() {
		return new RemoteAuthenticationStore(environment);
	}

	public RemoteUi withComponent(RemoteComponent remoteComponent) {
		this.remoteComponent = remoteComponent;
		return this;
	}
}