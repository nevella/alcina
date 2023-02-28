package cc.alcina.framework.entity.parser.layered;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cc.alcina.framework.common.client.util.NestedNameProvider;
import cc.alcina.framework.entity.parser.layered.LayeredTokenParser.LayerState;
import cc.alcina.framework.entity.parser.layered.LayeredTokenParser.LayerState.InputState;

public abstract class ParserLayer {
	public final Name name;

	public List<Token> tokens = new ArrayList<>();

	List<ParserLayer> children = new ArrayList<>();

	ParserLayer parent;

	public ParserLayer(Name name) {
		this.name = name;
	}

	protected ParserLayer() {
		name = Name.fromClass(getClass());
	}

	public abstract List<Slice>
			generateInputs(LayerState layerState);

	public void withParent(ParserLayer parent) {
		parent.children.add(this);
		this.parent = parent;
	}

	protected void addTokens(Token... tokens) {
		Arrays.stream(tokens).forEach(this.tokens::add);
	}

	protected void onAfterParse(InputState inputState) {
		inputState.copyOutputsToLayerState();
	}

	protected void onAfterParse(LayerState state) {
		if (this instanceof MatchesAreOutputs) {
			state.outputs = state.matches;
		}
	}

	protected void onBeforeParse(LayerState state) {
	}

	/*
	 * Marker, copy layerstate.matches->layerstate.outputs
	 */
	public interface MatchesAreOutputs {
	}

	public interface Name {
		public static Name fromClass(Class clazz) {
			return new NameFromClass(clazz);
		}

		String name();

		static final class NameFromClass implements Name {
			private final Class clazz;

			NameFromClass(Class clazz) {
				this.clazz = clazz;
			}

			@Override
			public String name() {
				return this.clazz.getName();
			}

			@Override
			public String toString() {
				return NestedNameProvider.get(this.clazz);
			}
		}
	}
}
