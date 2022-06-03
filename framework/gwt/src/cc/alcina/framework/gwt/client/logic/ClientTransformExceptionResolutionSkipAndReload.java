package cc.alcina.framework.gwt.client.logic;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory;

import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.common.client.actions.instances.OkAction;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequestException;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.gwt.client.dirndl.RenderContext;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;
import cc.alcina.framework.gwt.client.gwittir.customiser.ClassSimpleNameCustomiser;
import cc.alcina.framework.gwt.client.gwittir.customiser.ExpandableLabelCustomiser;
import cc.alcina.framework.gwt.client.gwittir.provider.CollectionDataProvider;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundTableExt;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory.NiceWidthBoundTable;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener.State;
import cc.alcina.framework.gwt.client.util.RelativePopupPositioning;
import cc.alcina.framework.gwt.client.widget.dialog.OkCancelDialogBox;

@Reflected
@Registration.Singleton(ClientTransformExceptionResolver.class)
public class ClientTransformExceptionResolutionSkipAndReload
		implements ClientTransformExceptionResolver,
		TopicListener<CommitToStorageTransformListener.State>,
		PermissibleActionListener {
	private FlowPanel fp;

	private StatusWidget status;

	private OkCancelDialogBox dialog;

	private Callback<ClientTransformExceptionResolutionToken> callback;

	private ClientTransformExceptionResolutionToken token = new ClientTransformExceptionResolutionToken();

	private boolean muted;

	public boolean isMuted() {
		return this.muted;
	}

	public void setMuted(boolean muted) {
		this.muted = muted;
	}

	public ClientTransformExceptionResolutionSkipAndReload() {
	}

	public static ClientTransformExceptionResolutionSkipAndReload cast() {
		return (ClientTransformExceptionResolutionSkipAndReload) Registry
				.impl(ClientTransformExceptionResolver.class);
	}

	@Override
	public void resolve(DomainTransformRequestException dtre,
			Callback<ClientTransformExceptionResolutionToken> callback) {
		if (isMuted()) {
			return;
		}
		final CommitToStorageTransformListener storage = Registry
				.impl(CommitToStorageTransformListener.class);
		if (dialog != null) {
			dialog.hide();
		}
		fp = new FlowPanel() {
			@Override
			protected void onAttach() {
				super.onAttach();
				storage.topicStateChanged().add(
						ClientTransformExceptionResolutionSkipAndReload.this);
			}

			@Override
			protected void onDetach() {
				storage.topicStateChanged().remove(
						ClientTransformExceptionResolutionSkipAndReload.this);
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
				reloadRequired = true;
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
			token.setResolverAction(
					ClientTransformExceptionResolverAction.THROW);
			callback.accept(token);
			return;
		}
		BoundWidgetTypeFactory factory = new BoundWidgetTypeFactory(true);
		Field[] fields = GwittirBridge.get()
				.fieldsForReflectedObjectAndSetupWidgetFactory(
						Reflections.at(DTEView.class).templateInstance(),
						factory, false, true);
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
		final String buttonText = irresolvable == null
				&& Registry.impl(CommitToStorageTransformListener.class)
						.isAllowPartialRetryRequests() ? "Retry"
								: "Reload application";
		dialog = new OkCancelDialogBox("Problems saving changes", fp, this) {
			@Override
			protected void adjustDisplay() {
				cancelButton.setVisible(false);
				okButton.setText(buttonText);
			}

			@Override
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

	@Override
	public void topicPublished(String key, State newState) {
		switch (newState) {
		case RELOAD:
			status.reloading();
			new Timer() {
				@Override
				public void run() {
					Window.Location.reload();
				}
			}.schedule(2000);
			break;
		case COMMITTED:
			dialog.hide();
			break;
		case COMMITTING:
			status.committing();
			break;
		}
	}

	@Override
	public void vetoableAction(PermissibleActionEvent evt) {
		if (status.isIrresolvable()
				|| !Registry.impl(CommitToStorageTransformListener.class)
						.isAllowPartialRetryRequests()) {
			Window.Location.reload();
		} else {
			token.setResolverAction(
					ClientTransformExceptionResolverAction.RESUBMIT);
			callback.accept(token);
		}
	}

		public static class DTEView extends Bindable {
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
				reloadRequired = exception.getEvent()
						.getTransformType() != TransformType.DELETE_OBJECT;
				break;
			case TOO_MANY_EXCEPTIONS:
			case INVALID_AUTHENTICATION:
			default:
				break;
			}
		}

		@Display(name = "Exception text", orderingHint = 50)
		@Custom(customiserClass = ExpandableLabelCustomiser.class, parameters = {
				@NamedParameter(name = ExpandableLabelCustomiser.FORCE_COLUMN_WIDTH, booleanValue = true),
				@NamedParameter(name = ExpandableLabelCustomiser.MAX_WIDTH, intValue = 30) })
		@PropertyPermissions(read = @Permission(access = AccessLevel.ADMIN))
		public String getExceptionText() {
			return exception.getMessage();
		}

		@Display(name = "New Value", orderingHint = 30)
		@Custom(customiserClass = ExpandableLabelCustomiser.class, parameters = {
				@NamedParameter(name = ExpandableLabelCustomiser.FORCE_COLUMN_WIDTH, booleanValue = true),
				@NamedParameter(name = ExpandableLabelCustomiser.MAX_WIDTH, intValue = 30) })
		public String getNewStringValue() {
			return event.getNewStringValue();
		}

		@Display(name = "Object", orderingHint = 10)
		@Custom(customiserClass = ClassSimpleNameCustomiser.class)
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

		@Display(name = "Object id", orderingHint = 15)
		public long getObjectId() {
			return event.getObjectId();
		}

		@Display(name = "Object name", orderingHint = 17)
		public String getObjectName() {
			return exception.getSourceObjectName();
		}

		@Display(name = "Property", orderingHint = 20)
		public String getPropertyName() {
			return event.getPropertyName();
		}

		@Display(name = "Action", orderingHint = 40)
		public RecommendedAction getRecommendedAction() {
			return this.recommendedAction;
		}

		@Display(name = "Problem", orderingHint = 40)
		public DomainTransformExceptionType getTransformExceptionType() {
			return exception.getType();
		}

		@Display(name = "Transform", orderingHint = 25)
		public TransformType getTransformType() {
			return event.getTransformType();
		}
	}

	@Reflected
	public enum RecommendedAction {
		OK, SKIP, IRRESOLVABLE, RETRY
	}

	private class StatusWidget extends Composite {
		private HTML html;

		private FlowPanel fp;

		private boolean irresolvable = false;

		private Label blurb;

		public StatusWidget() {
			this.html = new HTML();
			this.fp = new FlowPanel();
			blurb = new Label("Problems occurred saving changes. "
					+ "The changes you made will be altered to allow your work to be saved.");
			blurb.setStyleName("pad-15 italic");
			blurb.setVisible(
					Registry.impl(CommitToStorageTransformListener.class)
							.isAllowPartialRetryRequests());
			fp.add(blurb);
			fp.add(html);
			initWidget(fp);
			html.setStyleName("pad-15");
		}

		public void committing() {
			html.setText("Committing changes...");
		}

		public void irresolvable(DomainTransformException dte) {
			blurb.setVisible(false);
			String text = "<p>A problem has occurred which requires this page to be reloaded.</p>";
			switch (dte.getType()) {
			case INVALID_AUTHENTICATION:
				text += "<p>The page's authentication is invalid. You may have logged in or out in another tab to this one.</p>";
				break;
			case UNKNOWN:
				text += "<p>Unexpected exception.</p>";
				break;
			default:
				text += "<p>Too many problems occurred to be resolved automatically.</p>";
			}
			text += "<p><b>Detail: </b>" + dte.getMessage() + "</p>";
			html.setHTML(text);
			irresolvable = true;
		}

		public boolean isIrresolvable() {
			return this.irresolvable;
		}

		public void reloading() {
			html.setText("Reloading...");
		}

		public void reloadRequired() {
			html.setText(
					"The application will need to be reloaded after the problems are resolved.");
		}
	}
}
