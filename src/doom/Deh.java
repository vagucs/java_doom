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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** DeHackEd / BEX subset (deh_main / deh_str / deh_misc / deh_cheat / deh_thing). */
public final class Deh
{
    public static final Deh INSTANCE = new Deh();

    public final List<String> files = new ArrayList<>();
    public boolean nodeh;
    public boolean dehlump;
    public boolean applyCheats = true;
    public boolean allowLongCheats;
    public boolean allowExtendedStrings;
    public final Map<String, String> replacements = new HashMap<>();
    public final List<CheatSeq> cheats = makeCheats();
    public int initialHealth = 100;
    public int initialBullets = 50;
    public int maxHealth = 200;
    public int maxArmor = 200;
    public int greenArmorClass = 1;
    public int blueArmorClass = 2;
    public int maxSoulsphere = 200;
    public int soulsphereHealth = 100;
    public int megasphereHealth = 200;
    public int godModeHealth = 100;
    public int idfaArmor = 200;
    public int idfaArmorClass = 2;
    public int idkfaArmor = 200;
    public int idkfaArmorClass = 2;
    public int bfgCellsPerShot = 40;
    public int speciesInfighting;
    public final int[] maxammo = {200, 50, 300, 50};
    public final int[] clipammo = {10, 4, 20, 1};

    private static final String[] SIGS = {
        "Patch File for DeHackEd v2.3",
        "Patch File for DeHackEd v3.0",
    };
    private static final Map<Integer, Integer> THING_DOOMED = new HashMap<>();
    private static final Map<String, String> BEX = new HashMap<>();
    private static final Map<String, String> MISC = new HashMap<>();

