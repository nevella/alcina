package cc.alcina.framework.servlet.component.traversal;

import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registration.Priority;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestor;
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
import cc.alcina.framework.servlet.dom.SettingsSupport;

/**
 * A remote component that models a SelectionTraversal's process tree
 * 
 * The name displayed (for the traversal) can be customised by registering a
 * RootLayerNamer for the traversal's RootLayer
 * 
 * <h3>Notes
 * <ul>
 * <li>A traversal place will not be consistent (so will not, for instance, work
 * on app refresh) if the traversal is performed in parallel.
 * <li>A process view is linked to a specific traversal path if there's a ?path
 * querystring parameter in the accessing url - otherwise it's 'most recent',
 * and will update when that changes
 * </ul>
 *
 */
@Feature.Ref(Feature_TraversalProcessView.class)
public class TraversalProcessView {
	Logger logger = LoggerFactory.getLogger(getClass());

	static class ClientImpl extends ClientRemoteImpl {
		@Override
		protected void createPlaceController() {
			placeController = new PlaceController(eventBus);
		}

		@Override
		public void setupPlaceMapping() {
			historyHandler = new PlaceHistoryHandler(
					new RegistryHistoryMapperImpl());
			historyHandler.register(placeController, eventBus,
					() -> new TraversalPlace());
		}
	}

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
		public static Ui get() {
			return (Ui) Environment.get().ui;
		}

		Page page;

		Environment environment;

		public TraversalSettings settings;

		@Override
		public Client createClient() {
			return new ClientImpl();
		}

		public Environment getEnvironment() {
			return environment;
		}

		public RemoteComponentObservables<SelectionTraversal>.ObservableHistory
				getHistory() {
			return page.history;
		}

		public String getMainCaption() {
			return "Traversal process";
		}

		@Override
		public void init() {
			// FIXME - st - implement
			// Registry.impl(TopLevelCatchallHandler.class).register(this);
		}

		@Override
		public void initialiseSettings(String settings) {
			this.settings = SettingsSupport
					.deserializeSettings(TraversalSettings.class, settings);
		}

		@Override
		public void render() {
			injectCss("res/css/styles.css");
			Client.get().initAppHistory();
			page = new Page();
			new DirectedLayout().render(resolver(), page).getRendered()
					.appendToRoot();
		}

		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}

		public String getTraversalPath() {
			String sessionPath = getEnvironment().getSessionPath();
			return sessionPath == null ? null
					: sessionPath.replaceFirst("/.+?/", "");
		}

		public boolean isUseSelectionSegmentPath() {
			return false;
		}

		public void setPlace(TraversalPlace place) {
			// for subclasses
		}

		public TraversalAnswerSupplier createAnswerSupplier(int forLayer) {
			return new TraversalAnswers(forLayer);
		}

		public static TraversalPlace place() {
			Place place = Client.currentPlace();
			if (place instanceof TraversalPlace) {
				return (TraversalPlace) place;
			} else {
				return new TraversalPlace();
			}
		}

		protected SelectionTraversal traversal0() {
			return page.history.observable;
		}

		public static SelectionTraversal traversal() {
			return get().traversal0();
		}
	}

	public static abstract class TraversalAnswerSupplier
			implements AppSuggestor.AnswerSupplier {
		public TraversalAnswerSupplier(int forLayer) {
			this.forLayer = forLayer;
		}

		public int forLayer;
	}
}
