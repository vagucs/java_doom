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
import java.util.List;

public final class Collision
{
    public static boolean floatOk;
    public static int tmFloorZ;
    public static List<Line> lastSpechit = new ArrayList<>();
    public static Line ceilingLine;

    private static boolean earlyOut;
    private static final List<Intercept> intercepts = new ArrayList<>();
    private static final Div trace = new Div(0, 0, 0, 0);
    private static final int INT_MAX = 0x7FFFFFFF;

    private interface ThingFn
    {
        boolean call(Mobj th);
    }

    private interface LineFn
    {
        boolean call(Line ld);
    }

    private interface InterceptFn
    {
        boolean call(Intercept inn);
    }

    private Collision()
    {
    }

    public static void unsetThingPosition(World world, Mobj thing)
    {
        if (!thing.blocklinked) {
            return;
        }
        Mobj nxt = thing.bnext;
        Mobj prev = thing.bprev;
        if (nxt != null) {
            nxt.bprev = prev;
        }
        if (prev != null) {
            prev.bnext = nxt;
        } else if (thing.bindex >= 0 && thing.bindex < world.blocklinks.length && world.blocklinks[thing.bindex] == thing) {
            world.blocklinks[thing.bindex] = nxt;
        }
        thing.bnext = null;
        thing.bprev = null;
        thing.blocklinked = false;
    }

    public static void setThingPosition(World world, Mobj thing)
    {
        thing.bnext = null;
        thing.bprev = null;
        thing.blocklinked = false;
        if ((thing.flags & Defs.MF_NOBLOCKMAP) != 0 || world.blocklinks.length == 0) {
            return;
        }
        int bx = Compat.shar(thing.x - world.bmaporgx, Defs.MAPBLOCKSHIFT);
        int by = Compat.shar(thing.y - world.bmaporgy, Defs.MAPBLOCKSHIFT);
        if (bx < 0 || by < 0 || bx >= world.bmapwidth || by >= world.bmapheight) {
            return;
        }
        int i = by * world.bmapwidth + bx;
        Mobj head = world.blocklinks[i];
        thing.bnext = head;
        if (head != null) {
            head.bprev = thing;
        }
        world.blocklinks[i] = thing;
        thing.bindex = i;
        thing.blocklinked = true;
    }

    public static int pointOnSide(int x, int y, Node node)
    {
        int dx = Compat.asI32(x - node.x);
        int dy = Compat.asI32(y - node.y);
        // PHP/Node keep a wide product; Java int wrap here picks the wrong BSP child
        // (displaced view + false walls). Match the other ports with long.
        long left = (long) Compat.asI32(node.dy >> 16) * dx;
        long right = (long) dy * Compat.asI32(node.dx >> 16);
        return right >= left ? 1 : 0;
    }

    public static Subsector pointInSubsector(World world, int x, int y)
    {
        int num = world.numnodes - 1;
        if (num < 0) {
            return world.subsectors.get(0);
        }
        while ((num & Defs.NF_SUBSECTOR) == 0) {
            Node node = world.nodes.get(num);
            num = node.children[pointOnSide(x, y, node)];
        }
        return world.subsectors.get(num & ~Defs.NF_SUBSECTOR);
    }

    public static int pointOnLineSide(int x, int y, Line line)
    {
        if (line.dx == 0) {
            if (x <= line.v1.x) {
                return line.dy > 0 ? 1 : 0;
            }
            return line.dy < 0 ? 1 : 0;
        }
        if (line.dy == 0) {
            if (y <= line.v1.y) {
                return line.dx < 0 ? 1 : 0;
            }
            return line.dx > 0 ? 1 : 0;
        }
        int dx = Compat.asI32(x - line.v1.x);
        int dy = Compat.asI32(y - line.v1.y);
        int left = Compat.fixedMul(line.dy >> Defs.FRACBITS, dx);
        int right = Compat.fixedMul(dy, line.dx >> Defs.FRACBITS);
        return right < left ? 0 : 1;
    }

    public static int boxOnLineSide(int[] box, Line line)
    {
        int p1;
        int p2;
        if (line.dx == 0) {
            p1 = box[Defs.BOXRIGHT] < line.v1.x ? 0 : 1;
            p2 = box[Defs.BOXLEFT] < line.v1.x ? 0 : 1;
            if (line.dy > 0) {
                p1 ^= 1;
                p2 ^= 1;
            }
        } else if (line.dy == 0) {
            p1 = box[Defs.BOXTOP] > line.v1.y ? 0 : 1;
            p2 = box[Defs.BOXBOTTOM] > line.v1.y ? 0 : 1;
            if (line.dx < 0) {
                p1 ^= 1;
                p2 ^= 1;
            }
        } else if ((line.dy > 0) == (line.dx > 0)) {
            p1 = pointOnLineSide(box[Defs.BOXLEFT], box[Defs.BOXTOP], line);
            p2 = pointOnLineSide(box[Defs.BOXRIGHT], box[Defs.BOXBOTTOM], line);
        } else {
            p1 = pointOnLineSide(box[Defs.BOXRIGHT], box[Defs.BOXTOP], line);
            p2 = pointOnLineSide(box[Defs.BOXLEFT], box[Defs.BOXBOTTOM], line);
        }
        return p1 == p2 ? p1 : -1;
    }

    public static int[] lineOpening(Line line)
    {
        if (line.backsector == null) {
            return new int[] { 0, 0, 0 };
        }
        Sector front = line.frontsector;
        Sector back = line.backsector;
        int top = Math.min(front.ceilingheight, back.ceilingheight);
        if (front.floorheight > back.floorheight) {
            return new int[] { top, front.floorheight, back.floorheight };
        }
        return new int[] { top, back.floorheight, front.floorheight };
    }

