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
import java.util.Map;

public final class Enemy
{
    private static final int DI_EAST = 0;
    private static final int DI_NE = 1;
    private static final int DI_NORTH = 2;
    private static final int DI_NW = 3;
    private static final int DI_WEST = 4;
    private static final int DI_SW = 5;
    private static final int DI_SOUTH = 6;
    private static final int DI_SE = 7;
    private static final int DI_NODIR = 8;
    private static final int[] XSPEED = {
        Defs.FRACUNIT, 47000, 0, -47000, -Defs.FRACUNIT, -47000, 0, 47000,
    };
    private static final int[] YSPEED = {
        0, 47000, Defs.FRACUNIT, 47000, 0, -47000, -Defs.FRACUNIT, -47000,
    };
    private static final int[] OPPOSITE = {4, 5, 6, 7, 0, 1, 2, 3, 8};
    private static final int[] DIAGS = {DI_NW, DI_NE, DI_SW, DI_SE};
    private static final Map<Integer, Profile> PROFILES = new HashMap<>();
    private static int seed = 1;

    static {
        PROFILES.put(3004, new Profile(8, "hitscan", "posit1", "podth1", "pistol", 7, 5, 4));
        PROFILES.put(9, new Profile(8, "shotgun", "posit2", "podth2", "shotgn", 7, 5, 3));
        PROFILES.put(3001, new Profile(8, "imp", "bgsit1", "bgdth1", "claw", 7, 6, 3));
        PROFILES.put(3002, new Profile(10, "melee", "sgtsit", "sgtdth", "sgtatk", 7, 6, 2));
        PROFILES.put(58, new Profile(10, "melee", "sgtsit", "sgtdth", "sgtatk", 7, 6, 2));
        PROFILES.put(3003, new Profile(8, "baron", "brssit", "brsdth", "claw", 7, 7, 3));
        PROFILES.put(3005, new Profile(8, "caco", "cacsit", "cacdth", "claw", 5, 6, 3));
        PROFILES.put(3006, new Profile(8, "melee", "sklatk", "firxpl", "sklatk", 5, 6, 3));
        PROFILES.put(16, new Profile(16, "hitscan", "cybsit", "cybdth", "pistol", 5, 9, 3));
        PROFILES.put(7, new Profile(12, "shotgun", "spisit", "spidth", "shotgn", 5, 10, 3));
        PROFILES.put(68, new Profile(12, "hitscan", "bspsit", "bspdth", "plasma", 5, 7, 3));
        PROFILES.put(69, new Profile(8, "baron", "kntsit", "kntdth", "claw", 7, 7, 3));
        PROFILES.put(84, new Profile(8, "hitscan", "posit1", "podth1", "pistol", 7, 5, 3));
        PROFILES.put(2035, new Profile(0, "none", null, "barexp", null, 0, 5, 4));
    }

    private Enemy()
    {
    }

    private static Profile profile(int type)
    {
        return PROFILES.get(type);
    }

    private static int random()
    {
        seed = (int) (((long) seed * 1103515245L + 12345L) & 0x7fffffffL);
        return (seed >> 16) & 255;
    }

    private static int walkTics(Mobj mo)
    {
        Profile prof = profile(mo.type);
        if (prof == null) {
            return 3;
        }
        return prof.walkTics;
    }

    public static void tickEnemies(World world, Game game)
    {
        Player p = game.player;
        if (p == null || p.mo == null) {
            return;
        }
        for (Mobj mo : new ArrayList<>(world.mobjs)) {
            if (mo == p.mo) {
                continue;
            }
            if ((mo.flags & Defs.MF_MISSILE) != 0) {
                Enemy.tickMissile(world, mo, game);
                continue;
            }
            if (mo.info == null || mo.info.length == 0 || !"enemy".equals(mo.info[0])) {
                continue;
            }
            if (mo.health <= 0) {
                Enemy.tickDead(world, mo);
                continue;
            }
            Profile prof = profile(mo.type);
            if (prof != null && "none".equals(prof.attack)) {
                continue;
            }
            if ("attack".equals(mo.aiState)) {
                Enemy.tickAttack(world, mo, game);
            } else if ("chase".equals(mo.aiState)) {
                if (mo.chaseTics > 0) {
                    mo.chaseTics--;
                } else {
                    Enemy.chase(world, mo, p.mo, game);
                    if ("chase".equals(mo.aiState)) {
                        mo.chaseTics = Math.max(0, Enemy.walkTics(mo) - 1);
                    }
                }
            } else {
                Enemy.tickStand(mo);
                Enemy.look(world, mo, p.mo, game);
            }
        }
    }

