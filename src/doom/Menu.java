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

import java.util.HashMap;
import java.util.Map;

final class MenuItem
{
    public int status;
    public String name;
    public String action;
    public int alpha;

    MenuItem(int status, String name, String action, int alpha)
    {
        this.status = status;
        this.name = name;
        this.action = action;
        this.alpha = alpha;
    }
}

final class MenuDef
{
    public MenuItem[] items;
    public String routine;
    public int x;
    public int y;
    public int lastOn;
    public String previous;

    MenuDef(MenuItem[] items, String routine, int x, int y, int lastOn, String previous)
    {
        this.items = items;
        this.routine = routine;
        this.x = x;
        this.y = y;
        this.lastOn = lastOn;
        this.previous = previous;
    }
}

public final class Menu
{
    private static final int LINE_HEIGHT = 16;
    private final Wad wad;
    private final Sound sound;
    private final Game game;
    public boolean active;
    private String screen = "main";
    private int itemOn;
    private int skull;
    private int skullTics = 8;
    private int episode;
    private String message;
    private boolean confirm;
    private String messageAction;
    private final String[] saveStrings = new String[6];
    private final boolean[] saveOk = new boolean[6];
    private boolean enteringSave;
    private int saveSlot;
    private String oldSave = "";
    private int saveIndex;
    private final Map<String, MenuDef> menus = new HashMap<>();

    public Menu(Wad wad, Sound sound, Game game)
    {
        this.wad = wad;
        this.sound = sound;
        this.game = game;
        for (int i = 0; i < 6; ++i)
        {
            saveStrings[i] = Defs.LOADSAVEEMPTY;
            saveOk[i] = false;
        }
        menus.put(
            "main",
            new MenuDef(
                items(new Object[][] {
                    { 1, "M_NGAME", "newgame" },
                    { 1, "M_OPTION", "options" },
                    { 1, "M_LOADG", "loadgame" },
                    { 1, "M_SAVEG", "savegame" },
                    { 1, "M_RDTHIS", "readthis" },
                    { 1, "M_QUITG", "quit" },
                }),
                "main",
                97,
                64,
                0,
                null
            )
        );
        menus.put(
            "episode",
            new MenuDef(
                items(new Object[][] {
                    { 1, "M_EPI1", "episode" },
                    { 1, "M_EPI2", "episode" },
                    { 1, "M_EPI3", "episode" },
                    { 1, "M_EPI4", "episode" },
                }),
                "episode",
                48,
                63,
                0,
                "main"
            )
        );
        menus.put(
            "skill",
            new MenuDef(
                items(new Object[][] {
                    { 1, "M_JKILL", "skill" },
                    { 1, "M_ROUGH", "skill" },
                    { 1, "M_HURT", "skill" },
                    { 1, "M_ULTRA", "skill" },
                    { 1, "M_NMARE", "skill" },
                }),
                "skill",
                48,
                63,
                2,
                "episode"
            )
        );
        menus.put(
            "options",
            new MenuDef(
                items(new Object[][] {
                    { 1, "M_ENDGAM", "endgame" },
                    { 1, "M_MESSG", "messages" },
                    { 1, "M_DETAIL", "detail" },
                    { 2, "M_SCRNSZ", "scrnsize" },
                    { -1, "", "" },
                    { 2, "M_MSENS", "mousesens" },
                    { -1, "", "" },
                    { 1, "M_SVOL", "sound" },
                }),
                "options",
                60,
                37,
                0,
                "main"
            )
        );
        menus.put(
            "sound",
            new MenuDef(
                items(new Object[][] {
                    { 2, "M_SFXVOL", "sfxvol" },
                    { -1, "", "" },
                    { 2, "M_MUSVOL", "musvol" },
                    { -1, "", "" },
                }),
                "sound",
                80,
                64,
                0,
                "options"
            )
        );
        MenuItem[] loadItems = new MenuItem[6];
        MenuItem[] saveItems = new MenuItem[6];
        for (int i = 0; i < 6; ++i)
        {
            loadItems[i] = new MenuItem(1, "", "loadslot", 0);
            saveItems[i] = new MenuItem(1, "", "saveslot", 0);
        }
        menus.put("load", new MenuDef(loadItems, "load", 80, 54, 0, "main"));
        menus.put("save", new MenuDef(saveItems, "save", 80, 54, 0, "main"));
        menus.put("read1", new MenuDef(new MenuItem[] { new MenuItem(1, "", "read2", 0) }, "read1", 280, 185, 0, "main"));
        menus.put("read2", new MenuDef(new MenuItem[] { new MenuItem(1, "", "finishread", 0) }, "read2", 330, 175, 0, "read1"));
        if (!hasEpisodes())
        {
            menus.get("skill").previous = "main";
        }
    }

