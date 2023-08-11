package cc.alcina.framework.servlet.component.featuretree;

import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registration.Priority;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.module.support.login.pub.LoginPlace;
import cc.alcina.framework.gwt.client.place.BasePlaceTokenizer;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;
import cc.alcina.framework.servlet.component.featuretree.place.FeatureTreePlace;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponent;
import cc.alcina.framework.servlet.dom.ClientRemoteImpl;
import cc.alcina.framework.servlet.dom.Environment;
import cc.alcina.framework.servlet.dom.RemoteUi;

/**
 * A remote component that models the jvm-visible feature tree
 *
 *
 * FIXME - featuretree - leaves with no status should be open.
 */
@Feature.Ref(Feature_FeatureTree.class)
public class FeatureTree {
	Logger logger = LoggerFactory.getLogger(getClass());

	@Registration(RemoteComponent.class)
	public static class Component implements RemoteComponent {
		@Override
		public String getPath() {
			return "/feature-tree";
		}

		@Override
		public Class<? extends RemoteUi> getUiType() {
			return FeatureTree.Ui.class;
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
			return super.listTokenizers().filter(
					t -> Reflections.isAssignableFrom(FeatureTreePlace.class,
							t.getTokenizedClass()));
		}
	}

	public static class Ui implements RemoteUi {
		public static Ui get() {
			return (Ui) Environment.get().ui;
		}

		@Override
		public Client createClient() {
			return new ClientImpl();
		}

		public String getMainCaption() {
			return "feature tree";
		}

		@Override
		public void init() {
		}

		@Override
		public void render() {
			injectCss("res/css/styles.css");
			Client.get().initAppHistory();
			new DirectedLayout().render(new Page()).appendToRoot();
		}
	}

	static class ClientImpl extends ClientRemoteImpl {
		@Override
		public void setupPlaceMapping() {
			historyHandler = new PlaceHistoryHandler(
					new RegistryHistoryMapperImpl());
			historyHandler.register(placeController, eventBus,
					() -> Place.NOWHERE);
		}

		@Override
		protected void createPlaceController() {
			placeController = new PlaceController(eventBus);
		}
	}

	@Directed
	@Bean(PropertySource.FIELDS)
	static class Mock extends Model implements DomEvents.Click.Handler {
		@Directed(tag = "p")
		// @Directed(tag = "div")
		String m1 = "baa";

		@Directed(
			tag = "a",
			bindings = @Binding(
				type = Binding.Type.PROPERTY,
				to = "href",
				literal = "#mm"))
		String m2 = "maa";

		@Override
		public void onClick(Click event) {
			set("m1", "lamb");
			new LoginPlace().go();
		}
	}
}
