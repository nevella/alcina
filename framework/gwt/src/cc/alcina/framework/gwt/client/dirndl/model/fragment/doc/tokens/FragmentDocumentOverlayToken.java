package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens;

import static cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.FragmentDocumentOverlayToken.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocumentNode;

/**
 * Subtypes (and corresponding dom ranges) denote logical areas of a markup
 * document within the FragmentDocument logical model
 *
 *
 *
 * 
 *
 */
public interface FragmentDocumentOverlayToken extends Measure.Token {
	static final Map<Class<? extends FragmentDocumentOverlayToken>, FragmentDocumentOverlayToken> typeInstances = new LinkedHashMap<>();

	public static void register(FragmentDocumentOverlayToken typeInstance) {
		typeInstances.put(typeInstance.getClass(), typeInstance);
	}

	public static FragmentDocumentOverlayToken
			typeInstance(Class<FragmentDocumentOverlayToken> clazz) {
		return typeInstances.get(clazz);
	}

	/*
	 * FIXME - document the logic here. It can contain a leaf? What sort of
	 * leaf? Why, and who's asking?
	 */
	public interface CanContainLeaf {
		default boolean canContainLeaf() {
			return true;
		}
	}

	public abstract class AbstractToken
			implements FragmentDocumentOverlayToken {
		protected AbstractToken() {
			register(this);
		}

		@Override
		public String toString() {
			return NestedName.get(this);
		}
	}

	/**
	 * Transforms a document measure to an FragmentDocument node. By default it
	 * transforms using reflection based on the generic parameters of the
	 * subtype
	 *
	 */
	@Registration.NonGenericSubtypes(value = Transformer.class, size = 2)
	public interface Transformer<I extends FragmentDocumentOverlayToken, O extends FragmentDocumentNode>
			extends Function<MeasureSelection, O> {
		static <I extends FragmentDocumentOverlayToken> Class<I>
				from(Class<Transformer<?, ?>> transformer) {
			return Reflections.at(transformer).firstGenericBound();
		}

		static <O extends FragmentDocumentNode> Class<O>
				to(Class<Transformer<?, ?>> transformer) {
			return Reflections.at(transformer).getGenericBounds().bounds.get(1);
		}

		@Override
		default O apply(MeasureSelection t) {
			return (O) Reflections
					.newInstance(to((Class<Transformer<?, ?>>) getClass()));
		}
	}
}
