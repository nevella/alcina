package cc.alcina.framework.common.client.actions;

import cc.alcina.framework.common.client.csobjects.BaseBindable;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.BeanInfo;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
@BeanInfo(displayNamePropertyName = "",allPropertiesVisualisable=true)
@ObjectPermissions(read = @Permission(access = AccessLevel.ADMIN), write = @Permission(access = AccessLevel.ADMIN))
public class EmptyParams  extends BaseBindable implements RemoteParameters{
}
