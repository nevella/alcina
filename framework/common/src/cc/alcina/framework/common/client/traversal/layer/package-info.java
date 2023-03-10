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
 * <li>Slice - a tuple of [Token, Range]
 * <li>Larger/smaller - in the sense of the slice.range containment relationship
 * with another slice - so a HEAD slice might contain a METADATA:DOCTITLE slice
 * <li>Layer - a feature recognition layer
 * </ul>
 * <h2>Layers</h2>
 * <p>
 * Unlike selectiontraversal (and why partly the reason that's not being
 * reused), it's entirely possible that larger tokens are defined by the
 * presence or absence of smaller tokens - e.g. the HEAD might just be the union
 * of METADATA tokens. So it's not possible to compose the pipeline as a series
 * of DOM descents.
 * <p>
 * Layers form a tree, allowing a structure like [match 1, match 2, combine
 * (1,2)]. If layers contain children, they do not iterate over incoming tokens,
 * rather just emit the output of their last child output layer
 * <p>
 * Layer invariants:
 * <ul>
 * <li>Outputs from a given layer tree cannot have non-nested intersections
 * <li>Outputs from a given layer cannot intersect
 * </ul>
 *
 */
package cc.alcina.framework.common.client.traversal.layer;
