package cc.alcina.framework.common.client.traversal.layer;

/**
 * <p>
 * Unlike SelectionTraversal, does not use TreeProcess - since there's not
 * necessarily a containment direction associated with this process. There are
 * still many similarities (layer vs generation, e.g.)
 *
 * <p>
 * FIXME - layer - add LayerProcess (like TreeProcess)
 *
 * @author nick@alcina.cc
 *
 * @param <T>
 * @param <S>
 */
public class LayeredTokenParser {
	// class DocumentLayer extends Layer implements MatchesAreOutputs {
	// private TokenImpl token = new TokenImpl();
	//
	// DocumentLayer() {
	// tokens.add(token);
	// }
	//
	// @Override
	// public Iterator computeInputs() {
	// return null;
	// }
	//
	// public List<Slice> computeInputs0() {
	// throw new UnsupportedOperationException();
	// // Location.Range documentRange = state.document.getLocationRange();
	// // return List.of(
	// // new Slice(documentRange.start, documentRange.end, token));
	// }
	//
	// class TokenImpl implements LayerToken {
	// // matches self
	// @Override
	// public Slice match(InputState state) {
	// Preconditions.checkState(state.input.token == token);
	// return state.input;
	// }
	// }
	// }
}
