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
import java.util.function.ToIntFunction;

public final class Specials
{
    private static final String[][] SWITCH_PAIRS = {
        {"SW1BRCOM", "SW2BRCOM"}, {"SW1BRN1", "SW2BRN1"}, {"SW1BRN2", "SW2BRN2"},
        {"SW1BRNGN", "SW2BRNGN"}, {"SW1BROWN", "SW2BROWN"}, {"SW1COMM", "SW2COMM"},
        {"SW1COMP", "SW2COMP"}, {"SW1DIRT", "SW2DIRT"}, {"SW1EXIT", "SW2EXIT"},
        {"SW1GRAY", "SW2GRAY"}, {"SW1GRAY1", "SW2GRAY1"}, {"SW1METAL", "SW2METAL"},
        {"SW1PIPE", "SW2PIPE"}, {"SW1SLAD", "SW2SLAD"}, {"SW1STARG", "SW2STARG"},
        {"SW1STON1", "SW2STON1"}, {"SW1STON2", "SW2STON2"}, {"SW1STONE", "SW2STONE"},
        {"SW1STRTN", "SW2STRTN"}, {"SW1BLUE", "SW2BLUE"}, {"SW1CMT", "SW2CMT"},
        {"SW1GARG", "SW2GARG"}, {"SW1GSTON", "SW2GSTON"}, {"SW1HOT", "SW2HOT"},
        {"SW1LION", "SW2LION"}, {"SW1SATYR", "SW2SATYR"}, {"SW1SKIN", "SW2SKIN"},
        {"SW1VINE", "SW2VINE"}, {"SW1WOOD", "SW2WOOD"}, {"SW1PANEL", "SW2PANEL"},
        {"SW1ROCK", "SW2ROCK"}, {"SW1MET2", "SW2MET2"}, {"SW1WDMET", "SW2WDMET"},
        {"SW1BRIK", "SW2BRIK"}, {"SW1MOD1", "SW2MOD1"}, {"SW1ZIM", "SW2ZIM"},
        {"SW1STON6", "SW2STON6"}, {"SW1TEK", "SW2TEK"}, {"SW1MARB", "SW2MARB"},
        {"SW1SKULL", "SW2SKULL"},
    };

    public List<Object> thinkers = new ArrayList<>();
    public List<Button> buttons = new ArrayList<>();
    public boolean exitRequested;
    public boolean secretExit;
    public Map<Integer, Integer> switchMap = new HashMap<>();
    public World world;
    public Resources res;
    public Sound sound;

    public Specials(World world, Resources res, Sound sound)
    {
        this.world = world;
        this.res = res;
        this.sound = sound;
        for (String[] pair : SWITCH_PAIRS) {
            int ia = res.textureNumForName(pair[0]);
            int ib = res.textureNumForName(pair[1]);
            if (ia != 0 || ib != 0) {
                switchMap.put(ia, ib);
                switchMap.put(ib, ia);
            }
        }
    }

    private int movePlane(Sector s, int speed, int dest, int plane, int dir)
    {
        return movePlane(s, speed, dest, plane, dir, false);
    }

    private int movePlane(Sector s, int speed, int dest, int plane, int dir, boolean crush)
    {
        int last = plane != 0 ? s.ceilingheight : s.floorheight;
        boolean past = false;
        int next;
        if (dir == -1) {
            if (last - speed < dest) {
                next = dest;
                past = true;
            } else {
                next = last - speed;
            }
        } else if (last + speed > dest) {
            next = dest;
            past = true;
        } else {
            next = last + speed;
        }
        if (plane != 0) {
            s.ceilingheight = next;
        } else {
            s.floorheight = next;
        }
        boolean nofit = Collision.changeSector(world, s, crush);
        if (nofit) {
            if (!crush || past) {
                if (plane != 0) {
                    s.ceilingheight = last;
                } else {
                    s.floorheight = last;
                }
                Collision.changeSector(world, s, crush);
            }
            return past ? Defs.RESULT_PASTDEST : Defs.RESULT_CRUSHED;
        }
        return past ? Defs.RESULT_PASTDEST : Defs.RESULT_OK;
    }

    public static List<Sector> surroundingSectors(Sector s)
    {
        List<Sector> out = new ArrayList<>();
        for (Line ln : s.lines) {
            Sector o = ln.frontsector == s ? ln.backsector : ln.frontsector;
            if (o != null && o != s && !out.contains(o)) {
                out.add(o);
            }
        }
        return out;
    }

