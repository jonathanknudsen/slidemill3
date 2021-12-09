REMOTE=pi@10.0.1.197
APP=Applications/slidemill3
echo 'Shutting down SlideMill'
ssh ${REMOTE} "pkill java"
echo 'Creating directory'
ssh ${REMOTE} "mkdir -p ${APP}"
echo 'Copying files'
scp -q -r run.sh classes lib ${REMOTE}:${APP}
echo 'Starting SlideMill'
ssh ${REMOTE} "./start-slidemill.sh"
echo 'Done'