    private static boolean blockThings(World world, int x, int y, ThingFn func)
    {
        if (x < 0 || y < 0 || x >= world.bmapwidth || y >= world.bmapheight) {
            return true;
        }
        Mobj mo = world.blocklinks[y * world.bmapwidth + x];
        while (mo != null) {
            Mobj nxt = mo.bnext;
            if (!func.call(mo)) {
                return false;
            }
            mo = nxt;
        }
        return true;
    }

    private static boolean blockLines(World world, int x, int y, LineFn func)
    {
        if (x < 0 || y < 0 || x >= world.bmapwidth || y >= world.bmapheight) {
            return true;
        }
        int offset = world.blockmap[y * world.bmapwidth + x];
        int[] lump = world.blockmapShorts;
        while (offset >= 0 && offset < lump.length) {
            int n = lump[offset];
            offset++;
            if (n == 0xFFFF) {
                return true;
            }
            if (n >= world.lines.size()) {
                continue;
            }
            Line ld = world.lines.get(n);
            if (ld.validcount == world.validcount) {
                continue;
            }
            ld.validcount = world.validcount;
            if (!func.call(ld)) {
                return false;
            }
        }
        return true;
    }

    static boolean sameSpecies(Mobj target, Mobj other)
    {
        if (target.type == other.type) {
            return true;
        }
        if (target.type == Info.MT_KNIGHT && other.type == Info.MT_BRUISER) {
            return true;
        }
        return target.type == Info.MT_BRUISER && other.type == Info.MT_KNIGHT;
    }

    private static boolean pitThing(World world, Mobj tm, Mobj other, Game game)
    {
        if ((other.flags & (Defs.MF_SOLID | Defs.MF_SPECIAL | Defs.MF_SHOOTABLE)) == 0) {
            return true;
        }
        int dist = other.radius + tm.radius;
        if (Math.abs(other.x - tm.tmx) >= dist || Math.abs(other.y - tm.tmy) >= dist || other == tm) {
            return true;
        }
        if ((tm.flags & Defs.MF_SKULLFLY) != 0) {
            if (game != null) {
                game.damageMobj(other, tm, ((Enemy.publicRandom() % 8) + 1) * tm.damage, tm);
            }
            tm.flags &= ~Defs.MF_SKULLFLY;
            tm.momx = tm.momy = tm.momz = 0;
            Thinker.setMobjState(tm, Info.miInt(tm.type, Info.MI_SPAWNSTATE), world, game);
            return false;
        }
        if ((tm.flags & Defs.MF_MISSILE) != 0) {
            Mobj target = tm.target;
            if (target != null && sameSpecies(target, other)) {
                if (other == target) {
                    return true;
                }
                if (other.type != Info.MT_PLAYER) {
                    return false;
                }
            }
            if ((other.flags & Defs.MF_SHOOTABLE) == 0) {
                return (other.flags & Defs.MF_SOLID) == 0;
            }
            if (!Enemy.missileReaches(tm, other, tm.tmx, tm.tmy, tm.z)) {
                return true;
            }
            tm.struck = other;
            return false;
        }
        if ((other.flags & Defs.MF_SPECIAL) != 0) {
            if ((tm.flags & Defs.MF_PICKUP) != 0 && game != null) {
                game.touchSpecial(other, tm);
            }
            return (other.flags & Defs.MF_SOLID) == 0;
        }
        return (other.flags & Defs.MF_SOLID) == 0;
    }

    private static boolean pitLine(Mobj tm, MoveCheck chk, Line ld)
    {
        int[] box = chk.bbox;
        if (box[Defs.BOXRIGHT] <= ld.bbox[Defs.BOXLEFT]
            || box[Defs.BOXLEFT] >= ld.bbox[Defs.BOXRIGHT]
            || box[Defs.BOXTOP] <= ld.bbox[Defs.BOXBOTTOM]
            || box[Defs.BOXBOTTOM] >= ld.bbox[Defs.BOXTOP]) {
            return true;
        }
        if (boxOnLineSide(box, ld) != -1) {
            return true;
        }
        if (ld.backsector == null) {
            return false;
        }
        if ((tm.flags & Defs.MF_MISSILE) == 0) {
            if ((ld.flags & Defs.ML_BLOCKING) != 0) {
                return false;
            }
            if (tm.player == null && (ld.flags & Defs.ML_BLOCKMONSTERS) != 0) {
                return false;
            }
        }
        int[] open = lineOpening(ld);
        if (open[0] < chk.ceilingz) {
            chk.ceilingz = open[0];
            chk.ceilingline = ld;
        }
        if (open[1] > chk.floorz) {
            chk.floorz = open[1];
        }
        if (open[2] < chk.dropoffz) {
            chk.dropoffz = open[2];
        }
        if (ld.special != 0) {
            chk.spechit.add(ld);
        }
        return true;
    }

    public static MoveCheck checkPosition(World world, Mobj thing, int x, int y)
    {
        return checkPosition(world, thing, x, y, null);
    }

