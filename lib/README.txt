SDL3 is loaded from the tree, not from lib/:

  Windows:  sdl3/SDL3-3.4.18-win32-x64/SDL3.dll
            (32-bit: sdl3/SDL3-3.4.18-win32-x86/SDL3.dll)
            A copy in lib/SDL3.dll is also accepted.

  Linux:    lib/libSDL3.so.0
  macOS:    lib/libSDL3.dylib

Set SDL3_PATH to the full path of the library to override the search.

JDK 17+ is required (java + javac). JNA is lib/jna-5.17.0.jar (run.bat downloads it if missing).