    public static int lowestCeiling(Sector s)
    {
        int h = 0x7fffffff;
        for (Sector o : Specials.surroundingSectors(s)) {
            h = Math.min(h, o.ceilingheight);
        }
        return h == 0x7fffffff ? s.ceilingheight : h;
    }

    public static int lowestFloor(Sector s)
    {
        int h = s.floorheight;
        for (Sector o : Specials.surroundingSectors(s)) {
            h = Math.min(h, o.floorheight);
        }
        return h;
    }

    public static int highestFloor(Sector s)
    {
        int h = -500 * Defs.FRACUNIT;
        for (Sector o : Specials.surroundingSectors(s)) {
            h = Math.max(h, o.floorheight);
        }
        return h;
    }

    public static int nextHighestFloor(Sector s, int cur)
    {
        int h = 0x7fffffff;
        for (Sector o : Specials.surroundingSectors(s)) {
            if (o.floorheight > cur) {
                h = Math.min(h, o.floorheight);
            }
        }
        return h == 0x7fffffff ? cur : h;
    }

    public List<Sector> sectorsFromTag(int tag)
    {
        List<Sector> out = new ArrayList<>();
        if (tag == 0) {
            return out;
        }
        for (Sector s : world.sectors) {
            if (s.tag == tag) {
                out.add(s);
            }
        }
        return out;
    }

    public void tick()
    {
        List<Object> alive = new ArrayList<>();
        for (Object t : thinkers) {
            if (thinkerDead(t)) {
                continue;
            }
            if (t instanceof VerticalDoor) {
                tickDoor((VerticalDoor) t);
            } else if (t instanceof Plat) {
                tickPlat((Plat) t);
            } else if (t instanceof FloorMove) {
                tickFloor((FloorMove) t);
            } else if (t instanceof CeilingMove) {
                tickCeiling((CeilingMove) t);
            }
            if (!thinkerDead(t)) {
                alive.add(t);
            }
        }
        thinkers = alive;
        List<Button> remaining = new ArrayList<>();
        for (Button b : buttons) {
            b.timer--;
            if (b.timer <= 0) {
                Side side = b.line.sides[0];
                if (side != null) {
                    if ("top".equals(b.where)) {
                        side.toptexture = b.texture;
                    } else if ("mid".equals(b.where)) {
                        side.midtexture = b.texture;
                    } else if ("bottom".equals(b.where)) {
                        side.bottomtexture = b.texture;
                    }
                }
            } else {
                remaining.add(b);
            }
        }
        buttons = remaining;
    }

    private static boolean thinkerDead(Object t)
    {
        if (t instanceof VerticalDoor) {
            return ((VerticalDoor) t).dead;
        }
        if (t instanceof Plat) {
            return ((Plat) t).dead;
        }
        if (t instanceof FloorMove) {
            return ((FloorMove) t).dead;
        }
        if (t instanceof CeilingMove) {
            return ((CeilingMove) t).dead;
        }
        return true;
    }

    private void tickDoor(VerticalDoor d)
    {
        if (d.direction == 0) {
            d.topcountdown--;
            if (d.topcountdown <= 0) {
                if (d.type == Defs.VLD_NORMAL || d.type == Defs.VLD_BLAZERAISE) {
                    d.direction = -1;
                    sound.play(d.type == Defs.VLD_NORMAL ? "dorcls" : "bdcls");
                } else if (d.type == Defs.VLD_CLOSE30) {
                    d.direction = 1;
                    sound.play("doropn");
                }
            }
            return;
        }
        int dest = d.direction == 1 ? d.topheight : d.sector.floorheight;
        if (movePlane(d.sector, d.speed, dest, 1, d.direction) != Defs.RESULT_PASTDEST) {
            return;
        }
        if (d.direction == 1) {
            if (d.type == Defs.VLD_NORMAL || d.type == Defs.VLD_BLAZERAISE) {
                d.direction = 0;
                d.topcountdown = d.topwait;
            } else {
                d.sector.specialdata = null;
                d.dead = true;
            }
        } else if (d.type == Defs.VLD_CLOSE30) {
            d.direction = 0;
            d.topcountdown = Defs.TICRATE * 30;
        } else {
            d.sector.specialdata = null;
            d.dead = true;
        }
    }

