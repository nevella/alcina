package cc.alcina.framework.servlet.component.traversal;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestion;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestionEntry;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestor;
import cc.alcina.framework.gwt.client.dirndl.impl.form.FmsForm;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponent;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentObservables;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace;
import cc.alcina.framework.servlet.dom.AbstractUi;
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
@Feature.Ref(Feature_TraversalBrowser.class)
public class TraversalBrowser {
	@Registration(RemoteComponent.class)
	public static class Component implements RemoteComponent {
		@Override
		public String getPath() {
			return "/traversal";
		}

		@Override
		public Class<? extends RemoteUi> getUiType() {
			return TraversalBrowser.Ui.class;
		}
	}

	@TypedProperties
	public static class Ui extends AbstractUi<TraversalPlace> {
		public static Ui get() {
			return (Ui) Environment.get().ui;
		}

		public static TraversalPlace place() {
			return get().place;
		}

		public static SelectionTraversal traversal() {
			return get().traversal0();
		}

		Page page;

		public TraversalSettings settings;

		@Override
		public Class<? extends cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext>
				getAppCommandContext() {
			return CommandContext.class;
		}

		@Override
		public Client createClient() {
			return new TypedPlaceClient(TraversalPlace.class);
		}

		public RemoteComponentObservables<SelectionTraversal>.ObservableHistory
				getHistory() {
			return page.history;
		}

		public String getMainCaption() {
			return "Traversal ";
		}

		@Override
		public void init() {
			FmsForm.registerImplementations();
		}

		@Override
		public void initialiseSettings(String settings) {
			this.settings = SettingsSupport
					.deserializeSettings(TraversalSettings.class, settings);
		}

		public String getTraversalPath() {
			String sessionPath = Environment.get().getSessionPath();
			return sessionPath == null ? null
					: sessionPath.replaceFirst("/.+?/", "");
		}

		public boolean isUseSelectionSegmentPath() {
			return false;
		}

		public TraversalAnswerSupplier createAnswerSupplier(int forLayer) {
			return new TraversalAnswers(forLayer);
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

		protected SelectionTraversal traversal0() {
			return page.history == null ? null : page.history.getObservable();
		}
	}

	public static abstract class TraversalAnswerSupplier
			implements AppSuggestor.AnswerSupplier {
		public int fromLayer;

		public TraversalAnswerSupplier(int forLayer) {
			this.fromLayer = forLayer;
		}

		public static void proposeSetRowSuggestion(String query,
				List<AppSuggestion> suggestions) {
			Pattern pattern = Pattern.compile("set rows (\\d+)");
			Matcher matcher = pattern.matcher(query);
			if (matcher.matches()) {
				AppSuggestionEntry suggestion = new AppSuggestionEntry();
				suggestion.eventData = matcher.group(1);
				int tableRows = TraversalSettings.get().tableRows;
				suggestion.match = Ax.format("Set rows: '%s' (current=%s)",
						suggestion.eventData, tableRows);
				suggestion.modelEvent = TraversalEvents.SetSettingTableRows.class;
				suggestions.add(suggestion);
			}
		}
	}

	public interface CommandContext extends
			cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext {
	}
}
