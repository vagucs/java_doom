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

public final class Finale
{
    private static final int TEXT_SPEED = 3;
    private static final int TEXT_WAIT = 250;
    private static final int TEXT = 0;
    private static final int ART = 1;
    private static final String E1 =
        "Once you beat the big badasses and\nclean out the moon base you're supposed\nto win, aren't you? Aren't you? Where's\nyour fat reward and ticket home? What\nthe hell is this? It's not supposed to\nend this way!\n\nIt stinks like rotten meat, but looks\nlike the lost Deimos base.  Looks like\nyou're stuck on The Shores of Hell.\nThe only way out is through.\n\nTo continue the DOOM experience, play\nThe Shores of Hell and its amazing\nsequel, Inferno!\n";
    private static final String E2 =
        "You've done it! The hideous cyber-\ndemon lord that ruled the lost Deimos\nmoon base has been slain and you\nare triumphant! But ... where are\nyou? You clamber to the edge of the\nmoon and look down to see the awful\ntruth.\n\nDeimos floats above Hell itself!\nYou've never heard of anyone escaping\nfrom Hell, but you'll make the bastards\nsorry they ever heard of you! Quickly,\nyou rappel down to the surface of\nHell.\n\nNow, it's on to the final chapter of\nDOOM! -- Inferno.\n";
    private static final String E3 =
        "The loathsome spiderdemon that\nmasterminded the invasion of the moon\nbases and caused so much death has had\nits ass kicked for all time.\n\nA hidden doorway opens and you enter.\nYou've proven too tough for Hell to\ncontain, and now Hell at last plays\nfair -- for you emerge from the door\nto see the green fields of Earth!\nHome at last.\n\nYou wonder what's been happening on\nEarth while you were battling evil\nunleashed. It's good that no Hell-\nspawn could have come through that\ndoor with you ...\n";
    private static final String C1 =
        "You have won! Your victory has enabled\nhumankind to evacuate Earth and escape\nthe nightmare.  Now you are the only\nhuman left on the face of the planet.\nCan you defeat the final enemy and\nreturn to Earth, or will you just\nrot here with the rest of the walking\ndead?\n";

    private final Game game;
    private int stage = TEXT;
    private int count;
    public boolean done;
    private final String text;
    private final String flat;
    private byte[] flatLump;
    private byte[] art;
    private byte[] pfub1;
    private byte[] pfub2;
    private int lastBunnyStage = -1;

    public Finale(Game game)
    {
        this.game = game;
        boolean commercial = game.wad.checkNumForName("MAP01") >= 0;
        if (commercial)
        {
            text = C1;
            flat = "SLIME16";
            game.sound.changeMusic("read_m", true);
        }
        else
        {
            if (game.episode == 2)
            {
                text = E2;
            }
            else if (game.episode == 3)
            {
                text = E3;
            }
            else
            {
                text = E1;
            }
            if (game.episode == 2)
            {
                flat = "SFLR6_1";
            }
            else if (game.episode == 3)
            {
                flat = "MFLR8_4";
            }
            else if (game.episode == 4)
            {
                flat = "MFLR8_3";
            }
            else
            {
                flat = "FLOOR4_8";
            }
            game.sound.changeMusic("victor", true);
        }
        flatLump = lump(this.flat);
        String artName;
        if (game.episode == 2)
        {
            artName = "VICTORY2";
        }
        else if (game.episode == 4)
        {
            artName = "ENDPIC";
        }
        else
        {
            artName = has("CREDIT") ? "CREDIT" : "HELP2";
        }
        art = lump(artName);
        if (art == null)
        {
            art = lump("HELP1");
        }
        if (game.episode == 3 && !commercial)
        {
            pfub1 = lump("PFUB1");
            pfub2 = lump("PFUB2");
        }
    }

    public void ticker()
    {
        ++count;
        if (stage == TEXT && count > text.length() * TEXT_SPEED + TEXT_WAIT)
        {
            stage = ART;
            count = 0;
            game.forceWipe = true;
            if (game.episode == 3)
            {
                game.sound.changeMusic("bunny", true);
            }
        }
        else if (stage == ART && wantSkip() && count > 10)
        {
            done = true;
        }
    }

