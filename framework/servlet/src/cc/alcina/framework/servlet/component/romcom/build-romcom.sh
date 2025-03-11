realpath() {
    [[ $1 = /* ]] && echo "$1" || echo "$PWD/${1#./}"
}

SCRIPT=`realpath $0`
SCRIPT_PATH=`dirname $SCRIPT`
cd $SCRIPT_PATH

echo 'gwt.codestyle=pretty' >> local.ant.properties
ant
echo 'gwt.codestyle=obf' >> local.ant.properties
ant