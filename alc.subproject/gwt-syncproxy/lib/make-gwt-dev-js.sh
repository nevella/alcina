set -x;

mkdir -p /tmp/js/ && cd /tmp/js
#rsync -a /apdm/git/alcina/lib/framework/gwt/gwt-dev.jar gwt-dev
cd gwt-dev
#unzip -o gwt-dev.jar > /dev/null

rm -rf ../gwt-dev-js.jar

mkdir -p gwt-dev-js/com/google/gwt/dev || exit 1
mkdir -p gwt-dev-js/com/google/gwt/thirdparty || exit 1
mkdir -p gwt-dev-js/com/google/gwt/core || exit 1
cd /tmp/js/gwt-dev/gwt-dev-js/com/google/gwt/dev
rsync -a /tmp/js/gwt-dev/gwt-dev/com/google/gwt/dev/js .
rsync -a /tmp/js/gwt-dev/gwt-dev/com/google/gwt/dev/jjs .
rsync -a /tmp/js/gwt-dev/gwt-dev/com/google/gwt/dev/util .
cd /tmp/js/gwt-dev/gwt-dev-js/com/google/gwt/thirdparty
rsync -a /tmp/js/gwt-dev/gwt-dev/com/google/gwt/thirdparty/guava .
cd /tmp/js/gwt-dev/gwt-dev-js/com/google/gwt/core
rsync -a /tmp/js/gwt-dev/gwt-dev/com/google/gwt/core/ext .

cd /tmp/js/gwt-dev/gwt-dev-js
zip -r  ../gwt-dev-js.jar * > /dev/null
cd ..
cp gwt-dev-js.jar /private/var/local/git/mobility_lab/Software/alcina/thirdparty/gwt-syncproxy/lib
cp gwt-dev-js.jar /private/var/local/git/mobility_lab/Software/plugins/cc.alcina.rcp
set +x;