    static
    {
        THING_DOOMED.put(2, 3004);
        THING_DOOMED.put(3, 9);
        THING_DOOMED.put(4, 64);
        THING_DOOMED.put(6, 66);
        THING_DOOMED.put(9, 67);
        THING_DOOMED.put(12, 3001);
        THING_DOOMED.put(13, 3002);
        THING_DOOMED.put(15, 3005);
        THING_DOOMED.put(16, 3003);
        THING_DOOMED.put(18, 69);
        THING_DOOMED.put(19, 3006);
        THING_DOOMED.put(20, 7);
        THING_DOOMED.put(21, 68);
        THING_DOOMED.put(22, 16);
        THING_DOOMED.put(23, 71);
        THING_DOOMED.put(24, 84);
        THING_DOOMED.put(25, 72);
        THING_DOOMED.put(31, 2035);
        BEX.put("STSTR_DQDON", "Degreelessness Mode On");
        BEX.put("STSTR_DQDOFF", "Degreelessness Mode Off");
        BEX.put("STSTR_KFAADDED", "Very Happy Ammo Added");
        BEX.put("STSTR_FAADDED", "Ammo Added");
        BEX.put("STSTR_NCON", "No Clipping Mode ON");
        BEX.put("STSTR_NCOFF", "No Clipping Mode OFF");
        BEX.put("STSTR_BEHOLD", "invin visis rad allmap lite amp");
        BEX.put("STSTR_BEHOLDX", "Power-up Toggled");
        BEX.put("STSTR_CHOPPERS", "... doesn't suck - GM");
        BEX.put("STSTR_CLEV", "Changing Level...");
        BEX.put("STSTR_MUS", "Music Change");
        BEX.put("STSTR_NOMUS", "IMPOSSIBLE SELECTION");
        BEX.put("GOTSTIM", "Picked up a stimpack.");
        BEX.put("GOTMEDIKIT", "Picked up a medikit.");
        BEX.put("GOTHTHBONUS", "You pick up a health bonus.");
        BEX.put("GOTARMBONUS", "You pick up an armor bonus.");
        BEX.put("GOTARMOR", "Picked up the armor.");
        BEX.put("GOTMEGA", "Picked up the MegaArmor!");
        BEX.put("GOTSUPER", "Supercharge!");
        BEX.put("GOTMSPHERE", "MegaSphere!");
        BEX.put("GOTBERSERK", "Berserk!");
        BEX.put("GOTINVUL", "Invulnerability!");
        BEX.put("GOTINVIS", "Partial Invisibility");
        BEX.put("GOTSUIT", "Radiation Shielding Suit");
        BEX.put("GOTMAP", "Computer Area Map");
        BEX.put("GOTVISOR", "Light Amplification Visor");
        BEX.put("GGSAVED", "game saved.");
        BEX.put("AMSTR_FOLLOWON", "Follow Mode ON");
        BEX.put("AMSTR_FOLLOWOFF", "Follow Mode OFF");
        BEX.put("AMSTR_GRIDON", "Grid ON");
        BEX.put("AMSTR_GRIDOFF", "Grid OFF");
        BEX.put("AMSTR_MARKSCLEARED", "All Marks Cleared");
        BEX.put("MSGOFF", "Messages Off");
        BEX.put("MSGON", "Messages On");
        BEX.put("DETAILHI", "High detail");
        BEX.put("DETAILLO", "Low detail");
        BEX.put("GOTSHOTGUN", "You got the shotgun!");
        BEX.put("GOTSHOTGUN2", "You got the super shotgun!");
        BEX.put("GOTCHAINGUN", "You got the chaingun!");
        BEX.put("GOTLAUNCHER", "You got the rocket launcher!");
        BEX.put("GOTPLASMA", "You got the plasma gun!");
        BEX.put("GOTBFG9000", "You got the BFG9000!");
        BEX.put("GOTCHAINSAW", "A chainsaw!  Find some meat!");
        MISC.put("initial health", "initialHealth");
        MISC.put("initial bullets", "initialBullets");
        MISC.put("max health", "maxHealth");
        MISC.put("max armor", "maxArmor");
        MISC.put("green armor class", "greenArmorClass");
        MISC.put("blue armor class", "blueArmorClass");
        MISC.put("max soulsphere", "maxSoulsphere");
        MISC.put("soulsphere health", "soulsphereHealth");
        MISC.put("megasphere health", "megasphereHealth");
        MISC.put("god mode health", "godModeHealth");
        MISC.put("idfa armor", "idfaArmor");
        MISC.put("idfa armor class", "idfaArmorClass");
        MISC.put("idkfa armor", "idkfaArmor");
        MISC.put("idkfa armor class", "idkfaArmorClass");
        MISC.put("bfg cells/shot", "bfgCellsPerShot");
    }

    public static final class CheatSeq
    {
        public final String action;
        public String sequence;
        public final int paramChars;
        public final String dehName;
        public int charsRead;
        public String paramBuf = "";

        public CheatSeq(String action, String sequence, int paramChars, String dehName)
        {
            this.action = action;
            this.sequence = sequence;
            this.paramChars = paramChars;
            this.dehName = dehName;
        }

        public String feed(String ch)
        {
            if (sequence.isEmpty())
            {
                return null;
            }
            if (charsRead < sequence.length())
            {
                if (ch.equals(sequence.substring(charsRead, charsRead + 1)))
                {
                    charsRead++;
                }
                else
                {
                    charsRead = ch.equals(sequence.substring(0, 1)) ? 1 : 0;
                }
                if (charsRead < sequence.length())
                {
                    return null;
                }
                if (paramChars <= 0)
                {
                    charsRead = 0;
                    return "";
                }
                return null;
            }
            if (paramBuf.length() < paramChars)
            {
                paramBuf += ch;
            }
            if (paramBuf.length() >= paramChars)
            {
                String buf = paramBuf;
                charsRead = 0;
                paramBuf = "";
                return buf;
            }
            return null;
        }
    }

    private static class Ctx
    {
        final String data;
        final String name;
        int pos;
        int line = 1;

        Ctx(String data, String name)
        {
            this.data = data;
            this.name = name;
        }
    }

    private Deh()
    {
    }

