package cc.alcina.framework.common.client.util;

import java.io.Serializable;
import java.util.Objects;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.serializer.TreeSerializable;

@Reflected
@Bean(PropertySource.FIELDS)
public class ClassPair implements Serializable, TreeSerializable {
	public Class c1;

	public Class c2;

	public ClassPair() {
	}

	public ClassPair(Class c1, Class c2) {
		this.c1 = c1;
		this.c2 = c2;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ClassPair) {
			ClassPair o = (ClassPair) obj;
			return Objects.equals(c1, o.c1) && Objects.equals(c2, o.c2);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(c1, c2);
	}

	@Override
	public String toString() {
		return Ax.format("[%s,%s]", c1, c2);
	}
}