    private static void tickStand(Mobj mo)
    {
        if (mo.tics > 0) {
            mo.tics--;
            return;
        }
        mo.frame = (mo.frame + 1) % (mo.type == 3005 ? 1 : 2);
        mo.tics = 10;
    }

    public static void killMonster(Mobj mo, Game game, Mobj source)
    {
        mo.health = 0;
        mo.flags &= ~(Defs.MF_SOLID | Defs.MF_SHOOTABLE);
        mo.flags |= Defs.MF_CORPSE;
        mo.target = null;
        mo.aiState = "die";
        Profile prof = profile(mo.type);
        if (prof != null) {
            String atk = prof.attack;
            String sfx = prof.death;
            int d0 = prof.d0;
            mo.frame = d0;
            mo.tics = 5;
            if ("none".equals(atk)) {
                mo.sprite = "BEXP";
                mo.frame = 0;
            }
            if (sfx != null) {
                game.startSound(sfx);
            }
            if (mo.type == 2035) {
                Enemy.explodeBarrel(game.world, mo, game);
            }
        } else {
            mo.frame = 7;
            mo.tics = 5;
            game.startSound("podth1");
        }
        if (source != null && source.player != null) {
            source.player.killcount++;
        }
    }

    private static void tickDead(World world, Mobj mo)
    {
        if (!"die".equals(mo.aiState)) {
            return;
        }
        if (mo.tics > 0) {
            mo.tics--;
            return;
        }
        Profile prof = profile(mo.type);
        int d0;
        int dn;
        if (prof != null) {
            d0 = prof.d0;
            dn = prof.dn;
        } else {
            d0 = 7;
            dn = 5;
        }
        if ("BEXP".equals(mo.sprite)) {
            d0 = 0;
            dn = 5;
        }
        if (mo.frame + 1 < d0 + dn) {
            mo.frame++;
            mo.tics = 5;
            return;
        }
        mo.aiState = "dead";
        if ("BEXP".equals(mo.sprite)) {
            int i = world.mobjs.indexOf(mo);
            if (i != -1) {
                world.mobjs.remove(i);
            }
        }
    }

    public static void noiseAlert(World world, Mobj emitter)
    {
        noiseAlert(world, emitter, null);
    }

    public static void noiseAlert(World world, Mobj emitter, Game game)
    {
        if (emitter == null) {
            return;
        }
        Sector sec = Collision.pointInSubsector(world, emitter.x, emitter.y).sector;
        world.validcount++;
        Enemy.recursiveSound(world, sec, 0, emitter);
    }

    private static void recursiveSound(World world, Sector sec, int blocks, Mobj target)
    {
        if (sec.validcount == world.validcount && sec.soundtraversed <= blocks + 1) {
            return;
        }
        sec.validcount = world.validcount;
        sec.soundtraversed = blocks + 1;
        sec.soundtarget = target;
        for (Line ln : sec.lines) {
            if ((ln.flags & Defs.ML_TWOSIDED) == 0) {
                continue;
            }
            int[] opening = Collision.lineOpening(ln);
            int top = opening[0];
            int bottom = opening[1];
            if (top - bottom <= 0) {
                continue;
            }
            Sector other = ln.frontsector == sec ? ln.backsector : ln.frontsector;
            if (other == null) {
                continue;
            }
            if ((ln.flags & Defs.ML_SOUNDBLOCK) != 0) {
                if (blocks == 0) {
                    Enemy.recursiveSound(world, other, 1, target);
                }
            } else {
                Enemy.recursiveSound(world, other, blocks, target);
            }
        }
    }

