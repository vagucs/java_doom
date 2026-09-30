/**
 * DOOM generic portado de Harbour para Java CLI com SDL2.
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

/** SDL2 via JNA (same surface as PHP FFI / Node koffi). */
public interface Sdl2 extends Library
{
    int SDL_Init(int flags);

    void SDL_Quit();

    String SDL_GetError();

    int SDL_GetTicks();

    void SDL_Delay(int ms);

    int SDL_ShowCursor(int toggle);

    Pointer SDL_CreateWindow(String title, int x, int y, int w, int h, int flags);

    void SDL_DestroyWindow(Pointer window);

    int SDL_SetWindowFullscreen(Pointer window, int flags);

    void SDL_SetWindowSize(Pointer window, int w, int h);

    void SDL_SetWindowPosition(Pointer window, int x, int y);

    void SDL_SetWindowTitle(Pointer window, String title);

    Pointer SDL_CreateRenderer(Pointer window, int index, int flags);

    void SDL_DestroyRenderer(Pointer renderer);

    int SDL_RenderClear(Pointer renderer);

    int SDL_RenderCopy(Pointer renderer, Pointer texture, Pointer src, Pointer dst);

    void SDL_RenderPresent(Pointer renderer);

    Pointer SDL_CreateTexture(Pointer renderer, int format, int access, int w, int h);

    void SDL_DestroyTexture(Pointer texture);

    int SDL_UpdateTexture(Pointer texture, Pointer rect, Pointer pixels, int pitch);

    int SDL_PollEvent(Pointer event);

    int SDL_OpenAudioDevice(String device, int iscapture, Pointer desired, Pointer obtained, int allowed);

    void SDL_PauseAudioDevice(int dev, int pause);

    int SDL_QueueAudio(int dev, Pointer data, int len);

    int SDL_GetQueuedAudioSize(int dev);

    void SDL_CloseAudioDevice(int dev);
}
