package cc.alcina.framework.gwt.client.dirndl.model.edit;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.BeforeInput;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Focusout;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Input;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents.Mutation;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.layout.HasTag;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.Model.FocusOnBind;
import cc.alcina.framework.gwt.client.dirndl.model.dom.RelativeInputModel;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentModel;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentResolver;

/**
 * <p>
 * This class models an editable dom area. It maintains bi-directional sync
 * between the dom tree and the dirndl node/featurenode tree via the
 * FragmentModel helper, routing DOM mutation events to it
 *
 *
 * 
 *
 */
@Directed(
	bindings = { @Binding(type = Type.INNER_HTML, from = "value"),
			@Binding(type = Type.PROPERTY, from = "placeholder"),
			@Binding(
				type = Type.PROPERTY,
				literal = "true",
				to = "contenteditable") },
	receives = { DomEvents.Input.class, DomEvents.BeforeInput.class,
			DomEvents.Focusout.class, InferredDomEvents.Mutation.class },
	emits = { ModelEvents.Input.class })
@DirectedContextResolver(FragmentResolver.class)
public class EditArea extends Model implements FocusOnBind, HasTag,
		DomEvents.Input.Handler, DomEvents.BeforeInput.Handler,
		LayoutEvents.BeforeRender.Handler, DomEvents.Focusout.Handler,
		InferredDomEvents.Mutation.Handler, FragmentModel.Has {
	private String value;

	private String currentValue;

	private String placeholder;

	private boolean focusOnBind;

	private String tag = "edit";

	boolean stripFontTagsOnInput = false;

	FragmentModel fragmentModel;

	public EditArea() {
		createFragmentModel();
	}

	public EditArea(String value) {
		this();
		setValue(value);
	}

	public String getCurrentValue() {
		if (currentValue == null) {
			currentValue = elementValue();
		}
		return this.currentValue;
	}

	public String getPlaceholder() {
		return this.placeholder;
	}

	public String getTag() {
		return this.tag;
	}

	public String getValue() {
		return this.value;
	}

	@Override
	public boolean isFocusOnBind() {
		return focusOnBind;
	}

	// @Feature.Ref(Feature_Dirndl_ContentDecorator.Constraint_NonSuggesting_DecoratorTag_Selection.class)
	@Override
	public void onBeforeInput(BeforeInput event) {
		stripFontTagsOnInput = provideElement().asDomNode().children
				.noElements();
	}

	@Override
	public void onBind(Bind event) {
		super.onBind(event);
		fragmentModel.onBind(event);
	}

	@Override
	public void onFocusout(Focusout event) {
		// FIXME - editor.model - handle earlier (strip as soon as an edit
		// occurs)
		//
		// Also - should *insert* pre edit
		//
		/// List<DomNode> zeroWidthSpaceContainers =
		/// provideElement().asDomNode()
		// .stream().filter(DomNode::isElement)
		// .filter(n -> n.tagAndClassIs("span", "cursor-target"))
		// .collect(Collectors.toList());
		// zeroWidthSpaceContainers.forEach(DomNode::strip);
		// List<DomNode> removeZeroWidthSpace = provideElement().asDomNode()
		// .stream().filter(DomNode::isText)
		// .filter(n -> n.textContains("\u200b"))
		// .collect(Collectors.toList());
		// removeZeroWidthSpace
		// .forEach(n -> n.setText(n.textContent().replace("\u200b", "")));
		String elementValue = elementValue();
		if (Ax.ntrim(elementValue.replaceAll("<br>", "")).isEmpty()) {
			elementValue = "";
		}
		setValue(elementValue);
	}

	@Override
	public void onInput(Input event) {
		currentValue = elementValue();
		if (stripFontTagsOnInput) {
			new RelativeInputModel().strip(provideElement(), "font");
		}
		event.reemitAs(this, ModelEvents.Input.class);
	}

	@Override
	public void onMutation(Mutation event) {
		fragmentModel.onMutation(event);
	}

	@Override
	public FragmentModel provideFragmentModel() {
		return this.fragmentModel;
	}

	@Override
	public String provideTag() {
		return getTag();
	}

	public void setFocusOnBind(boolean focusOnBind) {
		this.focusOnBind = focusOnBind;
	}

	public void setPlaceholder(String placeholder) {
		this.placeholder = placeholder;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public void setValue(String value) {
		String old_value = this.value;
		this.value = value;
		propertyChangeSupport().firePropertyChange("value", old_value, value);
	}

	private String elementValue() {
		if (!provideIsShowingPlaceholder()) {
			return provideElement().getInnerHTML();
		} else {
			return null;
		}
	}

	private boolean provideIsShowingPlaceholder() {
		// TODO - disallow placeholder in normalisation
		DomNode firstNode = node().children.firstNode();
		return firstNode != null && firstNode.tagIs("placeholder");
	}

	protected void createFragmentModel() {
		fragmentModel = new FragmentModel(this);
	}

	DomNode node() {
		return provideElement().asDomNode();
	}
}