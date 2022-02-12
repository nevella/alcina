package cc.alcina.framework.common.client.actions;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;

@Bean(allPropertiesVisualisable = true)
@ObjectPermissions(read = @Permission(access = AccessLevel.ADMIN), write = @Permission(access = AccessLevel.ADMIN))
public class EmptyParams extends Bindable implements RemoteParameters {
}
