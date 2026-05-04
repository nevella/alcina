realpath() {
    [[ $1 = /* ]] && echo "$1" || echo "$PWD/${1#./}"
}

SCRIPT=`realpath $0`
SCRIPT_PATH=`dirname $SCRIPT`
cd $SCRIPT_PATH

# kill existing scss screens
screen -ls | grep Detached | grep sass | cut -d. -f1 | awk '{print $1}' | xargs kill
#

clear && printf '\e[3J'

#screen -S sass -d -m "$SCRIPT_PATH/watch-sass.sh"
# just run direct, no screen
$SCRIPT_PATH/watch-sass.sh

#echo "sass watcher started on $SCRIPT_PATH"