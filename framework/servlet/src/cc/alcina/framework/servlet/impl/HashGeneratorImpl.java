package cc.alcina.framework.servlet.impl;

import cc.alcina.framework.common.client.csobjects.UrlRequest.HashGenerator;
import cc.alcina.framework.entity.EncryptionUtils;

public class HashGeneratorImpl extends HashGenerator {
	@Override
	public String hash(String input) {
		return EncryptionUtils.get().SHA1(input);
	}
}
