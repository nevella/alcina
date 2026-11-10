/**
 * <p>
 * Hopefully the last token parser framework, learning lessons from the previous
 * two
 *
 * <h2>Main issues with previous parsers</h2>
 * <ul>
 * <li>Lack of layering/composition
 * <li>Use of w3c DOM (definitely use alcina DOM)
 * <li>Multiple document location models
 * <li>Too much cross-layer lookahead/behind
 * </ul>
 *
 * <h2>Terminology</h2>
 * <ul>
 * <li>Token - a LayeredParserToken
 * <li>Range - corresponds to a DOM range
 * <li>Measure - a tuple of [Token, Range]
 * 
 * </ul>
 * <p>
 * The LayerParser uses a grammar similar to Regex grammar to emit a sequence of
 * matching "sentences" from a given dom document. See the parser javadoc for
 * gotchas
 *
 * 
 */
package cc.alcina.framework.common.client.traversal.layer;
