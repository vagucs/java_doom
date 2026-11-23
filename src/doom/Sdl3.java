/**
 * DOOM generic portado de Harbour para Java CLI com SDL3.
 *
 * Por Wagner Nunes da Silva
 *
 * vagucs@bol.com.br
 * vagucs@vagucs.com.br
 * vagucs@gmail.com
 *
 * www.vagucs.com.br
 */
package doom;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

/** SDL3 via JNA. Bool de C é 1 byte: retorno e argumento usam {@code byte}. */
public interface Sdl3 extends Library
{
    byte SDL_Init(int flags);

    void SDL_Quit();

    String SDL_GetError();

    long SDL_GetTicks();

    void SDL_Delay(int ms);

    byte SDL_ShowCursor();

    byte SDL_HideCursor();

    byte SDL_SetWindowRelativeMouseMode(Pointer window, byte enabled);

    Pointer SDL_CreateWindow(String title, int w, int h, long flags);

    void SDL_DestroyWindow(Pointer window);

    byte SDL_SetWindowFullscreen(Pointer window, byte fullscreen);

    byte SDL_SetWindowSize(Pointer window, int w, int h);

    byte SDL_SetWindowPosition(Pointer window, int x, int y);

    byte SDL_SetWindowTitle(Pointer window, String title);

    Pointer SDL_CreateRenderer(Pointer window, String name);

    byte SDL_SetRenderVSync(Pointer renderer, int vsync);

    void SDL_DestroyRenderer(Pointer renderer);

    byte SDL_RenderClear(Pointer renderer);

    byte SDL_RenderTexture(Pointer renderer, Pointer texture, Pointer src, Pointer dst);

    byte SDL_RenderPresent(Pointer renderer);

    Pointer SDL_CreateTexture(Pointer renderer, int format, int access, int w, int h);

    byte SDL_SetTextureScaleMode(Pointer texture, int scaleMode);

    void SDL_DestroyTexture(Pointer texture);

    byte SDL_UpdateTexture(Pointer texture, Pointer rect, Pointer pixels, int pitch);

    byte SDL_PollEvent(Pointer event);

    Pointer SDL_OpenAudioDeviceStream(int devid, Pointer spec, Pointer callback, Pointer userdata);

    byte SDL_ResumeAudioStreamDevice(Pointer stream);

    byte SDL_PutAudioStreamData(Pointer stream, Pointer buf, int len);

    int SDL_GetAudioStreamQueued(Pointer stream);

    void SDL_DestroyAudioStream(Pointer stream);
}