    private static MenuItem[] items(Object[][] rows)
    {
        MenuItem[] out = new MenuItem[rows.length];
        for (int i = 0; i < rows.length; ++i)
        {
            out[i] = new MenuItem((Integer) rows[i][0], (String) rows[i][1], (String) rows[i][2], 0);
        }
        return out;
    }

    public void ticker()
    {
        if (active && --skullTics <= 0)
        {
            skull ^= 1;
            skullTics = 8;
        }
    }

    public void start()
    {
        if (active)
        {
            return;
        }
        active = true;
        screen = "main";
        itemOn = menus.get("main").lastOn;
        message = null;
        enteringSave = false;
        sound.play("swtchn");
    }

    public void clear()
    {
        active = false;
        message = null;
        enteringSave = false;
    }

    public boolean responder(int key, String unicode)
    {
        if (unicode == null)
        {
            unicode = "";
        }
        if (enteringSave)
        {
            return saveStringKey(key, unicode);
        }
        if (message != null)
        {
            if (confirm)
            {
                if (key == 'y' || key == Keys.RETURN || key == Keys.KP_ENTER)
                {
                    String action = messageAction;
                    message = null;
                    if ("quit".equals(action))
                    {
                        game.running = false;
                    }
                    else if ("endgame".equals(action))
                    {
                        game.returnToTitle();
                    }
                }
                else if (key == 'n' || key == Keys.ESC)
                {
                    message = null;
                }
                return true;
            }
            if (key != 0)
            {
                message = null;
            }
            return true;
        }
        if (key == Keys.F2)
        {
            action("savegame", 0);
            return true;
        }
        if (key == Keys.F3)
        {
            action("loadgame", 0);
            return true;
        }
        if (!active)
        {
            if (key == Keys.ESC)
            {
                start();
                return true;
            }
            if ((Keys.isMinus(key) || Keys.isPlus(key) || "+".equals(unicode) || "=".equals(unicode) || "-".equals(unicode) || "_".equals(unicode))
                && !game.automap.active)
            {
                game.sizeDisplay(Keys.isPlus(key) || "+".equals(unicode) || "=".equals(unicode) ? 1 : 0);
                return true;
            }
            return false;
        }
        MenuDef menu = menus.get(screen);
        if (key == Keys.ESC)
        {
            menu.lastOn = itemOn;
            clear();
            sound.play("swtchx");
            return true;
        }
        if (key == Keys.BACKSPACE)
        {
            menu.lastOn = itemOn;
            if (menu.previous != null)
            {
                go(menu.previous);
            }
            else
            {
                clear();
            }
            sound.play("swtchx");
            return true;
        }
        if (key == Keys.UP || key == Keys.DOWN)
        {
            int step = key == Keys.DOWN ? 1 : -1;
            int count = menu.items.length;
            do
            {
                itemOn = (itemOn + step + count) % count;
            }
            while (menu.items[itemOn].status == -1);
            sound.play("pstop");
            return true;
        }
        if (key == Keys.LEFT || key == Keys.RIGHT || Keys.isMinus(key) || Keys.isPlus(key))
        {
            MenuItem item = menu.items[itemOn];
            if (item.status == 2)
            {
                sound.play("stnmov");
                action(item.action, key == Keys.RIGHT || Keys.isPlus(key) ? 1 : 0);
                return true;
            }
            if (Keys.isMinus(key) || Keys.isPlus(key))
            {
                return false;
            }
            return true;
        }
        if (key == Keys.RETURN || key == Keys.KP_ENTER)
        {
            MenuItem item = menu.items[itemOn];
            if (item.status != 0)
            {
                menu.lastOn = itemOn;
                sound.play("pistol");
                action(item.action, item.status == 2 ? 1 : itemOn);
            }
            return true;
        }
        return true;
    }

