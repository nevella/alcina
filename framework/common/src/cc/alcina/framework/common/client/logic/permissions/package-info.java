/**
 * <h2>Alcina Permissions</h2>
 * <p>
 * The alcina permissions system is used primarily to control access to entities
 * and UI interface elements
 * 
 * (for alcina doc?)
 * <p>
 * The permission AccessLevel is one of [EVERYONE, LOGGED_IN, ADMIN, DEVELOPER,
 * ROOT] and their AccessLevel (which will be one of those values) has an
 * ordinal >= the permission AccessLevel ordinal
 * 
 * 
 * <p>
 * The permission AccessLevel == AccessLevel.GROUP and the user is a member of
 * the group with the name of the Permission rule() value *or* an Admin
 * 
 * 
 * <p>
 * The permission AccessLevel == AccessLevel.ADMIN_OR_OWNER and the user is the
 * object owner *or* an Admin
 * 
 * 
 * <p>
 * The permission rule() annotation attribute is non-empty, AccessLevel !=
 * AccessLevel.GROUP and the rule evaluator corresponding to the rule()
 * attribute returns true for the given user, object and context. The evaluator
 * can be found by searching for usages of the string value of the rule()
 * attribute
 * 
 * <h3>Uncategorised notes re the interaction between clientinstance and
 * permissions modelled by PermissionState (context)
 * 
 * <code><pre>
 * * doc - alcina - permissions + identity
    * a client instance is many things - an owner of a local domain, a record of user interaction with a system
    * by default server threads have no permission/identity context. This is because implicit context in a stackable context system is just...no
        * TODO - a transaction.begin requires.a ClientInstance (identity context)
        * note that rpc threads currently have the client user context - and need to be explicitly raised to allow domain mutation
 * 
 * </pre></code>
 * 
 */
package cc.alcina.framework.common.client.logic.permissions;