    private static void look(World world, Mobj mo, Mobj player, Game game)
    {
        if ((player.flags & Defs.MF_SHOOTABLE) == 0) {
            return;
        }
        boolean see = false;
        Sector sec = Collision.pointInSubsector(world, mo.x, mo.y).sector;
        Mobj target = sec.soundtarget;
        if (target != null && (target.flags & Defs.MF_SHOOTABLE) != 0) {
            mo.target = target;
            if ((mo.flags & Defs.MF_AMBUSH) != 0) {
                see = Collision.checkSight(world, mo, target);
            } else {
                see = true;
            }
        }
        if (!see && !Enemy.lookForPlayer(world, mo, player)) {
            return;
        }
        if (mo.target == null) {
            mo.target = player;
        }
        mo.aiState = "chase";
        mo.movedir = DI_NODIR;
        mo.movecount = 0;
        Profile prof = profile(mo.type);
        if (prof != null && prof.sit != null) {
            game.startSound(prof.sit);
        }
        mo.frame = 0;
    }

    private static boolean lookForPlayer(World world, Mobj mo, Mobj p)
    {
        if (p.health <= 0 || (p.flags & Defs.MF_SHOOTABLE) == 0 || !Collision.checkSight(world, mo, p)) {
            return false;
        }
        int angle = Compat.asU32(Collision.angleTo(mo.x, mo.y, p.x, p.y) - mo.angle);
        long uAngle = Integer.toUnsignedLong(angle);
        if (uAngle > Integer.toUnsignedLong(Defs.ANG90)
                && uAngle < Integer.toUnsignedLong(Defs.ANG270)
                && Collision.approxDistance(p.x - mo.x, p.y - mo.y) > Defs.MELEERANGE) {
            return false;
        }
        mo.target = p;
        return true;
    }

    private static void chase(World world, Mobj mo, Mobj player, Game game)
    {
        if (mo.reactiontime != 0) {
            mo.reactiontime--;
        }
        Mobj target = mo.target != null ? mo.target : player;
        if (target == null || target.health <= 0 || (target.flags & Defs.MF_SHOOTABLE) == 0) {
            mo.target = null;
            mo.aiState = "look";
            mo.frame = 0;
            mo.tics = 10;
            return;
        }
        mo.target = target;
        if (mo.justAttacked) {
            mo.justAttacked = false;
            Enemy.newChaseDir(world, mo, game);
            return;
        }
        if (mo.movedir == DI_NODIR) {
            Enemy.newChaseDir(world, mo, game);
        }
        Enemy.faceMoveDir(mo);
        int dist = Collision.approxDistance(target.x - mo.x, target.y - mo.y);
        Profile prof = profile(mo.type);
        if (prof == null) {
            prof = new Profile(8, "hitscan", null, null, null, 7, 5, 3);
        }
        int speed = prof.speed;
        String attack = prof.attack;
        String sfx = prof.atk;
        if ("none".equals(attack)) {
            return;
        }
        boolean melee = "melee".equals(attack) || "imp".equals(attack) || "baron".equals(attack) || "caco".equals(attack);
        boolean missile = "hitscan".equals(attack) || "shotgun".equals(attack) || "imp".equals(attack)
                || "baron".equals(attack) || "caco".equals(attack);
        if (melee
                && dist < Defs.MELEERANGE - 20 * Defs.FRACUNIT + target.radius
                && Collision.checkSight(world, mo, target)) {
            if (sfx != null) {
                game.startSound(sfx);
            }
            Enemy.startAttack(mo, "melee");
            return;
        }
        if (missile && mo.movecount == 0 && Enemy.missileOk(world, mo, target, dist, melee)) {
            Enemy.startAttack(mo, attack);
            mo.justAttacked = true;
            return;
        }
        mo.movecount--;
        if (mo.movecount < 0 || !Enemy.move(world, mo, speed, game)) {
            Enemy.newChaseDir(world, mo, game);
        }
        mo.frame = (mo.frame + 1) % 4;
    }

