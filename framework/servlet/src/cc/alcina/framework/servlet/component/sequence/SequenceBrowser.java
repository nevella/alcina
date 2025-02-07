package cc.alcina.framework.servlet.component.sequence;

import java.util.Set;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.util.HasStringRepresentation;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.activity.RootArea;
import cc.alcina.framework.gwt.client.dirndl.impl.form.FmsForm;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponent;
import cc.alcina.framework.servlet.environment.AbstractUi;
import cc.alcina.framework.servlet.environment.RemoteUi;
import cc.alcina.framework.servlet.environment.SettingsSupport;

/**
 * <p>
 * A remote component that browses a sequence of some kind
 * <p>
 * Filtering of sequence elements is handled by testing the _transformed_
 * element - see {@link Page#filteredSequenceElements()}. It's a
 * case-insensitive regex filter, using HasFilterableText.Query (which in most
 * cases just tests the property values of the transformed sequence element).
 * <p>
 * Highlightable text is provided by {@link HasStringRepresentation} - so having
 * the transformed elements implement that is generally a plus
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
		public static final Configuration.Key elementLimit = Configuration
				.key("elementLimit");

		static PackageProperties._SequenceBrowser_Ui properties = PackageProperties.sequenceBrowser_ui;

		public static Ui get() {
			return (Ui) RemoteUi.get();
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
			DirectedLayout layout = new DirectedLayout();
			layout.render(resolver(), new RootArea()).getRendered()
					.appendToRoot();
			return layout;
		}

		public SequenceAnswers createAnswerSupplier() {
			return new SequenceAnswers();
		}

		public static SequencePlace place() {
			return get().place;
		}

		@Override
		public Set<Class<? extends cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext>>
				getAppCommandContexts() {
			return Set.of(CommandContext.class);
		}

		int elementLimit() {
			return elementLimit.intValue();
		}
	}

	public interface CommandContext extends
			cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext {
	}
}
