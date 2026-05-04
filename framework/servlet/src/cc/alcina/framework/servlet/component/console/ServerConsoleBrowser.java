package cc.alcina.framework.servlet.component.console;

import java.util.Set;

import cc.alcina.framework.servlet.component.console.home.ServerConsoleHomePlace;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registration.EnvironmentRegistration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.EnvironmentRegistry;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.activity.RootArea;
import cc.alcina.framework.gwt.client.dirndl.impl.form.FmsForm;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BasePlace.HrefProvider;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponent;
import cc.alcina.framework.servlet.component.sequence.SequenceBrowser.IsDomain;
import cc.alcina.framework.servlet.environment.AbstractUi;
import cc.alcina.framework.servlet.environment.DomainUi;
import cc.alcina.framework.servlet.environment.RemoteUi;
import cc.alcina.framework.servlet.environment.SettingsSupport;

/**
 * <p>
 * The Alcina Servce sonsole UI (Dirndl)
 * 
 */
public class ServerConsoleBrowser {
	@Registration(RemoteComponent.class)
	public static class Component implements RemoteComponent {
		@Override
		public String getPath() {
			return "/serverconsole";
		}

		@Override
		public Class<? extends RemoteUi> getUiType() {
			return ServerConsoleBrowser.Ui.class;
		}
	}

	@TypedProperties
	static class Ui extends AbstractUi<ServerConsolePlace> implements DomainUi {
		PackageProperties._ServerConsoleBrowser_Ui.InstanceProperties
				subtypeProperties() {
			return PackageProperties.serverConsoleBrowser_ui.instance(this);
		}

		/**
		 * By default false, but the app can override this via a registry check
		 */
		@Override
		public boolean isDomain() {
			return Registry.impl(IsDomain.class).isDomain();
		}

		public static Ui get() {
			return (Ui) RemoteUi.get();
		}

		ServerConsolePage page;

		public ServerConsoleSettings settings;

		@Override
		public Client createClient() {
			return new TypedPlaceClient(ServerConsolePlace.class,
					ServerConsoleHomePlace.class);
		}

		public String getMainCaption() {
			return "ServerConsole browser";
		}

		@Override
		public void init() {
			FmsForm.registerImplementations();
			EnvironmentRegistry.registerEnvironmentOptionals(
					ServerConsoleBrowserCommand.ReloadApp.HandlerImpl.class);
			Registry.register().singleton(BasePlace.HrefProvider.class,
					new BasePlace.HrefProvider());
		}

		@Override
		public void initialiseSettings(String settings) {
			this.settings = SettingsSupport
					.deserializeSettings(ServerConsoleSettings.class, settings);
		}

		@Override
		protected DirectedLayout render0() {
			injectCss("res/css/styles.css");
			Client.get().initAppHistory();
			DirectedLayout layout = new DirectedLayout();
			layout.render(resolver(), new RootArea()).getRendered()
					.appendToRoot();
			return layout;
		}

		public ServerConsoleAnswers createAnswerSupplier() {
			return new ServerConsoleAnswers();
		}

		public static ServerConsolePlace place() {
			return get().place;
		}

		@Override
		public Set<Class<? extends cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext>>
				getAppCommandContexts() {
			return Set.of(CommandContext.class);
		}
	}

	public interface CommandContext extends
			cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext {
	}
}