    private void action(String action, int choice)
    {
        switch (action)
        {
            case "newgame":
                episode = 0;
                go(hasEpisodes() ? "episode" : "skill");
                break;
            case "options":
            case "sound":
                go(action);
                break;
            case "loadgame":
                openSlots("load");
                break;
            case "savegame":
                if (game.gamestate == Defs.GS_LEVEL && game.player != null)
                {
                    openSlots("save");
                }
                else
                {
                    sound.play("oof");
                }
                break;
            case "loadslot":
                if (saveOk[choice] && game.loadGame(choice))
                {
                    clear();
                }
                else
                {
                    sound.play("oof");
                }
                break;
            case "saveslot":
                beginSaveName(choice);
                break;
            case "readthis":
                go("read1");
                break;
            case "read2":
                go(has("HELP1") && "read1".equals(screen) ? "read2" : "main");
                break;
            case "finishread":
                go("main");
                break;
            case "quit":
                setConfirm("ARE YOU SURE YOU WANT TO QUIT?", "quit");
                break;
            case "endgame":
                if (game.gamestate == Defs.GS_LEVEL)
                {
                    setConfirm("END GAME?", "endgame");
                }
                else
                {
                    sound.play("oof");
                }
                break;
            case "messages":
                game.showMessages = !game.showMessages;
                if (game.player != null)
                {
                    game.player.setMessage(game.showMessages ? "Messages On" : "Messages Off");
                }
                break;
            case "detail":
                game.detailLevel ^= 1;
                game.applyViewSize();
                if (game.player != null)
                {
                    game.player.setMessage(game.detailLevel == 0 ? "High detail" : "Low detail");
                }
                break;
            case "scrnsize":
                game.sizeDisplay(choice);
                break;
            case "mousesens":
                game.mouseSensitivity = Math.max(0, Math.min(9, game.mouseSensitivity + (choice != 0 ? 1 : -1)));
                break;
            case "sfxvol":
                sound.setSfxVolume(sound.sfxVolume + (choice != 0 ? 1 : -1));
                break;
            case "musvol":
                sound.setMusicVolume(sound.musicVolume + (choice != 0 ? 1 : -1));
                break;
            case "episode":
                if (!has("E2M1") && choice != 0)
                {
                    message = "ONLY AVAILABLE IN THE REGISTERED VERSION.";
                    confirm = false;
                    go("read1");
                }
                else
                {
                    episode = choice;
                    go("skill");
                }
                break;
            case "skill":
                game.startNewGame(choice, episode + 1, 1);
                clear();
                break;
            default:
                break;
        }
    }

    private void setConfirm(String text, String action)
    {
        message = text;
        confirm = true;
        messageAction = action;
    }

    private void go(String next)
    {
        menus.get(screen).lastOn = itemOn;
        screen = next;
        itemOn = menus.get(next).lastOn;
    }

    private void openSlots(String next)
    {
        for (int i = 0; i < 6; ++i)
        {
            Object[] slot = Saveg.readSlotDescription(game, i);
            saveStrings[i] = (String) slot[0];
            saveOk[i] = (Boolean) slot[1];
            menus.get("load").items[i].status = saveOk[i] ? 1 : 0;
            menus.get("save").items[i].status = 1;
        }
        message = null;
        enteringSave = false;
        if (!active)
        {
            active = true;
            screen = next;
            itemOn = menus.get(next).lastOn;
        }
        else
        {
            go(next);
        }
        sound.play("swtchn");
    }

    private void beginSaveName(int slot)
    {
        enteringSave = true;
        saveSlot = slot;
        oldSave = saveStrings[slot];
        if (Defs.LOADSAVEEMPTY.equals(saveStrings[slot]))
        {
            saveStrings[slot] = "";
        }
        saveIndex = saveStrings[slot].length();
    }

