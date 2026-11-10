package cc.alcina.framework.common.client.logic.permissions;

import java.util.Set;
import java.util.stream.Stream;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.VersionableEntity;

@MappedSuperclass
/**
 *
 * <p>
 * This class models an access-control-list (a list of permissions, applicable
 * to one or more entities. It's (very) loosely modelled on the deprecated
 * java.security.Acl - um, where is its replacement? java.security.Policy is so
 * abstract as to be ... very abstract.
 *
 * 
 *
 */
public abstract class Acl extends VersionableEntity<Acl> {
	@Transient
	public abstract Set<? extends AclEntry> getEntries();

	public abstract Stream<? extends Entity> provideTargets();
}
