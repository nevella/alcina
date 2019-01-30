Jboss-logmanager (component of wildfly) has a code issue which plays havoc with programatically setting log levels

By default, the string-logger resolution is backed by weak maps, which means that configuring loggers before use (or per-instance logger usage) ignores programmatic configuration.

Solution is attached patch and module jar
