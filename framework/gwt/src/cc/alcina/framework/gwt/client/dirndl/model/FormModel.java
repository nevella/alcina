package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.google.gwt.event.shared.GwtEvent;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.ActionRef;
import cc.alcina.framework.gwt.client.dirndl.annotation.ActionRef.ActionHandler;
import cc.alcina.framework.gwt.client.dirndl.annotation.ActionRef.ActionRefHandler;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.Ref;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.NotRenderedNodeRenderer;
import cc.alcina.framework.gwt.client.entity.place.ActionRefPlace;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;

public class FormModel extends Model {
	protected List<FormElement> elements = new ArrayList<>();

	protected List<LinkModel> actions = new ArrayList<>();

	private FormModelArgs args;

	@Directed(renderer = NotRenderedNodeRenderer.class)
	public FormModelArgs getArgs() {
		return this.args;
	}

	public List<LinkModel> getActions() {
		return this.actions;
	}

	@Ref("submit")
	@ActionRefHandler(SaveHandler.class)
	public static class SubmitRef extends ActionRef {
	}

	public static class SaveHandler extends ActionHandler {
		@Override
		public void handleAction(Node node, GwtEvent event,
				ActionRefPlace place) {
			((FormModel) node.getModel()).onSave();
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
			((FormModel) node.getModel()).onCancel();
		}
	}

	public void onSave() {
		Ax.out(args);
	}

	public void onCancel() {
		// TODO Auto-generated method stub
	}

	@ClientInstantiable
	public static class FormModelEntityTransformer
			implements Function<EntityPlace, FormModel> {
		@Override
		public FormModel apply(EntityPlace place) {
			FormModelArgs args = new FormModelArgs();
			args.model = place.provideEntity();
			args.editable = place.action.isEditable();
			args.adjunct = true;
			return new FormModelTransformer().apply(args);
		}
	}

	public static class FormModelArgs {
		private boolean editable;

		private boolean adjunct;

		private Bindable model;

		public Bindable getModel() {
			return this.model;
		}
	}

	@ClientInstantiable
	public static class FormModelTransformer
			implements Function<FormModelArgs, FormModel> {
		@Override
		public FormModel apply(FormModelArgs args) {
			FormModel model = new FormModel();
			model.args = args;
			if (args.model == null) {
				return model;
			}
			BoundWidgetTypeFactory factory = new BoundWidgetTypeFactory(true);
			List<Field> fields = GwittirBridge.get()
					.fieldsForReflectedObjectAndSetupWidgetFactoryAsList(
							args.model, factory, args.editable, args.adjunct);
			fields.stream().map(field -> new FormElement(field, args.model))
					.forEach(model.elements::add);
			if (args.adjunct) {
				model.actions.add(new LinkModel()
						.withPlace(new ActionRefPlace(SubmitRef.class))
						.withPrimaryAction(true));
				model.actions.add(new LinkModel()
						.withPlace(new ActionRefPlace(CancelRef.class)));
			}
			return model;
		}
	}

	public static class LabelModel extends Model {
		protected FormElement formElement;

		@Directed(renderer = NotRenderedNodeRenderer.class)
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

	public static class ValueModel extends Model {
		protected FormElement formElement;

		public ValueModel() {
		}

		@Directed(renderer = NotRenderedNodeRenderer.class)
		public FormElement getFormElement() {
			return this.formElement;
		}

		public ValueModel(FormElement formElement) {
			this.formElement = formElement;
		}

		public Field getField() {
			return formElement.field;
		}

		@Directed(renderer = NotRenderedNodeRenderer.class)
		public String getValueId() {
			return formElement.provideId();
		}

		@Directed(renderer = NotRenderedNodeRenderer.class)
		public Bindable getBindable() {
			return formElement.bindable;
		}
	}

	public static class FormElement extends Model {
		protected LabelModel label;

		protected ValueModel value;

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
			this.value = new ValueModel(this);
		}

		public LabelModel getLabel() {
			return this.label;
		}

		public ValueModel getValue() {
			return this.value;
		}
	}
}