    public static String string(String text)
    {
        return INSTANCE.replacements.getOrDefault(text, text);
    }

    public static int[] powerTics()
    {
        int t = Defs.TICRATE;
        return new int[] {30 * t, 1, 60 * t, 60 * t, 1, 120 * t};
    }

    public static List<CheatSeq> makeCheats()
    {
        List<CheatSeq> list = new ArrayList<>();
        list.add(new CheatSeq("god", "iddqd", 0, "iddqd"));
        list.add(new CheatSeq("kfa", "idkfa", 0, "idkfa"));
        list.add(new CheatSeq("fa", "idfa", 0, "idfa"));
        list.add(new CheatSeq("noclip2", "idclip", 0, "idclip"));
        list.add(new CheatSeq("noclip", "idspispopd", 0, "idspispopd"));
        list.add(new CheatSeq("iddt", "iddt", 0, null));
        list.add(new CheatSeq("beholdv", "idbeholdv", 0, null));
        list.add(new CheatSeq("beholds", "idbeholds", 0, null));
        list.add(new CheatSeq("beholdi", "idbeholdi", 0, null));
        list.add(new CheatSeq("beholdr", "idbeholdr", 0, null));
        list.add(new CheatSeq("beholda", "idbeholda", 0, null));
        list.add(new CheatSeq("beholdl", "idbeholdl", 0, null));
        list.add(new CheatSeq("behold", "idbehold", 0, "idbehold"));
        list.add(new CheatSeq("choppers", "idchoppers", 0, "idchoppers"));
        list.add(new CheatSeq("mypos", "idmypos", 0, "idmypos"));
        list.add(new CheatSeq("clev", "idclev", 2, "idclev"));
        list.add(new CheatSeq("mus", "idmus", 2, "idmus"));
        return list;
    }

    public void loadAfterIwad(Wad wad, String iwadPath)
    {
        if (!nodeh)
        {
            for (int i = 0; i < wad.numLumps(); i++)
            {
                if ("DEHACKED".equals(wad.lumpName(i)))
                {
                    loadLump(wad, i);
                }
            }
        }
        for (String file : files)
        {
            if (Files.isRegularFile(Path.of(file)))
            {
                loadFile(file);
            }
            else
            {
                System.out.println("DEH_LoadFile: Unable to open " + file);
            }
        }
    }

    public void loadFile(String path)
    {
        try
        {
            byte[] raw = Files.readAllBytes(Path.of(path));
            System.out.println(" loading " + path);
            parse(new String(raw, StandardCharsets.ISO_8859_1), path);
        }
        catch (Exception e)
        {
            System.out.println("DEH_LoadFile: Unable to open " + path);
        }
    }

    public void loadLump(Wad wad, int lumpnum)
    {
        String name = wad.lumpName(lumpnum);
        System.out.println(" loading lump " + name);
        parse(new String(wad.cacheLumpNum(lumpnum), StandardCharsets.ISO_8859_1), name);
    }

    private void parse(String data, String name)
    {
        allowLongCheats = false;
        allowExtendedStrings = false;
        Ctx ctx = new Ctx(data, name);
        String first = readLine(ctx, false);
        if (first == null || (!SIGS[0].equals(first.trim()) && !SIGS[1].equals(first.trim())))
        {
            System.out.println(name + ": This is not a valid dehacked patch file!");
            return;
        }
        String section = null;
        Integer tag = null;
        while (true)
        {
            String line = readLine(ctx, "[STRINGS]".equals(section));
            if (line == null)
            {
                return;
            }
            String stripped = line.replaceFirst("^[ \t]+", "");
            if (stripped.startsWith("#"))
            {
                comment(stripped);
                continue;
            }
            if (stripped.trim().isEmpty())
            {
                section = null;
                tag = null;
                continue;
            }
            if (section != null)
            {
                parseLine(ctx, section, stripped, tag);
            }
            else
            {
                String word = stripped.split("\\s+", 2)[0];
                if ("[STRINGS]".equalsIgnoreCase(word) && !allowExtendedStrings)
                {
                    section = null;
                    continue;
                }
                section = word;
                tag = startSection(ctx, word, stripped);
            }
        }
    }

