package cc.alcina.framework.gwt.client.logic;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.common.client.actions.instances.OkAction;
import cc.alcina.framework.common.client.csobjects.BaseBindable;
import cc.alcina.framework.common.client.logic.StateChangeListener;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequestException;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.BeanInfo;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.CustomiserInfo;
import cc.alcina.framework.common.client.logic.reflection.DisplayInfo;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.VisualiserInfo;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;
import cc.alcina.framework.gwt.client.gwittir.customiser.ClassSimpleNameCustomiser;
import cc.alcina.framework.gwt.client.gwittir.customiser.ExpandableLabelCustomiser;
import cc.alcina.framework.gwt.client.gwittir.provider.CollectionDataProvider;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundTableExt;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory.NiceWidthBoundTable;
import cc.alcina.framework.gwt.client.util.RelativePopupPositioning;
import cc.alcina.framework.gwt.client.widget.dialog.OkCancelDialogBox;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory;

@RegistryLocation(registryPoint = ClientTransformExceptionResolver.class, implementationType = ImplementationType.SINGLETON)
@ClientInstantiable
public class ClientTransformExceptionResolutionSkipAndReload implements
		ClientTransformExceptionResolver, StateChangeListener,
		PermissibleActionListener {
	private FlowPanel fp;

	private StatusWidget status;

	private OkCancelDialogBox dialog;

	private Callback<ClientTransformExceptionResolutionToken> callback;

	private ClientTransformExceptionResolutionToken token = new ClientTransformExceptionResolutionToken();

	public ClientTransformExceptionResolutionSkipAndReload() {
	}

	public void resolve(DomainTransformRequestException dtre,
			Callback<ClientTransformExceptionResolutionToken> callback) {
		final CommitToStorageTransformListener storage = ClientLayerLocator
				.get().getCommitToStorageTransformListener();
		if (dialog != null) {
			dialog.hide();
		}
		fp = new FlowPanel() {
			@Override
			protected void onAttach() {
				super.onAttach();
				storage.addStateChangeListener(ClientTransformExceptionResolutionSkipAndReload.this);
			}

			@Override
			protected void onDetach() {
				storage.removeStateChangeListener(ClientTransformExceptionResolutionSkipAndReload.this);
				super.onDetach();
			}
		};
		this.callback = callback;
		boolean reloadRequired = false;
		List<DomainTransformException> exceptions = dtre
				.getDomainTransformResponse().getTransformExceptions();
		List<DTEView> adapters = new ArrayList<DTEView>();
		DomainTransformException irresolvable = null;
		for (DomainTransformException dte : exceptions) {
			if (dte.irresolvable()) {
				irresolvable = dte;
			}
			if (dte.getEvent() == null) {
				continue;
			}
			DTEView view = new DTEView(dte);
			if (view.recommendedAction != RecommendedAction.OK
					|| !dte.isSilent() || !PermissionsManager.get().isAdmin()) {
				adapters.add(view);
			}
			reloadRequired |= view.reloadRequired;
			if (view.recommendedAction == RecommendedAction.SKIP) {
				token.getEventIdsToIgnore().add(dte.getEvent().getEventId());
			}
		}
		if (adapters.isEmpty() && irresolvable == null) {
			// unknown exception - throw
			token.setResolverAction(ClientTransformExceptionResolverAction.THROW);
			callback.apply(token);
			return;
		}
		BoundWidgetTypeFactory factory = new BoundWidgetTypeFactory(true);
		Field[] fields = GwittirBridge.get()
				.fieldsForReflectedObjectAndSetupWidgetFactory(
						ClientReflector.get()
								.getTemplateInstance(DTEView.class), factory,
						false, true);
		int mask = BoundTableExt.HEADER_MASK | BoundTableExt.NO_NAV_ROW_MASK;
		CollectionDataProvider cp = new CollectionDataProvider(adapters);
		RenderContext.get().push();
		RelativePopupPositioning.setCurrentBoundingParent(RootPanel.get());
		NiceWidthBoundTable table = new NiceWidthBoundTable(mask, factory,
				fields, cp);
		RenderContext.get().pop();
		FlowPanel view = new FlowPanel();
		view.add(table);
		view.setStyleName("results-table");
		if (!adapters.isEmpty()) {
			fp.add(view);
		}
		status = new StatusWidget();
		fp.add(status);
		token.setReloadRequired(reloadRequired);
		if (reloadRequired) {
			status.reloadRequired();
		}
		if (irresolvable != null) {
			status.irresolvable(irresolvable);
		}
		final String buttonText = irresolvable == null ? "Retry"
				: "Reload application";
		dialog = new OkCancelDialogBox("Problems saving changes", fp, this) {
			@Override
			protected void adjustDisplay() {
				cancelButton.setVisible(false);
				okButton.setText(buttonText);
			}

			protected void onOkButtonClicked() {
				if (!checkValid()) {
					return;
				}
				okButton.setEnabled(false);
				if (vetoableActionListener != null) {
					vetoableActionListener
							.vetoableAction(new PermissibleActionEvent(this,
									OkAction.INSTANCE));
				}
			}
		};
	}

	public void stateChanged(Object source, String newState) {
		if (newState.equals(CommitToStorageTransformListener.RELOAD)) {
			status.reloading();
			new Timer() {
				@Override
				public void run() {
					Window.Location.reload();
				}
			}.schedule(2000);
		} else if (newState.equals(CommitToStorageTransformListener.COMMITTED)) {
			dialog.hide();
		} else if (newState.equals(CommitToStorageTransformListener.COMMITTING)) {
			status.committing();
		}
	}

	public void vetoableAction(PermissibleActionEvent evt) {
		if (status.isIrresolvable()) {
			Window.Location.reload();
		} else {
			token.setResolverAction(ClientTransformExceptionResolverAction.RESUBMIT);
			callback.apply(token);
		}
	}

	@BeanInfo(displayNamePropertyName = "recommendedAction")
	public static class DTEView extends BaseBindable {
		boolean reloadRequired = false;

		RecommendedAction recommendedAction;

		private DomainTransformException exception;

		private DomainTransformEvent event;

		public DTEView() {
		}

		public DTEView(DomainTransformException exception) {
			this.exception = exception;
			this.event = exception.getEvent();
			switch (exception.getType()) {
			case OPTIMISTIC_LOCK_EXCEPTION:
				recommendedAction = RecommendedAction.RETRY;
				break;
			case FK_CONSTRAINT_EXCEPTION:
			case PERMISSIONS_EXCEPTION:
			case TARGET_ENTITY_NOT_FOUND:
			case UNKNOWN:
			case VALIDATION_EXCEPTION:
				reloadRequired = true;
				recommendedAction = RecommendedAction.SKIP;
				// TODO - with real undo, could avoid some reloads
				break;
			case SOURCE_ENTITY_NOT_FOUND:
				recommendedAction = RecommendedAction.SKIP;
				reloadRequired = exception.getEvent().getTransformType() != TransformType.DELETE_OBJECT;
				break;
			case TOO_MANY_EXCEPTIONS:
			case INVALID_AUTHENTICATION:
			default:
				break;
			}
		}

		@VisualiserInfo(displayInfo = @DisplayInfo(name = "Exception text", orderingHint = 50))
		@CustomiserInfo(customiserClass = ExpandableLabelCustomiser.class, parameters = {
				@NamedParameter(name = ExpandableLabelCustomiser.FORCE_COLUMN_WIDTH, booleanValue = true),
				@NamedParameter(name = ExpandableLabelCustomiser.MAX_WIDTH, intValue = 30) })
		@PropertyPermissions(read = @Permission(access = AccessLevel.ADMIN))
		public String getExceptionText() {
			return exception.getMessage();
		}

		@VisualiserInfo(displayInfo = @DisplayInfo(name = "New Value", orderingHint = 30))
		@CustomiserInfo(customiserClass = ExpandableLabelCustomiser.class, parameters = {
				@NamedParameter(name = ExpandableLabelCustomiser.FORCE_COLUMN_WIDTH, booleanValue = true),
				@NamedParameter(name = ExpandableLabelCustomiser.MAX_WIDTH, intValue = 30) })
		public String getNewStringValue() {
			return event.getNewStringValue();
		}

		@VisualiserInfo(displayInfo = @DisplayInfo(name = "Object", orderingHint = 10))
		@CustomiserInfo(customiserClass = ClassSimpleNameCustomiser.class)
		public String getObjectClassName() {
			String objectClassName = event.getObjectClassName();
			if (objectClassName != null) {
				return objectClassName;
			}
			ClassRef objectClassRef = event.getObjectClassRef();
			if (objectClassRef != null) {
				return objectClassRef.getRefClassName();
			}
			return null;
		}

		@VisualiserInfo(displayInfo = @DisplayInfo(name = "Object id", orderingHint = 15))
		public long getObjectId() {
			return event.getObjectId();
		}

		@VisualiserInfo(displayInfo = @DisplayInfo(name = "Object name", orderingHint = 17))
		public String getObjectName() {
			return exception.getSourceObjectName();
		}

		@VisualiserInfo(displayInfo = @DisplayInfo(name = "Property", orderingHint = 20))
		public String getPropertyName() {
			return event.getPropertyName();
		}

		@VisualiserInfo(displayInfo = @DisplayInfo(name = "Action", orderingHint = 40))
		public RecommendedAction getRecommendedAction() {
			return this.recommendedAction;
		}

		@VisualiserInfo(displayInfo = @DisplayInfo(name = "Transform", orderingHint = 25))
		public TransformType getTransformType() {
			return event.getTransformType();
		}

		@VisualiserInfo(displayInfo = @DisplayInfo(name = "Problem", orderingHint = 40))
		public DomainTransformExceptionType getTransformExceptionType() {
			return exception.getType();
		}
	}

	public enum RecommendedAction {
		OK, SKIP, IRRESOLVABLE, RETRY
	}

	private class StatusWidget extends Composite {
		private HTML html;

		private FlowPanel fp;

		private boolean irresolvable = false;

		private Label blurb;

		public boolean isIrresolvable() {
			return this.irresolvable;
		}

		public StatusWidget() {
			this.html = new HTML();
			this.fp = new FlowPanel();
			blurb = new Label(
					"Problems occurred saving changes. "
							+ "The changes you made will be altered to allow your work to be saved.");
			blurb.setStyleName("pad-15 italic");
			fp.add(blurb);
			fp.add(html);
			initWidget(fp);
			html.setStyleName("pad-15");
		}

		public void irresolvable(DomainTransformException dte) {
			blurb.setVisible(false);
			String text = "<p>A problem has occurred which requires this page to be reloaded.</p>";
			if (dte.getType() == DomainTransformExceptionType.INVALID_AUTHENTICATION) {
				text += "<p>The page's authentication is invalid. You may have logged in or out in another tab to this one.</p>";
			} else {
				text += "<p>Too many problems occurred to be resolved automatically.</p>";
			}
			text += "<p><b>Detail: </b>" + dte.getMessage() + "</p>";
			html.setHTML(text);
			irresolvable = true;
		}

		public void committing() {
			html.setText("Committing changes...");
		}

		public void reloading() {
			html.setText("Reloading...");
		}

		public void reloadRequired() {
			html.setText("The application will need to be reloaded after the problems are resolved.");
		}
	}
}
