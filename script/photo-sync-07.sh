REMOTE=pi@10.0.1.199
APP=Applications/slidemill3
BASE=Pictures/tv-test-album
LOCALBASE=~/Documents/slidemill/tv-test-album
WORK=work

K_1NEW=1new
K_2POOL=2pool
K_3DONE=3done

mkdir -p ${WORK}

echo 'Comparing pictures'
ssh ${REMOTE} "ls ${BASE}/${K_1NEW} ; ls ${BASE}/${K_2POOL} ; ls ${BASE}/${K_3DONE}" | sort > ${WORK}/remote.txt

ls ${LOCALBASE} | sort > ${WORK}/local.txt

diff -u ${WORK}/remote.txt ${WORK}/local.txt | grep -v "^---" | grep -v "^+++" | grep -v "^@@" | grep "^+" | cut -c 2- > ${WORK}/local_only.txt
diff -u ${WORK}/remote.txt ${WORK}/local.txt | grep -v "^---" | grep -v "^+++" | grep -v "^@@" | grep "^-" | cut -c 2- > ${WORK}/remote_only.txt

# Copy local_only files to remote 1new directory.
count=`cat ${WORK}/local_only.txt | wc -l`
if [ ${count} -gt 0 ]
then
  #echo Found ${count} new pictures.
  index=1
  while IFS= read -r fs; do
    echo "Adding ${index} of ${count}: $fs"
    # Copy file to 1new.
    scp -q "${LOCALBASE}/${fs}" "${REMOTE}:${BASE}/${K_1NEW}"
    let "index++"
  done < ${WORK}/local_only.txt
fi

# Remove remote_only files.
count=`cat ${WORK}/remote_only.txt | wc -l`
if [ ${count} -gt 0 ]
then
  #echo Found ${count} pictures to remove.
  index=1
  while IFS= read -r fs; do
    echo "Removing ${index} of ${count}: $fs"
    ssh -n ${REMOTE} "rm \"\`find -name \"${fs}\"\`\""
    let "index++"
  done < ${WORK}/remote_only.txt
fi

echo 'All done!'