    private boolean saveStringKey(int key, String ch)
    {
        int slot = saveSlot;
        if (key == Keys.BACKSPACE)
        {
            if (saveIndex > 0)
            {
                --saveIndex;
                saveStrings[slot] = saveStrings[slot].substring(0, saveIndex);
            }
            return true;
        }
        if (key == Keys.ESC)
        {
            enteringSave = false;
            saveStrings[slot] = oldSave;
            return true;
        }
        if (key == Keys.RETURN || key == Keys.KP_ENTER)
        {
            enteringSave = false;
            if (!saveStrings[slot].isEmpty())
            {
                if (game.saveGame(slot, saveStrings[slot]))
                {
                    clear();
                }
                else
                {
                    sound.play("oof");
                }
            }
            else
            {
                saveStrings[slot] = oldSave;
            }
            return true;
        }
        ch = ch.toUpperCase();
        if (ch.length() != 1)
        {
            return true;
        }
        int code = ch.charAt(0);
        if (!" ".equals(ch) && (code < Defs.HU_FONTSTART || code > Defs.HU_FONTEND))
        {
            return true;
        }
        if (code >= 32 && code <= 127 && saveIndex < Defs.SAVESTRINGSIZE - 1
            && stringWidth(saveStrings[slot]) < (Defs.SAVESTRINGSIZE - 2) * 8)
        {
            saveStrings[slot] += ch;
            ++saveIndex;
        }
        return true;
    }

    public void draw(int[] fb)
    {
        if (!active)
        {
            return;
        }
        if (message != null)
        {
            writeText(fb, 10, 80, message + (confirm ? "  (Y/N)" : ""));
            return;
        }
        MenuDef menu = menus.get(screen);
        String name = null;
        int hx = 0;
        int hy = 0;
        if ("main".equals(menu.routine))
        {
            name = "M_DOOM";
            hx = 94;
            hy = 2;
        }
        else if ("skill".equals(menu.routine))
        {
            name = "M_NEWG";
            hx = 96;
            hy = 14;
        }
        else if ("episode".equals(menu.routine))
        {
            name = "M_EPISOD";
            hx = 54;
            hy = 38;
        }
        else if ("options".equals(menu.routine))
        {
            name = "M_OPTTTL";
            hx = 108;
            hy = 15;
        }
        else if ("sound".equals(menu.routine))
        {
            name = "M_SVOL";
            hx = 60;
            hy = 38;
        }
        else if ("load".equals(menu.routine))
        {
            name = "M_LOADG";
            hx = 72;
            hy = 28;
        }
        else if ("save".equals(menu.routine))
        {
            name = "M_SAVEG";
            hx = 72;
            hy = 28;
        }
        if (name != null)
        {
            byte[] patch = patch(name);
            if (patch != null)
            {
                VVideo.drawPatch(fb, hx, hy, patch);
            }
        }
        if ("read1".equals(menu.routine) || "read2".equals(menu.routine))
        {
            String rname;
            if ("read2".equals(menu.routine))
            {
                rname = "HELP1";
            }
            else if (has("HELP2"))
            {
                rname = "HELP2";
            }
            else if (has("HELP1"))
            {
                rname = "HELP1";
            }
            else if (has("HELP"))
            {
                rname = "HELP";
            }
            else
            {
                rname = "CREDIT";
            }
            byte[] patch = patch(rname);
            if (patch != null)
            {
                VVideo.drawPatch(fb, 0, 0, patch);
            }
        }
        else if ("load".equals(menu.routine) || "save".equals(menu.routine))
        {
            drawSlots(fb, menu);
        }
        else
        {
            for (int i = 0; i < menu.items.length; ++i)
            {
                MenuItem item = menu.items[i];
                if (!item.name.isEmpty())
                {
                    byte[] patch = patch(item.name);
                    if (patch != null)
                    {
                        VVideo.drawPatch(fb, menu.x, menu.y + i * LINE_HEIGHT, patch);
                    }
                }
            }
        }
        if ("options".equals(menu.routine))
        {
            String msg = game.showMessages ? "M_MSGON" : "M_MSGOFF";
            byte[] pm = patch(msg);
            if (pm != null)
            {
                VVideo.drawPatch(fb, menu.x + 120, menu.y + LINE_HEIGHT, pm);
            }
            String det = game.detailLevel == 0 ? "M_GDHIGH" : "M_GDLOW";
            byte[] pd = patch(det);
            if (pd != null)
            {
                VVideo.drawPatch(fb, menu.x + 175, menu.y + LINE_HEIGHT * 2, pd);
            }
            thermo(fb, menu.x, menu.y + 64, 9, game.screenSize);
            thermo(fb, menu.x, menu.y + 96, 10, game.mouseSensitivity);
        }
        else if ("sound".equals(menu.routine))
        {
            thermo(fb, menu.x, menu.y + 16, 16, sound.sfxVolume);
            thermo(fb, menu.x, menu.y + 48, 16, sound.musicVolume);
        }
        if (!"read1".equals(menu.routine) && !"read2".equals(menu.routine))
        {
            byte[] patch = patch(skull != 0 ? "M_SKULL2" : "M_SKULL1");
            if (patch != null)
            {
                VVideo.drawPatch(fb, menu.x - 32, menu.y - 5 + itemOn * 16, patch);
            }
        }
    }