    private void tickPlat(Plat p)
    {
        if (p.status == Defs.PLAT_WAITING) {
            p.count--;
            if (p.count <= 0) {
                p.status = p.sector.floorheight <= p.low ? Defs.PLAT_UP : Defs.PLAT_DOWN;
                sound.play("pstart");
            }
            return;
        }
        boolean up = p.status == Defs.PLAT_UP;
        if (movePlane(p.sector, p.speed, up ? p.high : p.low, 0, up ? 1 : -1) != Defs.RESULT_PASTDEST) {
            return;
        }
        if (!up) {
            p.status = Defs.PLAT_WAITING;
            p.count = p.wait;
            sound.play("pstop");
        } else {
            p.sector.specialdata = null;
            p.dead = true;
            sound.play("pstop");
        }
    }

    private void tickFloor(FloorMove f)
    {
        if (movePlane(f.sector, f.speed, f.dest, 0, f.direction) == Defs.RESULT_PASTDEST) {
            f.sector.specialdata = null;
            f.dead = true;
        }
    }

    private void tickCeiling(CeilingMove c)
    {
        if (movePlane(c.sector, c.speed, c.dest, 1, c.direction) == Defs.RESULT_PASTDEST) {
            c.sector.specialdata = null;
            c.dead = true;
        }
    }

    private boolean spawnDoor(Sector s, int type)
    {
        return spawnDoor(s, type, false);
    }

    private boolean spawnDoor(Sector s, int type, boolean reverse)
    {
        if (s.specialdata != null) {
            if (s.specialdata instanceof VerticalDoor
                    && (type == Defs.VLD_NORMAL || type == Defs.VLD_BLAZERAISE)) {
                VerticalDoor existing = (VerticalDoor) s.specialdata;
                existing.direction = existing.direction == -1 ? 1 : -1;
                return true;
            }
            return false;
        }
        boolean close = type == Defs.VLD_CLOSE || type == Defs.VLD_BLAZECLOSE || type == Defs.VLD_CLOSE30;
        VerticalDoor d = new VerticalDoor(
                s,
                type,
                reverse || close ? -1 : 1,
                Specials.lowestCeiling(s) - 4 * Defs.FRACUNIT,
                Defs.VDOORSPEED * (type >= Defs.VLD_BLAZERAISE ? 4 : 1),
                Defs.VDOORWAIT);
        if (type == Defs.VLD_CLOSE30) {
            d.topheight = s.ceilingheight;
        }
        s.specialdata = d;
        thinkers.add(d);
        String sfx;
        if (d.direction == 1) {
            sfx = type < Defs.VLD_BLAZERAISE ? "doropn" : "bdopn";
        } else {
            sfx = type < Defs.VLD_BLAZERAISE ? "dorcls" : "bdcls";
        }
        sound.play(sfx);
        return true;
    }

    public boolean doDoor(Line line, int type)
    {
        return doDoor(line, type, false);
    }

    public boolean doDoor(Line line, int type, boolean reverse)
    {
        boolean ok = false;
        for (Sector s : sectorsFromTag(line.tag)) {
            if (spawnDoor(s, type, reverse)) {
                ok = true;
            }
        }
        return ok;
    }

    public void verticalDoor(Line line, Mobj thing)
    {
        Player p = thing.player;
        int sp = line.special;
        int[][] locks = {
            {26, 32, Defs.IT_BLUECARD, Defs.IT_BLUESKULL},
            {27, 34, Defs.IT_YELLOWCARD, Defs.IT_YELLOWSKULL},
            {28, 33, Defs.IT_REDCARD, Defs.IT_REDSKULL},
        };
        String[] names = {"blue", "yellow", "red"};
        for (int i = 0; i < locks.length; i++) {
            int a = locks[i][0];
            int b = locks[i][1];
            int card = locks[i][2];
            int skull = locks[i][3];
            if ((sp == a || sp == b) && p != null && !(p.cards[card] || p.cards[skull])) {
                p.message = "You need a " + names[i] + " key to open this door";
                sound.play("oof");
                return;
            }
        }
        Side back = line.sides[1];
        if (back == null || back.sector == null) {
            return;
        }
        Sector s = back.sector;
        int type;
        if (sp == 1 || sp == 26 || sp == 27 || sp == 28) {
            type = Defs.VLD_NORMAL;
        } else if (sp == 31 || sp == 32 || sp == 33 || sp == 34) {
            type = Defs.VLD_OPEN;
            line.special = 0;
        } else if (sp == 117) {
            type = Defs.VLD_BLAZERAISE;
        } else if (sp == 118) {
            type = Defs.VLD_OPEN;
            line.special = 0;
        } else {
            type = Defs.VLD_NORMAL;
        }
        spawnDoor(s, type);
    }

