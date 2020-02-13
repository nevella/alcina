package cc.alcina.framework.common.client.logic.domaintransform;

import java.io.Serializable;

import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

/**
 * Important! Don't allow a signature part to contain a comma - that's the
 * delimiter character
 * 
 * @author nick@alcina.cc
 *
 */
public class DomainModelDeltaSignature implements Serializable {
	public static DomainModelDeltaSignature parseSignature(String text) {
		if (text == null || !text.startsWith("ds:")) {
			return null;
		}
		try {
			String[] szs = text.substring(3).split(",");
			return new DomainModelDeltaSignature().classSimpleName(szs[0])
					.id(Long.parseLong(szs[1])).sid(szs[2])
					.userId(Long.parseLong(szs[3])).contentHash(szs[4])
					.rpcSignature(szs[5]).contentLength(Long.parseLong(szs[6]));
		} catch (Exception e) {
			return null;
		}
	}

	private String classSimpleName = "";

	private long id;

	private long userId;

	private String sid = "0";

	private String contentHash = "";

	private String rpcSignature = "";

	private long contentLength;

	private transient boolean requiresHash;

	public DomainModelDeltaSignature() {
		userId = PermissionsManager.get().getUserId();
	}

	public DomainModelDeltaSignature checkValidUser() {
		return userId == PermissionsManager.get().getUserId() ? this : null;
	}

	public DomainModelDeltaSignature classSimpleName(String classSimpleName) {
		this.classSimpleName = classSimpleName;
		return this;
	}

	public DomainModelDeltaSignature clazz(Class clazz) {
		return classSimpleName(CommonUtils.simpleClassName(clazz));
	}

	public DomainModelDeltaSignature contentHash(String contentHash) {
		this.contentHash = contentHash;
		return this;
	}

	public DomainModelDeltaSignature contentLength(long contentLength) {
		this.contentLength = contentLength;
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DomainModelDeltaSignature) {
			return toString().equals(obj.toString());
		}
		return false;
	}

	public String getClassSimpleName() {
		return this.classSimpleName;
	}

	public String getContentHash() {
		return this.contentHash;
	}

	public long getContentLength() {
		return this.contentLength;
	}

	public long getId() {
		return this.id;
	}

	public String getRpcSignature() {
		return this.rpcSignature;
	}

	public String getSid() {
		return this.sid;
	}

	public long getUserId() {
		return this.userId;
	}

	public int hashCode() {
		return toString().hashCode();
	}

	public DomainModelDeltaSignature id(long id) {
		this.id = id;
		return this;
	}

	public String nonVersionedSignature() {
		return Ax.format("%s,%s,%s", classSimpleName, id, sid);
	}

	public DomainModelDeltaSignature nonVersionedSignatureObject() {
		DomainModelDeltaSignature sig = new DomainModelDeltaSignature();
		sig.classSimpleName = classSimpleName;
		sig.id = id;
		sig.sid = sid;
		return sig;
	}

	public boolean provideRequiresHash() {
		return this.requiresHash;
	}

	public DomainModelDeltaSignature requiresHash() {
		requiresHash = true;
		return this;
	}

	public DomainModelDeltaSignature rpcSignature(String rpcSignature) {
		this.rpcSignature = rpcSignature;
		return this;
	}

	public void setClassSimpleName(String classSimpleName) {
		this.classSimpleName = classSimpleName;
	}

	public void setContentHash(String contentHash) {
		this.contentHash = contentHash;
	}

	public void setContentLength(long contentLength) {
		this.contentLength = contentLength;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setRpcSignature(String rpcSignature) {
		this.rpcSignature = rpcSignature;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public DomainModelDeltaSignature sid(String sid) {
		this.sid = sid;
		return this;
	}

	@Override
	public String toString() {
		return Ax.format("ds:%s,%s,%s,%s,%s,%s,%s", classSimpleName,
				id, sid, userId, contentHash, rpcSignature, contentLength);
	}

	public DomainModelDeltaSignature userId(long userId) {
		this.userId = userId;
		return this;
	}
}
