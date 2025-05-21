package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.google.gwt.core.client.Scheduler;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
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
import cc.alcina.framework.gwt.client.dirndl.model.dom.EditSelection;
import cc.alcina.framework.gwt.client.dirndl.model.edit.DecoratorEvent.MutationStrings;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentModel;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentModel.ModelMutation;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentResolver;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.TextNode;

/**
 * <p>
 * This class models an editable dom area. It maintains bi-directional sync
 * between the dom tree and the dirndl node/fragmentnode tree via the
 * FragmentModel helper, routing DOM mutation events to it
 *
 *
 * 
 *
 */
@Directed(emits = { ModelEvents.Input.class })
@DirectedContextResolver(FragmentResolver.class)
@TypeSerialization(reflectiveSerializable = false)
@TypedProperties
public class EditArea extends Model.Fields
		implements FocusOnBind, HasTag, DomEvents.Input.Handler,
		DomEvents.BeforeInput.Handler, LayoutEvents.BeforeRender.Handler,
		DomEvents.Focusout.Handler, InferredDomEvents.Mutation.Handler,
		FragmentModel.Has, ModelMutation.Handler {
	public static PackageProperties._EditArea properties = PackageProperties.editArea;

	@Binding(type = Type.INNER_HTML)
	public String value;

	private String currentValue;

	@Binding(type = Type.PROPERTY)
	public String placeholder;

	public boolean focusOnBind;

	public String tag = "edit";

	boolean stripFontTagsOnInput = false;

	@Binding(type = Type.PROPERTY, to = "contenteditable")
	public boolean contentEditable = true;

	FragmentModel fragmentModel;

	/*
	 * Note that a cusor target will always be inserted at document end (unless
	 * there's an editable node). The following two parameters control other
	 * cursor target insertions
	 * 
	 * An example that _doesn't_ require inter-non-editable targets is an
	 * unordered ChoiceEditor
	 */
	protected boolean insertInterNonEditableCursorTargets = true;

	protected boolean insertEditorStartCursorTarget = true;

	public EditArea() {
		createFragmentModel();
	}

	public EditArea(String value) {
		this();
		setValue(value);
	}

	protected void createFragmentModel() {
		fragmentModel = new FragmentModel(this);
	}

	private String elementValue() {
		if (!provideIsShowingPlaceholder()) {
			return provideElement().getInnerHTML();
		} else {
			return null;
		}
	}

	public String getCurrentValue() {
		if (currentValue == null) {
			currentValue = elementValue();
		}
		return this.currentValue;
	}

	@Override
	public boolean isFocusOnBind() {
		return focusOnBind;
	}

	DomNode node() {
		return provideElement().asDomNode();
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
		boolean bindingsDisabled = true;
		if (Ax.ntrim(elementValue.replaceAll("<br>", "")).isEmpty()) {
			/*
			 * *DO* rewrite
			 */
			bindingsDisabled = false;
			elementValue = "";
		}
		try {
			provideNode().setBindingsDisabled(bindingsDisabled);
			setValue(elementValue);
		} finally {
			provideNode().setBindingsDisabled(false);
		}
	}

	@Override
	public void onInput(Input event) {
		currentValue = elementValue();
		if (stripFontTagsOnInput) {
			new EditSelection().strip(provideElement(), "font");
		}
		event.reemitAs(this, ModelEvents.Input.class);
	}

	@Override
	public void onMutation(Mutation event) {
		/*
		 * This is intended only for romcom (which has a configurable observer
		 * for DecoratorEvent)
		 */
		ProcessObservers.publish(DecoratorEvent.class, () -> {
			DecoratorEvent decoratorEvent = new DecoratorEvent()
					.withType(DecoratorEvent.Type.editor_transforms_applied);
			DecoratorEvent.MutationStrings mutationStrings = new MutationStrings();
			mutationStrings.mutationRecords = Ax.newlineJoin(event.records);
			mutationStrings.editorDom = provideElement().asDomNode()
					.prettyToString();
			decoratorEvent.mutationStrings = mutationStrings;
			return decoratorEvent;
		});
		fragmentModel.onMutation(event);
	}

	@Override
	public FragmentModel provideFragmentModel() {
		return this.fragmentModel;
	}

	private boolean provideIsShowingPlaceholder() {
		// TODO - disallow placeholder in normalisation
		DomNode firstNode = node().children.firstNode();
		return firstNode != null && firstNode.tagIs("placeholder");
	}

	@Override
	public String provideTag() {
		return tag;
	}

	public void setValue(String value) {
		String old_value = this.value;
		this.value = value;
		propertyChangeSupport().firePropertyChange("value", old_value, value);
	}

	@Override
	public void onModelMutation(ModelMutation event) {
		provideNode().deferIfFiring(() -> {
			new CursorTargetConstraint().alignWithConstraint();
			new SuggestorCurrencyConstraint().maybeRefreshOverlays(event);
		});
	}

	/**
	 * 
	 * 
	 * <p>
	 * Thsi behaviour ensures that a blank text node exists between adjacent
	 * non-editables (DecoratorNodes), and (optionally) at the editor
	 * Boundaries.
	 * 
	 * <p>
	 * It does not remove empty text nodes which are *not* required to allow
	 * cursor positioning
	 * 
	 * <p>
	 * Note (future) re validation cost - for a given mutation, determine the
	 * lowest affected block. If a decorator is not within the lowest affected
	 * block (and that itself is something computed by FM), no need to recompute
	 */
	class CursorTargetConstraint implements DecoratorBehavior {
		void alignWithConstraint() {
			// fragmentModel.byTypeAssignable(CursorTarget.class).toList()
			// .forEach(ZeroWidthCursorTarget::unwrapIfContainsNonZwsText);
			// Set<ZeroWidthCursorTarget> validZwsTargets = new
			// LinkedHashSet<>();
			// if (insertInterNonEditableCursorTargets) {
			// fragmentModel.byTypeAssignable(DecoratorNode.class).toList()
			// .stream()
			// .map(DecoratorNode::ensureInterNonEditableTarget)
			// .forEach(validZwsTargets::add);
			// }
			// ensureBoundaryCursorTargets().stream()
			// .forEach(validZwsTargets::add);
			// fragmentModel.byTypeAssignable(ZeroWidthCursorTarget.class).toList()
			// .stream().filter(zws -> !validZwsTargets.contains(zws))
			// .forEach(zws -> zws.nodes().removeFromParent());
			/*
			 * Note that because of fragment isolates, the TextNode list refers
			 * only to textnodes *outside* decorator nodes
			 */
			/*
			 * ensure there's at least an empty text in the editor
			 */
			if (fragmentModel.childList().isEmpty()) {
				fragmentModel.getFragmentRoot().append(new TextNode());
			}
			if (insertInterNonEditableCursorTargets) {
				/*
				 * ensure there's inter-non-editable targets
				 */
				fragmentModel.byTypeAssignable(DecoratorNode.class).toList()
						.forEach(DecoratorNode::ensureInterNonEditableTarget);
			}
			if (insertEditorStartCursorTarget) {
				FragmentNode first = Ax.first(fragmentModel.childList());
				if (DecoratorNode.isNonEditable(first)) {
					first.nodes().insertBeforeThis(new TextNode());
				}
			}
			/*
			 * always ensure there's a cursor target at the end
			 */
			{
				FragmentNode last = Ax.last(fragmentModel.childList());
				if (DecoratorNode.isNonEditable(last)) {
					last.nodes().insertAfterThis(new TextNode());
				}
			}
			new DecoratorEvent().withType(DecoratorEvent.Type.zws_refreshed)
					.publish();
		}
	}

	/*
	 * This class ensures the displayed suggestor (if any) is current - if say a
	 * choice is deleted, the overlay should be repositioned and the available
	 * choices modified
	 */
	class SuggestorCurrencyConstraint implements DecoratorBehavior {
		void maybeRefreshOverlays(ModelMutation event) {
			if (!event.getData().isEmpty()) {
				Scheduler.get().scheduleFinally(() -> emitEvent(
						ContentDecoratorEvents.NodeDelta.class));
			}
		}
	}
}