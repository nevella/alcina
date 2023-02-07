package cc.alcina.framework.gwt.client.dirndl.model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceChangeRequestEvent;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Focusable;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory;

import cc.alcina.framework.common.client.actions.LocalActionWithParameters;
import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.actions.PermissibleActionHandler.DefaultPermissibleActionHandler;
import cc.alcina.framework.common.client.actions.PermissibleEntityAction;
import cc.alcina.framework.common.client.actions.RemoteActionWithParameters;
import cc.alcina.framework.common.client.actions.instances.NonstandardObjectAction;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.UserProperty;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Action;
import cc.alcina.framework.common.client.logic.reflection.ModalDisplay.ModalResolver;
import cc.alcina.framework.common.client.logic.reflection.ObjectActions;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.activity.DirectedEntityActivity;
import cc.alcina.framework.gwt.client.dirndl.annotation.ActionRef;
import cc.alcina.framework.gwt.client.dirndl.annotation.ActionRef.ActionHandler;
import cc.alcina.framework.gwt.client.dirndl.annotation.ActionRef.ActionRefHandler;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.KeyDown;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Submit;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent.Context;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.EmitsModelEvent;
import cc.alcina.framework.gwt.client.dirndl.annotation.Ref;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform.AbstractContextSensitiveModelTransform;
import cc.alcina.framework.gwt.client.entity.EntityAction;
import cc.alcina.framework.gwt.client.entity.place.ActionRefPlace;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;
import cc.alcina.framework.gwt.client.gwittir.GwittirUtils;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.place.CategoryNamePlace;

