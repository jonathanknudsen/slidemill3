FULLSCREEN=no
PICTURES=tv-test-album
INTERVAL=1000

java -Xmx256m -cp classes:lib/metadata-extractor-2.9.1/metadata-extractor-2.9.1.jar:lib/metadata-extractor-2.9.1/xmpcore-5.1.2.jar SlideMill ${FULLSCREEN} ${PICTURES} ${INTERVAL}
