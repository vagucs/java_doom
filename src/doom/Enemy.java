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
    private static final int[] RNDTABLE = {
        0, 8, 109, 220, 222, 241, 149, 107, 75, 248, 254, 140, 16, 66,
        74, 21, 211, 47, 80, 242, 154, 27, 205, 128, 161, 89, 77, 36,
        95, 110, 85, 48, 212, 140, 211, 249, 22, 79, 200, 50, 28, 188,
        52, 140, 202, 120, 68, 145, 62, 70, 184, 190, 91, 197, 152, 224,
        149, 104, 25, 178, 252, 182, 202, 182, 141, 197, 4, 81, 181, 242,
        145, 42, 39, 227, 156, 198, 225, 193, 219, 93, 122, 175, 249, 0,
        175, 143, 70, 239, 46, 246, 163, 53, 163, 109, 168, 135, 2, 235,
        25, 92, 20, 145, 138, 77, 69, 166, 78, 176, 173, 212, 166, 113,
        94, 161, 41, 50, 239, 49, 111, 164, 70, 60, 2, 37, 171, 75,
        136, 156, 11, 56, 42, 146, 138, 229, 73, 146, 77, 61, 98, 196,
        135, 106, 63, 197, 195, 86, 96, 203, 113, 101, 170, 247, 181, 113,
        80, 250, 108, 7, 255, 237, 129, 226, 79, 107, 112, 166, 103, 241,
        24, 223, 239, 120, 198, 58, 60, 82, 128, 3, 184, 66, 143, 224,
        145, 224, 81, 206, 163, 45, 63, 90, 168, 114, 59, 33, 159, 95,
        28, 139, 123, 98, 125, 196, 15, 70, 194, 253, 54, 14, 109, 226,
        71, 17, 161, 93, 186, 87, 244, 138, 20, 52, 123, 251, 26, 36,
        17, 46, 52, 231, 232, 76, 31, 221, 84, 37, 216, 165, 212, 106,
        197, 242, 98, 43, 39, 175, 254, 145, 190, 84, 118, 222, 187, 136,
        120, 163, 236, 249,
    };
    private static final Map<Integer, Profile> PROFILES = new HashMap<>();
    private static int prndindex;

    static {
        PROFILES.put(3004, new Profile(8, "hitscan", "posit1", "podth1", "pistol", 7, 5, 4));
        PROFILES.put(9, new Profile(8, "shotgun", "posit2", "podth2", "shotgn", 7, 5, 3));
        PROFILES.put(3001, new Profile(8, "imp", "bgsit1", "bgdth1", "claw", 7, 6, 3));
        PROFILES.put(3002, new Profile(10, "melee", "sgtsit", "sgtdth", "sgtatk", 7, 6, 2));
        PROFILES.put(58, new Profile(10, "melee", "sgtsit", "sgtdth", "sgtatk", 7, 6, 2));
        PROFILES.put(3003, new Profile(8, "baron", "brssit", "brsdth", "claw", 7, 7, 3));
        PROFILES.put(3005, new Profile(8, "caco", "cacsit", "cacdth", "claw", 5, 6, 3));
        PROFILES.put(3006, new Profile(8, "skull", "sklatk", "firxpl", "sklatk", 5, 6, 3));
        PROFILES.put(16, new Profile(16, "rocket", "cybsit", "cybdth", "rlaunc", 5, 9, 3));
        PROFILES.put(7, new Profile(12, "spider", "spisit", "spidth", "shotgn", 5, 10, 3));
        PROFILES.put(68, new Profile(12, "plasma", "bspsit", "bspdth", "plasma", 5, 7, 3));
        PROFILES.put(65, new Profile(8, "chaingun", "posit2", "podth2", "shotgn", 7, 7, 3));
        PROFILES.put(69, new Profile(8, "baron", "kntsit", "kntdth", "claw", 7, 7, 3));
        PROFILES.put(84, new Profile(8, "hitscan", "posit1", "podth1", "pistol", 7, 5, 3));
        PROFILES.put(64, new Profile(15, "vile", "vilsit", "vildth", "vilatk", 7, 9, 2));
        PROFILES.put(66, new Profile(10, "revenant", "skesit", "skedth", "skeswg", 7, 5, 2));
        PROFILES.put(67, new Profile(8, "mancubus", "mansit", "mandth", "manatk", 7, 7, 3));
        PROFILES.put(71, new Profile(8, "pain", "pesit", "pedth", "pesit", 5, 6, 3));
        PROFILES.put(72, new Profile(0, "keen", "keenpn", "keendt", null, 0, 6, 4));
        PROFILES.put(88, new Profile(0, "brain", "bossit", "bosdth", null, 0, 3, 4));
        PROFILES.put(2035, new Profile(0, "none", null, "barexp", null, 0, 5, 4));
    }

    private static final int TRACEANGLE = 0xC000000;
    private static int brainTargetOn;

    private Enemy()
    {
    }

    private static Profile profile(int type)
    {
        return PROFILES.get(type);
    }

    public static void clearRandom()
    {
        prndindex = 0;
    }

    static int random()
    {
        prndindex = (prndindex + 1) & 255;
        return RNDTABLE[prndindex];
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
            if (mo != p.mo)
                Thinker.mobjThinker(world, mo, game);
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
        mo.flags &= ~(Defs.MF_SHOOTABLE | Defs.MF_FLOAT | Defs.MF_SKULLFLY);
        mo.flags |= Defs.MF_CORPSE | Defs.MF_DROPOFF;
        mo.height >>= 2;
        if (source != null && source.player != null && (mo.flags & Defs.MF_COUNTKILL) != 0) {
            source.player.killcount++;
        }
        int xdeath = Info.miInt(mo.type, Info.MI_XDEATHSTATE);
        int state = mo.health < -Info.miInt(mo.type, Info.MI_SPAWNHEALTH) && xdeath != Info.S_NULL
                ? xdeath : Info.miInt(mo.type, Info.MI_DEATHSTATE);
        Thinker.setMobjState(mo, state, game.world, game);
        if (mo.alive) {
            mo.tics -= Enemy.random() & 3;
            if (mo.tics < 1)
                mo.tics = 1;
            int drop = -1;
            if (mo.type == Info.MT_POSSESSED)
                drop = Info.MT_CLIP;
            else if (mo.type == Info.MT_SHOTGUY)
                drop = Info.MT_SHOTGUN;
            else if (mo.type == Info.MT_CHAINGUY)
                drop = Info.MT_CHAINGUN;
            if (drop >= 0) {
                Mobj item = Thinker.spawnMobj(game.world, mo.x, mo.y, Thinker.ONFLOORZ, drop, game);
                item.flags |= Defs.MF_DROPPED;
            }
        }
    }

    private static void tickDead(World world, Mobj mo, Game game)
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
        if (mo.type == Info.MT_KEEN) {
            Enemy.keenDie(world, mo, game);
        } else if (mo.type == Info.MT_FATSO || mo.type == Info.MT_BABY
                || mo.type == Info.MT_BRUISER || mo.type == Info.MT_CYBORG
                || mo.type == Info.MT_SPIDER) {
            Enemy.bossDeath(world, mo, game);
        }
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
        mo.threshold = 0;
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
        if (!see && !Enemy.lookForPlayer(world, mo, player, false)) {
            return;
        }
        mo.movedir = DI_NODIR;
        mo.movecount = 0;
        String seeSound = Info.miStr(mo.type, Info.MI_SEESOUND);
        if ("posit1".equals(seeSound) || "posit2".equals(seeSound) || "posit3".equals(seeSound)) {
            seeSound = new String[] {"posit1", "posit2", "posit3"}[Enemy.random() % 3];
        } else if ("bgsit1".equals(seeSound) || "bgsit2".equals(seeSound)) {
            seeSound = new String[] {"bgsit1", "bgsit2"}[Enemy.random() % 2];
        }
        if (!seeSound.isEmpty())
            game.startSound(seeSound);
        Thinker.setMobjState(mo, Info.miInt(mo.type, Info.MI_SEESTATE), world, game);
    }

    private static boolean lookForPlayer(World world, Mobj mo, Mobj p, boolean allaround)
    {
        if (p == null || p.health <= 0) {
            return false;
        }
        int c = 0;
        int stop = (mo.lastlook - 1) & 3;
        while (true) {
            if (mo.lastlook != 0) {
                mo.lastlook = (mo.lastlook + 1) & 3;
                continue;
            }
            if (c == 2 || mo.lastlook == stop) {
                return false;
            }
            c++;
            if (p.health <= 0 || (p.flags & Defs.MF_SHOOTABLE) == 0) {
                mo.lastlook = (mo.lastlook + 1) & 3;
                continue;
            }
            if (!Collision.checkSight(world, mo, p)) {
                mo.lastlook = (mo.lastlook + 1) & 3;
                continue;
            }
            if (!allaround) {
                int angle = Compat.asU32(Collision.angleTo(mo.x, mo.y, p.x, p.y) - mo.angle);
                long uAngle = Integer.toUnsignedLong(angle);
                if (uAngle > Integer.toUnsignedLong(Defs.ANG90)
                        && uAngle < Integer.toUnsignedLong(Defs.ANG270)
                        && Collision.approxDistance(p.x - mo.x, p.y - mo.y) > Defs.MELEERANGE) {
                    mo.lastlook = (mo.lastlook + 1) & 3;
                    continue;
                }
            }
            mo.target = p;
            return true;
        }
    }

    private static void chase(World world, Mobj mo, Mobj player, Game game)
    {
        if (mo.reactiontime != 0) {
            mo.reactiontime--;
        }
        if (mo.threshold != 0) {
            if (mo.target == null || mo.target.health <= 0) {
                mo.threshold = 0;
            } else {
                mo.threshold--;
            }
        }
        if (mo.movedir < 8) {
            Enemy.faceMoveDir(mo);
        }
        Mobj target = mo.target;
        if (target == null || target.health <= 0 || (target.flags & Defs.MF_SHOOTABLE) == 0) {
            if (!Enemy.lookForPlayer(world, mo, player, true)) {
                Thinker.setMobjState(mo, Info.miInt(mo.type, Info.MI_SPAWNSTATE), world, game);
            }
            return;
        }
        if ((mo.flags & Defs.MF_JUSTATTACKED) != 0) {
            mo.flags &= ~Defs.MF_JUSTATTACKED;
            if (game.skill != Defs.SK_NIGHTMARE && !game.fastparm)
                Enemy.newChaseDir(world, mo, game);
            return;
        }
        int dist = Collision.approxDistance(target.x - mo.x, target.y - mo.y);
        int speed = Info.miInt(mo.type, Info.MI_SPEED);
        int meleeState = Info.miInt(mo.type, Info.MI_MELEESTATE);
        int missileState = Info.miInt(mo.type, Info.MI_MISSILESTATE);
        if (meleeState != Info.S_NULL
                && dist < Defs.MELEERANGE - 20 * Defs.FRACUNIT + target.radius
                && Collision.checkSight(world, mo, target)) {
            String attackSound = Info.miStr(mo.type, Info.MI_ATTACKSOUND);
            if (!attackSound.isEmpty())
                game.startSound(attackSound);
            Thinker.setMobjState(mo, meleeState, world, game);
            return;
        }
        if (missileState != Info.S_NULL
                && (game.skill == Defs.SK_NIGHTMARE || game.fastparm || mo.movecount == 0)
                && Enemy.missileOk(world, mo, target, dist, meleeState != Info.S_NULL)) {
            Thinker.setMobjState(mo, missileState, world, game);
            mo.flags |= Defs.MF_JUSTATTACKED;
            return;
        }
        if (--mo.movecount < 0 || !Enemy.move(world, mo, speed, game)) {
            Enemy.newChaseDir(world, mo, game);
        }
        String activeSound = Info.miStr(mo.type, Info.MI_ACTIVESOUND);
        if (!activeSound.isEmpty() && Enemy.random() < 3)
            game.startSound(activeSound);
    }

    private static boolean missileOk(World w, Mobj mo, Mobj target, int dist, boolean melee)
    {
        if ((mo.flags & Defs.MF_JUSTHIT) != 0) {
            mo.flags &= ~Defs.MF_JUSTHIT;
            return true;
        }
        if (!Collision.checkSight(w, mo, target) || mo.reactiontime != 0) {
            return false;
        }
        int d = dist - 64 * Defs.FRACUNIT;
        if (!melee) {
            d -= 128 * Defs.FRACUNIT;
        }
        d >>= 16;
        if (mo.type == Info.MT_VILE && d > 14 * 64)
            return false;
        if (mo.type == Info.MT_UNDEAD) {
            if (d < 196)
                return false;
            d >>= 1;
        }
        if (mo.type == Info.MT_CYBORG || mo.type == Info.MT_SPIDER || mo.type == Info.MT_SKULL)
            d >>= 1;
        if (d > 200)
            d = 200;
        if (mo.type == Info.MT_CYBORG && d > 160)
            d = 160;
        return Enemy.random() >= d;
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
            String kind = mo.attackKind;
            if (("chaingun".equals(kind) || "spider".equals(kind))
                    && Enemy.refireOk(world, mo, "chaingun".equals(kind) ? 40 : 10)) {
                mo.tics = 8;
                mo.didFire = false;
                return;
            }
            mo.aiState = "chase";
            mo.frame = 0;
            mo.movecount = 15 + (Enemy.random() & 15);
        }
    }

    private static void doAttack(World world, Mobj mo, Game game)
    {
        String kind = mo.attackKind;
        Enemy.faceTarget(mo, mo.target);
        Mobj target = mo.target;
        if ("melee".equals(kind)) {
            if (Collision.approxDistance(target.x - mo.x, target.y - mo.y) < Defs.MELEERANGE + mo.radius) {
                game.damageMobj(target, mo, ((Enemy.random() % 8) + 1) * 3);
            }
        } else if ("shotgun".equals(kind)) {
            game.startSound("shotgn");
            int faced = mo.angle;
            int slope = Collision.aimSlope(world, mo, faced, Defs.MISSILERANGE);
            for (int i = 0; i < 3; i++) {
                mo.angle = Compat.asU32(faced + ((Enemy.random() - Enemy.random()) << 20));
                Collision.lineAttack(world, mo, ((Enemy.random() % 5) + 1) * 3, game, Defs.MISSILERANGE, mo.angle, slope);
            }
            mo.angle = faced;
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
                        "baron".equals(kind) ? 8 : 3,
                        "ball");
            }
        } else if ("rocket".equals(kind)) {
            game.startSound("rlaunc");
            Enemy.spawnMissile(world, mo, target, "MISL", 20 * Defs.FRACUNIT, 20, "rocket");
        } else if ("plasma".equals(kind)) {
            game.startSound("plasma");
            Enemy.spawnMissile(world, mo, target, "APLS", 25 * Defs.FRACUNIT, 5, "plasma");
        } else if ("skull".equals(kind)) {
            Enemy.skullAttack(mo, game);
        } else if ("chaingun".equals(kind)) {
            game.startSound("shotgn");
            int faced = mo.angle;
            int slope = Collision.aimSlope(world, mo, faced, Defs.MISSILERANGE);
            mo.angle = Compat.asU32(faced + ((Enemy.random() - Enemy.random()) << 20));
            Collision.lineAttack(world, mo, ((Enemy.random() % 5) + 1) * 3, game, Defs.MISSILERANGE, mo.angle, slope);
            mo.angle = faced;
        } else if ("spider".equals(kind)) {
            game.startSound("shotgn");
            int faced = mo.angle;
            int slope = Collision.aimSlope(world, mo, faced, Defs.MISSILERANGE);
            for (int i = 0; i < 3; i++) {
                mo.angle = Compat.asU32(faced + ((Enemy.random() - Enemy.random()) << 20));
                Collision.lineAttack(world, mo, ((Enemy.random() % 5) + 1) * 3, game, Defs.MISSILERANGE, mo.angle, slope);
            }
            mo.angle = faced;
        } else if ("revenant".equals(kind)) {
            if (Collision.approxDistance(target.x - mo.x, target.y - mo.y) < Defs.MELEERANGE + mo.radius) {
                game.startSound("skeswg");
                game.damageMobj(target, mo, ((Enemy.random() % 8) + 1) * 6);
            } else {
                game.startSound("skeatk");
                Mobj miss = Enemy.spawnMissile(world, mo, target, "FATB", 10 * Defs.FRACUNIT, 10, "tracer");
                miss.tracer = target;
            }
        } else if ("mancubus".equals(kind)) {
            game.startSound("firsht");
            int spread = Defs.ANG90 / 8;
            int[] deltas = new int[] {-spread, 0, spread};
            for (int da : deltas) {
                Enemy.spawnMissile(world, mo, target, "MANF", 20 * Defs.FRACUNIT, 8, "fat", Compat.asU32(mo.angle + da));
            }
        } else if ("pain".equals(kind)) {
            game.startSound("sklatk");
            Enemy.painShootSkull(world, mo, game, mo.angle);
        } else if ("vile".equals(kind)) {
            Enemy.vileAttack(world, mo, game);
        } else {
            game.startSound("pistol");
            int faced = mo.angle;
            int slope = Collision.aimSlope(world, mo, faced, Defs.MISSILERANGE);
            mo.angle = Compat.asU32(faced + ((Enemy.random() - Enemy.random()) << 20));
            Collision.lineAttack(world, mo, ((Enemy.random() % 5) + 1) * 3, game, Defs.MISSILERANGE, mo.angle, slope);
            mo.angle = faced;
        }
    }

    private static boolean refireOk(World world, Mobj mo, int keep)
    {
        if (Enemy.random() < keep) {
            return true;
        }
        Mobj t = mo.target;
        return t != null && t.health > 0 && Collision.checkSight(world, mo, t);
    }

    private static void skullAttack(Mobj mo, Game game)
    {
        Mobj dest = mo.target;
        if (dest == null) {
            return;
        }
        mo.flags |= Defs.MF_SKULLFLY;
        game.startSound("sklatk");
        Enemy.faceTarget(mo, dest);
        mo.momx = Compat.fixedMul(Defs.SKULLSPEED, Tables.fineCos(mo.angle));
        mo.momy = Compat.fixedMul(Defs.SKULLSPEED, Tables.fineSin(mo.angle));
        int dist = Collision.approxDistance(dest.x - mo.x, dest.y - mo.y);
        int steps = Math.max(1, Defs.SKULLSPEED != 0 ? Compat.intdiv(dist, Defs.SKULLSPEED) : 1);
        mo.momz = (dest.z + (dest.height >> 1) - mo.z) / steps;
    }

    private static void faceTarget(Mobj mo, Mobj t)
    {
        mo.angle = Collision.angleTo(mo.x, mo.y, t.x, t.y);
        if ((t.flags & Defs.MF_SHADOW) != 0) {
            mo.angle = Compat.asU32(mo.angle + (Enemy.random() - Enemy.random()) * 2097152);
        }
    }

    private static void faceMoveDir(Mobj mo)
    {
        if (mo.movedir < 0 || mo.movedir >= 8) {
            return;
        }
        mo.angle = Compat.asU32(mo.angle & (int) 0xE0000000L);
        int delta = Compat.asI32(mo.angle - mo.movedir * Defs.ANG45);
        if (delta > 0) {
            mo.angle = Compat.asU32(mo.angle - Defs.ANG45);
        } else if (delta < 0) {
            mo.angle = Compat.asU32(mo.angle + Defs.ANG45);
        }
    }

    private static boolean move(World world, Mobj mo, int speed, Game game)
    {
        if (mo.movedir < 0 || mo.movedir >= 8) {
            return false;
        }
        int nx = mo.x + speed * XSPEED[mo.movedir];
        int ny = mo.y + speed * YSPEED[mo.movedir];
        if (!Collision.tryMove(world, mo, nx, ny, game)) {
            if ((mo.flags & Defs.MF_FLOAT) != 0 && Collision.floatOk) {
                if (mo.z < Collision.tmFloorZ) {
                    mo.z += 4 * Defs.FRACUNIT;
                } else {
                    mo.z -= 4 * Defs.FRACUNIT;
                }
                mo.flags |= Defs.MF_INFLOAT;
                return true;
            }
            if (Collision.lastSpechit.isEmpty()) {
                return false;
            }
            mo.movedir = DI_NODIR;
            for (int i = Collision.lastSpechit.size() - 1; i >= 0; i--) {
                Line ln = Collision.lastSpechit.get(i);
                if (ln.special != 0) {
                    game.useSpecial(ln, mo, 0);
                    return true;
                }
            }
            return false;
        }
        mo.flags &= ~Defs.MF_INFLOAT;
        if ((mo.flags & Defs.MF_FLOAT) == 0) {
            mo.z = mo.floorz;
        }
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
        int speed = Info.miInt(mo.type, Info.MI_SPEED);
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

    private static Mobj spawnMissile(World world, Mobj src, Mobj dest, String sprite, int speed, int damage)
    {
        return Enemy.spawnMissile(world, src, dest, sprite, speed, damage, "ball", null);
    }

    private static Mobj spawnMissile(
            World world, Mobj src, Mobj dest, String sprite, int speed, int damage, String kind)
    {
        return Enemy.spawnMissile(world, src, dest, sprite, speed, damage, kind, null);
    }

    private static Mobj spawnMissile(
            World world, Mobj src, Mobj dest, String sprite, int speed, int damage, String kind, Integer ang)
    {
        int typ;
        if ("rocket".equals(kind))
            typ = Info.MT_ROCKET;
        else if ("plasma".equals(kind))
            typ = "APLS".equals(sprite) ? Info.MT_ARACHPLAZ : Info.MT_PLASMA;
        else if ("bfg".equals(kind))
            typ = Info.MT_BFG;
        else if ("tracer".equals(kind))
            typ = Info.MT_TRACER;
        else if ("fat".equals(kind))
            typ = Info.MT_FATSHOT;
        else if ("spawncube".equals(kind))
            typ = Info.MT_SPAWNSHOT;
        else if ("baron".equals(kind))
            typ = Info.MT_BRUISERSHOT;
        else if ("caco".equals(kind))
            typ = Info.MT_HEADSHOT;
        else
            typ = Info.MT_TROOPSHOT;
        return Enemy.spawnMissileMt(world, src, dest, typ, null, ang);
    }

    private static Mobj spawnMissileMt(
            World world, Mobj src, Mobj dest, int typ, Game game, Integer ang)
    {
        int a = ang != null ? ang : Collision.angleTo(src.x, src.y, dest.x, dest.y);
        if (ang == null && (dest.flags & Defs.MF_SHADOW) != 0) {
            a = Compat.asU32(a + (Enemy.random() - Enemy.random()) * 1048576);
        }
        int speed = Info.miInt(typ, Info.MI_SPEED);
        int dist = Collision.approxDistance(dest.x - src.x, dest.y - src.y);
        int steps = Math.max(1, speed != 0 ? Compat.intdiv(dist, speed) : 1);
        Mobj mo = Thinker.spawnMobj(world, src.x, src.y, src.z + 32 * Defs.FRACUNIT, typ, game);
        mo.angle = a;
        mo.momx = Compat.fixedMul(speed, Tables.fineCos(a));
        mo.momy = Compat.fixedMul(speed, Tables.fineSin(a));
        mo.target = src;
        mo.momz = (dest.z - src.z) / steps;
        Enemy.checkMissileSpawn(mo);
        return mo;
    }

    private static void checkMissileSpawn(Mobj mo)
    {
        mo.tics -= Enemy.random() & 3;
        if (mo.tics < 1)
            mo.tics = 1;
        mo.x += mo.momx >> 1;
        mo.y += mo.momy >> 1;
        mo.z += mo.momz >> 1;
    }

    static void spawnPlayerMissile(World world, Mobj src, String sprite, int speed, int damage, String kind)
    {
        int typ = "rocket".equals(kind) ? Info.MT_ROCKET
                : ("plasma".equals(kind) ? Info.MT_PLASMA : Info.MT_BFG);
        int a = src.angle;
        Mobj mo = Thinker.spawnMobj(world, src.x, src.y, src.z + 32 * Defs.FRACUNIT, typ, null);
        int actualSpeed = Info.miInt(typ, Info.MI_SPEED);
        mo.angle = a;
        mo.momx = Compat.fixedMul(actualSpeed, Tables.fineCos(a));
        mo.momy = Compat.fixedMul(actualSpeed, Tables.fineSin(a));
        mo.target = src;
        mo.momz = 0;
        Enemy.checkMissileSpawn(mo);
    }

    private static void tickMissile(World world, Mobj mo, Game game)
    {
        String kind = mo.missileKind != null && !mo.missileKind.isEmpty()
                ? mo.missileKind
                : (mo.info != null && mo.info.length > 1 && mo.info[1] != null ? String.valueOf(mo.info[1]) : "");
        if ("tracer".equals(kind) && (game.leveltime & 3) == 0) {
            Enemy.tracerHome(mo);
        }
        if ("spawncube".equals(kind)) {
            Mobj dest = mo.tracer;
            mo.x += mo.momx;
            mo.y += mo.momy;
            mo.z += mo.momz;
            if (dest == null || Collision.approxDistance(dest.x - mo.x, dest.y - mo.y) < 24 * Defs.FRACUNIT) {
                Enemy.spawnFly(world, mo, game);
            }
            return;
        }
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
            int damage = mo.damage != 0 ? mo.damage : Info.miInt(mo.type, Info.MI_DAMAGE);
            game.damageMobj(hit, source, damage * ((Enemy.random() % 8) + 1), mo);
        }
        mo.momx = mo.momy = mo.momz = 0;
        mo.flags &= ~Defs.MF_MISSILE;
        Thinker.setMobjState(mo, Info.miInt(mo.type, Info.MI_DEATHSTATE), world, game);
    }

    private static void radiusAttack(World world, Mobj spot, Mobj source, int damage, Game game)
    {
        for (Mobj o : new ArrayList<>(world.mobjs)) {
            if (o == spot || (o.flags & Defs.MF_SHOOTABLE) == 0
                    || o.type == Info.MT_CYBORG || o.type == Info.MT_SPIDER) {
                continue;
            }
            int dx = Math.abs(o.x - spot.x);
            int dy = Math.abs(o.y - spot.y);
            int dist = (dx > dy ? dx : dy) - o.radius;
            if (dist < 0) {
                dist = 0;
            }
            dist >>= 16;
            if (dist >= damage) {
                continue;
            }
            if (Collision.checkSight(world, o, spot)) {
                game.damageMobj(o, source != null ? source : spot, damage - dist, spot);
            }
        }
    }

    private static void bfgSpray(World world, Mobj ball, Game game)
    {
        Mobj shooter = ball.target;
        if (shooter == null) {
            return;
        }
        for (int i = 0; i < 40; i++) {
            int an = Compat.asU32(shooter.angle - Compat.intdiv(Defs.ANG90, 2) + Compat.intdiv(Defs.ANG90, 40) * i);
            Mobj target = Collision.aimLineAttack(world, shooter, an, 16 * 64 * Defs.FRACUNIT);
            if (target == null) {
                continue;
            }
            int damage = 0;
            for (int j = 0; j < 15; j++) {
                damage += (Enemy.random() & 7) + 1;
            }
            game.damageMobj(target, shooter, damage, ball);
        }
    }

    private static void explodeBarrel(World world, Mobj barrel, Game game)
    {
        Enemy.radiusAttack(world, barrel, barrel, 128, game);
    }

    private static int[] spriteAndFrame(String name)
    {
        String n = name == null ? "" : name.toUpperCase();
        String spr = n.length() <= 4 ? n : n.substring(0, 4);
        int frame = 0;
        if (n.length() >= 5) {
            char ch = n.charAt(4);
            if (ch >= 'A' && ch <= ']') {
                frame = ch - 65;
            }
        }
        return new int[] {frame};
    }

    private static String spriteName(String name)
    {
        String n = name == null ? "" : name.toUpperCase();
        return n.length() <= 4 ? n : n.substring(0, 4);
    }

    private static void tracerHome(Mobj mo)
    {
        Mobj dest = mo.tracer;
        if (dest == null || dest.health <= 0) {
            return;
        }
        int exact = Collision.angleTo(mo.x, mo.y, dest.x, dest.y);
        long diff = Integer.toUnsignedLong(exact - mo.angle);
        if (diff > 0x80000000L) {
            mo.angle = Compat.asU32(mo.angle - TRACEANGLE);
            if (Integer.toUnsignedLong(exact - mo.angle) < 0x80000000L) {
                mo.angle = exact;
            }
        } else {
            mo.angle = Compat.asU32(mo.angle + TRACEANGLE);
            if (Integer.toUnsignedLong(exact - mo.angle) > 0x80000000L) {
                mo.angle = exact;
            }
        }
        int speed = 10 * Defs.FRACUNIT;
        mo.momx = Compat.fixedMul(speed, Tables.fineCos(mo.angle));
        mo.momy = Compat.fixedMul(speed, Tables.fineSin(mo.angle));
        int dist = Collision.approxDistance(dest.x - mo.x, dest.y - mo.y);
        int steps = Math.max(1, speed != 0 ? Compat.intdiv(dist, speed) : 1);
        mo.momz = (dest.z + 40 * Defs.FRACUNIT - mo.z) / steps;
    }

    private static boolean vileChase(World world, Mobj mo, Game game)
    {
        for (Mobj other : world.mobjs) {
            if (other == mo || (other.flags & Defs.MF_CORPSE) == 0) {
                continue;
            }
            if (other.health > 0) {
                continue;
            }
            int raiseState = Info.miInt(other.type, Info.MI_RAISESTATE);
            if (raiseState == Info.S_NULL) {
                continue;
            }
            int maxdist = mo.radius + other.radius;
            if (Math.abs(other.x - mo.x) > maxdist || Math.abs(other.y - mo.y) > maxdist) {
                continue;
            }
            Thinker.setMobjState(mo, Info.S_VILE_HEAL1, world, game);
            game.startSound("slop");
            Thinker.setMobjState(other, raiseState, world, game);
            other.flags = Info.miInt(other.type, Info.MI_FLAGS);
            other.health = Info.miInt(other.type, Info.MI_SPAWNHEALTH);
            other.height = Info.miInt(other.type, Info.MI_HEIGHT);
            other.radius = Info.miInt(other.type, Info.MI_RADIUS);
            other.target = null;
            other.alive = true;
            other.z = other.floorz;
            return true;
        }
        return false;
    }

    private static void vileAttack(World world, Mobj mo, Game game)
    {
        Mobj dest = mo.target;
        if (dest == null || !Collision.checkSight(world, mo, dest)) {
            return;
        }
        game.startSound("vilatk");
        game.damageMobj(dest, mo, 20);
        Enemy.radiusAttack(world, dest, mo, 70, game);
    }

    private static void painShootSkull(World world, Mobj actor, Game game, int ang)
    {
        int n = 0;
        for (Mobj other : world.mobjs) {
            if (other.type == Info.MT_SKULL && other.health > 0) {
                n++;
            }
        }
        if (n >= 21) {
            return;
        }
        int pre = 4 * Defs.FRACUNIT + Compat.intdiv(3 * actor.radius, 2);
        int x = actor.x + Compat.fixedMul(pre, Tables.fineCos(ang));
        int y = actor.y + Compat.fixedMul(pre, Tables.fineSin(ang));
        Mobj skull = Thinker.spawnMobj(world, x, y, actor.z, Info.MT_SKULL, game);
        skull.angle = ang;
        MoveCheck chk = Collision.checkPosition(world, skull, skull.x, skull.y);
        if (chk.blocked) {
            game.damageMobj(skull, actor, 10000, actor);
            return;
        }
        skull.target = actor.target;
        Enemy.skullAttack(skull, game);
    }

    private static void painDie(World world, Mobj mo, Game game)
    {
        int[] deltas = new int[] {Defs.ANG90, Defs.ANG90 * 2, Defs.ANG270};
        for (int da : deltas) {
            Enemy.painShootSkull(world, mo, game, Compat.asU32(mo.angle + da));
        }
    }

    private static boolean aliveOfType(World world, int typ)
    {
        for (Mobj other : world.mobjs) {
            if (other.type == typ && other.health > 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean commercial(Game game)
    {
        return game.wad.checkNumForName("MAP01") >= 0;
    }

    private static void bossDeath(World world, Mobj mo, Game game)
    {
        if (Enemy.aliveOfType(world, mo.type) || game.specials == null) {
            return;
        }
        Specials spec = game.specials;
        if (Enemy.commercial(game) && game.mapn == 7) {
            if (mo.type == Info.MT_FATSO) {
                spec.doFloorTag(666, Specials::lowestFloor, -1);
            } else if (mo.type == Info.MT_BABY) {
                spec.raiseToTextureTag(667);
            }
            return;
        }
        if (Enemy.commercial(game)) {
            return;
        }
        if (game.episode == 1 && game.mapn == 8 && mo.type == Info.MT_BRUISER) {
            spec.doFloorTag(666, Specials::lowestFloor, -1);
        } else if (game.episode == 2 && game.mapn == 8 && mo.type == Info.MT_CYBORG) {
            spec.exitRequested = true;
        } else if (game.episode == 3 && game.mapn == 8 && mo.type == Info.MT_SPIDER) {
            spec.exitRequested = true;
        } else if (game.episode == 4 && game.mapn == 6 && mo.type == Info.MT_CYBORG) {
            spec.doFloorTag(666, Specials::lowestFloor, -1);
        } else if (game.episode == 4 && game.mapn == 8 && mo.type == Info.MT_BRUISER) {
            spec.doFloorTag(666, Specials::lowestFloor, -1);
        }
    }

    private static void keenDie(World world, Mobj mo, Game game)
    {
        if (Enemy.aliveOfType(world, Info.MT_KEEN) || game.specials == null) {
            return;
        }
        game.specials.doDoorTag(666, Defs.VLD_BLAZEOPEN);
    }

    private static void brainDie(Game game)
    {
        if (game.specials != null) {
            game.specials.exitRequested = true;
        }
    }

    private static void tickBrainEye(World world, Mobj mo, Game game)
    {
        if (mo.tics > 0) {
            mo.tics--;
            return;
        }
        mo.tics = 150;
        Enemy.brainSpit(world, mo, game);
    }

    private static void brainSpit(World world, Mobj actor, Game game)
    {
        if (game.skill <= Defs.SK_EASY) {
            actor.easySkip = !actor.easySkip;
            if (actor.easySkip) {
                return;
            }
        }
        java.util.ArrayList<Mobj> targs = new java.util.ArrayList<>();
        for (Mobj m : world.mobjs) {
            if (m.type == Info.MT_BOSSTARGET) {
                targs.add(m);
            }
        }
        if (targs.isEmpty()) {
            return;
        }
        Mobj dest = targs.get(brainTargetOn % targs.size());
        brainTargetOn++;
        game.startSound("bospit");
        Mobj miss = Enemy.spawnMissile(world, actor, dest, "BOSF", 10 * Defs.FRACUNIT, 0, "spawncube");
        miss.tracer = dest;
        miss.flags |= Defs.MF_NOCLIP;
    }

    private static void spawnFly(World world, Mobj cube, Game game)
    {
        Mobj dest = cube.tracer != null ? cube.tracer : cube;
        int r = Enemy.random();
        int typ;
        if (r < 50) {
            typ = 3001;
        } else if (r < 90) {
            typ = 3002;
        } else if (r < 120) {
            typ = 58;
        } else if (r < 130) {
            typ = 71;
        } else if (r < 160) {
            typ = 3005;
        } else if (r < 162) {
            typ = 64;
        } else if (r < 172) {
            typ = 66;
        } else if (r < 192) {
            typ = 68;
        } else if (r < 222) {
            typ = 67;
        } else if (r < 246) {
            typ = 69;
        } else {
            typ = 3003;
        }
        game.startSound("telept");
        int mobjType = Info.mobjTypeForDoomednum(typ);
        if (mobjType >= 0) {
            Mobj spawned = Thinker.spawnMobj(world, dest.x, dest.y, dest.z, mobjType, game);
            spawned.angle = dest.angle;
        }
        Thinker.removeMobj(world, cube);
    }

    private static Mobj spawnType(World world, int typ, int x, int y, int z, int angle)
    {
        Object[] info = Mobj.infoTable().get(typ);
        if (info == null) {
            return null;
        }
        String sprite = (String) info[0];
        int rad = (Integer) info[1];
        int h = (Integer) info[2];
        int health = (Integer) info[3];
        int flags = (Integer) info[4];
        String kind = (String) info[5];
        Object extra = info[6];
        if ("enemy".equals(kind) && typ != 2035) {
            flags |= Defs.MF_COUNTKILL;
        }
        Subsector sub = Collision.pointInSubsector(world, x, y);
        Mobj mo = new Mobj();
        mo.x = x;
        mo.y = y;
        mo.z = z != 0 ? z : sub.sector.floorheight;
        mo.angle = Compat.asU32(angle);
        mo.radius = rad * Defs.FRACUNIT;
        mo.height = h * Defs.FRACUNIT;
        mo.floorz = sub.sector.floorheight;
        mo.ceilingz = sub.sector.ceilingheight;
        mo.flags = flags;
        mo.health = health != 0 ? health : 1000;
        mo.type = typ;
        mo.sprite = spriteName(sprite);
        mo.info = new Object[] {kind, extra};
        mo.aiState = "enemy".equals(kind) ? "look" : "";
        mo.frame = spriteAndFrame(sprite)[0];
        mo.tics = "enemy".equals(kind) ? 10 : 0;
        mo.reactiontime = "enemy".equals(kind) ? 8 : 0;
        mo.tmx = mo.x;
        mo.tmy = mo.y;
        if (typ == 72) {
            mo.z = mo.ceilingz - mo.height;
        }
        world.mobjs.add(mo);
        Collision.setThingPosition(world, mo);
        return mo;
    }

    public static int publicRandom()
    {
        return Enemy.random();
    }

    /** Halve toward zero. Java / already truncates that way; Python // does not. */
    private static int trunc2(int n)
    {
        n = Compat.asI32(n);
        return n / 2;
    }

    /** P_XYMovement: half-steps above MAXMOVE/2, then friction on the floor. */
    public static void pXyMovement(World world, Mobj mo, Game game)
    {
        if (mo.momx == 0 && mo.momy == 0) {
            if ((mo.flags & Defs.MF_SKULLFLY) != 0) {
                mo.flags &= ~Defs.MF_SKULLFLY;
                mo.momx = mo.momy = mo.momz = 0;
                Thinker.setMobjState(mo, Info.miInt(mo.type, Info.MI_SPAWNSTATE), world, game);
            }
            return;
        }
        if (mo.momx > Defs.MAXMOVE) {
            mo.momx = Defs.MAXMOVE;
        } else if (mo.momx < -Defs.MAXMOVE) {
            mo.momx = -Defs.MAXMOVE;
        }
        if (mo.momy > Defs.MAXMOVE) {
            mo.momy = Defs.MAXMOVE;
        } else if (mo.momy < -Defs.MAXMOVE) {
            mo.momy = -Defs.MAXMOVE;
        }
        int xmove = mo.momx;
        int ymove = mo.momy;
        int half = Defs.MAXMOVE / 2;
        while (xmove != 0 || ymove != 0) {
            int ptryx;
            int ptryy;
            if (xmove > half || ymove > half) {
                ptryx = mo.x + trunc2(xmove);
                ptryy = mo.y + trunc2(ymove);
                xmove = trunc2(xmove);
                ymove = trunc2(ymove);
            } else {
                ptryx = mo.x + xmove;
                ptryy = mo.y + ymove;
                xmove = 0;
                ymove = 0;
            }
            if ((mo.flags & Defs.MF_NOCLIP) != 0) {
                Collision.unsetThingPosition(world, mo);
                mo.x = ptryx;
                mo.y = ptryy;
                Collision.setThingPosition(world, mo);
                continue;
            }
            if (Collision.tryMove(world, mo, ptryx, ptryy, game)) {
                continue;
            }
            if (mo.player != null) {
                Collision.slideMove(world, mo, mo.momx, mo.momy, game);
            } else if ((mo.flags & Defs.MF_MISSILE) != 0) {
                Line line = Collision.ceilingLine;
                int sky = game != null && game.res != null ? game.res.skyflatnum : -1;
                if (line != null && line.backsector != null && line.backsector.ceilingpic == sky) {
                    Thinker.removeMobj(world, mo);
                    return;
                }
                Enemy.explodeMissile(world, mo, game, null);
                return;
            } else {
                mo.momx = mo.momy = 0;
            }
        }
        Player player = mo.player;
        if (player != null && (player.cheats & Defs.CF_NOMOMENTUM) != 0) {
            mo.momx = mo.momy = 0;
            return;
        }
        if ((mo.flags & (Defs.MF_MISSILE | Defs.MF_SKULLFLY)) != 0) {
            return;
        }
        if (mo.z > mo.floorz) {
            return;
        }
        if ((mo.flags & Defs.MF_CORPSE) != 0) {
            int quarter = Defs.FRACUNIT / 4;
            if (mo.momx > quarter || mo.momx < -quarter || mo.momy > quarter || mo.momy < -quarter) {
                Sector sec = Collision.pointInSubsector(world, mo.x, mo.y).sector;
                if (mo.floorz != sec.floorheight) {
                    return;
                }
            }
        }
        if (mo.momx > -Defs.STOPSPEED && mo.momx < Defs.STOPSPEED
                && mo.momy > -Defs.STOPSPEED && mo.momy < Defs.STOPSPEED
                && (player == null || (player.cmd.forwardmove == 0 && player.cmd.sidemove == 0))) {
            if (player != null) {
                int n = mo.istate - Info.S_PLAY_RUN1;
                if (n >= 0 && n < 4) {
                    Thinker.setMobjState(mo, Info.S_PLAY, world, game);
                }
            }
            mo.momx = mo.momy = 0;
        } else {
            mo.momx = Compat.fixedMul(mo.momx, Defs.FRICTION);
            mo.momy = Compat.fixedMul(mo.momy, Defs.FRICTION);
        }
    }

    public static void missileXy(World world, Mobj mo, Game game)
    {
        Enemy.pXyMovement(world, mo, game);
    }

    public static void skullXy(World world, Mobj mo, Game game)
    {
        Enemy.pXyMovement(world, mo, game);
    }

    public static void groundXy(World world, Mobj mo, Game game)
    {
        Enemy.pXyMovement(world, mo, game);
    }

    /** P_ZMovement. Gravity applies only while airborne, and the first tic is doubled. */
    public static void mobjZ(Mobj mo, World world, Game game)
    {
        Player player = mo.player;
        if (player != null && mo.z < mo.floorz) {
            player.viewheight -= mo.floorz - mo.z;
            player.deltaviewheight = Compat.shar(Defs.VIEWHEIGHT - player.viewheight, 3);
        }
        mo.z += mo.momz;
        if ((mo.flags & Defs.MF_FLOAT) != 0 && mo.target != null
                && (mo.flags & (Defs.MF_SKULLFLY | Defs.MF_INFLOAT)) == 0) {
            int dist = Collision.approxDistance(mo.x - mo.target.x, mo.y - mo.target.y);
            int delta = (mo.target.z + Compat.shar(mo.height, 1)) - mo.z;
            if (delta < 0 && dist < -(delta * 3)) {
                mo.z -= 4 * Defs.FRACUNIT;
            } else if (delta > 0 && dist < (delta * 3)) {
                mo.z += 4 * Defs.FRACUNIT;
            }
        }
        if (mo.z <= mo.floorz) {
            if (mo.momz < 0) {
                if (player != null && mo.momz < -Defs.GRAVITY * 8) {
                    player.deltaviewheight = Compat.shar(mo.momz, 3);
                    if (game != null) {
                        game.startSound("oof");
                    }
                }
                mo.momz = 0;
            }
            mo.z = mo.floorz;
            if ((mo.flags & Defs.MF_SKULLFLY) != 0 && (mo.flags & Defs.MF_MISSILE) == 0) {
                mo.momz = -mo.momz;
            }
            if ((mo.flags & Defs.MF_MISSILE) != 0 && (mo.flags & Defs.MF_NOCLIP) == 0) {
                Enemy.explodeMissile(world, mo, game, null);
                return;
            }
        } else if ((mo.flags & Defs.MF_NOGRAVITY) == 0) {
            if (mo.momz == 0) {
                mo.momz = -Defs.GRAVITY * 2;
            } else {
                mo.momz -= Defs.GRAVITY;
            }
        }
        if (mo.z + mo.height > mo.ceilingz) {
            if (mo.momz > 0) {
                mo.momz = 0;
            }
            mo.z = mo.ceilingz - mo.height;
            if ((mo.flags & Defs.MF_SKULLFLY) != 0) {
                mo.momz = -mo.momz;
            }
            if ((mo.flags & Defs.MF_MISSILE) != 0 && (mo.flags & Defs.MF_NOCLIP) == 0) {
                Enemy.explodeMissile(world, mo, game, null);
            }
        }
    }

    private static void hitscan(World world, Mobj mo, Game game, int pellets, String sound)
    {
        if (mo.target == null)
            return;
        Enemy.faceTarget(mo, mo.target);
        int faced = mo.angle;
        int slope = Collision.aimSlope(world, mo, faced, Defs.MISSILERANGE);
        game.startSound(sound);
        for (int i = 0; i < pellets; i++) {
            mo.angle = Compat.asU32(faced + ((Enemy.random() - Enemy.random()) << 20));
            Collision.lineAttack(world, mo, ((Enemy.random() % 5) + 1) * 3, game, Defs.MISSILERANGE, mo.angle, slope);
        }
        mo.angle = faced;
    }

    private static void fatMissileAngle(Mobj miss, int angle)
    {
        miss.angle = Compat.asU32(angle);
        int speed = Info.miInt(miss.type, Info.MI_SPEED);
        miss.momx = Compat.fixedMul(speed, Tables.fineCos(miss.angle));
        miss.momy = Compat.fixedMul(speed, Tables.fineSin(miss.angle));
    }

    public static void callAction(String name, Mobj mo, World world, Game game)
    {
        Mobj player = game != null && game.player != null ? game.player.mo : null;
        switch (name) {
            case "Look":
                if (player != null) Enemy.look(world, mo, player, game);
                break;
            case "Chase":
                if (player != null) Enemy.chase(world, mo, player, game);
                break;
            case "VileChase":
                if (!Enemy.vileChase(world, mo, game) && player != null) Enemy.chase(world, mo, player, game);
                break;
            case "FaceTarget":
                if (mo.target != null) Enemy.faceTarget(mo, mo.target);
                break;
            case "Fall":
                mo.flags &= ~Defs.MF_SOLID;
                break;
            case "Scream": {
                String sound = Info.miStr(mo.type, Info.MI_DEATHSOUND);
                if (!sound.isEmpty()) game.startSound(sound);
                break;
            }
            case "XScream":
                game.startSound("slop");
                break;
            case "Pain": {
                String sound = Info.miStr(mo.type, Info.MI_PAINSOUND);
                if (!sound.isEmpty()) game.startSound(sound);
                break;
            }
            case "Explode":
                Enemy.radiusAttack(world, mo, mo.target, 128, game);
                break;
            case "PosAttack":
                Enemy.hitscan(world, mo, game, 1, "pistol");
                break;
            case "SPosAttack":
                Enemy.hitscan(world, mo, game, 3, "shotgn");
                break;
            case "CPosAttack":
                Enemy.hitscan(world, mo, game, 1, "shotgn");
                break;
            case "CPosRefire":
            case "SpidRefire":
                if (mo.target != null) Enemy.faceTarget(mo, mo.target);
                if (Enemy.random() >= ("CPosRefire".equals(name) ? 40 : 10)
                        && (mo.target == null || mo.target.health <= 0
                        || !Collision.checkSight(world, mo, mo.target)))
                    Thinker.setMobjState(mo, Info.miInt(mo.type, Info.MI_SEESTATE), world, game);
                break;
            case "TroopAttack":
            case "SargAttack":
            case "HeadAttack":
            case "BruisAttack":
                if (mo.target == null) break;
                Enemy.faceTarget(mo, mo.target);
                int dist = Collision.approxDistance(mo.target.x - mo.x, mo.target.y - mo.y);
                if (dist < Defs.MELEERANGE + mo.radius) {
                    int mul = "SargAttack".equals(name) ? 4
                            : ("HeadAttack".equals(name) || "BruisAttack".equals(name) ? 10 : 3);
                    if ("TroopAttack".equals(name)) game.startSound("claw");
                    game.damageMobj(mo.target, mo, ((Enemy.random() % 8) + 1) * mul);
                } else if (!"SargAttack".equals(name)) {
                    int typ = "TroopAttack".equals(name) ? Info.MT_TROOPSHOT
                            : ("HeadAttack".equals(name) ? Info.MT_HEADSHOT : Info.MT_BRUISERSHOT);
                    Enemy.spawnMissileMt(world, mo, mo.target, typ, game, null);
                }
                break;
            case "SkullAttack":
                Enemy.skullAttack(mo, game);
                break;
            case "CyberAttack":
            case "BspiAttack":
                if (mo.target != null) {
                    Enemy.faceTarget(mo, mo.target);
                    Enemy.spawnMissileMt(world, mo, mo.target,
                            "CyberAttack".equals(name) ? Info.MT_ROCKET : Info.MT_ARACHPLAZ, game, null);
                }
                break;
            case "Metal":
            case "BabyMetal":
            case "Hoof":
                game.startSound("Metal".equals(name) ? "metal" : ("Hoof".equals(name) ? "hoof" : "bspwlk"));
                if (player != null) Enemy.chase(world, mo, player, game);
                break;
            case "PainAttack":
                if (mo.target != null) {
                    Enemy.faceTarget(mo, mo.target);
                    Enemy.painShootSkull(world, mo, game, mo.angle);
                }
                break;
            case "PainDie":
                mo.flags &= ~Defs.MF_SOLID;
                Enemy.painDie(world, mo, game);
                break;
            case "KeenDie":
                mo.flags &= ~Defs.MF_SOLID;
                Enemy.keenDie(world, mo, game);
                break;
            case "BossDeath":
                Enemy.bossDeath(world, mo, game);
                break;
            case "VileStart":
                game.startSound("vilatk");
                break;
            case "VileTarget":
                if (mo.target != null) {
                    Enemy.faceTarget(mo, mo.target);
                    Mobj fire = Thinker.spawnMobj(world, mo.target.x, mo.target.y, mo.target.z, Info.MT_FIRE, game);
                    mo.tracer = fire;
                    fire.target = mo;
                    fire.tracer = mo.target;
                }
                break;
            case "VileAttack":
                Enemy.vileAttack(world, mo, game);
                break;
            case "StartFire":
            case "Fire":
            case "FireCrackle":
                if ("StartFire".equals(name)) game.startSound("flamst");
                else if ("FireCrackle".equals(name)) game.startSound("flame");
                if (mo.tracer != null && mo.target != null) {
                    mo.x = mo.tracer.x;
                    mo.y = mo.tracer.y;
                    mo.z = mo.tracer.z;
                }
                break;
            case "Tracer":
                if ((game.leveltime & 3) == 0) Enemy.tracerHome(mo);
                break;
            case "SkelWhoosh":
                if (mo.target != null) Enemy.faceTarget(mo, mo.target);
                game.startSound("skeswg");
                break;
            case "SkelFist":
                if (mo.target != null) {
                    Enemy.faceTarget(mo, mo.target);
                    if (Collision.approxDistance(mo.target.x - mo.x, mo.target.y - mo.y)
                            < Defs.MELEERANGE + mo.radius) {
                        game.startSound("skepch");
                        game.damageMobj(mo.target, mo, ((Enemy.random() % 8) + 1) * 6);
                    }
                }
                break;
            case "SkelMissile":
                if (mo.target != null) {
                    Enemy.faceTarget(mo, mo.target);
                    Mobj miss = Enemy.spawnMissileMt(world, mo, mo.target, Info.MT_TRACER, game, null);
                    miss.tracer = mo.target;
                    miss.z += 16 * Defs.FRACUNIT;
                }
                break;
            case "FatRaise":
                if (mo.target != null) Enemy.faceTarget(mo, mo.target);
                game.startSound("manatk");
                break;
            case "FatAttack1":
            case "FatAttack2":
            case "FatAttack3":
                if (mo.target == null) break;
                Enemy.faceTarget(mo, mo.target);
                int spread = Defs.ANG90 / 8;
                if ("FatAttack1".equals(name)) {
                    mo.angle = Compat.asU32(mo.angle + spread);
                    Enemy.spawnMissileMt(world, mo, mo.target, Info.MT_FATSHOT, game, null);
                    Mobj miss = Enemy.spawnMissileMt(world, mo, mo.target, Info.MT_FATSHOT, game, null);
                    Enemy.fatMissileAngle(miss, miss.angle + spread);
                } else if ("FatAttack2".equals(name)) {
                    mo.angle = Compat.asU32(mo.angle - spread);
                    Enemy.spawnMissileMt(world, mo, mo.target, Info.MT_FATSHOT, game, null);
                    Mobj miss = Enemy.spawnMissileMt(world, mo, mo.target, Info.MT_FATSHOT, game, null);
                    Enemy.fatMissileAngle(miss, miss.angle - spread * 2);
                } else {
                    Mobj left = Enemy.spawnMissileMt(world, mo, mo.target, Info.MT_FATSHOT, game, null);
                    Enemy.fatMissileAngle(left, mo.angle - spread / 2);
                    Mobj right = Enemy.spawnMissileMt(world, mo, mo.target, Info.MT_FATSHOT, game, null);
                    Enemy.fatMissileAngle(right, mo.angle + spread / 2);
                }
                break;
            case "BrainPain":
                game.startSound("bospn");
                break;
            case "BrainScream":
                game.startSound("bosdth");
                break;
            case "BrainDie":
                Enemy.brainDie(game);
                break;
            case "BrainAwake":
                game.startSound("bossit");
                break;
            case "BrainSpit":
                Enemy.brainSpit(world, mo, game);
                break;
            case "SpawnSound":
                game.startSound("boscub");
                Enemy.aSpawnFly(world, mo, game);
                break;
            case "SpawnFly":
                Enemy.aSpawnFly(world, mo, game);
                break;
            case "BFGSpray":
                Enemy.bfgSpray(world, mo, game);
                break;
            case "PlayerScream":
                if (game != null) game.startSound("pldeth");
                break;
            default:
                break;
        }
    }

    private static void aSpawnFly(World world, Mobj mo, Game game)
    {
        if (--mo.reactiontime == 0)
            Enemy.spawnFly(world, mo, game);
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