@Directed(receives = { DomEvents.KeyDown.class })
@Registration(FormModel.class)
public class FormModel extends Model
		implements DomEvents.Submit.Handler, DomEvents.KeyDown.Handler {
	private static Map<Model, HandlerRegistration> registrations = new LinkedHashMap<>();

	protected List<FormElement> elements = new ArrayList<>();

	protected List<Link> actions = new ArrayList<>();

	private FormModelState state;

	private boolean unAttachConfirmsTransformClear = false;

	private PlaceChangeRequestEvent.Handler dirtyChecker = e -> {
		CommitToStorageTransformListener.get().flush();
		// FIXME - mvcc.adjunct - need to ask adjuncts
		if (TransformManager.get()
				.getTransformsByCommitType(CommitType.TO_LOCAL_BEAN)
				.size() > 0) {
			e.setWarning("Form has unsaved changes. Please confirm to close");
			unAttachConfirmsTransformClear = true;
		}
	};

	public FormModel() {
	}

	public List<Link> getActions() {
		return this.actions;
	}

	public List<FormElement> getElements() {
		return this.elements;
	}

	public FormModelState getState() {
		return this.state;
	}

	@Override
	public void onBind(Bind event) {
		bind(event);
		focus(event);
		checkDirty(event);
		super.onBind(event);
	}

	@Override
	public void onKeyDown(KeyDown event) {
		KeyDownEvent domEvent = (KeyDownEvent) event.getContext().getGwtEvent();
		if (domEvent.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
			domEvent.preventDefault();
			domEvent.stopPropagation();
			// this is before KEY_ENTER is applied, so current form field
			// may not have fired 'onchange'
			GwittirUtils.commitAllTextBoxes(getState().formBinding);
			ActionRefPlace place = new ActionRefPlace(SubmitRef.class);
			new SubmitHandler().handleAction(event.getContext().node, domEvent,
					place);
		}
	}

	@Override
	public void onSubmit(Submit event) {
		submit(event.getContext().node);
	}

	public boolean submit(Node node) {
		Consumer<Void> onValid = o -> {
			if (getState().model instanceof Entity) {
				ClientTransformManager.cast()
						.promoteToDomainObject(getState().model);
				Consumer<EntityLocator> callback = createdLocator -> {
					// see somewhat similar handling in cancelhandler
					Place currentPlace = Client.currentPlace();
					if (currentPlace instanceof EntityPlace) {
						/*
						 * behaviour currently identical for either CREATE or
						 * EDIT ( -> view)
						 */
						EntityPlace currentEntityPlace = (EntityPlace) currentPlace;
						EntityPlace entityPlace = (EntityPlace) Reflections
								.newInstance(currentPlace.getClass());
						entityPlace.id = currentEntityPlace.action == EntityAction.CREATE
								? createdLocator.id
								: currentEntityPlace.id;
						entityPlace.go();
					}
					EntityPlace entityPlace = ((EntityPlace) Client
							.currentPlace()).copy();
					entityPlace.action = EntityAction.VIEW;
					entityPlace.go();
				};
				CommitToStorageTransformListener
						.flushAndRunWithFirstCreationConsumer(callback);
			}
			if (Client.currentPlace() instanceof EntityPlace) {
			} else if (Client.currentPlace() instanceof CategoryNamePlace) {
				CategoryNamePlace categoryNamePlace = (CategoryNamePlace) Client
						.currentPlace();
				DefaultPermissibleActionHandler.handleAction(null,
						categoryNamePlace.ensureAction(), node);
			}
		};
		return new FormValidation().validate(onValid, getState().formBinding);
	}

	private void bind(Bind event) {
		if (event.isBound()) {
			getState().formBinding.bind();
		} else {
			getState().formBinding.unbind();
		}
	}

	private void checkDirty(Bind event) {
		if (event.isBound()) {
			registrations.put(this, Client.eventBus()
					.addHandler(PlaceChangeRequestEvent.TYPE, dirtyChecker));
		} else {
			// if we're navigating away, and dirty
			if (unAttachConfirmsTransformClear) {
				TransformManager.get().clearTransforms();
			}
			HandlerRegistration registration = registrations.remove(this);
			if (registration != null) {
				registration.removeHandler();
			}
		}
	}

	private void focus(Bind event) {
		Optional<FormElement> focus = getElements().stream()
				.filter(FormElement::isFocusOnAttach).findFirst();
		if (focus.isPresent()) {
			Node childWithModel = event.getContext().node
					.childWithModel(m -> m != null && m instanceof ValueModel
							&& m == focus.get().getValue());
			((Focusable) childWithModel.getWidget()).setFocus(true);
		}
		// FIXME - dirndl 1x2 - this should be an annotation on the field,
		//
	}

	public static class BindableFormModelTransformer extends
			AbstractContextSensitiveModelTransform<Bindable, FormModel> {
		@Override
		public FormModel apply(Bindable bindable) {
			FormModelState state = new FormModelState();
			state.editable = true;
			if (bindable instanceof Entity && state.editable) {
				bindable = ClientTransformManager.cast()
						.ensureEditable((Entity) bindable);
			}
			state.model = bindable;
			state.adjunct = true;
			BindableFormModelTransformer.Args args = node
					.annotation(BindableFormModelTransformer.Args.class);
			if (args != null) {
				state.adjunct = args.adjunct();
			}
			return new FormModelTransformer().withContextNode(node)
					.apply(state);
		}

		@ClientVisible
		@Retention(RetentionPolicy.RUNTIME)
		@Documented
		@Target({ ElementType.TYPE, ElementType.METHOD })
		public @interface Args {
			boolean adjunct() default false;
		}
	}

	public static class CancelHandler extends ActionHandler {
		@Override
		public void handleAction(Node node, GwtEvent event,
				ActionRefPlace place) {
			Place currentPlace = Client.currentPlace();
			/*
			 * FIXME - dirndl 1x2 - should be an event, handled by formModel
			 */
			FormModel formModel = ((FormModel.Has) node.getResolver())
					.getFormModel();
			TransformManager.get()
					.removeTransformsFor(formModel.getState().model);
			TransformManager.get()
					.deregisterProvisionalObject(formModel.getState().model);
			if (currentPlace instanceof EntityPlace) {
				/*
				 * behaviour differs. If action was CREATE, go back - if EDIT,
				 * VIEW
				 */
				EntityPlace currentEntityPlace = (EntityPlace) currentPlace;
				if (currentEntityPlace.action == EntityAction.CREATE) {
					History.back();
				} else {
					EntityPlace entityPlace = (EntityPlace) Reflections
							.newInstance(currentPlace.getClass());
					entityPlace.id = currentEntityPlace.id;
					entityPlace.go();
				}
			} else if (currentPlace instanceof CategoryNamePlace) {
				CategoryNamePlace categoryNamePlace = ((CategoryNamePlace) currentPlace)
						.copy();
				categoryNamePlace.nodeName = null;
				categoryNamePlace.go();
			}
		}
	}

	@Ref("cancel")
	@ActionRefHandler(CancelHandler.class)
	@EmitsModelEvent(ModelEvents.Cancelled.class)
	public static class CancelRef extends ActionRef {
	}

	public static class EntityTransformer extends
			AbstractContextSensitiveModelTransform<DirectedEntityActivity<? extends EntityPlace, ? extends Entity>, FormModel> {
		@Override
		public FormModel apply(
				DirectedEntityActivity<? extends EntityPlace, ? extends Entity> activity) {
			FormModelState state = new FormModelState();
			Entity entity = activity.getEntity();
			state.editable = activity.getPlace().action.isEditable();
			if (entity != null && state.editable) {
				entity = ClientTransformManager.cast().ensureEditable(entity);
			}
			state.model = entity;
			state.adjunct = state.editable
					&& ClientTransformManager.cast().isProvisionalEditing();
			return new FormModelTransformer().withContextNode(node)
					.apply(state);
		}
	}

	public static class FormElement extends Model {
		protected LabelModel label;

		protected FormValueModel value;

		protected Field field;

		private Bindable bindable;

		private boolean focusOnAttach;

		public FormElement() {
		}

		public FormElement(Field field, Bindable bindable) {
			this.field = field;
			this.bindable = bindable;
			this.label = Registry.impl(LabelModel.class).withFormElement(this);
			this.value = new FormValueModel(this);
		}

		public String getElementName() {
			return Ax.format("_dl_form_%s", field.getPropertyName());
		}

		public Field getField() {
			return this.field;
		}

		@Directed
		public LabelModel getLabel() {
			return this.label;
		}

		@Directed
		public FormValueModel getValue() {
			return this.value;
		}

		public boolean isFocusOnAttach() {
			return this.focusOnAttach;
		}

		public void setFocusOnAttach(boolean focusOnAttach) {
			this.focusOnAttach = focusOnAttach;
		}
	}

	public static class FormModelState {
		public Bindable presentationModel;

		public boolean expectsModel;

		public boolean editable;

		private boolean adjunct;

		private Bindable model;

		// FIXME - dirndl 1x2 - general property binding rethink. Declarative?
		// Using propertyenum?
		public Binding formBinding = new Binding();

		public Bindable getModel() {
			return this.model;
		}
	}

	@Reflected
	public static class FormModelTransformer extends
			AbstractContextSensitiveModelTransform<FormModelState, FormModel> {
		@Override
		public FormModel apply(FormModelState state) {
			FormModel formModel = Registry.impl(FormModel.class);
			formModel.state = state;
			if (state.model == null && state.expectsModel) {
				return formModel;
			}
			Args args = node.annotation(Args.class);
			ActionsModulator actionsModulator = args != null
					? Reflections.newInstance(args.actionsModulator())
					: new ActionsModulator();
			FieldModulator fieldModulator = args != null
					? Reflections.newInstance(args.fieldModulator())
					: new FieldModulator();
			BoundWidgetTypeFactory factory = new BoundWidgetTypeFactory(true);
			ModalResolver resolver = ModalResolver.single(node,
					!state.editable);
			resolver.setFormModel(formModel);
			node.setResolver(resolver);
			if (state.model != null) {
				if (state.model instanceof UserProperty) {
					state.presentationModel = (Bindable) ((UserProperty) state.model)
							.ensureUserPropertySupport().getPersistable();
				}
				if (state.presentationModel == null) {
					state.presentationModel = state.model;
				}
			}
			if (state.presentationModel != null) {
				List<Field> fields = GwittirBridge.get()
						.fieldsForReflectedObjectAndSetupWidgetFactoryAsList(
								state.presentationModel, factory,
								state.editable, state.adjunct, resolver);
				fields.stream()
						.filter(field -> fieldModulator
								.accept(state.presentationModel, field))
						.map(field -> {
							FormElement e = new FormElement(field,
									state.presentationModel);
							if (args != null && args.focusOnAttach()
									.equals(field.getPropertyName())) {
								e.setFocusOnAttach(true);
							}
							return e;
						}).forEach(formModel.elements::add);
			}
			if (state.adjunct) {
				new Link().withPlace(new ActionRefPlace(SubmitRef.class))
						.withClassName(Link.PRIMARY_ACTION)
						.addTo(formModel.actions);
				new Link().withPlace(new ActionRefPlace(CancelRef.class))
						.addTo(formModel.actions);
			} else {
				if (state.presentationModel != null) {
					ObjectActions actions = Reflections
							.at(state.presentationModel)
							.annotation(ObjectActions.class);
					if (actions != null) {
						Arrays.stream(actions.value()).map(Action::actionClass)
								.filter(clazz -> Reflections.isAssignableFrom(
										NonstandardObjectAction.class, clazz))
								.forEach(clazz -> {
									new Link()
											.withNonstandardObjectAction(clazz)
											.addTo(formModel.actions);
								});
					}
				}
			}
			formModel.actions.removeIf(actionsModulator::isRemoveAction);
			for (Link link : formModel.actions) {
				String overrideLinkText = actionsModulator
						.getOverrideLinkText(link);
				if (overrideLinkText != null) {
					link.withText(overrideLinkText);
				}
			}
			return formModel;
		}

		@Reflected
		public static class ActionsModulator {
			public String getOverrideLinkText(Link linkModel) {
				return null;
			}

			public boolean isRemoveAction(Link linkModel) {
				return false;
			}
		}

		@Retention(RetentionPolicy.RUNTIME)
		@Documented
		@Target({ ElementType.TYPE, ElementType.METHOD })
		@ClientVisible
		public static @interface Args {
			Class<? extends ActionsModulator> actionsModulator() default ActionsModulator.class;

			Class<? extends FieldModulator> fieldModulator() default FieldModulator.class;

			String focusOnAttach() default "";
		}

		@Reflected
		public static class FieldModulator {
			public boolean accept(Bindable model, Field field) {
				return true;
			}
		}
	}

	public static class FormValueModel extends Model implements ValueModel {
		protected FormElement formElement;

		public FormValueModel() {
		}

		public FormValueModel(FormElement formElement) {
			this.formElement = formElement;
		}

		@Override
		public Bindable getBindable() {
			return formElement.bindable;
		}

		@Override
		public Field getField() {
			return formElement.field;
		}

		public FormElement getFormElement() {
			return this.formElement;
		}

		@Override
		public String getGroupName() {
			return formElement.getElementName();
		}
	}

	public interface Has {
		public FormModel getFormModel();
	}

	@Registration(LabelModel.class)
	public static class LabelModel extends Model {
		protected FormElement formElement;

		public Field getField() {
			return formElement.field;
		}

		public FormElement getFormElement() {
			return this.formElement;
		}

		public LabelModel withFormElement(FormElement formElement) {
			this.formElement = formElement;
			return this;
		}
	}

	public static class ModelEventContext {
	}

	@Reflected
	public static class PermissibleActionFormTransformer extends
			AbstractContextSensitiveModelTransform<PermissibleAction, FormModel> {
		@Override
		public FormModel apply(PermissibleAction action) {
			FormModelState state = new FormModelState();
			state.editable = true;
			state.adjunct = true;
			state.expectsModel = true;
			if (action instanceof PermissibleEntityAction) {
				Entity entity = ((PermissibleEntityAction) action).getEntity();
				ObjectPermissions op = Reflections.at(entity)
						.annotation(ObjectPermissions.class);
				op = op == null
						? PermissionsManager.get().getDefaultObjectPermissions()
						: op;
				state.editable = PermissionsManager.get().isPermitted(entity,
						op.write());
				if (state.editable) {
					entity = ClientTransformManager.cast()
							.ensureEditable(entity);
				} else {
					state.adjunct = false;
				}
				state.model = entity;
			} else if (action instanceof RemoteActionWithParameters) {
				state.model = (Bindable) ((RemoteActionWithParameters) action)
						.getParameters();
			} else if (action instanceof LocalActionWithParameters) {
				if (((LocalActionWithParameters) action)
						.getParameters() instanceof FormEditableParameters) {
					state.model = (Bindable) ((LocalActionWithParameters) action)
							.getParameters();
				} else {
					state.model = null;
					state.expectsModel = false;
				}
			}
			return new FormModelTransformer().withContextNode(node)
					.apply(state);
		}
	}

	/*
	 * FIXME - dirndl 1x2 - move to OlForm
	 */
	public static class SubmitHandler extends ActionHandler {
		@Override
		public void handleAction(Node node, GwtEvent event,
				ActionRefPlace place) {
			((DomEvent) event).preventDefault();
			FormModel formModel = ((FormModel.Has) node.getResolver())
					.getFormModel();
			if (formModel.submit(node)) {
				Optional<EmitsModelEvent> emitsType = place.emitsModelEvent();
				Class<? extends ModelEvent> type = emitsType.get().value();
				Context context = NodeEvent.Context.newModelContext(event,
						node);
				ModelEvent.dispatch(context, type, formModel);
			}
		}
	}

	@Ref("submit")
	@ActionRefHandler(SubmitHandler.class)
	@EmitsModelEvent(value = ModelEvents.Submit.class, hasValidation = true)
	public static class SubmitRef extends ActionRef {
	}

	public interface ValueModel {
		Bindable getBindable();

		Field getField();

		String getGroupName();
	}
}
