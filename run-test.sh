FULLSCREEN=no
PICTURES=tv-test-album
INTERVAL=1000
DB=photodb
LOG=run.log

CP=classes:lib/metadata-extractor-2.9.1/metadata-extractor-2.9.1.jar:lib/metadata-extractor-2.9.1/xmpcore-5.1.2.jar

java -Xmx256m -cp ${CP} SlideMill ${FULLSCREEN} ${PICTURES} ${INTERVAL} ${DB} ${LOG}
