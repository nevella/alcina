package cc.alcina.framework.common.client.search;

public class PaginationRange {
	public int skip = 0;

	public int limit = 50;

	public PaginationRange() {
	}

	public PaginationRange(int skip, int limit) {
		this.skip = skip;
		this.limit = limit;
	}
}