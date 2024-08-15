realpath() {
    [[ $1 = /* ]] && echo "$1" || echo "$PWD/${1#./}"
}
SCRIPT=`realpath $0`
SCRIPT_PATH=`dirname $SCRIPT`
cd $SCRIPT_PATH
echo 'watching sass/*'
rm -rf css/aba
rm -rf css/dirndl
rm -rf css/jade
rm -rf css/ol
rm -rf css/theme
sass --no-source-map --watch sass:css
 
