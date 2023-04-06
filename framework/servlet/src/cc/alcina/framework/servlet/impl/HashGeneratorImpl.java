package cc.alcina.framework.servlet.impl;

import cc.alcina.framework.common.client.csobjects.UrlRequest.HashGenerator;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registration.Priority;
import cc.alcina.framework.entity.EncryptionUtils;

@Registration(
	value = HashGenerator.class,
	priority = Priority.PREFERRED_LIBRARY)
public class HashGeneratorImpl extends HashGenerator {
	@Override
	public String hash(String input) {
		return EncryptionUtils.get().SHA1(input);
	}
}
