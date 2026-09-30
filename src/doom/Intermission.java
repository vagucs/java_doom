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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

final class WiAnim
{
    public int type;
    public int period;
    public int frames;
    public int x;
    public int y;
    public int data;
    public byte[][] patches;
    public int current = -1;
    public int nextTic;

    WiAnim(int type, int period, int frames, int x, int y, int data, byte[][] patches)
    {
        this.type = type;
        this.period = period;
        this.frames = frames;
        this.x = x;
        this.y = y;
        this.data = data;
        this.patches = patches;
    }
}

public final class Intermission
{
    public static final int NO_STATE = -1;
    public static final int STAT_COUNT = 0;
    public static final int SHOW_NEXT = 1;
    private static final int ALWAYS = 0;
    private static final int LEVEL = 2;
    private static final int[][] PARS = {
        { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
        { 0, 30, 75, 120, 90, 165, 180, 180, 30, 165 },
        { 0, 90, 90, 90, 120, 90, 360, 240, 30, 170 },
        { 0, 90, 45, 90, 150, 90, 90, 165, 30, 135 },
    };
    private static final int[] CPARS = {
        30, 90, 120, 120, 90, 150, 120, 120, 270, 90, 210, 150, 150, 150, 210, 150, 420, 150, 210, 150,
        240, 150, 180, 150, 150, 300, 330, 420, 300, 180, 120, 30,
    };
    private static final int[][][] NODES = {
        { { 185, 164 }, { 148, 143 }, { 69, 122 }, { 209, 102 }, { 116, 89 }, { 166, 55 }, { 71, 56 }, { 135, 29 }, { 71, 24 } },
        { { 254, 25 }, { 97, 50 }, { 188, 64 }, { 128, 78 }, { 214, 92 }, { 133, 130 }, { 208, 136 }, { 148, 140 }, { 235, 158 } },
        { { 156, 168 }, { 48, 154 }, { 174, 95 }, { 265, 75 }, { 130, 48 }, { 279, 23 }, { 198, 48 }, { 140, 25 }, { 281, 136 } },
    };
    private static final int[][][] ANIMS = {
        {
            { 0, 11, 3, 224, 104, 0 }, { 0, 11, 3, 184, 160, 0 }, { 0, 11, 3, 112, 136, 0 }, { 0, 11, 3, 72, 112, 0 },
            { 0, 11, 3, 88, 96, 0 }, { 0, 11, 3, 64, 48, 0 }, { 0, 11, 3, 192, 40, 0 }, { 0, 11, 3, 136, 16, 0 },
            { 0, 11, 3, 80, 16, 0 }, { 0, 11, 3, 64, 24, 0 },
        },
        {
            { 2, 11, 1, 128, 136, 1 }, { 2, 11, 1, 128, 136, 2 }, { 2, 11, 1, 128, 136, 3 }, { 2, 11, 1, 128, 136, 4 },
            { 2, 11, 1, 128, 136, 5 }, { 2, 11, 1, 128, 136, 6 }, { 2, 11, 1, 128, 136, 7 }, { 0, 11, 3, 192, 144, 8 },
            { 2, 11, 1, 128, 136, 8 },
        },
        {
            { 0, 11, 3, 104, 168, 0 }, { 0, 11, 3, 40, 136, 0 }, { 0, 11, 3, 160, 96, 0 }, { 0, 11, 3, 104, 80, 0 },
            { 0, 11, 3, 120, 32, 0 }, { 0, 8, 3, 40, 0, 0 },
        },
    };

    private final Game game;
    private final WbStart wbs;
    private int state = STAT_COUNT;
    private int accelerate;
    private int spState = 1;
    private int kills = -1;
    private int items = -1;
    private int secret = -1;
    private int time = -1;
    private int par = -1;
    private int pause = Defs.TICRATE;
    private int count;
    private int backgroundCount;
    private boolean pointer;
    public boolean done;
    private final Map<String, byte[]> patches = new HashMap<>();
    private final List<byte[]> numbers = new ArrayList<>();
    private final List<byte[]> levelNames = new ArrayList<>();
    private final List<WiAnim> animations = new ArrayList<>();
    private final byte[] background;

    public Intermission(Game game, WbStart wbs)
    {
        this.game = game;
        this.wbs = wbs;
        patches.put("finished", lump("WIF"));
        patches.put("entering", lump("WIENTER"));
        patches.put("kills", lump("WIOSTK"));
        patches.put("items", lump("WIOSTI"));
        patches.put("secret", lump("WISCRT2"));
        patches.put("percent", lump("WIPCNT"));
        patches.put("colon", lump("WICOLON"));
        patches.put("time", lump("WITIME"));
        patches.put("par", lump("WIPAR"));
        patches.put("sucks", lump("WISUCKS"));
        patches.put("minus", lump("WIMINUS"));
        patches.put("splat", lump("WISPLAT"));
        patches.put("yah0", lump("WIURH0"));
        patches.put("yah1", lump("WIURH1"));
        for (int i = 0; i < 10; ++i)
        {
            numbers.add(lump("WINUM" + i));
        }
        byte[] bg = lump(wbs.commercial || wbs.epsd == 3 ? "INTERPIC" : "WIMAP" + wbs.epsd);
        if (bg == null)
        {
            bg = lump("INTERPIC");
        }
        background = bg;
        int maps = wbs.commercial ? 32 : 9;
        for (int i = 0; i < maps; ++i)
        {
            levelNames.add(lump(wbs.commercial ? String.format("CWILV%02d", i) : "WILV" + wbs.epsd + i));
        }
        if (!wbs.commercial && wbs.epsd < 3)
        {
            int[][] anims = ANIMS[wbs.epsd];
            for (int j = 0; j < anims.length; ++j)
            {
                int type = anims[j][0];
                int period = anims[j][1];
                int frames = anims[j][2];
                int x = anims[j][3];
                int y = anims[j][4];
                int data = anims[j][5];
                byte[][] images = new byte[frames][];
                for (int i = 0; i < frames; ++i)
                {
                    if (wbs.epsd == 1 && j == 8)
                    {
                        images[i] = animations.size() > 4 ? animations.get(4).patches[i] : null;
                    }
                    else
                    {
                        images[i] = lump(String.format("WIA%d%02d%02d", wbs.epsd, j, i));
                    }
                }
                animations.add(new WiAnim(type, period, frames, x, y, data, images));
            }
        }
        initAnimated();
    }

    public static int parTime(int episode, int map, boolean commercial)
    {
        if (commercial)
        {
            int index = Math.max(0, Math.min(CPARS.length - 1, map - 1));
            return Defs.TICRATE * CPARS[index];
        }
        if (episode >= 1 && episode <= 3 && map >= 1 && map <= 9)
        {
            return Defs.TICRATE * PARS[episode][map];
        }
        return Defs.TICRATE * 30;
    }

    public void ticker()
    {
        ++backgroundCount;
        if (backgroundCount == 1)
        {
            game.sound.changeMusic(wbs.commercial ? "dm2int" : "inter", true);
        }
        checkAccelerate();
        if (state == STAT_COUNT)
        {
            updateStats();
        }
        else if (state == SHOW_NEXT)
        {
            updateShowNext();
        }
        else
        {
            updateNoState();
        }
    }

    private void checkAccelerate()
    {
        if (game.menu != null && game.menu.active)
        {
            return;
        }
        boolean attack = Boolean.TRUE.equals(game.keys.get(Keys.LCTRL)) || Boolean.TRUE.equals(game.keys.get(Keys.RCTRL));
        boolean use = Boolean.TRUE.equals(game.keys.get(Keys.SPACE))
            || Boolean.TRUE.equals(game.keys.get((int) 'e'))
            || Boolean.TRUE.equals(game.keys.get(Keys.RETURN))
            || Boolean.TRUE.equals(game.keys.get(Keys.KP_ENTER));
        Player player = game.player;
        if (player == null)
        {
            if (attack || use)
            {
                accelerate = 1;
            }
            return;
        }
        if (attack && !player.attackdown)
        {
            accelerate = 1;
        }
        if (use && !player.usedown)
        {
            accelerate = 1;
        }
        player.attackdown = attack;
        player.usedown = use;
    }

    private void updateStats()
    {
        updateAnimated();
        WbStart w = wbs;
        if (accelerate != 0 && spState != 10)
        {
            accelerate = 0;
            kills = pct(w.skills, w.maxkills);
            items = pct(w.sitems, w.maxitems);
            secret = pct(w.ssecret, w.maxsecret);
            time = Compat.intdiv(w.stime, Defs.TICRATE);
            par = Compat.intdiv(w.partime, Defs.TICRATE);
            game.startSound("barexp");
            spState = 10;
            return;
        }
        if ((spState & 1) != 0)
        {
            if (--pause == 0)
            {
                ++spState;
                pause = Defs.TICRATE;
                switch (spState)
                {
                    case 2:
                        kills = 0;
                        break;
                    case 4:
                        items = 0;
                        break;
                    case 6:
                        secret = 0;
                        break;
                    case 8:
                        time = 0;
                        par = 0;
                        break;
                    default:
                        break;
                }
            }
            return;
        }
        if (spState == 10)
        {
            if (accelerate != 0)
            {
                game.startSound("wpnup");
                if (w.commercial)
                {
                    initNoState();
                }
                else
                {
                    initShowNext();
                }
            }
            return;
        }
        if ((backgroundCount & 3) == 0)
        {
            game.startSound("pistol");
        }
        if (spState != 2 && spState != 4 && spState != 6)
        {
            time += 3;
            par += 3;
            time = Math.min(time, Compat.intdiv(w.stime, 35));
            par = Math.min(par, Compat.intdiv(w.partime, 35));
            if (time >= Compat.intdiv(w.stime, 35) && par >= Compat.intdiv(w.partime, 35))
            {
                finishCount();
            }
        }
        else
        {
            int src;
            int max;
            if (spState == 6)
            {
                src = w.ssecret;
                max = w.maxsecret;
            }
            else if (spState == 4)
            {
                src = w.sitems;
                max = w.maxitems;
            }
            else
            {
                src = w.skills;
                max = w.maxkills;
            }
            int target = pct(src, max);
            int cur;
            if (spState == 6)
            {
                cur = secret + 2;
            }
            else if (spState == 4)
            {
                cur = items + 2;
            }
            else
            {
                cur = kills + 2;
            }
            if (cur >= target)
            {
                if (spState == 6)
                {
                    secret = target;
                }
                else if (spState == 4)
                {
                    items = target;
                }
                else
                {
                    kills = target;
                }
                finishCount();
            }
            else if (spState == 6)
            {
                secret = cur;
            }
            else if (spState == 4)
            {
                items = cur;
            }
            else
            {
                kills = cur;
            }
        }
    }

    private void finishCount()
    {
        game.startSound("barexp");
        ++spState;
    }

    private static int pct(int value, int max)
    {
        return Compat.intdiv(value * 100, Math.max(1, max));
    }

    private void initShowNext()
    {
        state = SHOW_NEXT;
        accelerate = 0;
        count = 4 * 35;
        initAnimated();
    }

    private void initNoState()
    {
        state = NO_STATE;
        accelerate = 0;
        count = 10;
    }

    private void updateShowNext()
    {
        updateAnimated();
        if (--count == 0 || accelerate != 0)
        {
            initNoState();
        }
        else
        {
            pointer = (count & 31) < 20;
        }
    }

    private void updateNoState()
    {
        updateAnimated();
        if (--count == 0)
        {
            done = true;
        }
    }

    private static int randomInt(int min, int max)
    {
        return min + ThreadLocalRandom.current().nextInt(max - min + 1);
    }

    private void initAnimated()
    {
        for (WiAnim animation : animations)
        {
            animation.current = -1;
            animation.nextTic = backgroundCount + 1 + (animation.type == ALWAYS ? randomInt(0, Math.max(0, animation.period - 1)) : 0);
        }
    }

    private void updateAnimated()
    {
        for (int i = 0; i < animations.size(); ++i)
        {
            WiAnim a = animations.get(i);
            if (backgroundCount == a.nextTic)
            {
                if (a.type == ALWAYS)
                {
                    a.current = (a.current + 1) % a.frames;
                    a.nextTic = backgroundCount + a.period;
                }
                else if (!(state == STAT_COUNT && i == 7) && wbs.next == a.data)
                {
                    a.current = Math.min(a.frames - 1, a.current + 1);
                    a.nextTic = backgroundCount + a.period;
                }
            }
        }
    }

    public void draw(int[] fb)
    {
        drawBackground(fb);
        if (state == STAT_COUNT)
        {
            drawStats(fb);
        }
        else
        {
            drawNext(fb);
        }
    }

    private void drawBackground(int[] fb)
    {
        if (background != null)
        {
            VVideo.drawPatch(fb, 0, 0, background);
        }
        for (WiAnim a : animations)
        {
            if (a.current >= 0 && a.patches[a.current] != null)
            {
                VVideo.drawPatch(fb, a.x, a.y, a.patches[a.current]);
            }
        }
    }

    private void drawStats(int[] fb)
    {
        drawFinished(fb);
        int line = 24;
        drawStatRow(fb, "kills", kills, 50);
        drawStatRow(fb, "items", items, 50 + line);
        drawStatRow(fb, "secret", secret, 50 + 2 * line);
        if (patches.get("time") != null)
        {
            VVideo.drawPatch(fb, 16, 168, patches.get("time"));
        }
        drawTime(fb, 144, 168, time);
        if (wbs.epsd < 3)
        {
            if (patches.get("par") != null)
            {
                VVideo.drawPatch(fb, 176, 168, patches.get("par"));
            }
            drawTime(fb, 304, 168, par);
        }
    }

    private void drawStatRow(int[] fb, String name, int value, int y)
    {
        if (patches.get(name) != null)
        {
            VVideo.drawPatch(fb, 50, y, patches.get(name));
        }
        drawPercent(fb, 270, y, value);
    }

    private void drawFinished(int[] fb)
    {
        int y = 2;
        byte[] name = wbs.last >= 0 && wbs.last < levelNames.size() ? levelNames.get(wbs.last) : null;
        if (name != null)
        {
            int[] size = VVideo.patchSize(name);
            VVideo.drawPatch(fb, Compat.intdiv(320 - size[0], 2), y, name);
            y += Compat.intdiv(5 * size[1], 4);
        }
        if (patches.get("finished") != null)
        {
            int[] size = VVideo.patchSize(patches.get("finished"));
            VVideo.drawPatch(fb, Compat.intdiv(320 - size[0], 2), y, patches.get("finished"));
        }
    }

    private void drawNext(int[] fb)
    {
        if (state == NO_STATE)
        {
            pointer = true;
        }
        if (!wbs.commercial && wbs.epsd < 3)
        {
            int last = wbs.last == 8 ? wbs.next - 1 : wbs.last;
            for (int i = 0; i <= last; ++i)
            {
                drawNode(fb, i, new byte[][] { patches.get("splat") });
            }
            if (wbs.didsecret)
            {
                drawNode(fb, 8, new byte[][] { patches.get("splat") });
            }
            if (pointer)
            {
                drawNode(fb, wbs.next, new byte[][] { patches.get("yah0"), patches.get("yah1") });
            }
        }
        if (!wbs.commercial || wbs.next != 30)
        {
            drawEntering(fb);
        }
    }

    private void drawEntering(int[] fb)
    {
        int y = 2;
        byte[] p = patches.get("entering");
        if (p != null)
        {
            int[] size = VVideo.patchSize(p);
            VVideo.drawPatch(fb, Compat.intdiv(320 - size[0], 2), y, p);
            y += Compat.intdiv(5 * size[1], 4);
        }
        byte[] p2 = wbs.next >= 0 && wbs.next < levelNames.size() ? levelNames.get(wbs.next) : null;
        if (p2 != null)
        {
            int[] size = VVideo.patchSize(p2);
            VVideo.drawPatch(fb, Compat.intdiv(320 - size[0], 2), y, p2);
        }
    }

    private void drawNode(int[] fb, int number, byte[][] nodePatches)
    {
        if (wbs.epsd < 0 || wbs.epsd >= NODES.length || number < 0 || number >= NODES[wbs.epsd].length)
        {
            return;
        }
        int[] node = NODES[wbs.epsd][number];
        for (byte[] p : nodePatches)
        {
            if (p != null)
            {
                int[] size = VVideo.patchSize(p);
                int w = size[0];
                int h = size[1];
                int left = size[2];
                int top = size[3];
                if (node[0] - left >= 0 && node[0] - left + w < 320 && node[1] - top >= 0 && node[1] - top + h < 200)
                {
                    VVideo.drawPatch(fb, node[0], node[1], p);
                    return;
                }
            }
        }
    }

    private void drawPercent(int[] fb, int x, int y, int value)
    {
        if (value < 0)
        {
            return;
        }
        if (patches.get("percent") != null)
        {
            VVideo.drawPatch(fb, x, y, patches.get("percent"));
        }
        drawNumber(fb, x, y, value, -1);
    }

    private int drawNumber(int[] fb, int x, int y, int n, int digits)
    {
        int width = numbers.get(0) != null ? VVideo.patchSize(numbers.get(0))[0] : 8;
        if (digits < 0)
        {
            digits = n == 0 ? 1 : String.valueOf(Math.abs(n)).length();
        }
        boolean negative = n < 0;
        n = Math.abs(n);
        while (digits-- > 0)
        {
            x -= width;
            byte[] p = numbers.get(n % 10);
            if (p != null)
            {
                VVideo.drawPatch(fb, x, y, p);
            }
            n = Compat.intdiv(n, 10);
        }
        if (negative && patches.get("minus") != null)
        {
            x -= 8;
            VVideo.drawPatch(fb, x, y, patches.get("minus"));
        }
        return x;
    }

    private void drawTime(int[] fb, int x, int y, int t)
    {
        if (t < 0)
        {
            return;
        }
        if (t > 61 * 59)
        {
            byte[] p = patches.get("sucks");
            if (p != null)
            {
                int w = VVideo.patchSize(p)[0];
                VVideo.drawPatch(fb, x - w, y, p);
            }
            return;
        }
        int div = 1;
        do
        {
            int n = Compat.intdiv(t, div) % 60;
            x = drawNumber(fb, x, y, n, 2) - 8;
            div *= 60;
            if ((div == 60 || Compat.intdiv(t, div) > 0) && patches.get("colon") != null)
            {
                VVideo.drawPatch(fb, x, y, patches.get("colon"));
            }
        }
        while (Compat.intdiv(t, div) > 0);
    }

    private byte[] lump(String name)
    {
        int n = game.wad.checkNumForName(name);
        return n < 0 ? null : game.wad.cacheLumpNum(n);
    }
}