    private static boolean missileOk(World w, Mobj mo, Mobj target, int dist, boolean melee)
    {
        if (!Collision.checkSight(w, mo, target) || mo.reactiontime != 0) {
            return false;
        }
        int d = dist - 64 * Defs.FRACUNIT;
        if (!melee) {
            d -= 128 * Defs.FRACUNIT;
        }
        d >>= 16;
        if (d < 0) {
            return true;
        }
        return Enemy.random() >= Math.min(200, d);
    }

    private static void startAttack(Mobj mo, String kind)
    {
        mo.aiState = "attack";
        mo.tics = 26;
        mo.frame = 4;
        mo.attackKind = kind;
        mo.didFire = false;
    }

    private static void tickAttack(World world, Mobj mo, Game game)
    {
        Mobj t = mo.target;
        if (t == null || t.health <= 0) {
            mo.aiState = "look";
            mo.frame = 0;
            mo.tics = 10;
            return;
        }
        Enemy.faceTarget(mo, t);
        if (!mo.didFire && mo.tics <= 16) {
            Enemy.doAttack(world, mo, game);
            mo.didFire = true;
            mo.frame = 5;
        }
        mo.tics--;
        if (mo.tics <= 0) {
            mo.aiState = "chase";
            mo.frame = 0;
            mo.movecount = 15 + (Enemy.random() & 15);
        }
    }

    private static void doAttack(World world, Mobj mo, Game game)
    {
        String kind = mo.attackKind;
        int saved = mo.angle;
        Enemy.faceTarget(mo, mo.target);
        Mobj target = mo.target;
        if ("melee".equals(kind)) {
            if (Collision.approxDistance(target.x - mo.x, target.y - mo.y) < Defs.MELEERANGE + mo.radius) {
                game.damageMobj(target, mo, ((Enemy.random() % 8) + 1) * 3);
            }
        } else if ("shotgun".equals(kind)) {
            game.startSound("shotgn");
            for (int i = 0; i < 3; i++) {
                mo.angle = Compat.asU32(saved + ((Enemy.random() - Enemy.random()) << 20));
                Collision.lineAttack(world, mo, ((Enemy.random() % 5) + 1) * 3, game, Defs.MISSILERANGE);
            }
            mo.angle = saved;
        } else if ("imp".equals(kind) || "baron".equals(kind) || "caco".equals(kind)) {
            if (Collision.approxDistance(target.x - mo.x, target.y - mo.y) < Defs.MELEERANGE + mo.radius) {
                game.startSound("claw");
                game.damageMobj(target, mo, ((Enemy.random() % 8) + 1) * 3);
            } else {
                game.startSound("firsht");
                Enemy.spawnMissile(
                        world,
                        mo,
                        target,
                        "baron".equals(kind) ? "BAL2" : "BAL1",
                        ("baron".equals(kind) ? 15 : 10) * Defs.FRACUNIT,
                        "baron".equals(kind) ? 8 : 3);
            }
        } else {
            game.startSound("pistol");
            mo.angle = Compat.asU32(saved + ((Enemy.random() - Enemy.random()) << 20));
            Collision.lineAttack(world, mo, ((Enemy.random() % 5) + 1) * 3, game, Defs.MISSILERANGE);
            mo.angle = saved;
        }
    }

    private static void faceTarget(Mobj mo, Mobj t)
    {
        mo.angle = Collision.angleTo(mo.x, mo.y, t.x, t.y);
    }

