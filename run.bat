@echo off
setlocal
cd /d "%~dp0"

where java >nul 2>nul
if errorlevel 1 (
    echo Java nao encontrado. Instale o JDK 17+ e reabra o terminal.
    exit /b 1
)

where javac >nul 2>nul
if errorlevel 1 (
    echo javac nao encontrado. Precisa do JDK 17, nao so do JRE.
    exit /b 1
)

if not exist "lib\jna-5.17.0.jar" (
    echo Baixando JNA...
    curl -L -o "lib\jna-5.17.0.jar" "https://repo1.maven.org/maven2/net/java/dev/jna/jna/5.17.0/jna-5.17.0.jar"
    if errorlevel 1 exit /b 1
)

if not exist "sdl3\SDL3-3.4.18-win32-x64\SDL3.dll" if not exist "lib\SDL3.dll" if not defined SDL3_PATH (
    echo Aviso: falta SDL3.dll em sdl3\SDL3-3.4.18-win32-x64\ ou em lib\
)

if not exist "out\doom\Doom.class" (
    echo Compilando...
    if not exist out mkdir out
    javac -encoding UTF-8 -cp "lib\jna-5.17.0.jar" -d out src\doom\*.java
    if errorlevel 1 exit /b 1
)

set "IWAD_ARGS=%*"
echo %* | findstr /i /c:"-iwad" >nul
if errorlevel 1 (
    if exist "DOOM1.WAD" set "IWAD_ARGS=-iwad DOOM1.WAD %*"
    if exist "..\DOOM1.WAD" set "IWAD_ARGS=-iwad ..\DOOM1.WAD %*"
)

java -cp "out;lib\jna-5.17.0.jar" doom.Doom %IWAD_ARGS%
exit /b %ERRORLEVEL%
