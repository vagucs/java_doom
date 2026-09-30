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
import java.util.Comparator;
import java.util.List;

public final class Collision
{
    private Collision()
    {
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

    private static boolean thingBlocks(Mobj tm, Mobj other)
    {
        if (other == tm || ((tm.flags & Defs.MF_MISSILE) != 0 && other == tm.target)) {
            return false;
        }
        if ((other.flags & (Defs.MF_SOLID | Defs.MF_SPECIAL | Defs.MF_SHOOTABLE)) == 0) {
            return false;
        }
        int dist = other.radius + tm.radius;
        if (Math.abs(other.x - tm.tmx) >= dist || Math.abs(other.y - tm.tmy) >= dist) {
            return false;
        }
        if (tm.z >= other.z + other.height || tm.z + tm.height <= other.z) {
            return false;
        }
        if ((other.flags & Defs.MF_SPECIAL) != 0) {
            if ((tm.flags & Defs.MF_PICKUP) != 0) {
                tm.pickup = other;
            }
            return (other.flags & Defs.MF_SOLID) != 0;
        }
        return (other.flags & Defs.MF_SOLID) != 0;
    }

    public static MoveCheck checkPosition(World world, Mobj thing, int x, int y)
    {
        MoveCheck chk = new MoveCheck();
        thing.tmx = x;
        thing.tmy = y;
        thing.pickup = null;
        int r = thing.radius;
        int[] box = new int[4];
        box[Defs.BOXLEFT] = x - r;
        box[Defs.BOXRIGHT] = x + r;
        box[Defs.BOXBOTTOM] = y - r;
        box[Defs.BOXTOP] = y + r;
        Sector sec = pointInSubsector(world, x, y).sector;
        chk.floorz = sec.floorheight;
        chk.dropoffz = sec.floorheight;
        chk.ceilingz = sec.ceilingheight;
        if ((thing.flags & Defs.MF_NOCLIP) != 0) {
            return chk;
        }
        for (Mobj other : world.mobjs) {
            if (other == thing || !other.alive) {
                continue;
            }
            if (thingBlocks(thing, other)) {
                chk.blocked = true;
                chk.hitThing = other;
                return chk;
            }
            if (thing.pickup != null) {
                break;
            }
        }
        for (Line ln : world.lines) {
            if (box[Defs.BOXRIGHT] <= ln.bbox[Defs.BOXLEFT]
                || box[Defs.BOXLEFT] >= ln.bbox[Defs.BOXRIGHT]
                || box[Defs.BOXTOP] <= ln.bbox[Defs.BOXBOTTOM]
                || box[Defs.BOXBOTTOM] >= ln.bbox[Defs.BOXTOP]) {
                continue;
            }
            if (boxOnLineSide(box, ln) != -1) {
                continue;
            }
            if (ln.backsector == null) {
                chk.blocked = true;
                return chk;
            }
            if ((thing.flags & Defs.MF_MISSILE) == 0) {
                if ((ln.flags & Defs.ML_BLOCKING) != 0) {
                    chk.blocked = true;
                    return chk;
                }
                if (thing.player == null && (ln.flags & Defs.ML_BLOCKMONSTERS) != 0) {
                    chk.blocked = true;
                    return chk;
                }
            }
            int[] open = lineOpening(ln);
            int top = open[0];
            int bottom = open[1];
            int low = open[2];
            chk.ceilingz = Math.min(chk.ceilingz, top);
            chk.floorz = Math.max(chk.floorz, bottom);
            chk.dropoffz = Math.min(chk.dropoffz, low);
            if (ln.special != 0) {
                chk.spechit.add(ln);
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
        MoveCheck chk = checkPosition(world, thing, x, y);
        if (chk.blocked) {
            return false;
        }
        if ((thing.flags & Defs.MF_NOCLIP) == 0) {
            if (chk.ceilingz - chk.floorz < thing.height
                || chk.ceilingz - thing.z < thing.height
                || chk.floorz - thing.z > Defs.MAXSTEP) {
                return false;
            }
            if ((thing.flags & (Defs.MF_DROPOFF | Defs.MF_FLOAT)) == 0 && chk.floorz - chk.dropoffz > Defs.MAXSTEP) {
                return false;
            }
        }
        int oldx = thing.x;
        int oldy = thing.y;
        thing.floorz = chk.floorz;
        thing.ceilingz = chk.ceilingz;
        thing.x = x;
        thing.y = y;
        if (thing.pickup != null && game != null) {
            game.touchSpecial(thing.pickup, thing);
        }
        if (game != null && (thing.flags & Defs.MF_NOCLIP) == 0) {
            for (Line ln : chk.spechit) {
                int side = pointOnLineSide(thing.x, thing.y, ln);
                if (side != pointOnLineSide(oldx, oldy, ln) && ln.special != 0) {
                    game.crossSpecial(ln, pointOnLineSide(oldx, oldy, ln), thing);
                }
            }
        }
        return true;
    }

    public static void slideMove(World world, Mobj thing, int momx, int momy)
    {
        slideMove(world, thing, momx, momy, null);
    }

    public static void slideMove(World world, Mobj thing, int momx, int momy, Game game)
    {
        momx = Math.max(-Defs.MAXMOVE, Math.min(Defs.MAXMOVE, momx));
        momy = Math.max(-Defs.MAXMOVE, Math.min(Defs.MAXMOVE, momy));
        if (tryMove(world, thing, thing.x + momx, thing.y + momy, game)) {
            return;
        }
        tryMove(world, thing, thing.x + momx, thing.y, game);
        tryMove(world, thing, thing.x, thing.y + momy, game);
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

    public static void useLines(World world, Player player, Game game)
    {
        Mobj mo = player.mo;
        int x1 = mo.x;
        int y1 = mo.y;
        int x2 = x1 + Compat.shar(Defs.USERANGE, Defs.FRACBITS) * Tables.fineCos(mo.angle);
        int y2 = y1 + Compat.shar(Defs.USERANGE, Defs.FRACBITS) * Tables.fineSin(mo.angle);
        List<Hit> hits = new ArrayList<>();
        for (Line ln : world.lines) {
            Integer f = interceptFrac(x1, y1, x2, y2, ln);
            if (f != null && f >= 0 && f <= Defs.FRACUNIT) {
                hits.add(new Hit(f, "line", ln, null));
            }
        }
        hits.sort(Comparator.comparingInt(h -> h.frac));
        for (Hit hit : hits) {
            Line ln = hit.line;
            if (ln.special == 0) {
                int[] op = lineOpening(ln);
                if (ln.backsector == null || op[0] - op[1] <= 0) {
                    game.startSound("noway");
                    return;
                }
                continue;
            }
            game.useSpecial(ln, mo, pointOnLineSide(mo.x, mo.y, ln));
            return;
        }
    }

    public static boolean lineAttack(World world, Mobj source, int damage, Game game, int range)
    {
        int x1 = source.x;
        int y1 = source.y;
        int x2 = x1 + Compat.shar(range, Defs.FRACBITS) * Tables.fineCos(source.angle);
        int y2 = y1 + Compat.shar(range, Defs.FRACBITS) * Tables.fineSin(source.angle);
        List<Hit> hits = new ArrayList<>();
        for (Line ln : world.lines) {
            Integer f = interceptFrac(x1, y1, x2, y2, ln);
            if (f == null || f <= 0 || f > Defs.FRACUNIT) {
                continue;
            }
            // Harbour PTR_ShootTraverse: one-sided walls always stop the shot.
            // Two-sided lines (grates/bars) only stop it when the opening is closed.
            // ML_BLOCKING blocks walking, not hitscan — same as vanilla/Harbour.
            boolean solid = ln.backsector == null;
            if (!solid) {
                int[] open = lineOpening(ln);
                int top = open[0];
                int bottom = open[1];
                solid = top - bottom <= 32 * Defs.FRACUNIT;
            }
            if (solid) {
                hits.add(new Hit(f, "line", ln, null));
            }
        }
        for (Mobj other : world.mobjs) {
            if (other == source || (other.flags & Defs.MF_SHOOTABLE) == 0) {
                continue;
            }
            Integer f = thingHitFrac(x1, y1, x2, y2, other);
            if (f != null) {
                hits.add(new Hit(f, "thing", null, other));
            }
        }
        hits.sort(Comparator.comparingInt(h -> h.frac));
        for (Hit hit : hits) {
            if ("thing".equals(hit.kind)) {
                if (game != null) {
                    game.damageMobj(hit.thing, source, damage);
                }
                return true;
            }
            Line ln = hit.line;
            if (ln.special == 46 && game != null) {
                game.useSpecial(ln, source, 0);
            }
            return false;
        }
        return false;
    }

    private static Integer thingHitFrac(int x1, int y1, int x2, int y2, Mobj mo)
    {
        double vx = (double) x2 - (double) x1;
        double vy = (double) y2 - (double) y1;
        double wx = (double) mo.x - (double) x1;
        double wy = (double) mo.y - (double) y1;
        double den = vx * vx + vy * vy;
        if (den <= 0.0) {
            return null;
        }
        double tn = wx * vx + wy * vy;
        if (tn < 0.0 || tn > den) {
            return null;
        }
        int px = (int) (x1 + (tn * vx) / den);
        int py = (int) (y1 + (tn * vy) / den);
        if (approxDistance(mo.x - px, mo.y - py) > mo.radius + 4 * Defs.FRACUNIT) {
            return null;
        }
        return (int) ((tn * Defs.FRACUNIT) / den);
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

    private static final class Hit
    {
        final int frac;
        final String kind;
        final Line line;
        final Mobj thing;

        Hit(int frac, String kind, Line line, Mobj thing)
        {
            this.frac = frac;
            this.kind = kind;
            this.line = line;
            this.thing = thing;
        }
    }
}
