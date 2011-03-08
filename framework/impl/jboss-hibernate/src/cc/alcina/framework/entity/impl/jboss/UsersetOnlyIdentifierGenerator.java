package cc.alcina.framework.entity.impl.jboss;

import java.io.Serializable;

import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;

import cc.alcina.framework.common.client.logic.domain.HasId;

public class UsersetOnlyIdentifierGenerator implements IdentifierGenerator {
	public Serializable generate(SessionImplementor si, Object entity) {
		HasId myEntity = (HasId) entity;
		if (myEntity.getId() > 0) {
			// the identifier has been set manually => use it
			return myEntity.getId();
		} else {
			throw new RuntimeException(
					"Objects using this identity generator" +
					" must have id manually set prior to invocation");
		}
	}
}