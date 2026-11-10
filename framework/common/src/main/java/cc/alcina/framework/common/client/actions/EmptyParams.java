package cc.alcina.framework.common.client.actions;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;

@Bean
@Display.AllProperties
@ObjectPermissions(
	read = @Permission(access = AccessLevel.ADMIN),
	write = @Permission(access = AccessLevel.ADMIN))
public class EmptyParams extends Bindable implements RemoteParameters {
}
