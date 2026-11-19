/**
 *
 * <p>
 * A bidirectional, syncing object-tree/dom-tree document, mediated by a Dirndl
 * FragmentModel
 *
 * <p>
 * Nodes in the FragmentDocument object model structure are subtypes of the
 * {@link FragmentNode} and correspond to a node in the Dirndl node tree (one
 * such tree per FragmentDocument). The Dirndl nodes in turn correspond to an
 * XML node tree.
 * 
 * 
 * <ul>
 * <li>For xml schema validation, the fragmentDocument doc is transformed by
 * mapping tag/css-class - <code>block[@class='x']</code> - to a unique element
 * name (<code>block.x</code>)
 * <li>For the browser xml to be copy-pastable, certain non-schema elements are
 * created (such as joining commas) which would normally be expressed with CSS
 * :before and :after. Thos elements are tagged with attribute
 * <code>__ol_join="true"</code>
 * </ul>
 * 
 *
 * <h3>Transformation phases and expression</h3>
 * <p>
 * *How* selections should be processed in the traversal transformation varies
 * depending on where in the pipeline the processing takes place. The three main
 * phases and their corresponding expression/modifiers:
 * 
 * 
 * <ul>
 * <li>Feature recognition. {@link IgnoredNodeSelection} will encapsulate
 * ignored DOM nodes (non-ignored are encapsulated by {@link StyledNode)
 * <li>Overlap computation. Selections tagged with the interface IgnoreOverlaps
 * or with isOmit true will be ignored
 * <li>
 * <p>
 * MeasureTree (initial output doc) computation. Nodes tagged with the various
 * FragmentDocumentNode.NonOutput will not be output, and possibly the potential
 * subtree as well. This is correct since it's the *FragmentDocumentNode* that
 * defines output elision, rather than the selection (even though that
 * definition may then cause the selection's descendants to not be processed,
 * that's still an *output* rule)
 * <p>
 * MeasureSelection.Intermediate subtypes will also be ignored - that's
 * stretching it, but it can be thought of as "these selections only really
 * exist between layers X1 and X2 in the pipeline" - rather than a statement
 * about output per se, it's a statement about selection lifetime. Hence
 * 'intermediate'
 * </ul>
 * <p>
 * TODO topics:
 * <ul>
 * <li>{@link FragmentDocumentOverlayToken.Transformer} - transform an input
 * Measure to a FragmentDocumentNode
 * <li>{@link FragmentDocumentNode} - Java object corresponding to a logical
 * markup node
 * <li>{@link FragmentDocumentNode.Mapping} - Annotation defining per-schema
 * FragmentDocumentNode/XML mapping
 * </ul>
 *
 * 
 * <p>
 * Notes
 * </p>
 * <ul>
 * <li>
 *
 * </ul>
 * 
 * <p>
 * Pipelines (unidirection server, live/bidi client) are marked with the
 * {@link FragmentDocumentPipeline} interface
 * 
 * <h3>Useful examples</h3>
 * 
 * <p>
 * This will also be extended with validators (on the properties). Note that
 * order has to be defined in the &#064;PropertyOrder annotation (alas)
 * 
 * <h3>Philosophy of measures/selections</h3>
 * <ul>
 * <li>Selection boundaries are regarded as invariant - and *generally* the data
 * is. One exception is MeasureSelection.omit - this could be maintained via a
 * parallel model (map of MeasureSelection :-: Boolean) - but that's sorta
 * pointless. So to refine 'invariant' - 'invariant if defined during selection
 * creation'
 * <li>you *can* change the output fragmentnodes (the initial set correspond to
 * MeasureNode selections), you just need a selection (derived
 * from/selection-child of) the original MeasureNode) tracking the change
 * </ul>
 * <h3>TODO (doc)</h3>
 * <ul>
 * <li>How does information get from the &#064;Mapping annotation to the Dirndl
 * bidi transformer?
 * <p>
 * TransformerMatcher.createNodeTransformer <emph>matches</emph> the domnode -
 * ResolvedMapping provides the data which determine the match FragmentModel
 * <p>
 * To debug,
 * <ul>
 * <li>set a breakpoint on DirectedTransformer.appliesTo with expression
 * getFragmentNodeType().toString().contains("[classnamefragment]")
 * <li>eval fragmentModel.provideResolver().clearResolutionCache()
 * <li>set a breakpoint in FragmentDocumentResolver.contextAnnotation(), play
 * </ul>
 * <li>What about fancy Dirndl like &#064;Directed.Wrap? <b>A</b>By design,
 * there's a 1-1 dirndl node/dom node correspondence. So it's illegal in an
 * Fragment doc
 * <li>Where are the css/styles docs? See see also
 * </ul>
 * 
 * <h3>Gotchas</h3>
 * <ul>
 * <li>This pattern won't work - since FragmentNode iterates across the DomNode
 * contents, the contained Text DOM node needs to map to an Inline.Anonymous
 * FragmentNode
 * 
 * <pre>
 * <code>
 * 
 * 
&#064;Binding(type = Type.INNER_TEXT)
public String text;

 * </code>
 * </pre>
 * <p>
 * instead:
 * 
 * <pre>
 * <code>
 * 
public void onFragmentRegistration() {
	nodes().append(new Inline.Anonymous(text));
}
 * </code>
 * </pre>
 * 
 * <li>See ListItemLayer#patchOverlaps - and 'FIXME - fragmentDocument' in
 * general
 * </ul>
 * 
 * 
 */
package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc;
