package cc.alcina.framework.servlet.environment;

import java.util.LinkedHashSet;
import java.util.Set;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.user.client.Event;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeybindingsHandler;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.util.KeyboardShortcuts;

@TypedProperties
@TypeSerialization(flatSerializable = false, reflectiveSerializable = false)
public abstract class AbstractUi<P extends Place> extends Bindable.Fields
		implements RemoteUi {
	public static PackageProperties._AbstractUi properties = PackageProperties.abstractUi;

	DirectedLayout layout;

	/*
	 * In most cases Environment.get() will also give access to the environment
	 * - but in cases where (say) an off-thread event is being processed, this
	 * access to the environment must be used
	 * 
	 */
	@Property.Not
	Environment environment;

	@Override
	@Property.Not
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	public P place;

	private KeyboardShortcuts keyboardShortcuts;

	private KeybindingsHandler keybindingsHandler;

	public class CommandContextProviderImpl implements CommandContext.Provider {
		@Override
		public Set<Class<? extends CommandContext>> getContexts() {
			Set<Class<? extends CommandContext>> commandContexts = new LinkedHashSet<>();
			commandContexts.add(getAppCommandContext());
			return commandContexts;
		}
	}

	public void bindKeyboardShortcuts(boolean bound) {
		if (bound) {
			keyboardShortcuts = new KeyboardShortcuts();
			Event.addNativePreviewHandler(keyboardShortcuts);
		}
		keyboardShortcuts.deltaHandler(keybindingsHandler, bound);
	}

	public abstract Class<? extends CommandContext> getAppCommandContext();

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
		PlaceChangeEvent.Handler placeChangeHandler = evt -> properties.place
				.set(this, (P) evt.getNewPlace());
		keybindingsHandler = new KeybindingsHandler(eventType -> {
			layout.layoutResult.getRoot().dispatch(eventType, null);
		}, getCommandContextProvider());
		Client.eventBus().addHandler(PlaceChangeEvent.TYPE, placeChangeHandler);
		layout = render0();
	}

	protected abstract DirectedLayout render0();

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
}