    public boolean doPlatDwus(Line line)
    {
        return doPlatDwus(line, false);
    }

    public boolean doPlatDwus(Line line, boolean blaze)
    {
        boolean ok = false;
        for (Sector s : sectorsFromTag(line.tag)) {
            if (s.specialdata != null) {
                continue;
            }
            Plat p = new Plat(
                    s,
                    blaze ? Defs.PLAT_BLAZEDWUS : Defs.PLAT_DWUS,
                    Defs.PLAT_DOWN,
                    Defs.PLATSPEED * (blaze ? 8 : 1),
                    Specials.lowestFloor(s),
                    s.floorheight,
                    Defs.PLATWAIT * Defs.TICRATE);
            if (p.low == p.high) {
                p.low = p.high - 8 * Defs.FRACUNIT;
            }
            s.specialdata = p;
            thinkers.add(p);
            sound.play("pstart");
            ok = true;
        }
        return ok;
    }

    public boolean doFloor(Line line, ToIntFunction<Sector> dest, int dir)
    {
        boolean ok = false;
        for (Sector s : sectorsFromTag(line.tag)) {
            if (s.specialdata != null) {
                continue;
            }
            FloorMove f = new FloorMove(s, dir, dest.applyAsInt(s), Defs.FLOORSPEED);
            s.specialdata = f;
            thinkers.add(f);
            ok = true;
        }
        return ok;
    }

    public boolean doCeiling(Line line, ToIntFunction<Sector> dest)
    {
        return doCeiling(line, dest, -1, Defs.CEILSPEED);
    }

    public boolean doCeiling(Line line, ToIntFunction<Sector> dest, int dir)
    {
        return doCeiling(line, dest, dir, Defs.CEILSPEED);
    }

    public boolean doCeiling(Line line, ToIntFunction<Sector> dest, int dir, int speed)
    {
        boolean ok = false;
        for (Sector s : sectorsFromTag(line.tag)) {
            if (s.specialdata != null) {
                continue;
            }
            CeilingMove c = new CeilingMove(s, dir, dest.applyAsInt(s), speed);
            s.specialdata = c;
            thinkers.add(c);
            ok = true;
        }
        return ok;
    }

    public boolean doStairs(Line line, int step, int speed)
    {
        boolean ok = false;
        for (Sector s : sectorsFromTag(line.tag)) {
            if (s.specialdata != null) {
                continue;
            }
            int height = s.floorheight + step;
            FloorMove f = new FloorMove(s, 1, height, speed);
            s.specialdata = f;
            thinkers.add(f);
            ok = true;
            int texture = s.floorpic;
            Sector cur = s;
            while (true) {
                Sector next = null;
                for (Line ln : cur.lines) {
                    if ((ln.flags & Defs.ML_TWOSIDED) == 0) {
                        continue;
                    }
                    Sector o = ln.frontsector == cur ? ln.backsector : ln.frontsector;
                    if (o != null && o != cur && o.floorpic == texture && o.specialdata == null) {
                        next = o;
                        break;
                    }
                }
                if (next == null) {
                    break;
                }
                height += step;
                f = new FloorMove(next, 1, height, speed);
                next.specialdata = f;
                thinkers.add(f);
                cur = next;
            }
        }
        return ok;
    }

