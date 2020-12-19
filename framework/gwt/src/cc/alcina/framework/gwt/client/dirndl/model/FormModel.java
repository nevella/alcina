package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.place.shared.Place;
import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.actions.LocalActionWithParameters;
import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.actions.PermissibleActionHandler.DefaultPermissibleActionHandler;
import cc.alcina.framework.common.client.actions.PermissibleEntityAction;
import cc.alcina.framework.common.client.actions.RemoteActionWithParameters;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ModalDisplay.ModalResolver;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.activity.DirectedEntityActivity;
import cc.alcina.framework.gwt.client.dirndl.annotation.ActionRef;
import cc.alcina.framework.gwt.client.dirndl.annotation.ActionRef.ActionHandler;
import cc.alcina.framework.gwt.client.dirndl.annotation.ActionRef.ActionRefHandler;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.Ref;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransformNodeRenderer.AbstractContextSensitiveModelTransform;
import cc.alcina.framework.gwt.client.entity.EntityAction;
import cc.alcina.framework.gwt.client.entity.place.ActionRefPlace;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.entity.view.ClientFactory;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;
import cc.alcina.framework.gwt.client.place.CategoryNamePlace;

public class FormModel extends Model {
	protected List<FormElement> elements = new ArrayList<>();

	protected List<LinkModel> actions = new ArrayList<>();

	private FormModelState state;

	public FormModelState getState() {
		return this.state;
	}

	public List<LinkModel> getActions() {
		return this.actions;
	}

	@Ref("submit")
	@ActionRefHandler(SubmitHandler.class)
	public static class SubmitRef extends ActionRef {
	}

	public static class SubmitHandler extends ActionHandler {
		@Override
		public void handleAction(Node node, GwtEvent event,
				ActionRefPlace place) {
			FormModel formModel = (FormModel) node
					.ancestorModel(m -> m instanceof FormModel);
			formModel.onSubmit(node);
		}
	}

	public List<FormElement> getElements() {
		return this.elements;
	}

	@Ref("cancel")
	@ActionRefHandler(CancelHandler.class)
	public static class CancelRef extends ActionRef {
	}

	public static class CancelHandler extends ActionHandler {
		@Override
		public void handleAction(Node node, GwtEvent event,
				ActionRefPlace place) {
			Place currentPlace = ClientFactory.currentPlace();
			if (currentPlace instanceof EntityPlace) {
				EntityPlace entityPlace = ((EntityPlace) currentPlace).copy();
				entityPlace.action = EntityAction.VIEW;
				ClientFactory.goTo(entityPlace);
			} else if (currentPlace instanceof CategoryNamePlace) {
				CategoryNamePlace categoryNamePlace = ((CategoryNamePlace) currentPlace)
						.copy();
				categoryNamePlace.nodeName = null;
				ClientFactory.goTo(categoryNamePlace);
			}
		}
	}

	public void onSubmit(Node node) {
		Consumer<Void> onValid = o -> {
			if (getState().model instanceof Entity) {
				ClientTransformManager.cast()
						.promoteToDomainObject(getState().model);
			}
			if (ClientFactory.currentPlace() instanceof EntityPlace) {
				EntityPlace entityPlace = ((EntityPlace) ClientFactory
						.currentPlace()).copy();
				entityPlace.action = EntityAction.VIEW;
				ClientFactory.goTo(entityPlace);
			} else if (ClientFactory
					.currentPlace() instanceof CategoryNamePlace) {
				CategoryNamePlace categoryNamePlace = (CategoryNamePlace) ClientFactory
						.currentPlace();
				DefaultPermissibleActionHandler.handleAction(null,
						categoryNamePlace.ensureAction(), node);
			}
		};
		new FormValidation().validate(onValid, getState().formBinding);
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
				entity = ClientTransformManager.cast().ensureEditable(entity);
				state.model = entity;
			} else if (action instanceof RemoteActionWithParameters) {
				state.model = (Bindable) ((RemoteActionWithParameters) action)
						.getParameters();
			} else if (action instanceof LocalActionWithParameters) {
				state.model = null;
				state.expectsModel = false;
			}
			return new FormModelTransformer().withContextNode(node)
					.apply(state);
		}
	}

	public static class FormModelState {
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
		public FormModel apply(FormModelState args) {
			FormModel model = new FormModel();
			model.state = args;
			if (args.model == null && args.expectsModel) {
				return model;
			}
			BoundWidgetTypeFactory factory = new BoundWidgetTypeFactory(true);
			node.pushResolver(ModalResolver.single(!args.editable));
			if (args.model != null) {
				List<Field> fields = GwittirBridge.get()
						.fieldsForReflectedObjectAndSetupWidgetFactoryAsList(
								args.model, factory, args.editable,
								args.adjunct, node.getResolver());
				fields.stream().map(field -> new FormElement(field, args.model))
						.forEach(model.elements::add);
			}
			if (args.adjunct) {
				model.actions.add(new LinkModel()
						.withPlace(new ActionRefPlace(SubmitRef.class))
						.withPrimaryAction(true));
				model.actions.add(new LinkModel()
						.withPlace(new ActionRefPlace(CancelRef.class)));
			}else {
				if(args.model!=null) {
					Bean bean = Reflections.classLookup().getAnnotationForClass(args.model.getClass(), Bean.class);
					if(bean!=null) {
						Arrays.stream(bean.actions().value()).forEach(action->{
							
						});
					}
				}
				
			}
			return model;
		}
	}

	public static class LabelModel extends Model {
		protected FormElement formElement;

		public FormElement getFormElement() {
			return this.formElement;
		}

		public LabelModel() {
		}

		public LabelModel(FormElement formElement) {
			this.formElement = formElement;
		}

		public Field getField() {
			return formElement.field;
		}
	}

	public interface ValueModel {
		Bindable getBindable();

		Field getField();

		String getValueId();
	}

	public static class FormValueModel extends Model implements ValueModel {
		protected FormElement formElement;

		public FormValueModel() {
		}

		public FormElement getFormElement() {
			return this.formElement;
		}

		public FormValueModel(FormElement formElement) {
			this.formElement = formElement;
		}

		@Override
		public Field getField() {
			return formElement.field;
		}

		@Override
		public String getValueId() {
			return formElement.provideId();
		}

		@Override
		public Bindable getBindable() {
			return formElement.bindable;
		}
	}

	public static class FormElement extends Model {
		protected LabelModel label;

		protected FormValueModel value;

		private Field field;

		private Bindable bindable;

		private static transient int formElementIdxCounter;

		private int formElementIdx;

		public String provideId() {
			return Ax.format("_dl_form_%s", formElementIdx);
		}

		public FormElement() {
		}

		public FormElement(Field field, Bindable bindable) {
			this.field = field;
			this.bindable = bindable;
			this.formElementIdx = ++formElementIdxCounter;
			this.label = new LabelModel(this);
			this.value = new FormValueModel(this);
		}

		@Directed
		public LabelModel getLabel() {
			return this.label;
		}

		@Directed
		public FormValueModel getValue() {
			return this.value;
		}
	}
}
