/**
 * <h3>Alcina domain search</h3>
 * <p>
 * The alcina search pipeline takes a
 * {@link cc.alcina.framework.common.client.search.SearchDefinition
 * SearchDefinition} and returns a
 * {@link cc.alcina.framework.common.client.search.SearchResults SearchResults}.
 * Some steps of the implementation vary for non-memory search, but this doc
 * also applies to most of that pipeline
 * <p>
 * WIP - describe the search pipeline
 *
 * <table>
 * <tr>
 * <td>Step</td>
 * <td>Class</td>
 * <td>Notes</td>
 * <td>Example</td>
 * </tr>
 * <tr>
 * <td>Instantiate the search definition</td>
 * <td>{@link cc.alcina.framework.common.client.search.SearchDefinition
 * SearchDefinition}</td>
 * <td>Often the search definition constructor will call an init() method which
 * populates the initial criteria groups and criteria.</td>
 * <td>Event</td>
 * </tr>
 * <tr>
 * <td>Populate the search definition</td>
 * <td>Class</td>
 * <td>Event</td>
 * <td>Event</td>
 * </tr>
 * </table>
 */
package cc.alcina.framework.common.client.domain.search;
