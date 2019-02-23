package cc.alcina.framework.common.client.logic.domaintransform;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

public class DomainUpdate implements Serializable {
    public List<DomainTransformRequest> requests;

    public DomainTransformCommitPosition commitPosition;

    public static class DomainTransformCommitPosition implements Serializable {
        private Long lastRequestId;

        public Long firstRequestId;

        public int size;

        public DomainTransformCommitPosition() {
        }

        public DomainTransformCommitPosition(Long firstRequestId, int size,
                Long lastRequestId) {
            this.firstRequestId = firstRequestId;
            this.size = size;
            this.lastRequestId = lastRequestId;
        }

        public boolean after(DomainTransformCommitPosition other) {
            if (other.firstRequestId == null) {
                return firstRequestId != null;
            }
            if (Objects.equals(other.firstRequestId, firstRequestId)) {
                return size > other.size;
            } else {
                return true;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof DomainTransformCommitPosition) {
                DomainTransformCommitPosition o = (DomainTransformCommitPosition) obj;
                return CommonUtils.equals(firstRequestId, o.firstRequestId,
                        size, o.size);
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return Ax.format("commit position: %s/%s/%s", firstRequestId, size,
                    lastRequestId);
        }
    }

    @RegistryLocation(registryPoint = DomainTransformCommitPositionProvider.class, implementationType = ImplementationType.SINGLETON)
    @ClientInstantiable
    public static class DomainTransformCommitPositionProvider {
        public DomainTransformCommitPosition getPosition() {
            return null;
        }
    }
}
