/**
 * <p>
 * WIP. This will become the general support for Alcina contexts - transform,
 * permissions, http, document.
 * <p>
 * A {@code Provider} returns context instances - generally three classes of
 * provider :: threadlocal (server/console), single (script) and single/threaded
 * (devmode - todo - currently single).
 * <p>
 * Providers can also be explicitly tied to a LooseContext - see
 * DocumentContextProviderImpl
 */
package cc.alcina.framework.common.client.context;
