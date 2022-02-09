package cc.alcina.framework.common.client.logic.domain;

import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.IVersionable;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.logic.reflection.Registration;

@ObjectPermissions(create = @Permission(access = AccessLevel.ADMIN), read = @Permission(access = AccessLevel.EVERYONE), write = @Permission(access = AccessLevel.ADMIN), delete = @Permission(access = AccessLevel.ADMIN))
@MappedSuperclass
@RegistryLocation(registryPoint = VersionableEntity.class)
@Bean
@Registration(VersionableEntity.class)
public abstract class VersionableEntity<T extends VersionableEntity> extends Entity<T> implements IVersionable {

    public static final transient String CONTEXT_FIRE_CREATION_DATE_EVENTS = VersionableEntity.class.getName() + ".CONTEXT_FIRE_CREATION_DATE_EVENTS";

    public static final transient Comparator<? super VersionableEntity> LAST_MODIFIED_COMPARATOR = Comparator.comparing(VersionableEntity::getLastModificationDate);

    public static final transient Comparator<? super VersionableEntity> LAST_MODIFIED_COMPARATOR_DESCENDING = LAST_MODIFIED_COMPARATOR.reversed();

    private Date lastModificationDate;

    private Date creationDate;

    public VersionableEntity(boolean exDomain) {
    }

    protected VersionableEntity() {
    }

    @Override
    @Display(name = "Creation date", displayMask = Display.DISPLAY_RO_PROPERTY)
    public Date getCreationDate() {
        return this.creationDate;
    }

    @Override
    @PropertyPermissions(read = @Permission(access = AccessLevel.EVERYONE), write = @Permission(access = AccessLevel.ROOT))
    @Display(name = "Id", displayMask = Display.DISPLAY_RO_PROPERTY, styleName = "nowrap id", orderingHint = 5)
    @Transient
    public abstract long getId();

    @Override
    @Display(name = "Last modified", orderingHint = 999)
    @PropertyPermissions(read = @Permission(access = AccessLevel.EVERYONE), write = @Permission(access = AccessLevel.ROOT))
    public Date getLastModificationDate() {
        return this.lastModificationDate;
    }

    public boolean provideWasCreatedBefore(int i, TimeUnit unit) {
        long time = getCreationDate() == null && Ax.isTest() ? System.currentTimeMillis() : getCreationDate().getTime();
        return System.currentTimeMillis() - unit.toMillis(i) > time;
    }

    @Override
    public void setCreationDate(Date creationDate) {
        if (LooseContext.is(CONTEXT_FIRE_CREATION_DATE_EVENTS)) {
            Date old_creationDate = this.creationDate;
            this.creationDate = creationDate;
            propertyChangeSupport().firePropertyChange("creationDate", old_creationDate, creationDate);
        } else {
            this.creationDate = creationDate;
        }
    }

    @Override
    public void setLastModificationDate(Date lastModificationDate) {
        this.lastModificationDate = lastModificationDate;
    }
}
