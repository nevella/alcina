package cc.alcina.framework.servlet.component.sequence;

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
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext;
import cc.alcina.framework.gwt.client.dirndl.impl.form.FmsForm;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.place.BasePlaceTokenizer;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;
import cc.alcina.framework.servlet.component.featuretree.place.FeaturePlace;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponent;
import cc.alcina.framework.servlet.component.sequence.place.SequenceBrowserPlace;
import cc.alcina.framework.servlet.component.sequence.place.SequencePlace;
import cc.alcina.framework.servlet.dom.AbstractUi;
import cc.alcina.framework.servlet.dom.ClientRemoteImpl;
import cc.alcina.framework.servlet.dom.Environment;
import cc.alcina.framework.servlet.dom.RemoteUi;
import cc.alcina.framework.servlet.dom.SettingsSupport;

/**
 * <p>
 * A remote component that browses a sequence of some kind
 * 
 */
@Feature.Ref(Feature_SequenceBrowser.class)
public class SequenceBrowser {
	@Registration(RemoteComponent.class)
	public static class Component implements RemoteComponent {
		@Override
		public String getPath() {
			return "/seq";
		}

		@Override
		public Class<? extends RemoteUi> getUiType() {
			return SequenceBrowser.Ui.class;
		}
	}

	@TypedProperties
	static class Ui extends AbstractUi<SequencePlace> {
		static PackageProperties._SequenceBrowser_Ui properties = PackageProperties.sequenceBrowser_ui;

		public static Ui get() {
			return (Ui) Environment.get().ui;
		}

		Page page;

		public SequenceSettings settings;

		@Override
		public Client createClient() {
			return new TypedPlaceClient(SequencePlace.class);
		}

		public String getMainCaption() {
			return "Sequence browser";
		}

		@Override
		public void init() {
			FmsForm.registerImplementations();
		}

		@Override
		public void initialiseSettings(String settings) {
			this.settings = SettingsSupport
					.deserializeSettings(SequenceSettings.class, settings);
		}

		@Override
		protected DirectedLayout render0() {
			injectCss("res/css/styles.css");
			Client.get().initAppHistory();
			page = new Page();
			DirectedLayout layout = new DirectedLayout();
			layout.render(resolver(), page).getRendered().appendToRoot();
			return layout;
		}

		public SequenceAnswers createAnswerSupplier() {
			return new SequenceAnswers();
		}

		public static SequencePlace place() {
			return get().place;
		}

		@Override
		public Class<? extends cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext>
				getAppCommandContext() {
			return CommandContext.class;
		}
	}

	public interface CommandContext extends
			cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext {
	}
}