    public static MoveCheck checkPosition(World world, Mobj thing, int x, int y, Game game)
    {
        MoveCheck chk = new MoveCheck();
        thing.tmx = x;
        thing.tmy = y;
        int r = thing.radius;
        chk.bbox[Defs.BOXLEFT] = x - r;
        chk.bbox[Defs.BOXRIGHT] = x + r;
        chk.bbox[Defs.BOXBOTTOM] = y - r;
        chk.bbox[Defs.BOXTOP] = y + r;
        Sector sec = pointInSubsector(world, x, y).sector;
        chk.floorz = sec.floorheight;
        chk.dropoffz = sec.floorheight;
        chk.ceilingz = sec.ceilingheight;
        world.validcount++;
        if ((thing.flags & Defs.MF_NOCLIP) != 0 || world.blocklinks.length == 0) {
            return chk;
        }
        int[] box = chk.bbox;
        int orgx = world.bmaporgx;
        int orgy = world.bmaporgy;
        int xl = Compat.shar(box[Defs.BOXLEFT] - orgx - Defs.MAXRADIUS, Defs.MAPBLOCKSHIFT);
        int xh = Compat.shar(box[Defs.BOXRIGHT] - orgx + Defs.MAXRADIUS, Defs.MAPBLOCKSHIFT);
        int yl = Compat.shar(box[Defs.BOXBOTTOM] - orgy - Defs.MAXRADIUS, Defs.MAPBLOCKSHIFT);
        int yh = Compat.shar(box[Defs.BOXTOP] - orgy + Defs.MAXRADIUS, Defs.MAPBLOCKSHIFT);
        for (int bx = xl; bx <= xh; bx++) {
            for (int by = yl; by <= yh; by++) {
                if (!blockThings(world, bx, by, th -> pitThing(world, thing, th, game))) {
                    chk.blocked = true;
                    return chk;
                }
            }
        }
        xl = Compat.shar(box[Defs.BOXLEFT] - orgx, Defs.MAPBLOCKSHIFT);
        xh = Compat.shar(box[Defs.BOXRIGHT] - orgx, Defs.MAPBLOCKSHIFT);
        yl = Compat.shar(box[Defs.BOXBOTTOM] - orgy, Defs.MAPBLOCKSHIFT);
        yh = Compat.shar(box[Defs.BOXTOP] - orgy, Defs.MAPBLOCKSHIFT);
        for (int bx = xl; bx <= xh; bx++) {
            for (int by = yl; by <= yh; by++) {
                if (!blockLines(world, bx, by, ld -> pitLine(thing, chk, ld))) {
                    chk.blocked = true;
                    return chk;
                }
            }
        }
        return chk;
    }

    public static boolean tryMove(World world, Mobj thing, int x, int y)
    {
        return tryMove(world, thing, x, y, null);
    }

    public static boolean tryMove(World world, Mobj thing, int x, int y, Game game)
    {
        floatOk = false;
        ceilingLine = null;
        MoveCheck chk = checkPosition(world, thing, x, y, game);
        lastSpechit = chk.spechit;
        tmFloorZ = chk.floorz;
        ceilingLine = chk.ceilingline;
        if (chk.blocked) {
            return false;
        }
        if ((thing.flags & Defs.MF_NOCLIP) == 0) {
            if (chk.ceilingz - chk.floorz < thing.height) {
                return false;
            }
            floatOk = true;
            if ((thing.flags & Defs.MF_TELEPORT) == 0 && chk.ceilingz - thing.z < thing.height) {
                return false;
            }
            if ((thing.flags & Defs.MF_TELEPORT) == 0 && chk.floorz - thing.z > Defs.MAXSTEP) {
                return false;
            }
            if ((thing.flags & (Defs.MF_DROPOFF | Defs.MF_FLOAT)) == 0 && chk.floorz - chk.dropoffz > Defs.MAXSTEP) {
                return false;
            }
        }
        unsetThingPosition(world, thing);
        int oldx = thing.x;
        int oldy = thing.y;
        thing.floorz = chk.floorz;
        thing.ceilingz = chk.ceilingz;
        thing.x = x;
        thing.y = y;
        setThingPosition(world, thing);
        if (game != null && (thing.flags & (Defs.MF_TELEPORT | Defs.MF_NOCLIP)) == 0) {
            for (int i = chk.spechit.size() - 1; i >= 0; i--) {
                Line ln = chk.spechit.get(i);
                int side = pointOnLineSide(thing.x, thing.y, ln);
                int oldside = pointOnLineSide(oldx, oldy, ln);
                if (side != oldside && ln.special != 0) {
                    game.crossSpecial(ln, oldside, thing);
                }
            }
        }
        return true;
    }

    public static void slideMove(World world, Mobj thing, int momx, int momy)
    {
        slideMove(world, thing, momx, momy, null);
    }

    private static final class SlideBest
    {
        int frac = Defs.FRACUNIT + 1;
        Line line;
    }

    private static Div divTrace()
    {
        return new Div(trace.x, trace.y, trace.dx, trace.dy);
    }

    /** P_PointOnDivlineSide. */
    public static int pointOnDivlineSide(int x, int y, Div line)
    {
        if (line.dx == 0) {
            if (x <= line.x) {
                return line.dy > 0 ? 1 : 0;
            }
            return line.dy < 0 ? 1 : 0;
        }
        if (line.dy == 0) {
            if (y <= line.y) {
                return line.dx < 0 ? 1 : 0;
            }
            return line.dx > 0 ? 1 : 0;
        }
        int dx = x - line.x;
        int dy = y - line.y;
        int xor = line.dy ^ line.dx ^ dx ^ dy;
        if ((xor & 0x80000000) != 0) {
            return ((line.dy ^ dx) & 0x80000000) != 0 ? 1 : 0;
        }
        int left = Compat.fixedMul(Compat.shar(line.dy, 8), Compat.shar(dx, 8));
        int right = Compat.fixedMul(Compat.shar(dy, 8), Compat.shar(line.dx, 8));
        return right < left ? 0 : 1;
    }

    /** P_InterceptVector: frac of v2 along v1. */
    public static int interceptVector(Div v2, Div v1)
    {
        int den = Compat.asI32(
            Compat.fixedMul(Compat.shar(v1.dy, 8), v2.dx) - Compat.fixedMul(Compat.shar(v1.dx, 8), v2.dy));
        if (den == 0) {
            return 0;
        }
        int num = Compat.asI32(
            Compat.fixedMul(Compat.shar(v1.x - v2.x, 8), v1.dy)
                + Compat.fixedMul(Compat.shar(v2.y - v1.y, 8), v1.dx));
        return Compat.fixedDiv(num, den);
    }

