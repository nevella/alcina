/**
 * <p>
 * Support for document transformation to a FragmentDocument
 *
 * <p>
 * The transformer framework is really the third generation of Alcina 'parser'
 * framework - complex structured document processor - prior generations
 * ({@link TokenParser}, {@link StructuredTokenParser} were not as focussed on
 * document transformation (they were split between that and document
 * information extraction).
 *
 * <p>
 * Those parsers were single-pass, the selectiontraversal system is particulary
 * useful for support of multi-generational transformations where the result of
 * multiple preceding transforms are used to compute the current generation's
 * output.
 *
 * <p>
 * At a minimum, the FragmentDocument levels are:
 * <ul>
 * <li>DOM tags (handle general formatting such as font style, weight, size,
 * tables, lists)
 * <li>Custom structure (determine unique structural elements particular to the
 * journal and document type
 * <li>To-FragmentDocument transformation from the incoming model generations
 *
 * <p>
 * Update - the above is outdated, v3.1 (layer traversal) is a more layered
 * approach, appears to be much more legible
 * 
 * <h3>Markup to FragmentDocument</h3>
 * <p>
 * See e.g. {@link PersistentContent#getReportedJudgment}
 * 
 * <h3>Challenges</h3>
 * <p>
 * Variant containment. This is handled in
 * {@link MeasureComputeStructuredHtmlIgnores} and
 * {@link MeasureSplitAndHandleTextOverlaps} - these are just before the logical
 * model -to- output model transform, where the logical model ranges are if
 * necessary pushed down inside to content html nodes (TD, LI) to ensure the
 * html is valid
 */
package cc.alcina.framework.servlet.transform.fragment;
