Put SDL2 here so JNA can load it without a system install:

  Windows:  SDL2.dll
            https://github.com/libsdl-org/SDL/releases  (SDL2-devel-*-VC.zip, x64)
            The PHP port's php_doom/lib/SDL2.dll or node_doom/lib/SDL2.dll can be copied here.

  Linux:    libSDL2.so.0  (or: sudo apt install libsdl2-2.0-0)
  macOS:    libSDL2.dylib (or: brew install sdl2)

You can also set SDL2_PATH to the full path of the library.

JDK 17+ is required (java + javac). JNA is lib/jna-5.17.0.jar (run.bat downloads it if missing).
