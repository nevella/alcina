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
 */
package cc.alcina.framework.common.client.logic.permissions;
