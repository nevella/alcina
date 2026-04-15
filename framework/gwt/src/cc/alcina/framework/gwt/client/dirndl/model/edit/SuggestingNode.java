package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.List;

import com.google.gwt.dom.client.Selection;
import com.google.gwt.dom.client.behavior.ElementBehavior;
import com.google.gwt.dom.client.behavior.HasElementBehaviors;
import com.google.gwt.dom.client.behavior.RemoteElementBehaviors;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Focus;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Focusin;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.TextNode;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor;

/**
 * A node in an editable DOM region serving as the input for a {@link Suggestor}
 */
@Directed(className = "suggesting-node")
@TypedProperties
public class SuggestingNode extends EditNode
		implements Binding.TabIndexMinusOne, HasElementBehaviors,
		DomEvents.Focus.Handler, DomEvents.Focusin.Handler {
	protected PackageProperties._SuggestingNode.InstanceProperties
			properties() {
		return PackageProperties.suggestingNode.instance(this);
	}

	DecoratorNode.Descriptor decoratorDescriptor;

	TextNode textFragment;

	@Binding(to = "tabIndex", type = Binding.Type.PROPERTY)
	int tabIndex = -1;

	public SuggestingNode() {
		contentEditable = true;
	}

	/**
	 * 
	 * @param decoratorDescriptor
	 * @param textFragment
	 *            the textFragment to wrap (may be blank, or say '@')
	 */
	SuggestingNode(DecoratorNode.Descriptor decoratorDescriptor,
			TextNode textFragment) {
		this();
		this.decoratorDescriptor = decoratorDescriptor;
		this.textFragment = textFragment;
	}

	@Override
	public void onFragmentRegistration() {
		nodes().append(textFragment);
	}

	@Override
	public List<ElementBehavior> getBehaviors() {
		return List.of(new EditAreaBehavior.InterceptUpDownBehaviour(),
				new ElementBehavior.PreventDefaultEnterBehaviour(),
				new ElementBehavior.MarkContainsCursorBehaviour(),
				new RemoteElementBehaviors.ElementOffsetsRequired());
	}

	@Override
	public boolean provideIsContentEditable() {
		return true;
	}

	/**
	 * @return false if copy/pasted from another editor (and thus stripped)
	 */
	@Override
	public boolean isValid() {
		return decoratorDescriptor != null;
	}

	@Override
	public void onFocus(Focus event) {
		collapseSelectionToStart();
	}

	@Override
	public void onFocusin(Focusin event) {
		collapseSelectionToStart();
	}

	void collapseSelectionToStart() {
		if (!provideIsBound()) {
			return;
		}
		DomNode domNode = provideElement().asDomNode();
		Selection selection = domNode.gwtNode().getOwnerDocument()
				.getSelection();
		boolean collapse = !selection.hasAttachedSelection()
				|| !domNode.asRange().contains(selection.getFocusLocation());
		if (collapse) {
			selection.collapse(domNode.asLocation());
		}
	}
}