    private static boolean addLineIntercept(Line ld)
    {
        int big = 16 * Defs.FRACUNIT;
        int dx = trace.dx;
        int dy = trace.dy;
        int s1;
        int s2;
        if (dx > big || dy > big || dx < -big || dy < -big) {
            Div tr = divTrace();
            s1 = pointOnDivlineSide(ld.v1.x, ld.v1.y, tr);
            s2 = pointOnDivlineSide(ld.v2.x, ld.v2.y, tr);
        } else {
            s1 = pointOnLineSide(trace.x, trace.y, ld);
            s2 = pointOnLineSide(trace.x + dx, trace.y + dy, ld);
        }
        if (s1 == s2) {
            return true;
        }
        int frac = interceptVector(divTrace(), new Div(ld.v1.x, ld.v1.y, ld.dx, ld.dy));
        if (frac < 0) {
            return true;
        }
        if (earlyOut && frac < Defs.FRACUNIT && ld.backsector == null) {
            return false;
        }
        Intercept inn = new Intercept();
        inn.frac = frac;
        inn.isaline = true;
        inn.line = ld;
        intercepts.add(inn);
        return true;
    }

    private static boolean addThingIntercept(Mobj thing)
    {
        Div tr = divTrace();
        boolean positive = (tr.dx ^ tr.dy) > 0;
        int x1;
        int y1;
        int x2;
        int y2;
        if (positive) {
            x1 = thing.x - thing.radius;
            y1 = thing.y + thing.radius;
            x2 = thing.x + thing.radius;
            y2 = thing.y - thing.radius;
        } else {
            x1 = thing.x - thing.radius;
            y1 = thing.y - thing.radius;
            x2 = thing.x + thing.radius;
            y2 = thing.y + thing.radius;
        }
        if (pointOnDivlineSide(x1, y1, tr) == pointOnDivlineSide(x2, y2, tr)) {
            return true;
        }
        int frac = interceptVector(tr, new Div(x1, y1, x2 - x1, y2 - y1));
        if (frac < 0) {
            return true;
        }
        Intercept inn = new Intercept();
        inn.frac = frac;
        inn.isaline = false;
        inn.thing = thing;
        intercepts.add(inn);
        return true;
    }

    private static boolean traverseIntercepts(InterceptFn func, int maxfrac)
    {
        int count = intercepts.size();
        while (count > 0) {
            count--;
            int dist = INT_MAX;
            Intercept chosen = null;
            for (int i = 0; i < intercepts.size(); i++) {
                Intercept scan = intercepts.get(i);
                if (scan.frac < dist) {
                    dist = scan.frac;
                    chosen = scan;
                }
            }
            if (dist > maxfrac) {
                return true;
            }
            if (chosen == null || !func.call(chosen)) {
                return false;
            }
            chosen.frac = INT_MAX;
        }
        return true;
    }

    private static int abs32(int n)
    {
        n = Compat.asI32(n);
        return n < 0 ? -n : n;
    }

    /** P_PathTraverse: blockmap DDA, then intercepts from nearest to farthest. */
    public static boolean pathTraverse(
            World world, int x1, int y1, int x2, int y2, int flags, InterceptFn trav)
    {
        earlyOut = (flags & Defs.PT_EARLYOUT) != 0;
        world.validcount++;
        intercepts.clear();
        int orgx = world.bmaporgx;
        int orgy = world.bmaporgy;
        if (((x1 - orgx) & (Defs.MAPBLOCKSIZE - 1)) == 0) {
            x1 += Defs.FRACUNIT;
        }
        if (((y1 - orgy) & (Defs.MAPBLOCKSIZE - 1)) == 0) {
            y1 += Defs.FRACUNIT;
        }
        trace.x = x1;
        trace.y = y1;
        trace.dx = Compat.asI32(x2 - x1);
        trace.dy = Compat.asI32(y2 - y1);
        x1 = Compat.asI32(x1 - orgx);
        y1 = Compat.asI32(y1 - orgy);
        int xt1 = Compat.shar(x1, Defs.MAPBLOCKSHIFT);
        int yt1 = Compat.shar(y1, Defs.MAPBLOCKSHIFT);
        int x2m = Compat.asI32(x2 - orgx);
        int y2m = Compat.asI32(y2 - orgy);
        int xt2 = Compat.shar(x2m, Defs.MAPBLOCKSHIFT);
        int yt2 = Compat.shar(y2m, Defs.MAPBLOCKSHIFT);
        int mapxstep;
        int partial;
        int ystep;
        if (xt2 > xt1) {
            mapxstep = 1;
            partial = Defs.FRACUNIT - (Compat.shar(x1, Defs.MAPBTOFRAC) & (Defs.FRACUNIT - 1));
            ystep = Compat.fixedDiv(Compat.asI32(y2m - y1), abs32(Compat.asI32(x2m - x1)));
        } else if (xt2 < xt1) {
            mapxstep = -1;
            partial = Compat.shar(x1, Defs.MAPBTOFRAC) & (Defs.FRACUNIT - 1);
            ystep = Compat.fixedDiv(Compat.asI32(y2m - y1), abs32(Compat.asI32(x2m - x1)));
        } else {
            mapxstep = 0;
            partial = Defs.FRACUNIT;
            ystep = 256 * Defs.FRACUNIT;
        }
        int yintercept = Compat.asI32(Compat.shar(y1, Defs.MAPBTOFRAC) + Compat.fixedMul(partial, ystep));
        int mapystep;
        int xstep;
        if (yt2 > yt1) {
            mapystep = 1;
            partial = Defs.FRACUNIT - (Compat.shar(y1, Defs.MAPBTOFRAC) & (Defs.FRACUNIT - 1));
            xstep = Compat.fixedDiv(Compat.asI32(x2m - x1), abs32(Compat.asI32(y2m - y1)));
        } else if (yt2 < yt1) {
            mapystep = -1;
            partial = Compat.shar(y1, Defs.MAPBTOFRAC) & (Defs.FRACUNIT - 1);
            xstep = Compat.fixedDiv(Compat.asI32(x2m - x1), abs32(Compat.asI32(y2m - y1)));
        } else {
            mapystep = 0;
            partial = Defs.FRACUNIT;
            xstep = 256 * Defs.FRACUNIT;
        }
        int xintercept = Compat.asI32(Compat.shar(x1, Defs.MAPBTOFRAC) + Compat.fixedMul(partial, xstep));
        int mapx = xt1;
        int mapy = yt1;
        for (int count = 0; count < 64; count++) {
            if ((flags & Defs.PT_ADDLINES) != 0) {
                if (!blockLines(world, mapx, mapy, Collision::addLineIntercept)) {
                    return false;
                }
            }
            if ((flags & Defs.PT_ADDTHINGS) != 0) {
                if (!blockThings(world, mapx, mapy, Collision::addThingIntercept)) {
                    return false;
                }
            }
            if (mapx == xt2 && mapy == yt2) {
                break;
            }
            if (Compat.shar(yintercept, Defs.FRACBITS) == mapy) {
                yintercept = Compat.asI32(yintercept + ystep);
                mapx += mapxstep;
            } else if (Compat.shar(xintercept, Defs.FRACBITS) == mapx) {
                xintercept = Compat.asI32(xintercept + xstep);
                mapy += mapystep;
            }
        }
        return traverseIntercepts(trav, Defs.FRACUNIT);
    }

