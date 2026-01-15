package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.function.Function;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Selection;
import com.google.gwt.dom.client.Text;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNode.DomNodeText.SplitResult;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Commit;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.dom.EditSelection;
import cc.alcina.framework.gwt.client.dirndl.model.edit.StringRepresentable.RepresentableToStringTransform;
import cc.alcina.framework.gwt.client.dirndl.model.edit.StringRepresentable.RepresentableToStringTransform.HasStringRepresentableType;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentIsolate;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentModel;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.TextNode;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor;

/**
 * A node in an editable DOM region serving as the input for a {@link Suggestor}
 */
@Directed(className = "suggesting-node")
@TypedProperties
public class SuggestingNode extends EditNode
		implements Binding.TabIndexMinusOne {
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

	/*
	 * wip - dn
	 */
	@Binding(
		type = Type.PROPERTY,
		to = DecoratorBehavior.InterceptUpDownBehaviour.ATTR_NAME)
	public boolean isMagicName() {
		return true;
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
