package cc.alcina.framework.gwt.client.dirndl.model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory;

import cc.alcina.framework.common.client.Reflections;
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
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.ModalDisplay.ModalResolver;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.activity.DirectedEntityActivity;
import cc.alcina.framework.gwt.client.dirndl.annotation.ActionRef;
import cc.alcina.framework.gwt.client.dirndl.annotation.ActionRef.ActionHandler;
import cc.alcina.framework.gwt.client.dirndl.annotation.ActionRef.ActionRefHandler;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.EmitsTopic;
import cc.alcina.framework.gwt.client.dirndl.annotation.Ref;
import cc.alcina.framework.gwt.client.dirndl.behaviour.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.behaviour.DomEvents.Submit;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent.Context;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvents;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransformNodeRenderer.AbstractContextSensitiveModelTransform;
import cc.alcina.framework.gwt.client.dirndl.layout.TopicEvent;
import cc.alcina.framework.gwt.client.entity.EntityAction;
import cc.alcina.framework.gwt.client.entity.place.ActionRefPlace;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.place.CategoryNamePlace;
import cc.alcina.framework.gwt.client.util.Async;

@RegistryLocation(registryPoint = FormModel.class, implementationType = ImplementationType.INSTANCE)
public class FormModel extends Model implements DomEvents.Submit.Handler {
	protected List<FormElement> elements = new ArrayList<>();

	protected List<Link> actions = new ArrayList<>();

	private FormModelState state;

	public List<Link> getActions() {
		return this.actions;
	}

	public FormModel() {
	}

	public List<FormElement> getElements() {
		return this.elements;
	}

	public FormModelState getState() {
		return this.state;
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
				AsyncCallback callback = Async.callbackBuilder().success(o2 -> {
					EntityPlace entityPlace = ((EntityPlace) Client
							.currentPlace()).copy();
					entityPlace.action = EntityAction.VIEW;
					Client.goTo(entityPlace);
				}).build();
				CommitToStorageTransformListener.get()
						.flushWithOneoffCallback(callback);
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
			 * FIXME - adjunct
			 */
			FormModel formModel = (FormModel) node
					.ancestorModel(m -> m instanceof FormModel);
			TransformManager.get()
					.removeTransformsFor(formModel.getState().model);
			TransformManager.get()
					.deregisterProvisionalObject(formModel.getState().model);
			if (currentPlace instanceof EntityPlace) {
				EntityPlace entityPlace = ((EntityPlace) currentPlace).copy();
				entityPlace.action = EntityAction.VIEW;
				Client.goTo(entityPlace);
			} else if (currentPlace instanceof CategoryNamePlace) {
				CategoryNamePlace categoryNamePlace = ((CategoryNamePlace) currentPlace)
						.copy();
				categoryNamePlace.nodeName = null;
				Client.goTo(categoryNamePlace);
			}
		}
	}

	@Ref("cancel")
	@ActionRefHandler(CancelHandler.class)
	@EmitsTopic(NodeEvents.Cancelled.class)
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

		// FIXME - dirndl.1 - set up for object binding checks
		public Binding formBinding = new Binding();

		public Bindable getModel() {
			return this.model;
		}
	}

	@ClientInstantiable
	public static class FormModelTransformer extends
			AbstractContextSensitiveModelTransform<FormModelState, FormModel> {
		@Override
		public FormModel apply(FormModelState state) {
			FormModel model = Registry.impl(FormModel.class);
			model.state = state;
			if (state.model == null && state.expectsModel) {
				return model;
			}
			Args args = node.annotation(Args.class);
			ActionsModulator actionsModulator = args != null
					? Reflections.newInstance(args.actionsModulator())
					: new ActionsModulator();
			FieldModulator fieldModulator = args != null
					? Reflections.newInstance(args.fieldModulator())
					: new FieldModulator();
			BoundWidgetTypeFactory factory = new BoundWidgetTypeFactory(true);
			ModalResolver childResolver = ModalResolver
					.single(node.getResolver(), !state.editable);
			node.pushChildResolver(childResolver);
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
								state.editable, state.adjunct, childResolver);
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
						}).forEach(model.elements::add);
			}
			if (state.adjunct) {
				model.actions.add(new Link()
						.withPlace(new ActionRefPlace(SubmitRef.class))
						.withPrimaryAction(true));
				model.actions.add(new Link()
						.withPlace(new ActionRefPlace(CancelRef.class)));
			} else {
				if (state.presentationModel != null) {
					Bean bean = Reflections.classLookup().getAnnotationForClass(
							state.presentationModel.getClass(), Bean.class);
					if (bean != null) {
						Arrays.stream(bean.actions().value())
								.map(a -> Reflections
										.newInstance(a.actionClass()))
								.filter(a -> a instanceof NonstandardObjectAction)
								.map(a -> (NonstandardObjectAction) a)
								.forEach(action -> {
									model.actions.add(new Link()
											.withNonstandardObjectAction(
													action));
								});
					}
				}
			}
			model.actions.removeIf(actionsModulator::isRemoveAction);
			for (Link link : model.actions) {
				String overrideLinkText = actionsModulator
						.getOverrideLinkText(link);
				if (overrideLinkText != null) {
					link.withText(overrideLinkText);
				}
			}
			return model;
		}

		@ClientInstantiable
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

		@ClientInstantiable
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

	@RegistryLocation(registryPoint = LabelModel.class, implementationType = ImplementationType.INSTANCE)
	public static class LabelModel extends Model {
		protected FormElement formElement;

		public LabelModel withFormElement(FormElement formElement) {
			this.formElement = formElement;
			return this;
		}

		public Field getField() {
			return formElement.field;
		}

		public FormElement getFormElement() {
			return this.formElement;
		}
	}

	public static class ModelEventContext {
	}

	@ClientInstantiable
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
				ObjectPermissions op = Reflections.classLookup()
						.getAnnotationForClass(entity.getClass(),
								ObjectPermissions.class);
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
	 * FIXME - dirndl 1.2 - move to OlForm
	 */
	public static class SubmitHandler extends ActionHandler {
		@Override
		public void handleAction(Node node, GwtEvent event,
				ActionRefPlace place) {
			((DomEvent) event).preventDefault();
			FormModel formModel = (FormModel) node
					.ancestorModel(m -> m instanceof FormModel);
			if (formModel.submit(node)) {
				Optional<EmitsTopic> emitsTopic = place.emitsTopic();
				Class<? extends TopicEvent> type = emitsTopic.get().value();
				Context context = NodeEvent.Context.newTopicContext(event,
						node);
				TopicEvent.fire(context, type, formModel);
			}
		}
	}

	@Ref("submit")
	@ActionRefHandler(SubmitHandler.class)
	@EmitsTopic(value = NodeEvents.Submitted.class, hasValidation = true)
	public static class SubmitRef extends ActionRef {
	}

	public interface ValueModel {
		Bindable getBindable();

		Field getField();

		String getGroupName();
	}
}
