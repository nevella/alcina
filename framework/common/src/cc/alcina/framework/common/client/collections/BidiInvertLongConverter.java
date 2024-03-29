package cc.alcina.framework.common.client.collections;

public class BidiInvertLongConverter implements BidiConverter<Long, Long> {
	@Override
	public Long leftToRight(Long a) {
		return -a;
	}

	@Override
	public Long rightToLeft(Long b) {
		return -b;
	}
}