    /** A wall the body cannot cross, including a two-sided line flagged blocking. */
    private static boolean slideBlocks(Mobj thing, Line li)
    {
        if (li.v1 == null || li.v2 == null) {
            return false;
        }
        if ((li.flags & Defs.ML_TWOSIDED) == 0 || li.backsector == null || li.frontsector == null) {
            return pointOnLineSide(thing.x, thing.y, li) == 0;
        }
        int[] open = lineOpening(li);
        int opentop = open[0];
        int openbottom = open[1];
        if (opentop - openbottom < thing.height) {
            return true;
        }
        if (opentop - thing.z < thing.height) {
            return true;
        }
        if (openbottom - thing.z > 24 * Defs.FRACUNIT) {
            return true;
        }
        return (li.flags & Defs.ML_BLOCKING) != 0;
    }

    /** Every linedef, including one the body is already touching. The blockmap walk misses that corner. */
    private static void traceSlideCorner(World world, Mobj thing, int x1, int y1, int x2, int y2, SlideBest best)
    {
        for (Line ln : world.lines) {
            Integer frac = interceptFrac(x1, y1, x2, y2, ln);
            if (frac == null || frac < 0 || frac > Defs.FRACUNIT || !slideBlocks(thing, ln)) {
                continue;
            }
            if (frac < best.frac) {
                best.frac = frac;
                best.line = ln;
            }
        }
    }

    /** P_HitSlideLine: keep the part of the move that runs along the wall. */
    private static int[] hitSlideLine(Mobj thing, Line line, int tmx, int tmy)
    {
        if (line.dy == 0) {
            return new int[] { tmx, 0 };
        }
        if (line.dx == 0) {
            return new int[] { 0, tmy };
        }
        int lineangle = angleTo(0, 0, line.dx, line.dy);
        if (pointOnLineSide(thing.x, thing.y, line) == 1) {
            lineangle = Compat.asU32(lineangle + Defs.ANG180);
        }
        int delta = Compat.asU32(angleTo(0, 0, tmx, tmy) - lineangle);
        if (Integer.compareUnsigned(delta, Defs.ANG180) > 0) {
            delta = Compat.asU32(delta + Defs.ANG180);
        }
        int newlen = Compat.fixedMul(approxDistance(tmx, tmy), Tables.fineCos(delta));
        return new int[] {
            Compat.fixedMul(newlen, Tables.fineCos(lineangle)),
            Compat.fixedMul(newlen, Tables.fineSin(lineangle))
        };
    }

    private static void stairstep(World world, Mobj thing, Game game)
    {
        if (!tryMove(world, thing, thing.x, thing.y + thing.momy, game)) {
            tryMove(world, thing, thing.x + thing.momx, thing.y, game);
        }
    }

    /** P_SlideMove: ride the wall instead of dropping the blocked axis. */
    public static void slideMove(World world, Mobj thing, int momx, int momy, Game game)
    {
        if (momx > Defs.MAXMOVE) {
            momx = Defs.MAXMOVE;
        } else if (momx < -Defs.MAXMOVE) {
            momx = -Defs.MAXMOVE;
        }
        if (momy > Defs.MAXMOVE) {
            momy = Defs.MAXMOVE;
        } else if (momy < -Defs.MAXMOVE) {
            momy = -Defs.MAXMOVE;
        }
        thing.momx = momx;
        thing.momy = momy;
        int hitcount = 0;
        while (true) {
            hitcount++;
            if (hitcount == 3) {
                stairstep(world, thing, game);
                return;
            }
            int leadx;
            int trailx;
            if (thing.momx > 0) {
                leadx = thing.x + thing.radius;
                trailx = thing.x - thing.radius;
            } else {
                leadx = thing.x - thing.radius;
                trailx = thing.x + thing.radius;
            }
            int leady;
            int traily;
            if (thing.momy > 0) {
                leady = thing.y + thing.radius;
                traily = thing.y - thing.radius;
            } else {
                leady = thing.y - thing.radius;
                traily = thing.y + thing.radius;
            }
            SlideBest best = new SlideBest();
            int mx = thing.momx;
            int my = thing.momy;
            traceSlideCorner(world, thing, leadx, leady, leadx + mx, leady + my, best);
            traceSlideCorner(world, thing, trailx, leady, trailx + mx, leady + my, best);
            traceSlideCorner(world, thing, leadx, traily, leadx + mx, traily + my, best);
            if (best.frac == Defs.FRACUNIT + 1 || best.line == null) {
                stairstep(world, thing, game);
                return;
            }
            best.frac -= 0x800;
            if (best.frac > 0) {
                int newx = Compat.fixedMul(thing.momx, best.frac);
                int newy = Compat.fixedMul(thing.momy, best.frac);
                if (!tryMove(world, thing, thing.x + newx, thing.y + newy, game)) {
                    stairstep(world, thing, game);
                    return;
                }
            }
            best.frac = Defs.FRACUNIT - (best.frac + 0x800);
            if (best.frac > Defs.FRACUNIT) {
                best.frac = Defs.FRACUNIT;
            }
            if (best.frac <= 0) {
                return;
            }
            int tmx = Compat.fixedMul(thing.momx, best.frac);
            int tmy = Compat.fixedMul(thing.momy, best.frac);
            int[] slid = hitSlideLine(thing, best.line, tmx, tmy);
            thing.momx = slid[0];
            thing.momy = slid[1];
            if (tryMove(world, thing, thing.x + slid[0], thing.y + slid[1], game)) {
                return;
            }
        }
    }