    public void changeSwitch(Line line, int again)
    {
        Side side = line.sides[0];
        if (side == null) {
            return;
        }
        if (again == 0) {
            line.special = 0;
        }
        String sfx = line.special == 11 ? "swtchx" : "swtchn";
        String[] wheres = {"top", "mid", "bottom"};
        for (String where : wheres) {
            int tex;
            if ("top".equals(where)) {
                tex = side.toptexture;
            } else if ("mid".equals(where)) {
                tex = side.midtexture;
            } else {
                tex = side.bottomtexture;
            }
            if (switchMap.containsKey(tex)) {
                if (again != 0) {
                    buttons.add(new Button(line, where, tex, Defs.BUTTONTIME));
                }
                int mapped = switchMap.get(tex);
                if ("top".equals(where)) {
                    side.toptexture = mapped;
                } else if ("mid".equals(where)) {
                    side.midtexture = mapped;
                } else {
                    side.bottomtexture = mapped;
                }
                sound.play(sfx);
                return;
            }
        }
        sound.play(sfx);
    }

    public void useSpecial(Line line, Mobj thing, int side)
    {
        if (side != 0) {
            return;
        }
        int sp = line.special;
        if (sp == 1 || sp == 26 || sp == 27 || sp == 28 || sp == 31 || sp == 32 || sp == 33 || sp == 34
                || sp == 117 || sp == 118) {
            verticalDoor(line, thing);
            return;
        }
        if (sp == 11 || sp == 51) {
            changeSwitch(line, 0);
            exitRequested = true;
            if (sp == 51) {
                secretExit = true;
            }
            return;
        }
        boolean once = false;
        boolean repeat = false;
        boolean ok = false;
        switch (sp) {
            case 29:
                once = true;
                ok = doDoor(line, Defs.VLD_NORMAL);
                break;
            case 50:
                once = true;
                ok = doDoor(line, Defs.VLD_CLOSE);
                break;
            case 103:
                once = true;
                ok = doDoor(line, Defs.VLD_OPEN);
                break;
            case 111:
                once = true;
                ok = doDoor(line, Defs.VLD_BLAZERAISE);
                break;
            case 112:
                once = true;
                ok = doDoor(line, Defs.VLD_BLAZEOPEN);
                break;
            case 113:
                once = true;
                ok = doDoor(line, Defs.VLD_BLAZECLOSE);
                break;
            case 21:
                once = true;
                ok = doPlatDwus(line);
                break;
            case 122:
                once = true;
                ok = doPlatDwus(line, true);
                break;
            case 18:
                once = true;
                ok = doFloor(line, sector -> Specials.nextHighestFloor(sector, sector.floorheight), 1);
                break;
            case 23:
                once = true;
                ok = doFloor(line, Specials::lowestFloor, -1);
                break;
            case 71:
                once = true;
                ok = doFloor(line, Specials::highestFloor, -1);
                break;
            case 101:
                once = true;
                ok = doFloor(line, sector -> Specials.nextHighestFloor(sector, sector.floorheight), 1);
                break;
            case 102:
                once = true;
                ok = doFloor(line, sector -> sector.floorheight - 8 * Defs.FRACUNIT, -1);
                break;
            case 7:
                once = true;
                ok = doStairs(line, 8 * Defs.FRACUNIT, Compat.intdiv(Defs.FLOORSPEED, 4));
                break;
            case 127:
                once = true;
                ok = doStairs(line, 16 * Defs.FRACUNIT, Defs.FLOORSPEED * 4);
                break;
            case 41:
                once = true;
                ok = doCeiling(line, sector -> sector.floorheight);
                break;
            case 49:
                once = true;
                ok = doCeiling(line, sector -> sector.floorheight + 8 * Defs.FRACUNIT);
                break;
            case 14:
                once = true;
                ok = doPlatDwus(line);
                break;
            case 15:
                once = true;
                ok = doPlatDwus(line);
                break;
            case 20:
                once = true;
                ok = doPlatDwus(line);
                break;
            case 42:
                repeat = true;
                ok = doDoor(line, Defs.VLD_CLOSE);
                break;
            case 61:
                repeat = true;
                ok = doDoor(line, Defs.VLD_OPEN);
                break;
            case 63:
                repeat = true;
                ok = doDoor(line, Defs.VLD_NORMAL);
                break;
            case 62:
                repeat = true;
                ok = doPlatDwus(line);
                break;
            case 114:
                repeat = true;
                ok = doDoor(line, Defs.VLD_BLAZERAISE);
                break;
            case 115:
                repeat = true;
                ok = doDoor(line, Defs.VLD_BLAZEOPEN);
                break;
            case 116:
                repeat = true;
                ok = doDoor(line, Defs.VLD_BLAZECLOSE);
                break;
            case 120:
                repeat = true;
                ok = doPlatDwus(line, true);
                break;
            case 45:
                repeat = true;
                ok = doFloor(line, sector -> sector.floorheight - 8 * Defs.FRACUNIT, -1);
                break;
            case 60:
                repeat = true;
                ok = doFloor(line, Specials::lowestFloor, -1);
                break;
            case 64:
                repeat = true;
                ok = doFloor(line, sector -> Specials.nextHighestFloor(sector, sector.floorheight), 1);
                break;
            case 70:
                repeat = true;
                ok = doFloor(line, Specials::highestFloor, -1);
                break;
            case 43:
                repeat = true;
                ok = doCeiling(line, sector -> sector.floorheight);
                break;
            default:
                break;
        }
        if (once) {
            if (ok) {
                changeSwitch(line, 0);
            }
        } else if (repeat && ok) {
            changeSwitch(line, 1);
        }
    }

