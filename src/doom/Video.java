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

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** SDL2 video + audio (doomgeneric / i_video) via JNA. */
public final class Video
{
    public static final int SCALE_MIN = 1;
    public static final int SCALE_MAX = 6;
    private static final int SDL_INIT_AUDIO = 0x00000010;
    private static final int SDL_INIT_VIDEO = 0x00000020;
    private static final int SDL_INIT_EVENTS = 0x00004000;
    private static final int SDL_WINDOWPOS_CENTERED = 0x2fff0000;
    private static final int SDL_WINDOW_SHOWN = 0x00000004;
    private static final int SDL_WINDOW_RESIZABLE = 0x00000020;
    private static final int SDL_WINDOW_FULLSCREEN_DESKTOP = 0x00001001;
    private static final int SDL_RENDERER_ACCELERATED = 0x00000002;
    private static final int SDL_RENDERER_PRESENTVSYNC = 0x00000004;
    private static final int SDL_PIXELFORMAT_ARGB8888 = 0x16362004;
    private static final int SDL_TEXTUREACCESS_STREAMING = 1;
    private static final int SDL_QUIT = 0x100;
    private static final int SDL_KEYDOWN = 0x300;
    private static final int SDL_KEYUP = 0x301;
    private static final int SDL_MOUSEMOTION = 0x400;
    private static final int SDL_MOUSEBUTTONDOWN = 0x401;
    private static final int SDL_MOUSEBUTTONUP = 0x402;
    private static final int AUDIO_S16LSB = 0x8010;

    public final int[] fb = new int[Defs.SCREENWIDTH * Defs.SCREENHEIGHT];
    public boolean fullscreen;
    public int scale = 2;
    public boolean showFps;
    public boolean crt;
    public String windowTitle = "DOOM";
    public int fpsValue;

    private Sdl2 sdl;
    private Pointer window;
    private Pointer renderer;
    private Pointer texture;
    private int audioDev;
    private final int[][] palette = new int[256][3];
    private final byte[] argbBytes = new byte[Defs.SCREENWIDTH * Defs.SCREENHEIGHT * 4];
    private final Memory argb = new Memory(Defs.SCREENWIDTH * Defs.SCREENHEIGHT * 4L);
    private final Memory eventBuf = new Memory(64);
    private final Memory audioSpec = new Memory(32);
    private final Memory audioHave = new Memory(32);
    private int fpsFrames;
    private int fpsStamp;
    private boolean mouseGrab;
    private final List<Integer> mix = new ArrayList<>();