    public void draw(int[] fb)
    {
        if (stage == ART)
        {
            if (game.episode == 3 && pfub1 != null && pfub2 != null)
            {
                drawBunny(fb);
            }
            else if (art != null)
            {
                VVideo.fill(fb, 0);
                VVideo.drawPatch(fb, 0, 0, art);
            }
            return;
        }
        fillFlat(fb);
        int shown = Compat.intdiv(count, TEXT_SPEED);
        int x = 10;
        int y = 10;
        for (int i = 0; i < text.length(); ++i)
        {
            if (i >= shown)
            {
                break;
            }
            char ch = text.charAt(i);
            if (ch == '\n')
            {
                x = 10;
                y += 11;
                continue;
            }
            int code = Character.toUpperCase(ch);
            if (ch == ' ' || code < Defs.HU_FONTSTART || code > Defs.HU_FONTEND)
            {
                x += 4;
                continue;
            }
            byte[] patch = lump(String.format("STCFN%03d", code));
            if (patch == null)
            {
                x += 4;
                continue;
            }
            int width = VVideo.patchSize(patch)[0];
            if (x + width > Defs.SCREENWIDTH)
            {
                break;
            }
            VVideo.drawPatch(fb, x, y, patch);
            x += width;
        }
    }

    private void fillFlat(int[] fb)
    {
        if (flatLump == null || flatLump.length < 4096)
        {
            VVideo.fill(fb, 0);
            return;
        }
        for (int y = 0; y < Defs.SCREENHEIGHT; ++y)
        {
            int row = (y & 63) << 6;
            for (int x = 0; x < Defs.SCREENWIDTH; ++x)
            {
                fb[y * Defs.SCREENWIDTH + x] = flatLump[row + (x & 63)] & 0xff;
            }
        }
    }

    private void drawBunny(int[] fb)
    {
        VVideo.fill(fb, 0);
        int scroll = Math.max(0, Math.min(320, 320 - Compat.intdiv(count - 230, 2)));
        for (int x = 0; x < Defs.SCREENWIDTH; ++x)
        {
            int column = x + scroll;
            drawPatchColumn(fb, x, column < 320 ? pfub2 : pfub1, column < 320 ? column : column - 320);
        }
        if (count < 1130)
        {
            return;
        }
        int stageNow = count < 1180 ? 0 : Math.min(6, Compat.intdiv(count - 1180, 5));
        if (stageNow > lastBunnyStage)
        {
            game.sound.play("pistol");
            lastBunnyStage = stageNow;
        }
        byte[] patch = lump("END" + stageNow);
        if (patch != null)
        {
            VVideo.drawPatch(fb, Compat.intdiv(320 - 104, 2), Compat.intdiv(200 - 64, 2), patch);
        }
    }

    private void drawPatchColumn(int[] fb, int x, byte[] patch, int column)
    {
        if (column < 0)
        {
            return;
        }
        int offsetPos = 8 + column * 4;
        if (offsetPos + 4 > patch.length)
        {
            return;
        }
        int offset = Bin.u32(patch, offsetPos);
        while (offset < patch.length && (patch[offset] & 0xff) != 255)
        {
            int top = patch[offset] & 0xff;
            int length = patch[offset + 1] & 0xff;
            int source = offset + 3;
            for (int i = 0; i < length && top + i < 200; ++i)
            {
                fb[(top + i) * 320 + x] = patch[source + i] & 0xff;
            }
            offset += length + 4;
        }
    }

    private boolean wantSkip()
    {
        if (game.menu != null && game.menu.active)
        {
            return false;
        }
        int[] keys = { Keys.LCTRL, Keys.RCTRL, Keys.SPACE, Keys.RETURN, Keys.KP_ENTER, 'e' };
        for (int key : keys)
        {
            if (Boolean.TRUE.equals(game.keys.get(key)))
            {
                return true;
            }
        }
        return false;
    }

    private boolean has(String name)
    {
        return game.wad.checkNumForName(name) >= 0;
    }

    private byte[] lump(String name)
    {
        int n = game.wad.checkNumForName(name);
        return n < 0 ? null : game.wad.cacheLumpNum(n);
    }
}
