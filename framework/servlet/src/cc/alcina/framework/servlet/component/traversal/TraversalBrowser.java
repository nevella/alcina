package cc.alcina.framework.servlet.component.traversal;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.EnvironmentRegistry;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.SelectionMarkup;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.activity.RootArea;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestion;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestionEntry;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestor;
import cc.alcina.framework.gwt.client.dirndl.cmp.help.HelpContentProvider;
import cc.alcina.framework.gwt.client.dirndl.cmp.status.StatusModule;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent.TopLevelCatchallHandler;
import cc.alcina.framework.gwt.client.dirndl.impl.form.FmsForm;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.model.Choices;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponent;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentObservables;
import cc.alcina.framework.servlet.component.shared.ExecCommand;
import cc.alcina.framework.servlet.component.shared.ExecCommand;
import cc.alcina.framework.servlet.component.traversal.TraversalPlace.SelectionPath;
import cc.alcina.framework.servlet.component.traversal.TraversalPlace.SelectionType;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings.SecondaryAreaDisplayMode;
import cc.alcina.framework.servlet.environment.AbstractUi;
import cc.alcina.framework.servlet.environment.RemoteUi;
import cc.alcina.framework.servlet.environment.SettingsSupport;

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

	// Allows project-specific customisation of
	// TraversalBrowser.onBeforeEnterContext()
	@Registration.Self
	public interface BeforeEnterContext {
		void onBeforeEnterContext();
	}

	/**
	 * <p>
	 * Although UI logically contains page, traversal and history are both
	 * 'primary' there - #traversal is mutated via a binding on page (since page
	 * is a model, it's easier to manage bindings there)
	 * 
	 * <p>
	 * FIXME - If there's a breaking history change (i.e. an incompatible
	 * observable), refresh the page rather than add multiple bindings in Page
	 */
	@TypedProperties
	public static class Ui extends AbstractUi<TraversalPlace>
			implements HasPage {
		static PackageProperties._TraversalBrowser_Ui properties = PackageProperties.traversalBrowser_ui;

		public static Ui get() {
			return (Ui) RemoteUi.get();
		}

		public static TraversalPlace place() {
			return get().place;
		}

		public static SelectionTraversal traversal() {
			return get().traversal;
		}

		Page page;

		public SelectionTraversal traversal;

		public TraversalSettings settings;

		@Override
		public Set<Class<? extends cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext>>
				getAppCommandContexts() {
			return Set.of(CommandContext.class,
					FlightEventCommand.CommandContext.class);
		}

		@Override
		public Client createClient() {
			return new TypedPlaceClient(TraversalPlace.class);
		}

		public RemoteComponentObservables<SelectionTraversal>.ObservableEntry
				getHistory() {
			return page.history;
		}

		public String getMainCaption() {
			return "Traversal ";
		}

		@Override
		public void onBeforeEnterContext() {
			Registry.optional(BeforeEnterContext.class)
					.ifPresent(BeforeEnterContext::onBeforeEnterContext);
		}

		@Override
		public void init() {
			FmsForm.registerImplementations();
			StatusModule.get();
			Registry.register().singleton(HelpContentProvider.class,
					new HelpContentProviderImpl());
			EnvironmentRegistry.registerEnvironmentOptionals(
					TraversalCommand.ReloadApp.HandlerImpl.class);
		}

		public static class TopLevelCatchallHandlerImpl
				extends ModelEvent.TopLevelCatchallHandler.MissedEventEmitter {
		}

		@Override
		public void initialiseSettings(String settings) {
			this.settings = SettingsSupport
					.deserializeSettings(TraversalSettings.class, settings);
		}

		public String getTraversalPath() {
			String sessionPath = RemoteUi.get().getSessionPath();
			return sessionPath == null ? null
					: sessionPath.replaceFirst("/.+?/", "");
		}

		public boolean isUseSelectionSegmentPath() {
			return false;
		}

		public TraversalAnswerSupplier createAnswerSupplier(int forLayer,
				boolean hasClearableFilter) {
			return new TraversalAnswers(forLayer, hasClearableFilter);
		}

		@Override
		protected DirectedLayout render0() {
			injectCss("res/css/styles.css");
			Client.get().initAppHistory();
			DirectedLayout layout = new DirectedLayout();
			RootArea rootArea = new RootArea();
			layout.render(resolver(), rootArea).getRendered().appendToRoot();
			Registry.register().singleton(TopLevelCatchallHandler.class,
					new TopLevelCatchallHandlerImpl()
							.withEmittingModel(rootArea));
			return layout;
		}

		@Property.Not
		public SecondaryAreaDisplayMode[] getValidSecondaryAreadModes() {
			if (getSelectionMarkup() != null) {
				return SecondaryAreaDisplayMode.values();
			} else {
				return new SecondaryAreaDisplayMode[] {
						SecondaryAreaDisplayMode.TABLE,
						SecondaryAreaDisplayMode.NONE };
			}
		}

		private SelectionMarkup selectionMarkup;

		public SelectionMarkup getSelectionMarkup() {
			if (selectionMarkup == null && traversal != null) {
				SelectionMarkup.Has markupProvider = traversal
						.context(SelectionMarkup.Has.class);
				if (markupProvider != null) {
					selectionMarkup = markupProvider.getSelectionMarkup();
				}
			}
			return selectionMarkup;
		}

		public static Layer getSelectedLayer() {
			return get().getSelectedLayer0();
		}

		public static Layer getSelectedLayer(Selection selection) {
			return get().getSelectedLayer0(selection);
		}

		protected Layer getSelectedLayer0() {
			int selectedLayerIndex = place().provideSelectedLayerIndex();
			return traversal() == null ? null
					: traversal().layers().get(selectedLayerIndex);
		}

		protected Layer getSelectedLayer0(Selection selection) {
			return traversal().layers().get(selection);
		}

		public static Layer getListSourceLayer() {
			return get().getListSourceLayer0();
		}
		//

		protected Layer getListSourceLayer0() {
			int listSourceLayerIndex = place().provideListSourceLayerIndex();
			return traversal() == null ? null
					: traversal().layers().get(listSourceLayerIndex);
		}

		@Override
		public Page providePage() {
			return page;
		}

		public boolean isClearPostSelectionLayers() {
			return false;
		}

		/*
		 * create additional leftheader models
		 */
		protected List<?> createAdditionalLeftHeader() {
			return null;
		}

		/*
		 * For a decision chain (EntityBrowser), return true
		 */
		public boolean isAppendTableSelections() {
			return false;
		}

		public static TraversalPlace activePlace() {
			return get().activePlace0();
		}

		protected TraversalPlace activePlace0() {
			return place;
		}

		public SelectionPath getSelectionPath(Selection selection) {
			TraversalPlace.SelectionType selectionType = SelectionType.VIEW;
			SelectionPath selectionPath = new TraversalPlace.SelectionPath();
			selectionPath.selection = selection;
			selectionPath.path = selection.processNode().treePath();
			if (isUseSelectionSegmentPath()) {
				selectionPath.segmentPath = selection.fullPath();
			}
			selectionPath.type = selectionType;
			return selectionPath;
		}

		protected Model getEventHandlerCustomisation() {
			return null;
		}
	}

	@Reflected
	static class ValidSecondaryAreaDisplayModes
			implements Choices.Values.ValueSupplier {
		@Override
		public List<?> get() {
			return Arrays.asList(Ui.get().getValidSecondaryAreadModes());
		}
	}

	/**
	 * The default documentation is for the RecipeMarkupParser traversal. Other
	 * traversals can register via {@link SelectionTraversal.Context} (if the
	 * Context implements {@link HelpContentProvider})
	 */
	static class HelpContentProviderImpl implements HelpContentProvider {
		HelpContentProviderImpl_Default _default = new HelpContentProviderImpl_Default();

		public String getHelpMarkup() {
			return delegate().getHelpMarkup();
		}

		HelpContentProvider delegate() {
			SelectionTraversal traversal = Ui.traversal();
			if (traversal != null) {
				HelpContentProvider contextProvider = traversal
						.context(HelpContentProvider.class);
				if (contextProvider != null) {
					return contextProvider;
				}
			}
			return _default;
		}
	}

	static class HelpContentProviderImpl_Default
			implements HelpContentProvider {
		public String getHelpMarkup() {
			return Io.read().resource("story/document.html").asDomDocument()
					.html().body().getInnerMarkup();
		}
	}

	public static abstract class TraversalAnswerSupplier
			implements AppSuggestor.AnswerSupplier {
		public int fromLayer;

		public TraversalAnswerSupplier(int forLayer) {
			this.fromLayer = forLayer;
		}

		// FIXME - tb - docco
		public static void proposeSetSuggestions(String query,
				List<AppSuggestion> suggestions) {
			{
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
			{
				Pattern pattern = Pattern
						.compile("set selectionAreaHeight (\\d+)");
				Matcher matcher = pattern.matcher(query);
				if (matcher.matches()) {
					AppSuggestionEntry suggestion = new AppSuggestionEntry();
					suggestion.eventData = matcher.group(1);
					int selectionAreaHeight = TraversalSettings
							.get().selectionAreaHeight;
					suggestion.match = Ax.format(
							"Set selectionAreaHeight: '%s' (current=%s)",
							suggestion.eventData, selectionAreaHeight);
					suggestion.modelEvent = TraversalEvents.SetSettingSelectionAreaHeight.class;
					suggestions.add(suggestion);
				}
			}
		}

		public void addExecSuggestion(String query,
				List<AppSuggestion> suggestions) {
			{
				Pattern pattern = Pattern.compile("exec (\\S+)");
				Matcher matcher = pattern.matcher(query);
				if (matcher.matches()) {
					AppSuggestionEntry suggestion = new AppSuggestionEntry();
					suggestion.eventData = matcher.group(1);
					suggestion.match = Ax.format(
							"Exec '%s' ['l' lists available commands]",
							suggestion.eventData);
					suggestion.modelEvent = ExecCommand.PerformCommand.class;
					suggestions.add(suggestion);
				}
			}
		}
	}

	public interface CommandContext extends
			cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext {
	}
}