    public static Integer interceptFrac(int x1, int y1, int x2, int y2, Line line)
    {
        double u = Defs.FRACUNIT;
        double ax = x1 / u;
        double ay = y1 / u;
        double bx = x2 / u;
        double by = y2 / u;
        double cx = line.v1.x / u;
        double cy = line.v1.y / u;
        double dx = line.v2.x / u;
        double dy = line.v2.y / u;
        double den = (bx - ax) * (dy - cy) - (by - ay) * (dx - cx);
        if (Math.abs(den) < 1e-8) {
            return null;
        }
        double t = ((cx - ax) * (dy - cy) - (cy - ay) * (dx - cx)) / den;
        double v = ((cx - ax) * (by - ay) - (cy - ay) * (bx - ax)) / den;
        if (t < 0 || t > 1 || v < 0 || v > 1) {
            return null;
        }
        return (int) (t * u);
    }

    /** P_UseLines + PTR_UseTraverse along the blockmap. */
    public static void useLines(World world, Player player, Game game)
    {
        Mobj mo = player.mo;
        int x1 = mo.x;
        int y1 = mo.y;
        int x2 = x1 + Compat.shar(Defs.USERANGE, Defs.FRACBITS) * Tables.fineCos(mo.angle);
        int y2 = y1 + Compat.shar(Defs.USERANGE, Defs.FRACBITS) * Tables.fineSin(mo.angle);
        pathTraverse(world, x1, y1, x2, y2, Defs.PT_ADDLINES, inn -> {
            Line ln = inn.line;
            if (ln.special == 0) {
                int[] op = lineOpening(ln);
                if (op[0] - op[1] <= 0) {
                    if (game != null) {
                        game.startSound("noway");
                    }
                    return false;
                }
                return true;
            }
            int side = pointOnLineSide(mo.x, mo.y, ln) == 1 ? 1 : 0;
            if (game != null) {
                game.useSpecial(ln, mo, side);
            }
            return false;
        });
    }

    private static int[] shotEnds(Mobj source, int angle, int attackrange)
    {
        int x2 = source.x + Compat.shar(attackrange, Defs.FRACBITS) * Tables.fineCos(angle);
        int y2 = source.y + Compat.shar(attackrange, Defs.FRACBITS) * Tables.fineSin(angle);
        int shootz = source.z + (source.height >> 1) + 8 * Defs.FRACUNIT;
        return new int[] { x2, y2, shootz };
    }

    /** PTR_AimTraverse over P_PathTraverse. Returns (aimslope, target). */
    private static Aim aim(World world, Mobj source, int angle, int range)
    {
        int[] ends = shotEnds(source, angle, range);
        int shootz = ends[2];
        int window = (100 * Defs.FRACUNIT) / 160;
        AimState state = new AimState();
        state.top = window;
        state.bottom = -window;
        pathTraverse(world, source.x, source.y, ends[0], ends[1], Defs.PT_ADDLINES | Defs.PT_ADDTHINGS, inn -> {
            if (inn.isaline) {
                Line li = inn.line;
                if ((li.flags & Defs.ML_TWOSIDED) == 0) {
                    return false;
                }
                int[] open = lineOpening(li);
                int opentop = open[0];
                int openbottom = open[1];
                if (openbottom >= opentop) {
                    return false;
                }
                int dist = Compat.fixedMul(range, inn.frac);
                Sector front = li.frontsector;
                Sector back = li.backsector;
                if (back == null || front.floorheight != back.floorheight) {
                    int slope = Compat.fixedDiv(openbottom - shootz, dist);
                    if (slope > state.bottom) {
                        state.bottom = slope;
                    }
                }
                if (back == null || front.ceilingheight != back.ceilingheight) {
                    int slope = Compat.fixedDiv(opentop - shootz, dist);
                    if (slope < state.top) {
                        state.top = slope;
                    }
                }
                return state.top > state.bottom;
            }
            Mobj th = inn.thing;
            if (th == source || (th.flags & Defs.MF_SHOOTABLE) == 0) {
                return true;
            }
            int dist = Compat.fixedMul(range, inn.frac);
            int thingtop = Compat.fixedDiv(th.z + th.height - shootz, dist);
            if (thingtop < state.bottom) {
                return true;
            }
            int thingbot = Compat.fixedDiv(th.z - shootz, dist);
            if (thingbot > state.top) {
                return true;
            }
            if (thingtop > state.top) {
                thingtop = state.top;
            }
            if (thingbot < state.bottom) {
                thingbot = state.bottom;
            }
            state.slope = Math.floorDiv(thingtop + thingbot, 2);
            state.target = th;
            return false;
        });
        if (state.target != null) {
            return new Aim(state.slope, state.target);
        }
        return new Aim(0, null);
    }

