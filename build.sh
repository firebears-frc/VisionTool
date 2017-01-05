./gradlew distZip
zip build/distributions/VisionTool.zip VisionTool/lib/bridj-0.7.0.jar
mkdir -p tmp/
cd tmp/
unzip ../build/distributions/VisionTool.zip
./VisionTool/bin/VisionTool