    private static String libraryPath()
    {
        String env = System.getenv("SDL2_PATH");
        if (env != null && !env.isEmpty() && Files.isRegularFile(Path.of(env))) {
            return env;
        }
        Path here = Path.of("").toAbsolutePath();
        Path[] candidates = {
            here.resolve("lib").resolve("SDL2.dll"),
            here.resolve("SDL2.dll"),
            here.resolve("lib").resolve("libSDL2.so"),
            here.resolve("lib").resolve("libSDL2.so.0"),
            here.resolve("lib").resolve("libSDL2.dylib"),
            here.resolve("..").resolve("php_doom").resolve("lib").resolve("SDL2.dll"),
            here.resolve("..").resolve("node_doom").resolve("lib").resolve("SDL2.dll"),
        };
        for (Path p : candidates) {
            if (Files.isRegularFile(p)) {
                return p.toAbsolutePath().normalize().toString();
            }
        }
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return "SDL2";
        }
        if (os.contains("mac")) {
            return "SDL2";
        }
        return "SDL2";
    }

    public void init(boolean fullscreen, String title)
    {
        java.util.Arrays.fill(fb, 0);
        this.fullscreen = fullscreen;
        this.windowTitle = title;
        this.sdl = Native.load(libraryPath(), Sdl2.class);
        int flags = SDL_INIT_VIDEO | SDL_INIT_EVENTS | SDL_INIT_AUDIO;
        if (this.sdl.SDL_Init(flags) != 0) {
            throw new RuntimeException("SDL_Init: " + error());
        }
        this.sdl.SDL_ShowCursor(0);
        applyMode();
        openAudio();
        fpsStamp = ticksMs();
    }

    public int ticksMs()
    {
        return sdl != null ? sdl.SDL_GetTicks() : (int) System.currentTimeMillis();
    }

    public void delay(int ms)
    {
        if (sdl != null) {
            sdl.SDL_Delay(ms);
        }
    }

    public void setRelativeMouse(boolean enabled)
    {
        if (sdl == null || enabled == mouseGrab) {
            return;
        }
        mouseGrab = enabled;
        sdl.SDL_SetRelativeMouseMode(enabled ? 1 : 0);
    }

    public void toggleFullscreen()
    {
        fullscreen = !fullscreen;
        applyMode();
    }

    public boolean changeScale(int delta)
    {
        if (fullscreen) {
            return false;
        }
        int next = Math.max(SCALE_MIN, Math.min(SCALE_MAX, scale + delta));
        if (next == scale) {
            return false;
        }
        scale = next;
        applyMode();
        return true;
    }

    public void setPalette(byte[] playpal)
    {
        setPaletteRaw(java.util.Arrays.copyOf(playpal, 768));
    }

    public void setPaletteRaw(byte[] rgb768)
    {
        for (int i = 0; i < 256; i++) {
            palette[i][0] = rgb768[i * 3] & 0xff;
            palette[i][1] = rgb768[i * 3 + 1] & 0xff;
            palette[i][2] = rgb768[i * 3 + 2] & 0xff;
        }
    }

    public void playSfx(byte[] pcm, int volume)
    {
        if (pcm == null || pcm.length == 0 || sdl == null || audioDev == 0) {
            return;
        }
        double gain = Math.max(0, Math.min(15, volume)) / 15.0;
        int n = pcm.length / 2;
        while (mix.size() < n) {
            mix.add(0);
        }
        for (int i = 0; i < n; i++) {
            int s = (pcm[i * 2] & 0xff) | ((pcm[i * 2 + 1] & 0xff) << 8);
            if (s >= 0x8000) {
                s -= 0x10000;
            }
            int mixed = mix.get(i) + (int) (s * gain);
            mix.set(i, Math.max(-32768, Math.min(32767, mixed)));
        }
    }

    public void present()
    {
        if (sdl == null || renderer == null || texture == null) {
            return;
        }
        pumpAudio();
        int n = Defs.SCREENWIDTH * Defs.SCREENHEIGHT;
        for (int i = 0; i < n; i++) {
            int[] rgb = palette[fb[i] & 0xff];
            int r = rgb[0];
            int g = rgb[1];
            int b = rgb[2];
            if (crt) {
                int scan = ((i / Defs.SCREENWIDTH) & 1) != 0 ? 180 : 256;
                r = Math.min(255, (r * scan) / 256);
                g = Math.min(255, (g * scan) / 256);
                b = Math.min(255, (b * scan) / 256);
            }
            int o = i * 4;
            argbBytes[o] = (byte) b;
            argbBytes[o + 1] = (byte) g;
            argbBytes[o + 2] = (byte) r;
            argbBytes[o + 3] = (byte) 255;
        }
        argb.write(0, argbBytes, 0, argbBytes.length);
        sdl.SDL_UpdateTexture(texture, null, argb, Defs.SCREENWIDTH * 4);
        sdl.SDL_RenderClear(renderer);
        sdl.SDL_RenderCopy(renderer, texture, null, null);
        sdl.SDL_RenderPresent(renderer);
        fpsFrames++;
        int now = ticksMs();
        if (now - fpsStamp >= 1000) {
            fpsValue = fpsFrames;
            fpsFrames = 0;
            fpsStamp = now;
        }
        if (showFps) {
            sdl.SDL_SetWindowTitle(window, windowTitle + " - " + fpsValue + " FPS");
        }
    }

    public List<GameEvent> pollEvents()
    {
        List<GameEvent> out = new ArrayList<>();
        if (sdl == null) {
            return out;
        }
        while (sdl.SDL_PollEvent(eventBuf) != 0) {
            int type = eventBuf.getInt(0);
            if (type == SDL_QUIT) {
                out.add(new GameEvent("quit"));
                continue;
            }
            if (type == SDL_MOUSEMOTION) {
                GameEvent motion = new GameEvent("mousemotion");
                motion.dx = eventBuf.getInt(28);
                motion.dy = eventBuf.getInt(32);
                out.add(motion);
                continue;
            }
            if (type == SDL_MOUSEBUTTONDOWN || type == SDL_MOUSEBUTTONUP) {
                GameEvent button = new GameEvent(type == SDL_MOUSEBUTTONDOWN ? "mousedown" : "mouseup");
                button.button = eventBuf.getByte(16) & 0xff;
                out.add(button);
                continue;
            }
            if (type != SDL_KEYDOWN && type != SDL_KEYUP) {
                continue;
            }
            boolean repeat = eventBuf.getByte(13) != 0;
            if (repeat && type == SDL_KEYDOWN) {
                continue;
            }
            int scan = eventBuf.getInt(16);
            int sym = eventBuf.getInt(20);
            if (scan == 45 || scan == 86) {
                sym = scan == 86 ? Keys.KP_MINUS : Keys.MINUS;
            } else if (scan == 46 || scan == 87) {
                sym = scan == 87 ? Keys.KP_PLUS : Keys.EQUALS;
            } else if (sym == Keys.PLUS) {
                sym = Keys.EQUALS;
            }
            int mod = eventBuf.getShort(24) & 0xffff;
            GameEvent ev = new GameEvent(type == SDL_KEYDOWN ? "keydown" : "keyup");
            ev.key = sym;
            ev.sym = sym;
            ev.repeat = repeat;
            ev.mod = mod;
            ev.text = (sym >= 32 && sym < 127) ? String.valueOf((char) sym) : "";
            out.add(ev);
        }
        return out;
    }

    public void shutdown()
    {
        if (sdl == null) {
            return;
        }
        if (audioDev != 0) {
            sdl.SDL_CloseAudioDevice(audioDev);
            audioDev = 0;
        }
        if (texture != null) {
            sdl.SDL_DestroyTexture(texture);
            texture = null;
        }
        if (renderer != null) {
            sdl.SDL_DestroyRenderer(renderer);
            renderer = null;
        }
        if (window != null) {
            sdl.SDL_DestroyWindow(window);
            window = null;
        }
        sdl.SDL_Quit();
        sdl = null;
    }

    private void applyMode()
    {
        if (sdl == null) {
            return;
        }
        int w = Defs.SCREENWIDTH * scale;
        int h = Defs.SCREENHEIGHT * scale;
        if (window == null) {
            window = sdl.SDL_CreateWindow(
                windowTitle,
                SDL_WINDOWPOS_CENTERED,
                SDL_WINDOWPOS_CENTERED,
                w,
                h,
                SDL_WINDOW_SHOWN | SDL_WINDOW_RESIZABLE
            );
            if (window == null) {
                throw new RuntimeException("SDL_CreateWindow: " + error());
            }
            renderer = sdl.SDL_CreateRenderer(window, -1, SDL_RENDERER_ACCELERATED | SDL_RENDERER_PRESENTVSYNC);
            if (renderer == null) {
                renderer = sdl.SDL_CreateRenderer(window, -1, 0);
            }
            if (renderer == null) {
                throw new RuntimeException("SDL_CreateRenderer: " + error());
            }
            texture = sdl.SDL_CreateTexture(renderer, SDL_PIXELFORMAT_ARGB8888, SDL_TEXTUREACCESS_STREAMING, Defs.SCREENWIDTH, Defs.SCREENHEIGHT);
            if (texture == null) {
                throw new RuntimeException("SDL_CreateTexture: " + error());
            }
        }
        sdl.SDL_SetWindowFullscreen(window, fullscreen ? SDL_WINDOW_FULLSCREEN_DESKTOP : 0);
        if (!fullscreen) {
            sdl.SDL_SetWindowSize(window, w, h);
            sdl.SDL_SetWindowPosition(window, SDL_WINDOWPOS_CENTERED, SDL_WINDOWPOS_CENTERED);
        }
        sdl.SDL_SetWindowTitle(window, windowTitle);
    }

    private void openAudio()
    {
        if (sdl == null) {
            return;
        }
        try {
            audioSpec.clear();
            audioSpec.setInt(0, 11025);
            audioSpec.setShort(4, (short) AUDIO_S16LSB);
            audioSpec.setByte(6, (byte) 1);
            audioSpec.setShort(8, (short) 512);
            audioHave.clear();
            int dev = sdl.SDL_OpenAudioDevice(null, 0, audioSpec, audioHave, 0);
            if (dev != 0) {
                audioDev = dev;
                sdl.SDL_PauseAudioDevice(audioDev, 0);
            }
        } catch (Exception e) {
            audioDev = 0;
        }
    }

    private void pumpAudio()
    {
        if (sdl == null || audioDev == 0 || mix.isEmpty()) {
            return;
        }
        int queued = sdl.SDL_GetQueuedAudioSize(audioDev);
        if (queued > 11025 * 2) {
            return;
        }
        int n = Math.min(mix.size(), 2048);
        byte[] pcm = new byte[n * 2];
        for (int i = 0; i < n; i++) {
            int sample = mix.remove(0) & 0xffff;
            pcm[i * 2] = (byte) sample;
            pcm[i * 2 + 1] = (byte) (sample >> 8);
        }
        Memory buf = new Memory(pcm.length);
        buf.write(0, pcm, 0, pcm.length);
        sdl.SDL_QueueAudio(audioDev, buf, pcm.length);
    }

    private String error()
    {
        try {
            return sdl != null ? String.valueOf(sdl.SDL_GetError()) : "unknown SDL error";
        } catch (Exception e) {
            return "unknown SDL error";
        }
    }
}
