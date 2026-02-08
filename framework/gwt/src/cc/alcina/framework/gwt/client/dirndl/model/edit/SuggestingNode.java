package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.List;

import com.google.gwt.dom.client.behavior.ElementBehavior;
import com.google.gwt.dom.client.behavior.HasElementBehaviors;

import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.TextNode;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor;

/**
 * A node in an editable DOM region serving as the input for a {@link Suggestor}
 */
@Directed(className = "suggesting-node")
@TypedProperties
public class SuggestingNode extends EditNode
		implements Binding.TabIndexMinusOne, HasElementBehaviors {
	protected PackageProperties._SuggestingNode.InstanceProperties
			properties() {
		return PackageProperties.suggestingNode.instance(this);
	}

	DecoratorNode.Descriptor decoratorDescriptor;

	TextNode textFragment;

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
				new ElementBehavior.PreventDefaultEnterBehaviour());
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
}
