package cc.alcina.framework.gwt.client.dirndl.impl.form;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.totsp.gwittir.client.ui.table.Field;

import cc.alcina.framework.common.client.csobjects.BaseSourcesPropertyChangeEvents;
import cc.alcina.framework.common.client.logic.reflection.ModalDisplay;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registration.Priority;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.gwt.client.dirndl.RenderContext;
import cc.alcina.framework.gwt.client.dirndl.activity.DirectedEntityActivity;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.impl.form.FmsContentCells.FmsValidationFeedbackSupplier;
import cc.alcina.framework.gwt.client.dirndl.model.BeanForm;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel.FormElement;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel.FormModelProvider;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel.LabelModel;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.gwittir.customiser.OneToManyCustomiser;
import cc.alcina.framework.gwt.client.gwittir.customiser.OneToManyMultipleSummaryCustomiser;

public class FmsForm {
	public static void registerImplementations() {
		FormModelProvider.get().register(FormModel.class, FmsFormModel.class);
		FormModelProvider.get().register(BeanForm.FormTransform.Impl.class,
				FmsFormTransform.class);
		FormModelProvider.get().register(FormModel.LabelModel.class,
				FmsLabelModel.class);
		FormModelProvider.get().register(ModalDisplay.ModeTransformer.class,
				ModalDisplay.ModeTransformer.class);
	}

	// FIXME - ol.dirndl.1d - sass!
	@Directed(
		tag = "form-element",
		reemits = { ModelEvents.LabelClicked.class,
				ModelEvents.FormElementLabelClicked.class },
		bindings = @Binding(
			from = "elementName",
			type = Type.PROPERTY,
			to = "name"))
	public static class FmsFormElement extends FormElement {
		public FmsFormElement() {
		}
	}

	@Directed(
		tag = "form",
		bindings = @Binding(
			to = "autocomplete",
			type = Type.PROPERTY,
			literal = "off"))
	@DirectedContextResolver(FmsContentCells.FmsCellsContextResolver.class)
	@Registration(
		value = FormModel.class,
		priority = Registration.Priority.REMOVE)
	public static class FmsFormModel extends FormModel {
		@Directed(
			tag = "nested-form",
			bindings = @Binding(
				to = "autocomplete",
				type = Type.PROPERTY,
				literal = "off"))
		public static class NestedForm extends FmsFormModel {
		}

		public FmsFormModel() {
		}

		@Override
		@Directed.Wrap("actions")
		public List<Link> getActions() {
			List<Link> actions = super.getActions().stream()
					.collect(Collectors.toList());
			if (actions.isEmpty()) {
				return null;
			}
			// right-aligned
			Collections.reverse(actions);
			return actions;
		}

		@Override
		@Directed.Wrap("section")
		public List<FormElement> getElements() {
			return super.getElements();
		}

		@Override
		@Directed
		public Model getFormValidationResult() {
			return super.getFormValidationResult();
		}
	}

	@Reflected
	@Registration(
		value = BeanForm.FormTransform.Impl.class,
		priority = Priority.REMOVE)
	public static class FmsFormTransform extends BeanForm.FormTransform.Impl {
		@Override
		public FormModel apply(BaseSourcesPropertyChangeEvents model) {
			try {
				RenderContext.get().push();
				/*
				 * FIXME - dirndl 1x2 - RenderContext => DirectedContext
				 */
				RenderContext.get().setValidationFeedbackSupplier(
						new FmsValidationFeedbackSupplier());
				AbstractContextSensitiveModelTransform transformer = new FormModel.BindableFormModelTransformer();
				if (model instanceof DirectedEntityActivity) {
					transformer = new FormModel.EntityTransformer();
				}
				transformer.withContextNode(node);
				FormModel formModel = (FormModel) transformer.apply(model);
				formModel.setSubmitTextBoxesOnEnter(true);
				if (model instanceof DirectedEntityActivity) {
					DirectedEntityActivity<?, ?> activity = (DirectedEntityActivity<?, ?>) model;
					if (activity.getPlace().getAction().isEditable()) {
						formModel.getElements().removeIf(e -> e
								.provideValueModel().getField()
								.getCellProvider() instanceof OneToManyMultipleSummaryCustomiser
								|| e.provideValueModel().getField()
										.getCellProvider() instanceof OneToManyCustomiser);
					}
				}
				return formModel;
			} finally {
				RenderContext.get().pop();
			}
		}
	}

	@Directed(
		tag = "label",
		className = "ol-label",
		reemits = { DomEvents.Click.class, ModelEvents.LabelClicked.class },
		bindings = { @Binding(from = "for", type = Type.PROPERTY),
				@Binding(from = "text", type = Type.INNER_TEXT),
				@Binding(from = "title", type = Type.PROPERTY) })
	public static class FmsLabelModel extends LabelModel {
		public FmsLabelModel() {
		}

		public FmsLabelModel(LabelModel labelModel) {
			formElement = labelModel.getFormElement();
		}

		@Override
		public Field getField() {
			return super.getField();
		}

		public String getFor() {
			return getFormElement().getElementName();
		}

		public String getText() {
			return getField().getLabel();
		}

		public String getTitle() {
			return getField().getHelpText();
		}
	}
}