    public static int aimSlope(World world, Mobj source, int angle, int range)
    {
        return aim(world, source, angle, range).slope;
    }

    public static final class MissileAim
    {
        public final int angle;
        public final int slope;

        MissileAim(int angle, int slope)
        {
            this.angle = angle;
            this.slope = slope;
        }
    }

    /** P_SpawnPlayerMissile aim: straight, then a step left and right. */
    public static MissileAim missileAim(World world, Mobj source)
    {
        int base = source.angle;
        int span = 16 * 64 * Defs.FRACUNIT;
        int shifted = Compat.asU32(base + (1 << 26));
        int[] angles = new int[] { base, shifted, Compat.asU32(shifted - (2 << 26)) };
        for (int ang : angles) {
            Aim aimed = aim(world, source, ang, span);
            if (aimed.target != null) {
                return new MissileAim(ang, aimed.slope);
            }
        }
        return new MissileAim(base, 0);
    }

    public static int bulletSlope(World world, Mobj source)
    {
        return missileAim(world, source).slope;
    }

    public static boolean lineAttack(World world, Mobj source, int damage, Game game, int range)
    {
        return lineAttack(world, source, damage, game, range, source.angle, null);
    }

    public static boolean lineAttack(World world, Mobj source, int damage, Game game, int range, int angle)
    {
        return lineAttack(world, source, damage, game, range, angle, null);
    }

    public static boolean lineAttack(
            World world, Mobj source, int damage, Game game, int range, int angle, Integer slope)
    {
        int aimslope = slope == null ? aim(world, source, angle, range).slope : slope;
        int[] ends = shotEnds(source, angle, range);
        int shootz = ends[2];
        int sky = game != null && game.res != null ? game.res.skyflatnum : -1;
        boolean[] hit = new boolean[] { false };
        pathTraverse(world, source.x, source.y, ends[0], ends[1], Defs.PT_ADDLINES | Defs.PT_ADDTHINGS,
            inn -> shootTraverse(world, source, damage, game, range, aimslope, shootz, sky, hit, inn));
        return hit[0];
    }

    private static boolean shootTraverse(
            World world, Mobj source, int damage, Game game, int range,
            int aimslope, int shootz, int sky, boolean[] hit, Intercept inn)
    {
        if (inn.isaline) {
            Line li = inn.line;
            if (li.special != 0 && game != null) {
                game.shootSpecial(li, source);
            }
            boolean hitLine = false;
            if ((li.flags & Defs.ML_TWOSIDED) == 0) {
                hitLine = true;
            } else {
                int[] open = lineOpening(li);
                int dist = Compat.fixedMul(range, inn.frac);
                Sector front = li.frontsector;
                Sector back = li.backsector;
                if (back == null) {
                    if (Compat.fixedDiv(open[1] - shootz, dist) > aimslope) {
                        hitLine = true;
                    } else if (Compat.fixedDiv(open[0] - shootz, dist) < aimslope) {
                        hitLine = true;
                    }
                } else {
                    if (front.floorheight != back.floorheight
                            && Compat.fixedDiv(open[1] - shootz, dist) > aimslope) {
                        hitLine = true;
                    }
                    if (!hitLine && front.ceilingheight != back.ceilingheight
                            && Compat.fixedDiv(open[0] - shootz, dist) < aimslope) {
                        hitLine = true;
                    }
                }
            }
            if (!hitLine) {
                return true;
            }
            int frac = inn.frac - Compat.fixedDiv(4 * Defs.FRACUNIT, range);
            int x = trace.x + Compat.fixedMul(trace.dx, frac);
            int y = trace.y + Compat.fixedMul(trace.dy, frac);
            int z = shootz + Compat.fixedMul(aimslope, Compat.fixedMul(frac, range));
            Sector front = li.frontsector;
            if (front != null && front.ceilingpic == sky) {
                if (z > front.ceilingheight) {
                    return false;
                }
                if (li.backsector != null && li.backsector.ceilingpic == sky) {
                    return false;
                }
            }
            spawnPuff(world, x, y, z, game, range);
            return false;
        }
        Mobj th = inn.thing;
        if (th == source || (th.flags & Defs.MF_SHOOTABLE) == 0) {
            return true;
        }
        int dist = Compat.fixedMul(range, inn.frac);
        if (Compat.fixedDiv(th.z + th.height - shootz, dist) < aimslope) {
            return true;
        }
        if (Compat.fixedDiv(th.z - shootz, dist) > aimslope) {
            return true;
        }
        int frac = inn.frac - Compat.fixedDiv(10 * Defs.FRACUNIT, range);
        int x = trace.x + Compat.fixedMul(trace.dx, frac);
        int y = trace.y + Compat.fixedMul(trace.dy, frac);
        int z = shootz + Compat.fixedMul(aimslope, Compat.fixedMul(frac, range));
        if ((th.flags & Defs.MF_NOBLOOD) != 0) {
            spawnPuff(world, x, y, z, game, range);
        } else {
            spawnBlood(world, x, y, z, game, damage);
        }
        if (game != null && damage != 0) {
            game.damageMobj(th, source, damage, source);
        }
        hit[0] = true;
        return false;
    }

    private static void spawnPuff(World world, int x, int y, int z, Game game, int attackrange)
    {
        z += (Enemy.publicRandom() - Enemy.publicRandom()) * 1024;
        Mobj th = Thinker.spawnMobj(world, x, y, z, Info.MT_PUFF, game);
        th.momz = Defs.FRACUNIT;
        th.tics -= Enemy.publicRandom() & 3;
        if (th.tics < 1) {
            th.tics = 1;
        }
        if (attackrange == Defs.MELEERANGE) {
            Thinker.setMobjState(th, Info.S_PUFF3, world, game);
        }
    }

