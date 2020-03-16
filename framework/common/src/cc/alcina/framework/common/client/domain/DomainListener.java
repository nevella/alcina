package cc.alcina.framework.common.client.domain;

import cc.alcina.framework.common.client.logic.domain.Entity;

public interface DomainListener<H extends Entity> {
    public abstract Class<H> getListenedClass();

    public abstract void insert(H o);

    public boolean isEnabled();

    public abstract void remove(H o);

    public void setEnabled(boolean enabled);

    IDomainStore getDomainStore();

    boolean matches(H h, Object[] keys);
}