    private static void faceMoveDir(Mobj mo)
    {
        if (mo.movedir < 0 || mo.movedir >= 8) {
            return;
        }
        int want = Compat.asU32(mo.movedir * Defs.ANG45);
        int delta = Compat.asU32(want - mo.angle);
        if (delta == 0) {
            return;
        }
        int step = Compat.intdiv(Defs.ANG45, 2);
        long uDelta = Integer.toUnsignedLong(delta);
        if (uDelta < 0x80000000L) {
            int add = uDelta < Integer.toUnsignedLong(step) ? (int) uDelta : step;
            mo.angle = Compat.asU32(mo.angle + add);
        } else {
            long wrap = 0x100000000L - uDelta;
            int sub = wrap < Integer.toUnsignedLong(step) ? (int) wrap : step;
            mo.angle = Compat.asU32(mo.angle - sub);
        }
    }

    private static boolean move(World world, Mobj mo, int speed, Game game)
    {
        if (mo.movedir < 0 || mo.movedir >= 8) {
            return false;
        }
        int nx = mo.x + speed * XSPEED[mo.movedir];
        int ny = mo.y + speed * YSPEED[mo.movedir];
        MoveCheck chk = Collision.checkPosition(world, mo, nx, ny);
        if (!Collision.tryMove(world, mo, nx, ny, game)) {
            for (Line ln : chk.spechit) {
                if (ln.special != 0) {
                    game.useSpecial(ln, mo, 0);
                }
            }
            return false;
        }
        mo.z = mo.floorz;
        return true;
    }

    private static void newChaseDir(World world, Mobj mo, Game game)
    {
        Mobj t = mo.target;
        if (t == null) {
            return;
        }
        int old = mo.movedir;
        int turn = (old >= 0 && old < OPPOSITE.length) ? OPPOSITE[old] : DI_NODIR;
        int dx = t.x - mo.x;
        int dy = t.y - mo.y;
        int d2;
        if (dx > 10 * Defs.FRACUNIT) {
            d2 = DI_EAST;
        } else if (dx < -10 * Defs.FRACUNIT) {
            d2 = DI_WEST;
        } else {
            d2 = DI_NODIR;
        }
        int d3;
        if (dy < -10 * Defs.FRACUNIT) {
            d3 = DI_SOUTH;
        } else if (dy > 10 * Defs.FRACUNIT) {
            d3 = DI_NORTH;
        } else {
            d3 = DI_NODIR;
        }
        Profile prof = profile(mo.type);
        int speed = prof != null ? prof.speed : 8;
        if (d2 != DI_NODIR && d3 != DI_NODIR) {
            mo.movedir = DIAGS[(dy < 0 ? 2 : 0) + (dx > 0 ? 1 : 0)];
            if (mo.movedir != turn && Enemy.move(world, mo, speed, game)) {
                mo.movecount = Enemy.random() & 15;
                return;
            }
        }
        if (Enemy.random() > 200 || Math.abs(dy) > Math.abs(dx)) {
            int tmp = d2;
            d2 = d3;
            d3 = tmp;
        }
        if (d2 == turn) {
            d2 = DI_NODIR;
        }
        if (d3 == turn) {
            d3 = DI_NODIR;
        }
        int[] firstTry = {d2, d3, old};
        for (int d : firstTry) {
            if (d != DI_NODIR) {
                mo.movedir = d;
                if (Enemy.move(world, mo, speed, game)) {
                    mo.movecount = Enemy.random() & 15;
                    return;
                }
            }
        }
        int[] dirs;
        if ((Enemy.random() & 1) != 0) {
            dirs = new int[] {0, 1, 2, 3, 4, 5, 6, 7};
        } else {
            dirs = new int[] {7, 6, 5, 4, 3, 2, 1, 0};
        }
        for (int d : dirs) {
            if (d != turn) {
                mo.movedir = d;
                if (Enemy.move(world, mo, speed, game)) {
                    mo.movecount = Enemy.random() & 15;
                    return;
                }
            }
        }
        if (turn != DI_NODIR) {
            mo.movedir = turn;
            if (Enemy.move(world, mo, speed, game)) {
                mo.movecount = Enemy.random() & 15;
                return;
            }
        }
        mo.movedir = DI_NODIR;
        mo.movecount = Enemy.random() & 15;
    }

