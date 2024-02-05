package cc.alcina.framework.servlet.component.traversal;

import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registration.Priority;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.place.BasePlaceTokenizer;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponent;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentObservables;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace;
import cc.alcina.framework.servlet.component.traversal.place.TraversalProcessPlace;
import cc.alcina.framework.servlet.dom.ClientRemoteImpl;
import cc.alcina.framework.servlet.dom.Environment;
import cc.alcina.framework.servlet.dom.RemoteUi;

/**
 * A remote component that models a SelectionTraversal's process tree
 *
 */
@Feature.Ref(Feature_TraversalProcessView.class)
public class TraversalProcessView {
	Logger logger = LoggerFactory.getLogger(getClass());

	@Registration(RemoteComponent.class)
	public static class Component implements RemoteComponent {
		@Override
		public String getPath() {
			return "/traversal";
		}

		@Override
		public Class<? extends RemoteUi> getUiType() {
			return TraversalProcessView.Ui.class;
		}
	}

	/*
	 * Manually registered
	 */
	@Registration.Singleton(
		value = RegistryHistoryMapper.class,
		priority = Priority.REMOVE)
	public static class RegistryHistoryMapperImpl
			extends RegistryHistoryMapper {
		@Override
		protected Stream<BasePlaceTokenizer> listTokenizers() {
			return super.listTokenizers().filter(t -> Reflections
					.isAssignableFrom(TraversalProcessPlace.class,
							t.getTokenizedClass()));
		}
	}

	public static class Ui implements RemoteUi {
		Page page;

		Environment environment;

		public Environment getEnvironment() {
			return environment;
		}

		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}

		public static Ui get() {
			return (Ui) Environment.get().ui;
		}

		@Override
		public Client createClient() {
			return new ClientImpl();
		}

		public String getMainCaption() {
			return "traversal process";
		}

		@Override
		public void init() {
		}

		@Override
		public void render() {
			injectCss("res/css/styles.css");
			Client.get().initAppHistory();
			page = new Page();
			new DirectedLayout().render(resolver(), page).getRendered()
					.appendToRoot();
		}

		public RemoteComponentObservables<SelectionTraversal>.ObservableHistory
				getHistory() {
			return page.history;
		}
	}

	static class ClientImpl extends ClientRemoteImpl {
		@Override
		public void setupPlaceMapping() {
			historyHandler = new PlaceHistoryHandler(
					new RegistryHistoryMapperImpl());
			historyHandler.register(placeController, eventBus,
					() -> new TraversalPlace());
		}

		@Override
		protected void createPlaceController() {
			placeController = new PlaceController(eventBus);
		}
	}
}
