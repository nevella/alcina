#Generates custom gwt-user.jar with custom serialization (async) classes

#(in mac ui)

cd /tmp
rm -rf gwittir-trunk.jar
mkdir gwittir-trunk
cd gwittir-trunk
cp /var/local/git/alcina/lib/framework/gwt/gwittir-trunk.jar .
unzip -o gwittir-trunk.jar
rm gwittir-trunk.jar
cp /var/local/git/alcina/bin/com/totsp/gwittir/client/beans/Converter*.* com/totsp/gwittir/client/beans
cp /var/local/git/alcina/framework/common/src/com/totsp/gwittir/client/beans/Converter*.* com/totsp/gwittir/client/beans
cp /var/local/git/alcina/bin/com/totsp/gwittir/client/ui/Converter*.* com/totsp/gwittir/client/ui
cp /var/local/git/alcina/framework/common/src/com/totsp/gwittir/client/ui/Converter*.* com/totsp/gwittir/client/ui
 

cd gwt-user
zip -r  ../gwittir-trunk.jar *
cd ..
rm -rf gwittir-trunk
