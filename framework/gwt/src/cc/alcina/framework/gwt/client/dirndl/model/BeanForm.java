package cc.alcina.framework.gwt.client.dirndl.model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.csobjects.BaseSourcesPropertyChangeEvents;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.activity.DirectedEntityActivity;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Submit;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform.AbstractContextSensitiveModelTransform;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform.ContextSensitiveTransform;
import cc.alcina.framework.gwt.client.dirndl.model.FormEvents.QueryValidity;
import cc.alcina.framework.gwt.client.dirndl.model.FormEvents.ValidationResultEvent;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel.FormModelProvider;

/**
 * <p>
 * This class transforms a {@link Bindable} into a labelled, possibly editable
 * form
 * <p>
 * Topic/notes:
 * <ul>
 * <li>Multiple section edit UIs: use a {@link MultipleEditorSupport}
 * <ul>
 * <li>Use a
 * </ul>
 * </ul>
 */
@Directed(tag = "bean-editor")
public class BeanForm extends Model {
	/*
	 * @formatter:off



	 * @formatter:on
	 */
	public static class MultipleEditorSupport implements
			ModelEvents.Submit.Handler, FormEvents.QueryValidity.Emitter,
			FormEvents.ValidationResultEvent.Handler {
		public interface Host {
			void setInert(boolean inert);

			void setValidationMessage(String validationMessage);

			List<Bindable> provideBindables();

			default Model provideModel() {
				return (Model) this;
			}
		}

		Host host;

		List<Bindable> bindables;

		int validBindableCount;

		public MultipleEditorSupport(Host host) {
			this.host = host;
		}

		@Override
		public void onValidationResult(ValidationResultEvent event) {
			switch (event.getModel().state) {
			case VALID:
				validBindableCount++;
				if (validBindableCount == bindables.size()) {
					event.reemitAs(host.provideModel(),
							FormEvents.ValidationSuccessMultiple.class);
				}
				break;
			case INVALID:
				host.setInert(false);
				host.setValidationMessage(event.getModel().exceptionMessage);
				break;
			default:
				throw new UnsupportedOperationException();
			}
		}

		@Override
		public void onSubmit(Submit event) {
			host.setInert(true);
			host.setValidationMessage(null);
			validBindableCount = 0;
			event.reemitAs(host.provideModel(), QueryValidity.class);
		}
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
	public @interface Classes {
		ClassName[] value();
	}

	@Reflected
	public enum ClassName {
		grid, wide, horizontal_validation, vertical_validation, vertical,
		label_over, tight_rows
	}

	/**
	 * this indirection allows (requires) an app to define its own bean viewer
	 * implementations
	 */
	@Reflected
	public static class FormTransform extends
			AbstractContextSensitiveModelTransform<BaseSourcesPropertyChangeEvents, FormModel> {
		public abstract static class Impl extends
				AbstractContextSensitiveModelTransform<BaseSourcesPropertyChangeEvents, FormModel> {
		}

		@Reflected
		public static class BasicFormTransform
				extends BeanForm.FormTransform.Impl {
			@Override
			public FormModel apply(BaseSourcesPropertyChangeEvents model) {
				AbstractContextSensitiveModelTransform transformer = new FormModel.BindableFormModelTransformer();
				if (model instanceof DirectedEntityActivity) {
					transformer = new FormModel.EntityTransformer();
				}
				transformer.withContextNode(node);
				FormModel formModel = (FormModel) transformer.apply(model);
				formModel.setSubmitTextBoxesOnEnter(true);
				return formModel;
			}
		}

		private Impl delegate;

		public FormTransform() {
			delegate = FormModelProvider.get()
					.impl(BeanForm.FormTransform.Impl.class);
		}

		@Override
		public FormModel apply(BaseSourcesPropertyChangeEvents t) {
			return delegate.apply(t);
		}

		@Override
		public AbstractContextSensitiveModelTransform<BaseSourcesPropertyChangeEvents, FormModel>
				withContextNode(Node node) {
			return delegate.withContextNode(node);
		}
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
	public @interface Actions {
		Class<? extends ModelEvent>[] value();
	}

	/*
	 * FIXME - dirndl general 'from parent' directed.passparent(Actions.class)
	 */
	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
	public @interface ActionsFromParent {
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
	public @interface Headings {
		String heading();

		String description() default "";
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
	public @interface ActionsProvider {
		Class<? extends ActionsProviderType> value();
	}

	public abstract static class ActionsProviderType
			extends AbstractContextSensitiveModelTransform<Object, List<Link>> {
	}

	public static class NonAdjunct extends BeanForm {
		@Override
		@BeanViewModifiers(adjunct = false)
		public Bindable getBindable() {
			return super.getBindable();
		}
	}

	public static class Viewer extends BeanForm
			implements ContextSensitiveTransform<Bindable, Viewer> {
		TransformSupport transformSupport = new TransformSupport(this);

		@Override
		public Viewer apply(Bindable t) {
			setBindable(t);
			transformSupport.applyAnnotations();
			return this;
		}

		@Override
		@BeanViewModifiers(editable = false, nodeEditors = true)
		@BeanForm.ActionsFromParent
		public Bindable getBindable() {
			return super.getBindable();
		}

		@Override
		public ContextSensitiveTransform<Bindable, Viewer>
				withContextNode(Node contextNode) {
			transformSupport.withContextNode(contextNode);
			return this;
		}
	}

	public static class Editor extends BeanForm
			implements ContextSensitiveTransform<Bindable, Editor> {
		public static class Adjunct extends Editor {
			@BeanViewModifiers(
				editable = true,
				nodeEditors = true,
				adjunct = true,
				cancel = false)
			@BeanForm.ActionsFromParent
			public Bindable getBindable() {
				return super.getBindable();
			}
		}

		TransformSupport transformSupport = new TransformSupport(this);

		@Override
		public Editor apply(Bindable t) {
			setBindable(t);
			transformSupport.applyAnnotations();
			return this;
		}

		@Override
		@BeanViewModifiers(editable = true, nodeEditors = true)
		@BeanForm.ActionsFromParent
		public Bindable getBindable() {
			return super.getBindable();
		}

		@Override
		public ContextSensitiveTransform<Bindable, Editor>
				withContextNode(Node contextNode) {
			transformSupport.withContextNode(contextNode);
			return this;
		}
	}

	static class TransformSupport {
		BeanForm beanForm;

		Node contextNode;

		TransformSupport(BeanForm beanForm) {
			this.beanForm = beanForm;
		}

		void withContextNode(Node contextNode) {
			this.contextNode = contextNode;
		}

		void applyAnnotations() {
			Headings headings = contextNode.annotation(Headings.class);
			if (headings != null) {
				beanForm.setHeading(headings.heading());
				if (headings.description().length() > 0) {
					beanForm.setDescription(headings.description());
				}
			}
		}
	}

	private String heading;

	private String description;

	private Bindable bindable;

	private String className;

	private boolean inert;

	@Directed.Transform(FormTransform.class)
	public Bindable getBindable() {
		return this.bindable;
	}

	@Binding(type = Type.CLASS_PROPERTY)
	public String getClassName() {
		return this.className;
	}

	@Directed
	public String getDescription() {
		return this.description;
	}

	@Directed
	public String getHeading() {
		return this.heading;
	}

	@Binding(type = Type.PROPERTY)
	public boolean isInert() {
		return this.inert;
	}

	@Override
	public void onBeforeRender(BeforeRender event) {
		Classes classes = event.getContext().node.annotation(Classes.class);
		if (classes != null) {
			className = Arrays.stream(classes.value()).map(Ax::cssify)
					.collect(Collectors.joining(" "));
		}
		super.onBeforeRender(event);
	}

	public void setBindable(Bindable bindable) {
		this.bindable = bindable;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setHeading(String heading) {
		this.heading = heading;
	}

	public void setInert(boolean inert) {
		set("inert", this.inert, inert, () -> this.inert = inert);
	}
}
