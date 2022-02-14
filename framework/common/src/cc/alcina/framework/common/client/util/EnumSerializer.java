package cc.alcina.framework.common.client.util;

import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecordType;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;


@ClientInstantiable
@Registration.Singleton
public class EnumSerializer {
	public <T extends Enum> T deserialize(Class<T> enumClass, String text) {
		if (enumClass == DeltaApplicationRecordType.class && text != null) {
			if (text.equals("TO_REMOTE")) {
				return (T) DeltaApplicationRecordType.LOCAL_TRANSFORMS_APPLIED;
			} else if (text.equals("TO_REMOTE_COMPLETED")) {
				return (T) DeltaApplicationRecordType.LOCAL_TRANSFORMS_REMOTE_PERSISTED;
			} else if (text.equals("CLIENT_OBJECT_LOAD")) {
				return (T) DeltaApplicationRecordType.LOCAL_TRANSFORMS_REMOTE_PERSISTED;
			} else if (text.equals("CLIENT_SYNC")) {
				return (T) DeltaApplicationRecordType.LOCAL_TRANSFORMS_REMOTE_PERSISTED;
			}
		}
		return (T) (CommonUtils.isNullOrEmpty(text) ? null
				: Enum.valueOf(enumClass, text));
	}
}