    private boolean hasEpisodes()
    {
        return !has("MAP01") && has("E2M1");
    }

    private boolean has(String name)
    {
        return wad.checkNumForName(name) >= 0;
    }

    private byte[] patch(String name)
    {
        int n = wad.checkNumForName(name);
        return n < 0 ? null : wad.cacheLumpNum(n);
    }

    private void drawSlots(int[] fb, MenuDef menu)
    {
        for (int i = 0; i < 6; ++i)
        {
            int y = menu.y + 16 * i;
            int x = menu.x;
            int nameCount = 1 + Defs.SAVESTRINGSIZE + 1;
            for (int j = 0; j < nameCount; ++j)
            {
                String name;
                if (j == 0)
                {
                    name = "M_LSLEFT";
                }
                else if (j == nameCount - 1)
                {
                    name = "M_LSRGHT";
                }
                else
                {
                    name = "M_LSCNTR";
                }
                byte[] p = patch(name);
                if (p != null)
                {
                    VVideo.drawPatch(fb, x - (j == 0 ? 8 : 0), y + 7, p);
                }
                if (j > 0)
                {
                    x += 8;
                }
            }
            int end = writeText(fb, menu.x, y, saveStrings[i]);
            if (enteringSave && i == saveSlot)
            {
                writeText(fb, end, y, "_");
            }
        }
    }

    private void thermo(int[] fb, int x, int y, int width, int dot)
    {
        int nameCount = width + 2;
        for (int i = 0; i < nameCount; ++i)
        {
            String name;
            if (i == 0)
            {
                name = "M_THERML";
            }
            else if (i == nameCount - 1)
            {
                name = "M_THERMR";
            }
            else
            {
                name = "M_THERMM";
            }
            byte[] p = patch(name);
            if (p != null)
            {
                VVideo.drawPatch(fb, x, y, p);
            }
            x += 8;
        }
        byte[] po = patch("M_THERMO");
        if (po != null)
        {
            int pos = Math.max(0, Math.min(width - 1, dot));
            VVideo.drawPatch(fb, x - (width + 2) * 8 + 8 + pos * 8, y, po);
        }
    }

    private int stringWidth(String text)
    {
        return writeText(null, 0, 0, text);
    }

    private int writeText(int[] fb, int x, int y, String text)
    {
        for (char raw : text.toUpperCase().toCharArray())
        {
            String ch = String.valueOf(raw);
            byte[] patch = " ".equals(ch) ? null : patch(String.format("STCFN%03d", (int) raw));
            if (patch != null)
            {
                if (fb != null)
                {
                    VVideo.drawPatch(fb, x, y, patch);
                }
                int width = VVideo.patchSize(patch)[0];
                x += Math.max(4, width);
            }
            else
            {
                x += " ".equals(ch) ? 4 : 8;
            }
            if (fb != null && x > 300)
            {
                x = 10;
                y += 10;
            }
        }
        return x;
    }
}