    private int getChar(Ctx ctx)
    {
        if (ctx.pos >= ctx.data.length())
        {
            return -1;
        }
        char ch = ctx.data.charAt(ctx.pos++);
        if (ch == '\n')
        {
            ctx.line++;
        }
        return ch;
    }

    private String readLine(Ctx ctx, boolean extended)
    {
        if (ctx.pos >= ctx.data.length())
        {
            return null;
        }
        StringBuilder parts = new StringBuilder();
        while (true)
        {
            StringBuilder buf = new StringBuilder();
            while (ctx.pos < ctx.data.length())
            {
                char ch = ctx.data.charAt(ctx.pos++);
                if (ch == '\n')
                {
                    ctx.line++;
                    break;
                }
                if (ch != '\r')
                {
                    buf.append(ch);
                }
            }
            if (extended && buf.length() > 0 && buf.charAt(buf.length() - 1) == '\\')
            {
                parts.append(buf.substring(0, buf.length() - 1)).append('\n');
                if (ctx.pos >= ctx.data.length())
                {
                    break;
                }
                continue;
            }
            parts.append(buf);
            break;
        }
        return parts.toString();
    }

    private void comment(String comment)
    {
        if (comment.contains("*allow-long-cheats*"))
        {
            allowLongCheats = true;
        }
        if (comment.contains("*allow-extended-strings*"))
        {
            allowExtendedStrings = true;
        }
    }

    private Integer startSection(Ctx ctx, String word, String line)
    {
        String key = word.toLowerCase(Locale.ROOT);
        if ("thing".equals(key) || "ammo".equals(key))
        {
            String[] tok = line.trim().split("\\s+");
            if (tok.length > 1)
            {
                try
                {
                    return Integer.parseInt(tok[1]);
                }
                catch (NumberFormatException e)
                {
                    return null;
                }
            }
            return null;
        }
        if ("text".equals(key))
        {
            parseText(ctx, line);
        }
        return null;
    }

    private void parseLine(Ctx ctx, String section, String line, Integer tag)
    {
        String key = section.toLowerCase(Locale.ROOT);
        if ("misc".equals(key))
        {
            parseMisc(line);
        }
        else if ("thing".equals(key))
        {
            parseThing(line, tag);
        }
        else if ("ammo".equals(key))
        {
            parseAmmo(line, tag);
        }
        else if ("cheat".equals(key))
        {
            parseCheat(line);
        }
        else if ("[strings]".equals(key))
        {
            parseBex(line);
        }
    }

    private String[] assignment(String line)
    {
        int eq = line.indexOf('=');
        if (eq < 0)
        {
            return null;
        }
        return new String[] {line.substring(0, eq).trim(), line.substring(eq + 1).trim()};
    }

    private void parseMisc(String line)
    {
        String[] asg = assignment(line);
        if (asg == null)
        {
            return;
        }
        int value;
        try
        {
            value = Integer.parseInt(asg[1].trim().split("\\s+")[0]);
        }
        catch (NumberFormatException e)
        {
            return;
        }
        if ("monsters infight".equalsIgnoreCase(asg[0]))
        {
            speciesInfighting = value == 221 ? 1 : 0;
            return;
        }
        String field = MISC.get(asg[0].toLowerCase(Locale.ROOT));
        if (field == null)
        {
            return;
        }
        switch (field)
        {
            case "initialHealth":
                initialHealth = value;
                break;
            case "initialBullets":
                initialBullets = value;
                break;
            case "maxHealth":
                maxHealth = value;
                break;
            case "maxArmor":
                maxArmor = value;
                break;
            case "greenArmorClass":
                greenArmorClass = value;
                break;
            case "blueArmorClass":
                blueArmorClass = value;
                break;
            case "maxSoulsphere":
                maxSoulsphere = value;
                break;
            case "soulsphereHealth":
                soulsphereHealth = value;
                break;
            case "megasphereHealth":
                megasphereHealth = value;
                break;
            case "godModeHealth":
                godModeHealth = value;
                break;
            case "idfaArmor":
                idfaArmor = value;
                break;
            case "idfaArmorClass":
                idfaArmorClass = value;
                break;
            case "idkfaArmor":
                idkfaArmor = value;
                break;
            case "idkfaArmorClass":
                idkfaArmorClass = value;
                break;
            case "bfgCellsPerShot":
                bfgCellsPerShot = value;
                break;
            default:
                break;
        }
    }

