package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;

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
import cc.alcina.framework.gwt.client.dirndl.layout.FragmentNode;
import cc.alcina.framework.gwt.client.dirndl.layout.HasTag;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.Model.FocusOnBind;
import cc.alcina.framework.gwt.client.dirndl.model.dom.EditSelection;
import cc.alcina.framework.gwt.client.dirndl.model.edit.DecoratorEvent.MutationStrings;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentModel;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentModel.ModelMutation;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentResolver;

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
		new NonEditableCursorTargetConstraint().alignWithConstraint();
	}

	/**
	 * 
	 * 
	 * <p>
	 * Thsi behaviour ensures that a ZWS text node exists between adjacent
	 * non-editables (DecoratorNodes), and (optionally) at the editor
	 * Boundaries.
	 * 
	 * <p>
	 * It also ensures unneeded ZWS nodes are removed or unwrapped
	 */
	class NonEditableCursorTargetConstraint implements DecoratorBehavior {
		void alignWithConstraint() {
			fragmentModel.byTypeAssignable(ZeroWidthCursorTarget.class).toList()
					.forEach(ZeroWidthCursorTarget::unwrapIfContainsNonZwsText);
			Set<ZeroWidthCursorTarget> validZwsTargets = new LinkedHashSet<>();
			if (insertInterNonEditableCursorTargets) {
				fragmentModel.byTypeAssignable(DecoratorNode.class).toList()
						.stream()
						.map(DecoratorNode::ensureInterNonEditableTarget)
						.forEach(validZwsTargets::add);
			}
			ensureBoundaryCursorTargets().stream()
					.forEach(validZwsTargets::add);
			fragmentModel.byTypeAssignable(ZeroWidthCursorTarget.class).toList()
					.stream().filter(zws -> !validZwsTargets.contains(zws))
					.forEach(zws -> zws.nodes().removeFromParent());
			new DecoratorEvent().withType(DecoratorEvent.Type.zws_refreshed)
					.publish();
		}
	}

	List<ZeroWidthCursorTarget> ensureBoundaryCursorTargets() {
		List<ZeroWidthCursorTarget> result = new ArrayList<>();
		/*
		 * start boundary
		 */
		{
			boolean requiresInsert = false;
			// not required - handled by end-of-pass validation
			// boolean requiresDelete=false;
			List<? extends FragmentNode> children = fragmentModel.children()
					.toList();
			int size = children.size();
			FragmentNode first = Ax.first(children);
			FragmentNode second = size >= 2 ? children.get(1) : null;
			if (size == 0) {
				requiresInsert = true;
			} else if (size == 1) {
				if (first instanceof ZeroWidthCursorTarget) {
					result.add((ZeroWidthCursorTarget) first);
				} else if (HasContentEditable.isUneditable(first)
						&& insertEditorStartCursorTarget) {
					requiresInsert = true;
				}
			} else {
				if (first instanceof ZeroWidthCursorTarget) {
					if (HasContentEditable.isUneditable(second)) {
						result.add((ZeroWidthCursorTarget) first);
					}
				} else if (HasContentEditable.isUneditable(first)
						&& insertEditorStartCursorTarget) {
					requiresInsert = true;
				}
			}
			if (requiresInsert) {
				ZeroWidthCursorTarget cursorTarget = new ZeroWidthCursorTarget();
				if (first == null) {
					fragmentModel.getFragmentRoot().append(cursorTarget);
				} else {
					first.nodes().insertBeforeThis(cursorTarget);
				}
				result.add(cursorTarget);
			}
		}
		/*
		 * end boundary
		 */
		{
			boolean requiresInsert = false;
			List<? extends FragmentNode> children = fragmentModel.children()
					.toList();
			int size = children.size();
			Preconditions.checkState(size > 0);
			FragmentNode last = Ax.last(children);
			FragmentNode secondLast = size >= 2 ? children.get(size - 2) : null;
			if (size == 1) {
				if (last instanceof ZeroWidthCursorTarget) {
					result.add((ZeroWidthCursorTarget) last);
				} else if (HasContentEditable.isUneditable(last)) {
					requiresInsert = true;
				}
			} else {
				if (last instanceof ZeroWidthCursorTarget) {
					if (HasContentEditable.isUneditable(secondLast)) {
						result.add((ZeroWidthCursorTarget) last);
					}
				} else if (HasContentEditable.isUneditable(last)) {
					requiresInsert = true;
				}
			}
			if (requiresInsert) {
				ZeroWidthCursorTarget cursorTarget = new ZeroWidthCursorTarget();
				last.nodes().insertAfterThis(cursorTarget);
				result.add(cursorTarget);
			}
		}
		return result;
	}
}