    public void crossSpecial(Line line, int side, Mobj thing)
    {
        int sp = line.special;
        boolean clear = true;
        if (sp == 2) {
            doDoor(line, Defs.VLD_OPEN);
        } else if (sp == 3) {
            doDoor(line, Defs.VLD_CLOSE);
        } else if (sp == 4) {
            doDoor(line, Defs.VLD_NORMAL);
        } else if (sp == 5) {
            doFloor(line, sector -> Specials.nextHighestFloor(sector, sector.floorheight), 1);
        } else if (sp == 10) {
            doPlatDwus(line);
        } else if (sp == 16) {
            doDoor(line, Defs.VLD_CLOSE30, true);
        } else if (sp == 19) {
            doFloor(line, sector -> sector.floorheight - 8 * Defs.FRACUNIT, -1);
        } else if (sp == 36) {
            doFloor(line, Specials::highestFloor, -1);
        } else if (sp == 38) {
            doFloor(line, Specials::lowestFloor, -1);
        } else if (sp == 39) {
            teleport(line, side, thing);
            clear = false;
        } else if (sp == 52) {
            exitRequested = true;
            clear = false;
        } else if (sp == 88) {
            doPlatDwus(line);
            clear = false;
        } else if (sp == 86) {
            doDoor(line, Defs.VLD_OPEN);
            clear = false;
        } else if (sp == 90) {
            doDoor(line, Defs.VLD_NORMAL);
            clear = false;
        } else if (sp == 105) {
            doDoor(line, Defs.VLD_BLAZERAISE);
            clear = false;
        } else if (sp == 106) {
            doDoor(line, Defs.VLD_BLAZEOPEN);
            clear = false;
        } else if (sp == 107) {
            doDoor(line, Defs.VLD_BLAZECLOSE);
            clear = false;
        } else if (sp == 120) {
            doPlatDwus(line, true);
            clear = false;
        } else if (sp == 121) {
            doPlatDwus(line, true);
        } else if (sp == 124) {
            exitRequested = true;
            secretExit = true;
            clear = false;
        } else {
            clear = false;
        }
        if (clear) {
            line.special = 0;
        }
    }

    private void teleport(Line line, int side, Mobj thing)
    {
        if (side == 1 || (thing.flags & Defs.MF_MISSILE) != 0) {
            return;
        }
        int tag = line.tag;
        for (int i = 0; i < world.sectors.size(); i++) {
            Sector sector = world.sectors.get(i);
            if (sector.tag != tag) {
                continue;
            }
            for (Mobj dest : world.mobjs) {
                if (dest.type != 14) {
                    continue;
                }
                Sector destSector = Collision.pointInSubsector(world, dest.x, dest.y).sector;
                if (destSector != sector && destSector.iSector != i) {
                    continue;
                }
                thing.momx = 0;
                thing.momy = 0;
                thing.momz = 0;
                thing.x = dest.x;
                thing.y = dest.y;
                Subsector ss = Collision.pointInSubsector(world, thing.x, thing.y);
                thing.floorz = ss.sector.floorheight;
                thing.ceilingz = ss.sector.ceilingheight;
                thing.z = thing.floorz;
                thing.angle = dest.angle;
                if (thing.player != null) {
                    thing.player.viewz = thing.z + thing.player.viewheight;
                    thing.reactiontime = 18;
                }
                sound.play("telept");
                return;
            }
        }
    }
}
