/**
 * <h1>Welcome to Alcina</h1>
 * <p>
 * Alcina is an opinionated app development framework with strong client-side
 * ties to GWT - but it's a fairly even-handed mix of client and server-side
 * support for mid-sized domain applications with strong interrelationships.
 * </p>
 * <h2>Main components</h2>
 *
 * <h2>Naming conventions</h2>
 * <h3>Subclasses</h3>
 * <p>
 * If a subclass is named as a refinement of the superclass name, the refining
 * adjective can go either at the beginning or end of the name.
 * <p>
 * If the superclass name is single-word (say <code>Filter</code>), prefer
 * prefix naming (say <code>EntityFilter</code>), but prefer postfix naming for
 * compound superclass names: <code>EntityFilter</code> -&gt;
 * <code>EntityFilterRestriction</code>
 * <p>
 * The reasoning is derived by working backwards from usage in IDE autocomplete,
 * that specialised subclasses of compound names are rarely referred to directly
 * in Alcina code, more often they're created reflectively and so postfix naming
 * prevents clustering around common adjectives
 *
 * @category philosophy
 *
 * @see alcina/doc/readme.md
 */
package cc.alcina.framework;
