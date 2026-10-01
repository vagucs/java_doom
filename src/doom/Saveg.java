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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Saveg
{
    private static final String MAGIC = "DOOMPY01";
    private static final String[] PLAYER_FIELDS = {
        "playerstate", "viewz", "viewheight", "deltaviewheight", "bob", "health",
        "armorpoints", "armortype", "ammo", "maxammo", "weaponowned", "pendingweapon",
        "readyweapon", "cards", "cheats", "message", "messageTics", "attackdown",
        "usedown", "damagecount", "bonuscount", "extralight", "refire", "killcount",
        "itemcount", "secretcount", "didsecret", "pspriteY", "pspriteSy",
        "pspriteState", "pspriteTics", "pspriteStep", "pspriteBody", "pspriteFlash",
        "flashTics",
    };
    private static final String[] MOBJ_FIELDS = {
        "x", "y", "z", "angle", "momx", "momy", "momz", "radius", "height",
        "floorz", "ceilingz", "flags", "health", "type", "sprite", "info", "alive",
        "reactiontime", "movedir", "movecount", "aiState", "frame", "tics",
        "chaseTics", "justAttacked", "damage", "attackKind", "didFire",
    };

    private Saveg()
    {
    }

    public static String savePath(Game game, int slot)
    {
        String directory;
        if (game.iwadPath != null && !game.iwadPath.isEmpty())
        {
            try
            {
                Path parent = Path.of(game.iwadPath).toRealPath().getParent();
                directory = parent != null ? parent.toString() : Path.of(game.iwadPath).toString();
            }
            catch (Exception e)
            {
                Path parent = Path.of(game.iwadPath).getParent();
                directory = parent != null ? parent.toString() : System.getProperty("user.dir");
            }
        }
        else
        {
            directory = System.getProperty("user.dir");
        }
        return Path.of(directory, Defs.SAVEGAMENAME + slot + ".dsg").toString();
    }

    public static Object[] readSlotDescription(Game game, int slot)
    {
        byte[] data;
        try
        {
            data = Files.readAllBytes(Path.of(savePath(game, slot)));
        }
        catch (Exception e)
        {
            return new Object[] { Defs.LOADSAVEEMPTY, false };
        }
        if (data.length < Defs.SAVESTRINGSIZE + 8
            || !MAGIC.equals(new String(data, Defs.SAVESTRINGSIZE, 8, StandardCharsets.ISO_8859_1)))
        {
            return new Object[] { Defs.LOADSAVEEMPTY, false };
        }
        String raw = new String(data, 0, Defs.SAVESTRINGSIZE, StandardCharsets.ISO_8859_1);
        int nul = raw.indexOf('\0');
        if (nul >= 0)
        {
            raw = raw.substring(0, nul);
        }
        String description = rtrim(raw);
        return new Object[] { !description.isEmpty() ? description : Defs.LOADSAVEEMPTY, true };
    }

    public static boolean writeSave(Game game, int slot, String description)
    {
        if (game.world == null || game.player == null)
        {
            return false;
        }
        String json = stringify(dumpState(game));
        if (description.length() > Defs.SAVESTRINGSIZE)
        {
            description = description.substring(0, Defs.SAVESTRINGSIZE);
        }
        byte[] name = new byte[Defs.SAVESTRINGSIZE];
        byte[] descBytes = description.getBytes(StandardCharsets.ISO_8859_1);
        System.arraycopy(descBytes, 0, name, 0, Math.min(descBytes.length, Defs.SAVESTRINGSIZE));
        byte[] magic = MAGIC.getBytes(StandardCharsets.ISO_8859_1);
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        byte[] blob = new byte[Defs.SAVESTRINGSIZE + 8 + jsonBytes.length];
        System.arraycopy(name, 0, blob, 0, Defs.SAVESTRINGSIZE);
        System.arraycopy(magic, 0, blob, Defs.SAVESTRINGSIZE, 8);
        System.arraycopy(jsonBytes, 0, blob, Defs.SAVESTRINGSIZE + 8, jsonBytes.length);
        Path p = Path.of(savePath(game, slot));
        Path temporary = Path.of(p.toString() + ".tmp");
        try
        {
            Files.write(temporary, blob);
        }
        catch (Exception e)
        {
            return false;
        }
        try
        {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win") && Files.exists(p))
            {
                Files.delete(p);
            }
            Files.move(temporary, p, StandardCopyOption.REPLACE_EXISTING);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public static boolean readAndRestore(Game game, int slot)
    {
        try
        {
            byte[] data = Files.readAllBytes(Path.of(savePath(game, slot)));
            int offset = Defs.SAVESTRINGSIZE + 8;
            if (data.length < offset
                || !MAGIC.equals(new String(data, Defs.SAVESTRINGSIZE, 8, StandardCharsets.ISO_8859_1)))
            {
                return false;
            }
            Object parsed = parse(new String(data, offset, data.length - offset, StandardCharsets.UTF_8));
            if (!(parsed instanceof Map))
            {
                return false;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> state = (Map<String, Object>) parsed;
            restoreState(game, state);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private static Map<String, Object> dumpState(Game game)
    {
        World world = game.world;
        List<Mobj> mobjs = world.mobjs;
        List<Map<String, Object>> mobjRecords = new ArrayList<>();
        for (Mobj mobj : mobjs)
        {
            Map<String, Object> record = dumpMobj(mobj);
            if (record.get("info") instanceof Object[])
            {
                record.put("info", copyInfo((Object[]) record.get("info")));
            }
            record.put("target", mobj.target == null ? null : mobjs.indexOf(mobj.target));
            record.put("isPlayer", mobj.player != null);
            mobjRecords.add(record);
        }
        List<Map<String, Object>> thinkers = new ArrayList<>();
        if (game.specials != null)
        {
            for (Object thinker : game.specials.thinkers)
            {
                if (thinker instanceof VerticalDoor)
                {
                    VerticalDoor door = (VerticalDoor) thinker;
                    if (door.dead)
                    {
                        continue;
                    }
                    int sector = world.sectors.indexOf(door.sector);
                    if (sector == -1)
                    {
                        continue;
                    }
                    thinkers.add(record(door, "door", sector, new String[] { "type", "direction", "topheight", "speed", "topwait", "topcountdown" }));
                }
                else if (thinker instanceof Plat)
                {
                    Plat plat = (Plat) thinker;
                    if (plat.dead)
                    {
                        continue;
                    }
                    int sector = world.sectors.indexOf(plat.sector);
                    if (sector == -1)
                    {
                        continue;
                    }
                    thinkers.add(record(plat, "plat", sector, new String[] { "type", "status", "speed", "low", "high", "wait", "count" }));
                }
                else if (thinker instanceof FloorMove)
                {
                    FloorMove floor = (FloorMove) thinker;
                    if (floor.dead)
                    {
                        continue;
                    }
                    int sector = world.sectors.indexOf(floor.sector);
                    if (sector == -1)
                    {
                        continue;
                    }
                    thinkers.add(record(floor, "floor", sector, new String[] { "direction", "dest", "speed" }));
                }
            }
        }
        List<Map<String, Object>> buttons = new ArrayList<>();
        List<Button> buttonList = game.specials != null ? game.specials.buttons : new ArrayList<>();
        for (Button button : buttonList)
        {
            Map<String, Object> rec = new LinkedHashMap<>();
            rec.put("line", button.line != null ? button.line.iLine : -1);
            rec.put("where", button.where);
            rec.put("texture", button.texture);
            rec.put("timer", button.timer);
            buttons.add(rec);
        }
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("episode", game.episode);
        state.put("mapn", game.mapn);
        state.put("skill", game.skill);
        state.put("leveltime", game.leveltime);
        state.put("player", dumpPlayer(game.player));
        List<Map<String, Object>> sectors = new ArrayList<>();
        for (Sector s : world.sectors)
        {
            Map<String, Object> rec = new LinkedHashMap<>();
            rec.put("floorheight", s.floorheight);
            rec.put("ceilingheight", s.ceilingheight);
            rec.put("floorpic", s.floorpic);
            rec.put("ceilingpic", s.ceilingpic);
            rec.put("lightlevel", s.lightlevel);
            rec.put("special", s.special);
            sectors.add(rec);
        }
        List<Map<String, Object>> sides = new ArrayList<>();
        for (Side s : world.sides)
        {
            Map<String, Object> rec = new LinkedHashMap<>();
            rec.put("textureoffset", s.textureoffset);
            rec.put("rowoffset", s.rowoffset);
            rec.put("toptexture", s.toptexture);
            rec.put("bottomtexture", s.bottomtexture);
            rec.put("midtexture", s.midtexture);
            sides.add(rec);
        }
        List<Map<String, Object>> lines = new ArrayList<>();
        for (Line l : world.lines)
        {
            Map<String, Object> rec = new LinkedHashMap<>();
            rec.put("flags", l.flags);
            rec.put("special", l.special);
            lines.add(rec);
        }
        state.put("sectors", sectors);
        state.put("sides", sides);
        state.put("lines", lines);
        state.put("mobjs", mobjRecords);
        state.put("thinkers", thinkers);
        state.put("buttons", buttons);
        state.put("totalkills", game.totalkills);
        state.put("totalitems", game.totalitems);
        state.put("totalsecret", game.totalsecret);
        return state;
    }

    private static Map<String, Object> dumpPlayer(Player player)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("playerstate", player.playerstate);
        result.put("viewz", player.viewz);
        result.put("viewheight", player.viewheight);
        result.put("deltaviewheight", player.deltaviewheight);
        result.put("bob", player.bob);
        result.put("health", player.health);
        result.put("armorpoints", player.armorpoints);
        result.put("armortype", player.armortype);
        result.put("ammo", player.ammo);
        result.put("maxammo", player.maxammo);
        result.put("weaponowned", player.weaponowned);
        result.put("pendingweapon", player.pendingweapon);
        result.put("readyweapon", player.readyweapon);
        result.put("cards", player.cards);
        result.put("cheats", player.cheats);
        result.put("message", player.message);
        result.put("messageTics", player.messageTics);
        result.put("attackdown", player.attackdown);
        result.put("usedown", player.usedown);
        result.put("damagecount", player.damagecount);
        result.put("bonuscount", player.bonuscount);
        result.put("extralight", player.extralight);
        result.put("refire", player.refire);
        result.put("killcount", player.killcount);
        result.put("itemcount", player.itemcount);
        result.put("secretcount", player.secretcount);
        result.put("didsecret", player.didsecret);
        result.put("pspriteY", player.pspriteY);
        result.put("pspriteSy", player.pspriteSy);
        result.put("pspriteState", player.pspriteState);
        result.put("pspriteTics", player.pspriteTics);
        result.put("pspriteStep", player.pspriteStep);
        result.put("pspriteBody", player.pspriteBody);
        result.put("pspriteFlash", player.pspriteFlash);
        result.put("flashTics", player.flashTics);
        return result;
    }

    private static Map<String, Object> dumpMobj(Mobj mobj)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("x", mobj.x);
        result.put("y", mobj.y);
        result.put("z", mobj.z);
        result.put("angle", mobj.angle);
        result.put("momx", mobj.momx);
        result.put("momy", mobj.momy);
        result.put("momz", mobj.momz);
        result.put("radius", mobj.radius);
        result.put("height", mobj.height);
        result.put("floorz", mobj.floorz);
        result.put("ceilingz", mobj.ceilingz);
        result.put("flags", mobj.flags);
        result.put("health", mobj.health);
        result.put("type", mobj.type);
        result.put("sprite", mobj.sprite);
        result.put("info", mobj.info);
        result.put("alive", mobj.alive);
        result.put("reactiontime", mobj.reactiontime);
        result.put("movedir", mobj.movedir);
        result.put("movecount", mobj.movecount);
        result.put("aiState", mobj.aiState);
        result.put("frame", mobj.frame);
        result.put("tics", mobj.tics);
        result.put("chaseTics", mobj.chaseTics);
        result.put("justAttacked", mobj.justAttacked);
        result.put("damage", mobj.damage);
        result.put("attackKind", mobj.attackKind);
        result.put("didFire", mobj.didFire);
        return result;
    }

    private static Object copyInfo(Object[] info)
    {
        Object[] copy = new Object[info.length];
        System.arraycopy(info, 0, copy, 0, info.length);
        return copy;
    }

    private static Map<String, Object> record(Object object, String kind, int sector, String[] fields)
    {
        Map<String, Object> rec = new LinkedHashMap<>();
        rec.put("kind", kind);
        rec.put("sector", sector);
        for (String field : fields)
        {
            rec.put(field, thinkerField(object, field));
        }
        return rec;
    }

    private static Object thinkerField(Object object, String field)
    {
        if (object instanceof VerticalDoor)
        {
            VerticalDoor door = (VerticalDoor) object;
            switch (field)
            {
                case "type":
                    return door.type;
                case "direction":
                    return door.direction;
                case "topheight":
                    return door.topheight;
                case "speed":
                    return door.speed;
                case "topwait":
                    return door.topwait;
                case "topcountdown":
                    return door.topcountdown;
                default:
                    return null;
            }
        }
        if (object instanceof Plat)
        {
            Plat plat = (Plat) object;
            switch (field)
            {
                case "type":
                    return plat.type;
                case "status":
                    return plat.status;
                case "speed":
                    return plat.speed;
                case "low":
                    return plat.low;
                case "high":
                    return plat.high;
                case "wait":
                    return plat.wait;
                case "count":
                    return plat.count;
                default:
                    return null;
            }
        }
        if (object instanceof FloorMove)
        {
            FloorMove floor = (FloorMove) object;
            switch (field)
            {
                case "direction":
                    return floor.direction;
                case "dest":
                    return floor.dest;
                case "speed":
                    return floor.speed;
                default:
                    return null;
            }
        }
        return null;
    }

    private static void restoreState(Game game, Map<String, Object> state)
    {
        game.episode = asInt(state.get("episode"));
        game.mapn = asInt(state.get("mapn"));
        game.skill = asInt(state.get("skill"));
        game.leveltime = asInt(state.getOrDefault("leveltime", 0));
        game.totalkills = asInt(state.getOrDefault("totalkills", 0));
        game.totalitems = asInt(state.getOrDefault("totalitems", 0));
        game.totalsecret = asInt(state.getOrDefault("totalsecret", 0));
        game.world = new World();
        game.world.setupLevel(game.wad, game.res, game.episode, game.mapn);
        game.specials = new Specials(game.world, game.res, game.sound);
        World world = game.world;
        restoreSectors(world.sectors, asList(state.get("sectors")));
        restoreSides(world.sides, asList(state.get("sides")));
        restoreLines(world.lines, asList(state.get("lines")));
        for (Sector sector : world.sectors)
        {
            sector.specialdata = null;
        }
        List<Object> thinkers = new ArrayList<>();
        for (Object raw : asList(state.get("thinkers")))
        {
            Map<String, Object> record = asMap(raw);
            if (record == null)
            {
                continue;
            }
            int sectorIndex = asInt(record.get("sector"));
            if (sectorIndex < 0 || sectorIndex >= world.sectors.size())
            {
                continue;
            }
            Sector sector = world.sectors.get(sectorIndex);
            Object thinker = null;
            String kind = asString(record.getOrDefault("kind", ""));
            if ("door".equals(kind))
            {
                thinker = new VerticalDoor(
                    sector,
                    asInt(record.get("type")),
                    asInt(record.get("direction")),
                    asInt(record.get("topheight")),
                    asInt(record.get("speed")),
                    asInt(record.get("topwait")),
                    asInt(record.get("topcountdown"))
                );
            }
            else if ("plat".equals(kind))
            {
                thinker = new Plat(
                    sector,
                    asInt(record.get("type")),
                    asInt(record.get("status")),
                    asInt(record.get("speed")),
                    asInt(record.get("low")),
                    asInt(record.get("high")),
                    asInt(record.get("wait")),
                    asInt(record.get("count"))
                );
            }
            else if ("floor".equals(kind))
            {
                thinker = new FloorMove(sector, asInt(record.get("direction")), asInt(record.get("dest")), asInt(record.get("speed")));
            }
            if (thinker != null)
            {
                sector.specialdata = thinker;
                thinkers.add(thinker);
            }
        }
        game.specials.thinkers = thinkers;
        game.specials.buttons = new ArrayList<>();
        for (Object raw : asList(state.get("buttons")))
        {
            Map<String, Object> record = asMap(raw);
            if (record == null)
            {
                continue;
            }
            int lineIndex = asInt(record.get("line"));
            if (lineIndex >= 0 && lineIndex < world.lines.size())
            {
                Line line = world.lines.get(lineIndex);
                game.specials.buttons.add(new Button(line, asString(record.get("where")), asInt(record.get("texture")), asInt(record.get("timer"))));
            }
        }
        List<Mobj> mobjs = new ArrayList<>();
        List<Object> rawMobjs = asList(state.get("mobjs"));
        for (Object raw : rawMobjs)
        {
            Map<String, Object> record = asMap(raw);
            Mobj mobj = new Mobj();
            if (record != null)
            {
                applyMobj(mobj, record);
            }
            mobjs.add(mobj);
        }
        for (int i = 0; i < mobjs.size(); ++i)
        {
            Map<String, Object> record = i < rawMobjs.size() ? asMap(rawMobjs.get(i)) : null;
            Object target = record != null ? record.get("target") : null;
            if (target instanceof Number)
            {
                int index = ((Number) target).intValue();
                if (index >= 0 && index < mobjs.size())
                {
                    mobjs.get(i).target = mobjs.get(index);
                }
            }
        }
        game.player = null;
        Map<String, Object> playerState = asMap(state.get("player"));
        for (int i = 0; i < mobjs.size(); ++i)
        {
            Map<String, Object> record = i < rawMobjs.size() ? asMap(rawMobjs.get(i)) : null;
            if (record != null && asBool(record.get("isPlayer")))
            {
                Mobj mobj = mobjs.get(i);
                Player player = new Player(mobj);
                if (playerState != null)
                {
                    applyPlayer(player, playerState);
                }
                mobj.player = player;
                mobj.health = player.health;
                game.player = player;
                break;
            }
        }
        if (game.player == null)
        {
            throw new RuntimeException("save has no player");
        }
        world.mobjs = mobjs;
        for (Mobj linked : mobjs) {
            Collision.setThingPosition(world, linked);
        }
        game.gamestate = Defs.GS_LEVEL;
        game.sound.playLevelMusic(game.episode, game.mapn);
    }

    private static void restoreSectors(List<Sector> objects, List<Object> records)
    {
        for (int i = 0; i < records.size(); ++i)
        {
            if (i >= objects.size())
            {
                break;
            }
            Map<String, Object> record = asMap(records.get(i));
            if (record == null)
            {
                continue;
            }
            Sector s = objects.get(i);
            if (record.containsKey("floorheight"))
            {
                s.floorheight = asInt(record.get("floorheight"));
            }
            if (record.containsKey("ceilingheight"))
            {
                s.ceilingheight = asInt(record.get("ceilingheight"));
            }
            if (record.containsKey("floorpic"))
            {
                s.floorpic = asInt(record.get("floorpic"));
            }
            if (record.containsKey("ceilingpic"))
            {
                s.ceilingpic = asInt(record.get("ceilingpic"));
            }
            if (record.containsKey("lightlevel"))
            {
                s.lightlevel = asInt(record.get("lightlevel"));
            }
            if (record.containsKey("special"))
            {
                s.special = asInt(record.get("special"));
            }
        }
    }

    private static void restoreSides(List<Side> objects, List<Object> records)
    {
        for (int i = 0; i < records.size(); ++i)
        {
            if (i >= objects.size())
            {
                break;
            }
            Map<String, Object> record = asMap(records.get(i));
            if (record == null)
            {
                continue;
            }
            Side s = objects.get(i);
            if (record.containsKey("textureoffset"))
            {
                s.textureoffset = asInt(record.get("textureoffset"));
            }
            if (record.containsKey("rowoffset"))
            {
                s.rowoffset = asInt(record.get("rowoffset"));
            }
            if (record.containsKey("toptexture"))
            {
                s.toptexture = asInt(record.get("toptexture"));
            }
            if (record.containsKey("bottomtexture"))
            {
                s.bottomtexture = asInt(record.get("bottomtexture"));
            }
            if (record.containsKey("midtexture"))
            {
                s.midtexture = asInt(record.get("midtexture"));
            }
        }
    }

    private static void restoreLines(List<Line> objects, List<Object> records)
    {
        for (int i = 0; i < records.size(); ++i)
        {
            if (i >= objects.size())
            {
                break;
            }
            Map<String, Object> record = asMap(records.get(i));
            if (record == null)
            {
                continue;
            }
            Line l = objects.get(i);
            if (record.containsKey("flags"))
            {
                l.flags = asInt(record.get("flags"));
            }
            if (record.containsKey("special"))
            {
                l.special = asInt(record.get("special"));
            }
        }
    }

    private static void applyMobj(Mobj mobj, Map<String, Object> record)
    {
        if (record.containsKey("x"))
        {
            mobj.x = asInt(record.get("x"));
        }
        if (record.containsKey("y"))
        {
            mobj.y = asInt(record.get("y"));
        }
        if (record.containsKey("z"))
        {
            mobj.z = asInt(record.get("z"));
        }
        if (record.containsKey("angle"))
        {
            mobj.angle = asInt(record.get("angle"));
        }
        if (record.containsKey("momx"))
        {
            mobj.momx = asInt(record.get("momx"));
        }
        if (record.containsKey("momy"))
        {
            mobj.momy = asInt(record.get("momy"));
        }
        if (record.containsKey("momz"))
        {
            mobj.momz = asInt(record.get("momz"));
        }
        if (record.containsKey("radius"))
        {
            mobj.radius = asInt(record.get("radius"));
        }
        if (record.containsKey("height"))
        {
            mobj.height = asInt(record.get("height"));
        }
        if (record.containsKey("floorz"))
        {
            mobj.floorz = asInt(record.get("floorz"));
        }
        if (record.containsKey("ceilingz"))
        {
            mobj.ceilingz = asInt(record.get("ceilingz"));
        }
        if (record.containsKey("flags"))
        {
            mobj.flags = asInt(record.get("flags"));
        }
        if (record.containsKey("health"))
        {
            mobj.health = asInt(record.get("health"));
        }
        if (record.containsKey("type"))
        {
            mobj.type = asInt(record.get("type"));
        }
        if (record.containsKey("sprite"))
        {
            mobj.sprite = asString(record.get("sprite"));
        }
        if (record.containsKey("alive"))
        {
            mobj.alive = asBool(record.get("alive"));
        }
        if (record.containsKey("reactiontime"))
        {
            mobj.reactiontime = asInt(record.get("reactiontime"));
        }
        if (record.containsKey("movedir"))
        {
            mobj.movedir = asInt(record.get("movedir"));
        }
        if (record.containsKey("movecount"))
        {
            mobj.movecount = asInt(record.get("movecount"));
        }
        if (record.containsKey("aiState"))
        {
            mobj.aiState = asString(record.get("aiState"));
        }
        if (record.containsKey("frame"))
        {
            mobj.frame = asInt(record.get("frame"));
        }
        if (record.containsKey("tics"))
        {
            mobj.tics = asInt(record.get("tics"));
        }
        if (record.containsKey("chaseTics"))
        {
            mobj.chaseTics = asInt(record.get("chaseTics"));
        }
        if (record.containsKey("justAttacked"))
        {
            mobj.justAttacked = asBool(record.get("justAttacked"));
        }
        if (record.containsKey("damage"))
        {
            mobj.damage = asInt(record.get("damage"));
        }
        if (record.containsKey("attackKind"))
        {
            mobj.attackKind = asString(record.get("attackKind"));
        }
        if (record.containsKey("didFire"))
        {
            mobj.didFire = asBool(record.get("didFire"));
        }
        Object info = record.get("info");
        if (info instanceof List || info instanceof Object[])
        {
            mobj.info = toInfoArray(info);
        }
    }

    private static void applyPlayer(Player player, Map<String, Object> record)
    {
        if (record.containsKey("playerstate"))
        {
            player.playerstate = asInt(record.get("playerstate"));
        }
        if (record.containsKey("viewz"))
        {
            player.viewz = asInt(record.get("viewz"));
        }
        if (record.containsKey("viewheight"))
        {
            player.viewheight = asInt(record.get("viewheight"));
        }
        if (record.containsKey("deltaviewheight"))
        {
            player.deltaviewheight = asInt(record.get("deltaviewheight"));
        }
        if (record.containsKey("bob"))
        {
            player.bob = asInt(record.get("bob"));
        }
        if (record.containsKey("health"))
        {
            player.health = asInt(record.get("health"));
        }
        if (record.containsKey("armorpoints"))
        {
            player.armorpoints = asInt(record.get("armorpoints"));
        }
        if (record.containsKey("armortype"))
        {
            player.armortype = asInt(record.get("armortype"));
        }
        if (record.containsKey("ammo"))
        {
            player.ammo = asIntArray(record.get("ammo"));
        }
        if (record.containsKey("maxammo"))
        {
            player.maxammo = asIntArray(record.get("maxammo"));
        }
        if (record.containsKey("weaponowned"))
        {
            player.weaponowned = asBoolArray(record.get("weaponowned"));
        }
        if (record.containsKey("pendingweapon"))
        {
            player.pendingweapon = asInt(record.get("pendingweapon"));
        }
        if (record.containsKey("readyweapon"))
        {
            player.readyweapon = asInt(record.get("readyweapon"));
        }
        if (record.containsKey("cards"))
        {
            player.cards = asBoolArray(record.get("cards"));
        }
        if (record.containsKey("cheats"))
        {
            player.cheats = asInt(record.get("cheats"));
        }
        if (record.containsKey("message"))
        {
            player.message = asString(record.get("message"));
        }
        if (record.containsKey("messageTics"))
        {
            player.messageTics = asInt(record.get("messageTics"));
        }
        if (record.containsKey("attackdown"))
        {
            player.attackdown = asBool(record.get("attackdown"));
        }
        if (record.containsKey("usedown"))
        {
            player.usedown = asBool(record.get("usedown"));
        }
        if (record.containsKey("damagecount"))
        {
            player.damagecount = asInt(record.get("damagecount"));
        }
        if (record.containsKey("bonuscount"))
        {
            player.bonuscount = asInt(record.get("bonuscount"));
        }
        if (record.containsKey("extralight"))
        {
            player.extralight = asInt(record.get("extralight"));
        }
        if (record.containsKey("refire"))
        {
            player.refire = asInt(record.get("refire"));
        }
        if (record.containsKey("killcount"))
        {
            player.killcount = asInt(record.get("killcount"));
        }
        if (record.containsKey("itemcount"))
        {
            player.itemcount = asInt(record.get("itemcount"));
        }
        if (record.containsKey("secretcount"))
        {
            player.secretcount = asInt(record.get("secretcount"));
        }
        if (record.containsKey("didsecret"))
        {
            player.didsecret = asBool(record.get("didsecret"));
        }
        if (record.containsKey("pspriteY"))
        {
            player.pspriteY = asInt(record.get("pspriteY"));
        }
        if (record.containsKey("pspriteSy"))
        {
            player.pspriteSy = asInt(record.get("pspriteSy"));
        }
        if (record.containsKey("pspriteState"))
        {
            player.pspriteState = asString(record.get("pspriteState"));
        }
        if (record.containsKey("pspriteTics"))
        {
            player.pspriteTics = asInt(record.get("pspriteTics"));
        }
        if (record.containsKey("pspriteStep"))
        {
            player.pspriteStep = asInt(record.get("pspriteStep"));
        }
        if (record.containsKey("pspriteBody"))
        {
            player.pspriteBody = asString(record.get("pspriteBody"));
        }
        if (record.containsKey("pspriteFlash"))
        {
            player.pspriteFlash = asString(record.get("pspriteFlash"));
        }
        if (record.containsKey("flashTics"))
        {
            player.flashTics = asInt(record.get("flashTics"));
        }
    }

    private static Object[] toInfoArray(Object value)
    {
        List<Object> list;
        if (value instanceof Object[])
        {
            Object[] arr = (Object[]) value;
            list = new ArrayList<>();
            for (Object item : arr)
            {
                list.add(item);
            }
        }
        else
        {
            list = asList(value);
        }
        Object[] out = new Object[list.size()];
        for (int i = 0; i < list.size(); ++i)
        {
            Object item = list.get(i);
            if (item instanceof List)
            {
                out[i] = asIntArray(item);
            }
            else if (item instanceof int[])
            {
                out[i] = item;
            }
            else
            {
                out[i] = item;
            }
        }
        return out;
    }

    private static int asInt(Object value)
    {
        if (value == null)
        {
            return 0;
        }
        if (value instanceof Number)
        {
            return ((Number) value).intValue();
        }
        if (value instanceof Boolean)
        {
            return ((Boolean) value) ? 1 : 0;
        }
        if (value instanceof String)
        {
            try
            {
                return Integer.parseInt((String) value);
            }
            catch (NumberFormatException e)
            {
                return 0;
            }
        }
        return 0;
    }

    private static boolean asBool(Object value)
    {
        if (value instanceof Boolean)
        {
            return (Boolean) value;
        }
        if (value instanceof Number)
        {
            return ((Number) value).intValue() != 0;
        }
        return false;
    }

    private static String asString(Object value)
    {
        return value == null ? "" : String.valueOf(value);
    }

    private static int[] asIntArray(Object value)
    {
        if (value instanceof int[])
        {
            return (int[]) value;
        }
        List<Object> list = asList(value);
        int[] out = new int[list.size()];
        for (int i = 0; i < list.size(); ++i)
        {
            out[i] = asInt(list.get(i));
        }
        return out;
    }

    private static boolean[] asBoolArray(Object value)
    {
        if (value instanceof boolean[])
        {
            return (boolean[]) value;
        }
        List<Object> list = asList(value);
        boolean[] out = new boolean[list.size()];
        for (int i = 0; i < list.size(); ++i)
        {
            out[i] = asBool(list.get(i));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object value)
    {
        if (value instanceof List)
        {
            return (List<Object>) value;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value)
    {
        if (value instanceof Map)
        {
            return (Map<String, Object>) value;
        }
        return null;
    }

    private static String rtrim(String text)
    {
        int end = text.length();
        while (end > 0 && Character.isWhitespace(text.charAt(end - 1)))
        {
            --end;
        }
        return text.substring(0, end);
    }

    private static String stringify(Object value)
    {
        StringBuilder sb = new StringBuilder();
        writeJson(sb, value);
        return sb.toString();
    }

    private static void writeJson(StringBuilder sb, Object value)
    {
        if (value == null)
        {
            sb.append("null");
            return;
        }
        if (value instanceof Boolean)
        {
            sb.append(((Boolean) value) ? "true" : "false");
            return;
        }
        if (value instanceof Number)
        {
            sb.append(((Number) value).intValue());
            return;
        }
        if (value instanceof String)
        {
            writeJsonString(sb, (String) value);
            return;
        }
        if (value instanceof int[])
        {
            int[] arr = (int[]) value;
            sb.append('[');
            for (int i = 0; i < arr.length; ++i)
            {
                if (i > 0)
                {
                    sb.append(',');
                }
                sb.append(arr[i]);
            }
            sb.append(']');
            return;
        }
        if (value instanceof boolean[])
        {
            boolean[] arr = (boolean[]) value;
            sb.append('[');
            for (int i = 0; i < arr.length; ++i)
            {
                if (i > 0)
                {
                    sb.append(',');
                }
                sb.append(arr[i] ? "true" : "false");
            }
            sb.append(']');
            return;
        }
        if (value instanceof Object[])
        {
            Object[] arr = (Object[]) value;
            sb.append('[');
            for (int i = 0; i < arr.length; ++i)
            {
                if (i > 0)
                {
                    sb.append(',');
                }
                writeJson(sb, arr[i]);
            }
            sb.append(']');
            return;
        }
        if (value instanceof List)
        {
            List<?> list = (List<?>) value;
            sb.append('[');
            for (int i = 0; i < list.size(); ++i)
            {
                if (i > 0)
                {
                    sb.append(',');
                }
                writeJson(sb, list.get(i));
            }
            sb.append(']');
            return;
        }
        if (value instanceof Map)
        {
            Map<?, ?> map = (Map<?, ?>) value;
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet())
            {
                if (!first)
                {
                    sb.append(',');
                }
                first = false;
                writeJsonString(sb, String.valueOf(entry.getKey()));
                sb.append(':');
                writeJson(sb, entry.getValue());
            }
            sb.append('}');
            return;
        }
        writeJsonString(sb, String.valueOf(value));
    }

    private static void writeJsonString(StringBuilder sb, String text)
    {
        sb.append('"');
        for (int i = 0; i < text.length(); ++i)
        {
            char c = text.charAt(i);
            switch (c)
            {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 32)
                    {
                        sb.append(String.format("\\u%04x", (int) c));
                    }
                    else
                    {
                        sb.append(c);
                    }
                    break;
            }
        }
        sb.append('"');
    }

    private static Object parse(String json)
    {
        return new JsonParser(json).parseValue();
    }

    private static final class JsonParser
    {
        private final String json;
        private int pos;

        JsonParser(String json)
        {
            this.json = json;
        }

        Object parseValue()
        {
            skip();
            if (pos >= json.length())
            {
                throw new RuntimeException("empty json");
            }
            char c = json.charAt(pos);
            if (c == '{')
            {
                return parseObject();
            }
            if (c == '[')
            {
                return parseArray();
            }
            if (c == '"')
            {
                return parseString();
            }
            if (c == 't' || c == 'f')
            {
                return parseBoolean();
            }
            if (c == 'n')
            {
                parseNull();
                return null;
            }
            return parseNumber();
        }

        private Map<String, Object> parseObject()
        {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            skip();
            if (peek() == '}')
            {
                ++pos;
                return map;
            }
            while (true)
            {
                skip();
                String key = parseString();
                skip();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skip();
                char c = peek();
                if (c == '}')
                {
                    ++pos;
                    return map;
                }
                expect(',');
            }
        }

        private List<Object> parseArray()
        {
            expect('[');
            List<Object> list = new ArrayList<>();
            skip();
            if (peek() == ']')
            {
                ++pos;
                return list;
            }
            while (true)
            {
                list.add(parseValue());
                skip();
                char c = peek();
                if (c == ']')
                {
                    ++pos;
                    return list;
                }
                expect(',');
            }
        }

        private String parseString()
        {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (pos < json.length())
            {
                char c = json.charAt(pos++);
                if (c == '"')
                {
                    return sb.toString();
                }
                if (c != '\\')
                {
                    sb.append(c);
                    continue;
                }
                if (pos >= json.length())
                {
                    break;
                }
                char e = json.charAt(pos++);
                switch (e)
                {
                    case '"':
                    case '\\':
                    case '/':
                        sb.append(e);
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        if (pos + 4 <= json.length())
                        {
                            int code = Integer.parseInt(json.substring(pos, pos + 4), 16);
                            sb.append((char) code);
                            pos += 4;
                        }
                        break;
                    default:
                        sb.append(e);
                        break;
                }
            }
            throw new RuntimeException("unterminated string");
        }

        private Boolean parseBoolean()
        {
            if (json.startsWith("true", pos))
            {
                pos += 4;
                return true;
            }
            if (json.startsWith("false", pos))
            {
                pos += 5;
                return false;
            }
            throw new RuntimeException("invalid boolean");
        }

        private void parseNull()
        {
            if (!json.startsWith("null", pos))
            {
                throw new RuntimeException("invalid null");
            }
            pos += 4;
        }

        private Number parseNumber()
        {
            int start = pos;
            if (peek() == '-')
            {
                ++pos;
            }
            while (pos < json.length() && Character.isDigit(json.charAt(pos)))
            {
                ++pos;
            }
            boolean frac = false;
            if (pos < json.length() && json.charAt(pos) == '.')
            {
                frac = true;
                ++pos;
                while (pos < json.length() && Character.isDigit(json.charAt(pos)))
                {
                    ++pos;
                }
            }
            if (pos < json.length() && (json.charAt(pos) == 'e' || json.charAt(pos) == 'E'))
            {
                frac = true;
                ++pos;
                if (pos < json.length() && (json.charAt(pos) == '+' || json.charAt(pos) == '-'))
                {
                    ++pos;
                }
                while (pos < json.length() && Character.isDigit(json.charAt(pos)))
                {
                    ++pos;
                }
            }
            String raw = json.substring(start, pos);
            if (!frac)
            {
                long n = Long.parseLong(raw);
                return (int) n;
            }
            return (int) Double.parseDouble(raw);
        }

        private void skip()
        {
            while (pos < json.length())
            {
                char c = json.charAt(pos);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t')
                {
                    ++pos;
                }
                else
                {
                    break;
                }
            }
        }

        private char peek()
        {
            if (pos >= json.length())
            {
                throw new RuntimeException("unexpected end of json");
            }
            return json.charAt(pos);
        }

        private void expect(char c)
        {
            skip();
            if (peek() != c)
            {
                throw new RuntimeException("expected " + c);
            }
            ++pos;
        }
    }
}
