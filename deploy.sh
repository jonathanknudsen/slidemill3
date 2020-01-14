REMOTE=pi@10.0.1.199
APP=Applications/slidemill3
echo 'Creating directory'
ssh ${REMOTE} "mkdir -p ${APP}"
echo 'Copying files'
scp -q -r run.sh classes lib ${REMOTE}:${APP}
echo 'Done'
