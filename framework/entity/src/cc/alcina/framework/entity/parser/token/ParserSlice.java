package cc.alcina.framework.entity.parser.token;

public interface ParserSlice<T extends ParserToken> {
	public T getToken();
}
