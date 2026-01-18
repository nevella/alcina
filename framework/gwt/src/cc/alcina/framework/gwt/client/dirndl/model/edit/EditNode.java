package cc.alcina.framework.gwt.client.dirndl.model.edit;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Selection;

import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.TextNode;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.Suggestion;

/**
 * The base class for FragmentModel subtypes understood by the EditArea -
 * subtypes are SuggestingNode, an editable region which acts as the input to a
 * {@link Suggestor}, and {@link DecoratorNode} which is the non-editable
 * representation of a selected {@link Suggestion}
 */
@TypedProperties
public abstract class EditNode extends FragmentNode
		implements Binding.TabIndexMinusOne, HasContentEditable {
	protected PackageProperties._EditNode.InstanceProperties
			_EditNode_properties() {
		return PackageProperties.editNode.instance(this);
	}

	@Binding(type = Type.CSS_CLASS)
	public boolean selected;

	@Binding(
		type = Type.PROPERTY,
		to = "contentEditable",
		transform = Binding.DisplayFalseTrueBidi.class)
	public boolean contentEditable = false;

	@Override
	public boolean provideIsContentEditable() {
		return contentEditable;
	}

	void updateSelected() {
		Selection selection = Document.get().getSelection();
		boolean selected = selection.hasSelection() && !selection.isCollapsed()
				&& selection.asRange()
						.contains(provideElement().asDomNode().asRange());
		_EditNode_properties().selected().set(selected);
	}

	@Override
	public void onBind(Bind event) {
		super.onBind(event);
		new DecoratorEvent()
				.withType(event.isBound() ? DecoratorEvent.Type.node_bound
						: DecoratorEvent.Type.node_unbound)
				.withSubtype(NestedName.get(this)).withMessage(toString())
				.publish();
	}

	// FIXME - DN server shd validate entity on update. and other
	// validations (e.g. not contained in a decorator)
	public abstract boolean isValid();

	void stripIfInvalid() {
		if (!isValid()) {
			nodes().strip();
			/*
			 * FIXME - dn - tree change listener (FragmentModel?) should
			 * probably merge adjacent FragmentNode.Text children if any result
			 * from the strip
			 */
		}
	}

	/*
	 * insert a blank text node if the subsequent is also a non-editable
	 * decorator
	 */
	void ensureInterNonEditableTarget() {
		if (!HasContentEditable.isUneditable(this)) {
			return;
		}
		/*
		 * Possibly review this - the treeSubsequentNodeNoDescent usage is
		 * intended to handle effectively adjacent decorators such as
		 * <container><decorator/></container><decorator>
		 */
		FragmentNode checkSubsequent = nodes().treeSubsequentNodeNoDescent();
		if (HasContentEditable.isUneditable(checkSubsequent)) {
			TextNode newCursorTarget = new TextNode();
			nodes().insertAfterThis(newCursorTarget);
		}
	}
}