    private void parseThing(String line, Integer tag)
    {
        String[] asg = assignment(line);
        if (asg == null || tag == null || !"hit points".equalsIgnoreCase(asg[0]))
        {
            return;
        }
        Integer doomed = THING_DOOMED.get(tag);
        if (doomed == null)
        {
            return;
        }
        try
        {
            Mobj.patchHitPoints(doomed, Integer.parseInt(asg[1].trim().split("\\s+")[0]));
        }
        catch (NumberFormatException ignored)
        {
        }
    }

    private void parseAmmo(String line, Integer tag)
    {
        String[] asg = assignment(line);
        if (asg == null || tag == null || tag < 0 || tag > 3)
        {
            return;
        }
        int value;
        try
        {
            value = Integer.parseInt(asg[1].trim().split("\\s+")[0]);
        }
        catch (NumberFormatException e)
        {
            return;
        }
        if ("max ammo".equalsIgnoreCase(asg[0]))
        {
            maxammo[tag] = value;
        }
        else if ("per ammo".equalsIgnoreCase(asg[0]))
        {
            clipammo[tag] = value;
        }
    }

    private void parseCheat(String line)
    {
        String[] asg = assignment(line);
        if (asg == null || !applyCheats)
        {
            return;
        }
        CheatSeq cheat = null;
        String name = asg[0].toLowerCase(Locale.ROOT);
        for (CheatSeq item : cheats)
        {
            if (name.equals(item.dehName))
            {
                cheat = item;
                break;
            }
        }
        if (cheat == null)
        {
            return;
        }
        StringBuilder seq = new StringBuilder();
        for (int i = 0; i < asg[1].length(); i++)
        {
            int code = asg[1].charAt(i) & 0xff;
            if (code == 0 || code == 0xff)
            {
                break;
            }
            if (!allowLongCheats && i + 1 > cheat.sequence.length())
            {
                break;
            }
            seq.append(asg[1].charAt(i));
        }
        cheat.sequence = seq.toString();
        cheat.charsRead = 0;
        cheat.paramBuf = "";
    }

    private void parseBex(String line)
    {
        String[] asg = assignment(line);
        if (asg == null)
        {
            return;
        }
        String original = BEX.get(asg[0].toUpperCase(Locale.ROOT));
        if (original == null)
        {
            return;
        }
        replacements.put(original, asg[1].replace("\\n", "\n"));
    }

    private void parseText(Ctx ctx, String line)
    {
        String[] parts = line.trim().split("\\s+");
        if (parts.length < 3)
        {
            return;
        }
        int nFrom;
        int nTo;
        try
        {
            nFrom = Integer.parseInt(parts[1]);
            nTo = Integer.parseInt(parts[2]);
        }
        catch (NumberFormatException e)
        {
            return;
        }
        StringBuilder src = new StringBuilder();
        StringBuilder dst = new StringBuilder();
        for (int i = 0; i < nFrom; i++)
        {
            int code = getChar(ctx);
            if (code < 0)
            {
                break;
            }
            src.append((char) code);
        }
        for (int i = 0; i < nTo; i++)
        {
            int code = getChar(ctx);
            if (code < 0)
            {
                break;
            }
            dst.append((char) code);
        }
        replacements.put(src.toString(), dst.toString());
    }
}
