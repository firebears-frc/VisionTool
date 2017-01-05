@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  VisionTool startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Add default JVM options here. You can also use JAVA_OPTS and VISION_TOOL_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
@rem Get command-line arguments, handling Windows variants

if not "%OS%" == "Windows_NT" goto win9xME_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\VisionTool.jar;%APP_HOME%\lib\slf4j-api-1.7.21.jar;%APP_HOME%\lib\all-0.26.jar;%APP_HOME%\lib\georegression-0.12.jar;%APP_HOME%\lib\calibration-0.26.jar;%APP_HOME%\lib\feature-0.26.jar;%APP_HOME%\lib\geo-0.26.jar;%APP_HOME%\lib\io-0.26.jar;%APP_HOME%\lib\ip-0.26.jar;%APP_HOME%\lib\learning-0.26.jar;%APP_HOME%\lib\recognition-0.26.jar;%APP_HOME%\lib\sfm-0.26.jar;%APP_HOME%\lib\visualize-0.26.jar;%APP_HOME%\lib\android-0.26.jar;%APP_HOME%\lib\openkinect-0.26.jar;%APP_HOME%\lib\jcodec-0.26.jar;%APP_HOME%\lib\WebcamCapture-0.26.jar;%APP_HOME%\lib\javacv-0.26.jar;%APP_HOME%\lib\ddogleg-0.10.jar;%APP_HOME%\lib\xstream-1.4.7.jar;%APP_HOME%\lib\snakeyaml-1.17.jar;%APP_HOME%\lib\main-0.3.jar;%APP_HOME%\lib\models-0.3.jar;%APP_HOME%\lib\jna-3.5.2.jar;%APP_HOME%\lib\platform-3.5.2.jar;%APP_HOME%\lib\jcodec-0.1.9.jar;%APP_HOME%\lib\webcam-capture-0.3.11.jar;%APP_HOME%\lib\javacv-1.1.jar;%APP_HOME%\lib\opencv-3.0.0-1.1-linux-x86_64.jar;%APP_HOME%\lib\opencv-3.0.0-1.1-macosx-x86_64.jar;%APP_HOME%\lib\opencv-3.0.0-1.1-windows-x86_64.jar;%APP_HOME%\lib\opencv-3.0.0-1.1.jar;%APP_HOME%\lib\ffmpeg-2.8.1-1.1-linux-x86_64.jar;%APP_HOME%\lib\ffmpeg-2.8.1-1.1-macosx-x86_64.jar;%APP_HOME%\lib\ffmpeg-2.8.1-1.1-windows-x86_64.jar;%APP_HOME%\lib\ffmpeg-2.8.1-1.1.jar;%APP_HOME%\lib\core-0.30.jar;%APP_HOME%\lib\dense64-0.30.jar;%APP_HOME%\lib\simple-0.30.jar;%APP_HOME%\lib\equation-0.30.jar;%APP_HOME%\lib\xmlpull-1.1.3.1.jar;%APP_HOME%\lib\xpp3_min-1.1.4c.jar;%APP_HOME%\lib\learning-0.3.jar;%APP_HOME%\lib\io-0.3.jar;%APP_HOME%\lib\bridj-0.7.0.jar;%APP_HOME%\lib\javacpp-1.1.jar;%APP_HOME%\lib\flycapture-2.8.3.1-1.1.jar;%APP_HOME%\lib\libdc1394-2.2.3-1.1.jar;%APP_HOME%\lib\libfreenect-0.5.3-1.1.jar;%APP_HOME%\lib\videoinput-0.200-1.1.jar;%APP_HOME%\lib\artoolkitplus-2.3.1-1.1.jar;%APP_HOME%\lib\flandmark-1.07-1.1.jar;%APP_HOME%\lib\protobuf-java-2.6.1.jar;%APP_HOME%\lib\jarchivelib-0.5.0.jar;%APP_HOME%\lib\zip4j-1.3.2.jar;%APP_HOME%\lib\commons-compress-1.7.jar;%APP_HOME%\lib\xz-1.4.jar

@rem Execute VisionTool
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %VISION_TOOL_OPTS%  -classpath "%CLASSPATH%" org.firebears.visiontool.VisionTool %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable VISION_TOOL_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%VISION_TOOL_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