    private static void spawnMissile(World world, Mobj src, Mobj dest, String sprite, int speed, int damage)
    {
        int a = Collision.angleTo(src.x, src.y, dest.x, dest.y);
        int dist = Collision.approxDistance(dest.x - src.x, dest.y - src.y);
        int steps = Math.max(1, speed != 0 ? Compat.intdiv(dist, speed) : 1);
        Mobj mo = new Mobj();
        mo.x = src.x + Compat.fixedMul(12 * Defs.FRACUNIT, Tables.fineCos(a));
        mo.y = src.y + Compat.fixedMul(12 * Defs.FRACUNIT, Tables.fineSin(a));
        mo.z = src.z + 32 * Defs.FRACUNIT;
        mo.angle = a;
        mo.momx = Compat.fixedMul(speed, Tables.fineCos(a));
        mo.momy = Compat.fixedMul(speed, Tables.fineSin(a));
        mo.momz = (dest.z - src.z) / steps;
        mo.radius = 6 * Defs.FRACUNIT;
        mo.height = 8 * Defs.FRACUNIT;
        mo.floorz = src.floorz;
        mo.ceilingz = src.ceilingz;
        mo.flags = Defs.MF_MISSILE | Defs.MF_DROPOFF | Defs.MF_NOGRAVITY;
        mo.health = 1000;
        mo.type = 0;
        mo.sprite = sprite;
        mo.info = new Object[] {"missile", null};
        mo.damage = damage;
        mo.aiState = "missile";
        mo.target = src;
        mo.x += mo.momx >> 1;
        mo.y += mo.momy >> 1;
        mo.z += mo.momz >> 1;
        world.mobjs.add(mo);
    }

    private static void tickMissile(World world, Mobj mo, Game game)
    {
        int nx = mo.x + mo.momx;
        int ny = mo.y + mo.momy;
        MoveCheck chk = Collision.checkPosition(world, mo, nx, ny);
        if (chk.blocked || !Collision.tryMove(world, mo, nx, ny, game)) {
            Enemy.explodeMissile(world, mo, game, chk.hitThing == mo ? null : chk.hitThing);
            return;
        }
        mo.z += mo.momz;
        if (mo.z <= mo.floorz || mo.z + mo.height > mo.ceilingz) {
            Enemy.explodeMissile(world, mo, game, null);
            return;
        }
        mo.frame = (mo.frame + 1) & 1;
    }

    private static void explodeMissile(World world, Mobj mo, Game game, Mobj hit)
    {
        if (hit != null) {
            Mobj source = mo.target != null ? mo.target : mo;
            game.damageMobj(hit, source, mo.damage * ((Enemy.random() % 8) + 1), mo);
        }
        game.startSound("firxpl");
        int i = world.mobjs.indexOf(mo);
        if (i != -1) {
            world.mobjs.remove(i);
        }
    }

    private static void explodeBarrel(World world, Mobj barrel, Game game)
    {
        for (Mobj o : new ArrayList<>(world.mobjs)) {
            if (o == barrel || (o.flags & Defs.MF_SHOOTABLE) == 0) {
                continue;
            }
            int d = Math.max(0, Collision.approxDistance(o.x - barrel.x, o.y - barrel.y) - o.radius);
            if (d < 128 * Defs.FRACUNIT) {
                game.damageMobj(o, barrel, (128 * Defs.FRACUNIT - d) >> 16);
            }
        }
    }

    private static final class Profile
    {
        final int speed;
        final String attack;
        final String sit;
        final String death;
        final String atk;
        final int d0;
        final int dn;
        final int walkTics;

        Profile(int speed, String attack, String sit, String death, String atk, int d0, int dn, int walkTics)
        {
            this.speed = speed;
            this.attack = attack;
            this.sit = sit;
            this.death = death;
            this.atk = atk;
            this.d0 = d0;
            this.dn = dn;
            this.walkTics = walkTics;
        }
    }
}
