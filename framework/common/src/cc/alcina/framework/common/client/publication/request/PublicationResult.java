package cc.alcina.framework.common.client.publication.request;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class PublicationResult extends Model implements TreeSerializable {
	private String content;

	private Long publicationId;

	private String publicationUid;

	private String contentToken;

	private String contentUrl;

	public void ensureMinimal() {
		setContent(null);
	}

	public String getContent() {
		return content;
	}

	public String getContentToken() {
		return contentToken;
	}

	public String getContentUrl() {
		return contentUrl;
	}

	public Long getPublicationId() {
		return publicationId;
	}

	public String getPublicationUid() {
		return publicationUid;
	}

	/* Server-only */
	public void log() {
		Registry.impl(ResultLogger.class).log(this);
	}

	public void setContent(String content) {
		this.content = content;
	}

	public void setContentToken(String contentToken) {
		this.contentToken = contentToken;
	}

	public void setContentUrl(String contentUrl) {
		this.contentUrl = contentUrl;
	}

	public void setPublicationId(Long publicationId) {
		this.publicationId = publicationId;
	}

	public void setPublicationUid(String publicationUid) {
		this.publicationUid = publicationUid;
	}

	public interface ResultLogger {
		void log(PublicationResult publicationResult);
	}
}