    private static void spawnBlood(World world, int x, int y, int z, Game game, int damage)
    {
        z += (Enemy.publicRandom() - Enemy.publicRandom()) * 1024;
        Mobj th = Thinker.spawnMobj(world, x, y, z, Info.MT_BLOOD, game);
        th.momz = Defs.FRACUNIT * 2;
        th.tics -= Enemy.publicRandom() & 3;
        if (th.tics < 1) {
            th.tics = 1;
        }
        if (damage <= 12 && damage >= 9) {
            Thinker.setMobjState(th, Info.S_BLOOD2, world, game);
        } else if (damage < 9) {
            Thinker.setMobjState(th, Info.S_BLOOD3, world, game);
        }
    }

    public static Mobj aimLineAttack(World world, Mobj source, int angle, int range)
    {
        return aim(world, source, angle, range).target;
    }

    public static boolean checkSight(World world, Mobj a, Mobj b)
    {
        Sector s1 = pointInSubsector(world, a.x, a.y).sector;
        Sector s2 = pointInSubsector(world, b.x, b.y).sector;
        int n = world.sectors.size();
        byte[] rej = world.rejectmatrix;
        if (n != 0 && rej.length != 0) {
            int p = s1.iSector * n + s2.iSector;
            int byteIndex = p >> 3;
            if (byteIndex < rej.length && (Bin.u8(rej, byteIndex) & (1 << (p & 7))) != 0) {
                return false;
            }
        }
        if (s1 == s2) {
            return true;
        }
        for (Line ln : world.lines) {
            if (ln.backsector != null) {
                int[] open = lineOpening(ln);
                int top = open[0];
                int bottom = open[1];
                if (top - bottom > 0) {
                    continue;
                }
            }
            Integer f = interceptFrac(a.x, a.y, b.x, b.y, ln);
            if (f != null && f > Compat.intdiv(Defs.FRACUNIT, 64) && f < Defs.FRACUNIT - Compat.intdiv(Defs.FRACUNIT, 64)) {
                return false;
            }
        }
        return true;
    }

    public static boolean thingHeightClip(World world, Mobj thing)
    {
        boolean onFloor = thing.z == thing.floorz;
        MoveCheck chk = checkPosition(world, thing, thing.x, thing.y);
        thing.floorz = chk.floorz;
        thing.ceilingz = chk.ceilingz;
        if (onFloor) {
            thing.z = thing.floorz;
        } else if (thing.z + thing.height > thing.ceilingz) {
            thing.z = thing.ceilingz - thing.height;
        }
        if (thing.player != null) {
            thing.player.viewz = thing.z + thing.player.viewheight;
        }
        return thing.ceilingz - thing.floorz >= thing.height;
    }

    public static boolean changeSector(World world, Sector sector, boolean crush)
    {
        boolean nofit = false;
        for (Mobj thing : world.mobjs) {
            if (pointInSubsector(world, thing.x, thing.y).sector != sector) {
                continue;
            }
            if (thingHeightClip(world, thing)) {
                continue;
            }
            if (thing.health <= 0) {
                thing.flags &= ~Defs.MF_SOLID;
                thing.height = 0;
                continue;
            }
            if ((thing.flags & Defs.MF_SHOOTABLE) == 0) {
                continue;
            }
            nofit = true;
            if (crush) {
                thing.health -= 10;
                if (thing.player != null) {
                    thing.player.health = thing.health;
                }
                if (thing.health <= 0) {
                    thing.flags &= ~Defs.MF_SOLID;
                    thing.height = 0;
                }
            }
        }
        return nofit;
    }

    public static int approxDistance(int dx, int dy)
    {
        dx = Math.abs(dx);
        dy = Math.abs(dy);
        if (dx < dy) {
            int tmp = dx;
            dx = dy;
            dy = tmp;
        }
        return dx + Compat.intdiv(dy, 2);
    }

    public static int angleTo(int x1, int y1, int x2, int y2)
    {
        Tables.initTables();
        int x = Compat.asI32(x2 - x1);
        int y = Compat.asI32(y2 - y1);
        if (x == 0 && y == 0) {
            return 0;
        }
        int[] ta = Tables.tantoangle;
        if (x >= 0) {
            if (y >= 0) {
                if (x > y) {
                    return ta[Tables.slopeDiv(y, x)];
                }
                return Compat.asU32(Defs.ANG90 - 1 - ta[Tables.slopeDiv(x, y)]);
            }
            y = -y;
            if (x > y) {
                return Compat.asU32(-ta[Tables.slopeDiv(y, x)]);
            }
            return Compat.asU32(0xc0000000 + ta[Tables.slopeDiv(x, y)]);
        }
        x = -x;
        if (y >= 0) {
            if (x > y) {
                return Compat.asU32(Defs.ANG180 - 1 - ta[Tables.slopeDiv(y, x)]);
            }
            return Compat.asU32(Defs.ANG90 + ta[Tables.slopeDiv(x, y)]);
        }
        y = -y;
        if (x > y) {
            return Compat.asU32(Defs.ANG180 + ta[Tables.slopeDiv(y, x)]);
        }
        return Compat.asU32(0xc0000000 - 1 - ta[Tables.slopeDiv(x, y)]);
    }

    private static final class Div
    {
        int x;
        int y;
        int dx;
        int dy;

        Div(int x, int y, int dx, int dy)
        {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
        }
    }

    private static final class Intercept
    {
        int frac;
        boolean isaline;
        Line line;
        Mobj thing;
    }

    private static final class AimState
    {
        int top;
        int bottom;
        int slope;
        Mobj target;
    }

    private static final class Aim
    {
        final int slope;
        final Mobj target;

        Aim(int slope, Mobj target)
        {
            this.slope = slope;
            this.target = target;
        }
    }
}
