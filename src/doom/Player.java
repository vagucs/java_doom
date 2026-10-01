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

public final class Player
{
    public static final int[] FORWARDMOVE = {0x19, 0x32};
    public static final int[] SIDEMOVE = {0x18, 0x28};
    public static final int[] ANGLETURN = {640, 1280, 320};
    public static final int[] MAXAMMO = {200, 50, 300, 50};
    public static final int[] CLIPAMMO = {10, 4, 20, 1};
    public static final Integer[] WEAPON_AMMO = new Integer[9];

    static {
        WEAPON_AMMO[Defs.WP_PISTOL] = Defs.AM_CLIP;
        WEAPON_AMMO[Defs.WP_SHOTGUN] = Defs.AM_SHELL;
        WEAPON_AMMO[Defs.WP_SUPERSHOTGUN] = Defs.AM_SHELL;
        WEAPON_AMMO[Defs.WP_CHAINGUN] = Defs.AM_CLIP;
        WEAPON_AMMO[Defs.WP_MISSILE] = Defs.AM_MISL;
        WEAPON_AMMO[Defs.WP_PLASMA] = Defs.AM_CELL;
        WEAPON_AMMO[Defs.WP_BFG] = Defs.AM_CELL;
    }

    public Mobj mo;
    public Ticcmd cmd;
    public int playerstate = Defs.PST_LIVE;
    public int viewz;
    public int viewheight = Defs.VIEWHEIGHT;
    public int deltaviewheight;
    public int bob;
    public int health = 100;
    public int armorpoints;
    public int armortype;
    public int[] ammo = {50, 0, 0, 0};
    public int[] maxammo = MAXAMMO.clone();
    public boolean[] weaponowned = {true, true, false, false, false, false, false, false, false};
    public int pendingweapon = Defs.WP_NOCHANGE;
    public int readyweapon = Defs.WP_PISTOL;
    public boolean[] cards = {false, false, false, false, false, false};
    public int cheats;
    public String message = "";
    public int messageTics;
    public boolean attackdown;
    public boolean usedown;
    public int damagecount;
    public int bonuscount;
    public Mobj attacker;
    public int extralight;
    public int fixedcolormap;
    public int refire;
    public int killcount;
    public int itemcount;
    public int secretcount;
    public boolean didsecret;
    public int pspriteY = 32;
    public int pspriteSy = 128 * Defs.FRACUNIT;
    public String pspriteState = "up";
    public int pspriteTics;
    public int pspriteStep;
    public String pspriteBody = "";
    public String pspriteFlash = "";
    public int flashTics;
    public int[] powers = new int[6];

    public Player()
    {
        this(null, 0);
    }

    public Player(Mobj mo)
    {
        this(mo, 0);
    }

    public Player(Mobj mo, int cheats)
    {
        this.mo = mo;
        this.cheats = cheats;
        this.cmd = new Ticcmd();
    }

    public void setMessage(String text)
    {
        message = Deh.string(text);
        messageTics = 4 * Defs.TICRATE;
    }

    public static boolean givePower(Player p, int power)
    {
        if (power == Defs.PW_INVULNERABILITY) {
            p.powers[power] = Defs.INVULNTICS;
            return true;
        }
        if (power == Defs.PW_INVISIBILITY) {
            p.powers[power] = Defs.INVISTICS;
            if (p.mo != null) {
                p.mo.flags |= Defs.MF_SHADOW;
            }
            return true;
        }
        if (power == Defs.PW_INFRARED) {
            p.powers[power] = Defs.INFRATICS;
            return true;
        }
        if (power == Defs.PW_IRONFEET) {
            p.powers[power] = Defs.IRONTICS;
            return true;
        }
        if (power == Defs.PW_STRENGTH) {
            if (p.health < Defs.MAXHEALTH) {
                p.health = Math.min(Defs.MAXHEALTH, p.health + 100);
                if (p.mo != null) {
                    p.mo.health = p.health;
                }
            }
            p.powers[power] = 1;
            return true;
        }
        if (p.powers[power] != 0) {
            return false;
        }
        p.powers[power] = 1;
        return true;
    }

    public static Player spawnPlayer(World world, MapThing start)
    {
        return spawnPlayer(world, start, 0);
    }

    public static Player spawnPlayer(World world, MapThing start, int cheats)
    {
        int x = start.x * Defs.FRACUNIT;
        int y = start.y * Defs.FRACUNIT;
        Sector sec = Collision.pointInSubsector(world, x, y).sector;
        Mobj mo = new Mobj();
        mo.x = x;
        mo.y = y;
        mo.z = sec.floorheight;
        mo.angle = Compat.asU32(Compat.intdiv(start.angle, 45) * 0x20000000);
        mo.floorz = sec.floorheight;
        mo.ceilingz = sec.ceilingheight;
        Player p = new Player(mo, cheats);
        mo.player = p;
        p.health = Deh.INSTANCE.initialHealth;
        mo.health = Deh.INSTANCE.initialHealth;
        p.ammo = new int[] {Deh.INSTANCE.initialBullets, 0, 0, 0};
        p.maxammo = Deh.INSTANCE.maxammo.clone();
        if ((cheats & 1) != 0) {
            mo.flags |= Defs.MF_NOCLIP;
        }
        p.viewz = mo.z + Defs.VIEWHEIGHT;
        mo.lastlook = Enemy.publicRandom() % 4;
        world.mobjs.add(mo);
        Collision.setThingPosition(world, mo);
        return p;
    }

    public static void thrust(Mobj mo, int angle, int move)
    {
        mo.momx += Compat.fixedMul(move, Tables.fineCos(angle));
        mo.momy += Compat.fixedMul(move, Tables.fineSin(angle));
    }

    public static void calcHeight(Player p, int leveltime)
    {
        Mobj mo = p.mo;
        p.bob = Compat.intdiv(Compat.fixedMul(mo.momx, mo.momx) + Compat.fixedMul(mo.momy, mo.momy), 4);
        p.bob = Math.min(p.bob, Defs.MAXBOB);
        if (mo.z > mo.floorz) {
            p.viewz = Math.min(mo.z + p.viewheight, mo.ceilingz - 4 * Defs.FRACUNIT);
            return;
        }
        Tables.initTables();
        int angle = (Compat.intdiv(Defs.FINEANGLES, 20) * leveltime) & Defs.FINEMASK;
        int sine = (angle >= 0 && angle < Tables.finesine.length) ? Tables.finesine[angle] : 0;
        int bob = Compat.fixedMul(Compat.intdiv(p.bob, 2), sine);
        if (p.playerstate == Defs.PST_LIVE) {
            p.viewheight += p.deltaviewheight;
            if (p.viewheight > Defs.VIEWHEIGHT) {
                p.viewheight = Defs.VIEWHEIGHT;
                p.deltaviewheight = 0;
            }
            if (p.viewheight < Compat.intdiv(Defs.VIEWHEIGHT, 2)) {
                p.viewheight = Compat.intdiv(Defs.VIEWHEIGHT, 2);
                if (p.deltaviewheight <= 0) {
                    p.deltaviewheight = 1;
                }
            }
            if (p.deltaviewheight != 0) {
                int bump = Compat.intdiv(Defs.FRACUNIT, 4);
                if (bump == 0) {
                    bump = 1;
                }
                p.deltaviewheight += bump;
            }
        }
        p.viewz = Math.min(mo.z + p.viewheight + bob, mo.ceilingz - 4 * Defs.FRACUNIT);
    }

    public static void xyMovement(World world, Mobj mo, Game game)
    {
        Enemy.pXyMovement(world, mo, game);
    }

    public static void zMovement(Mobj mo, World world, Game game)
    {
        Enemy.mobjZ(mo, world, game);
    }

    private static void specialSector(World world, Player p, Game game, int time)
    {
        Mobj mo = p.mo;
        Sector sec = Collision.pointInSubsector(world, mo.x, mo.y).sector;
        if (mo.z != sec.floorheight || sec.special == 0) {
            return;
        }
        if (sec.special == 9) {
            p.secretcount++;
            sec.special = 0;
            return;
        }
        int sp = sec.special;
        if (sp == 5 || sp == 7 || sp == 4 || sp == 16 || sp == 11) {
            if (p.powers[Defs.PW_IRONFEET] != 0) {
                return;
            }
            if ((time & 0x1f) == 0) {
            int damage;
            if (sp == 7) {
                damage = 5;
            } else if (sp == 5) {
                damage = 10;
            } else {
                damage = 20;
            }
            game.damageMobj(mo, null, damage);
            if (sp == 11 && p.health <= 10 && game.specials != null) {
                game.specials.exitRequested = true;
            }
            }
        }
    }

    private static void deathThink(World world, Player p, Game game, int leveltime)
    {
        Mobj mo = p.mo;
        Ticcmd cmd = p.cmd;
        if (p.viewheight > 6 * Defs.FRACUNIT) {
            p.viewheight -= Defs.FRACUNIT;
        }
        if (p.viewheight < 6 * Defs.FRACUNIT) {
            p.viewheight = 6 * Defs.FRACUNIT;
        }
        p.deltaviewheight = 0;
        Player.xyMovement(world, mo, game);
        Player.zMovement(mo, world, game);
        Player.calcHeight(p, leveltime);
        if (p.attacker != null && p.attacker != mo) {
            int angle = Collision.angleTo(mo.x, mo.y, p.attacker.x, p.attacker.y);
            int delta = Compat.asU32(angle - mo.angle);
            int ang5 = Defs.ANG90 / 18;
            if (Integer.compareUnsigned(delta, ang5) < 0
                || Integer.compareUnsigned(delta, Compat.asU32(-ang5)) > 0) {
                mo.angle = angle;
                if (p.damagecount != 0) {
                    p.damagecount--;
                }
            } else if (Integer.compareUnsigned(delta, Defs.ANG180) < 0) {
                mo.angle = Compat.asU32(mo.angle + ang5);
            } else {
                mo.angle = Compat.asU32(mo.angle - ang5);
            }
        } else if (p.damagecount != 0) {
            p.damagecount--;
        }
        Player.weaponThink(p, game);
        if ((cmd.buttons & Defs.BT_USE) != 0) {
            p.playerstate = Defs.PST_REBORN;
        }
    }

    public static void playerThink(World world, Player p, Game game, int leveltime)
    {
        Mobj mo = p.mo;
        Ticcmd cmd = p.cmd;
        if (p.playerstate == Defs.PST_DEAD) {
            Player.deathThink(world, p, game, leveltime);
            return;
        }
        mo.angle = Compat.asU32(mo.angle + (cmd.angleturn << 16));
        if (mo.z <= mo.floorz) {
            if (cmd.forwardmove != 0) {
                Player.thrust(mo, mo.angle, cmd.forwardmove * 2048);
            }
            if (cmd.sidemove != 0) {
                Player.thrust(mo, Compat.asU32(mo.angle - Defs.ANG90), cmd.sidemove * 2048);
            }
        }
        Player.xyMovement(world, mo, game);
        Player.zMovement(mo, world, game);
        Player.calcHeight(p, leveltime);
        Player.specialSector(world, p, game, leveltime);
        if ((cmd.buttons & Defs.BT_USE) != 0) {
            if (!p.usedown) {
                Collision.useLines(world, p, game);
                p.usedown = true;
            }
        } else {
            p.usedown = false;
        }
        if ((cmd.buttons & Defs.BT_CHANGE) != 0) {
            int w = (cmd.buttons & Defs.BT_WEAPONMASK) >> Defs.BT_WEAPONSHIFT;
            if (w >= 0 && w <= Defs.WP_SUPERSHOTGUN && p.weaponowned[w] && w != p.readyweapon) {
                p.pendingweapon = w;
            }
        }
        Player.weaponThink(p, game);
        if (p.powers[Defs.PW_STRENGTH] != 0) {
            p.powers[Defs.PW_STRENGTH]++;
        }
        if (p.powers[Defs.PW_INVULNERABILITY] != 0) {
            p.powers[Defs.PW_INVULNERABILITY]--;
        }
        if (p.powers[Defs.PW_INVISIBILITY] != 0) {
            p.powers[Defs.PW_INVISIBILITY]--;
            if (p.powers[Defs.PW_INVISIBILITY] == 0 && p.mo != null) {
                p.mo.flags &= ~Defs.MF_SHADOW;
            }
        }
        if (p.powers[Defs.PW_INFRARED] != 0) {
            p.powers[Defs.PW_INFRARED]--;
        }
        if (p.powers[Defs.PW_IRONFEET] != 0) {
            p.powers[Defs.PW_IRONFEET]--;
        }
        int inv = p.powers[Defs.PW_INVULNERABILITY];
        int ir = p.powers[Defs.PW_INFRARED];
        if (inv != 0) {
            p.fixedcolormap = (inv > 4 * 32 || (inv & 8) != 0) ? Defs.INVERSECOLORMAP : 0;
        } else if (ir != 0) {
            p.fixedcolormap = (ir > 4 * 32 || (ir & 8) != 0) ? 1 : 0;
        } else {
            p.fixedcolormap = 0;
        }
        if (p.damagecount != 0) {
            p.damagecount--;
        }
        if (p.bonuscount != 0) {
            p.bonuscount--;
        }
        if (p.messageTics != 0) {
            p.messageTics--;
            if (p.messageTics <= 0) {
                p.message = "";
            }
        }
    }

    private static Integer[] weaponAmmo()
    {
        return WEAPON_AMMO;
    }

    private static String[] weaponPatch()
    {
        String[] patch = new String[9];
        patch[Defs.WP_FIST] = "PUNGA0";
        patch[Defs.WP_PISTOL] = "PISGA0";
        patch[Defs.WP_SHOTGUN] = "SHTGA0";
        patch[Defs.WP_CHAINGUN] = "CHGGA0";
        patch[Defs.WP_MISSILE] = "MISGA0";
        patch[Defs.WP_PLASMA] = "PLSGA0";
        patch[Defs.WP_BFG] = "BFGGA0";
        patch[Defs.WP_CHAINSAW] = "SAWGC0";
        patch[Defs.WP_SUPERSHOTGUN] = "SHT2A0";
        return patch;
    }

    private static String weaponPatchName(int weapon)
    {
        String[] patch = weaponPatch();
        if (weapon < 0 || weapon >= patch.length || patch[weapon] == null) {
            return "PISGA0";
        }
        return patch[weapon];
    }

    private static AttackStep[][] attackSequences()
    {
        AttackStep[][] seq = new AttackStep[9][];
        seq[Defs.WP_FIST] = new AttackStep[] {
            new AttackStep("PUNGB0", 4, 0, "", 0, 0),
            new AttackStep("PUNGC0", 4, 1, "", 0, 0),
            new AttackStep("PUNGD0", 5, 0, "", 0, 0),
            new AttackStep("PUNGC0", 4, 0, "", 0, 0),
            new AttackStep("PUNGB0", 5, 0, "", 0, 0),
        };
        seq[Defs.WP_PISTOL] = new AttackStep[] {
            new AttackStep("PISGA0", 4, 0, "", 0, 0),
            new AttackStep("PISGB0", 6, 1, "PISFA0", 7, 1),
            new AttackStep("PISGC0", 4, 0, "", 0, 0),
            new AttackStep("PISGB0", 5, 0, "", 0, 0),
        };
        seq[Defs.WP_SHOTGUN] = new AttackStep[] {
            new AttackStep("SHTGA0", 3, 0, "", 0, 0),
            new AttackStep("SHTGA0", 7, 1, "SHTFA0", 7, 1),
            new AttackStep("SHTGB0", 5, 0, "", 0, 0),
            new AttackStep("SHTGC0", 5, 0, "", 0, 0),
            new AttackStep("SHTGD0", 4, 0, "", 0, 0),
            new AttackStep("SHTGC0", 5, 0, "", 0, 0),
            new AttackStep("SHTGB0", 5, 0, "", 0, 0),
            new AttackStep("SHTGA0", 3, 0, "", 0, 0),
            new AttackStep("SHTGA0", 7, 0, "", 0, 0),
        };
        seq[Defs.WP_CHAINGUN] = new AttackStep[] {
            new AttackStep("CHGGA0", 4, 1, "CHGFA0", 5, 1),
            new AttackStep("CHGGB0", 4, 1, "CHGFB0", 5, 2),
        };
        seq[Defs.WP_MISSILE] = new AttackStep[] {
            new AttackStep("MISGB0", 8, 0, "MISFA0", 15, 1),
            new AttackStep("MISGB0", 12, 1, "", 0, 2),
        };
        seq[Defs.WP_PLASMA] = new AttackStep[] {
            new AttackStep("PLSGA0", 3, 1, "PLSFA0", 4, 1),
            new AttackStep("PLSGB0", 20, 0, "", 0, 0),
        };
        seq[Defs.WP_BFG] = new AttackStep[] {
            new AttackStep("BFGGA0", 20, 0, "", 0, 0),
            new AttackStep("BFGGB0", 10, 0, "BFGFA0", 17, 1),
            new AttackStep("BFGGB0", 10, 1, "", 0, 2),
            new AttackStep("BFGGB0", 20, 0, "", 0, 0),
        };
        seq[Defs.WP_CHAINSAW] = new AttackStep[] {
            new AttackStep("SAWGA0", 4, 1, "", 0, 0),
            new AttackStep("SAWGB0", 4, 1, "", 0, 0),
        };
        seq[Defs.WP_SUPERSHOTGUN] = new AttackStep[] {
            new AttackStep("SHT2A0", 3, 0, "", 0, 0),
            new AttackStep("SHT2A0", 7, 1, "SHT2I0", 9, 1),
            new AttackStep("SHT2B0", 7, 0, "", 0, 0),
            new AttackStep("SHT2C0", 7, 0, "", 0, 0),
            new AttackStep("SHT2D0", 7, 0, "", 0, 0),
            new AttackStep("SHT2E0", 7, 0, "", 0, 0),
            new AttackStep("SHT2F0", 7, 0, "", 0, 0),
            new AttackStep("SHT2G0", 6, 0, "", 0, 0),
            new AttackStep("SHT2H0", 6, 0, "", 0, 0),
            new AttackStep("SHT2A0", 5, 0, "", 0, 0),
        };
        return seq;
    }

    private static void weaponThink(Player p, Game game)
    {
        if (p.playerstate == Defs.PST_DEAD || p.health <= 0) {
            Player.lowerWeapon(p, game);
            return;
        }
        boolean firing = (p.cmd.buttons & Defs.BT_ATTACK) != 0;
        Integer[] ammoMap = Player.weaponAmmo();
        Integer ammo = ammoFor(ammoMap, p.readyweapon);
        int need = Player.ammoNeeded(p.readyweapon);
        boolean can = ammo == null || p.ammo[ammo] >= need;
        if (!can) {
            int[] fallback = {
                Defs.WP_PISTOL,
                Defs.WP_SHOTGUN,
                Defs.WP_CHAINGUN,
                Defs.WP_MISSILE,
                Defs.WP_PLASMA,
                Defs.WP_BFG,
                Defs.WP_FIST,
            };
            for (int w : fallback) {
                Integer a = ammoFor(ammoMap, w);
                if (p.weaponowned[w] && (a == null || p.ammo[a] >= Player.ammoNeeded(w))) {
                    p.pendingweapon = w;
                    break;
                }
            }
        }
        if (p.flashTics > 0) {
            p.flashTics--;
            if (p.flashTics <= 0) {
                p.pspriteFlash = "";
                p.extralight = 0;
            }
        }
        if ("fire".equals(p.pspriteState)) {
            p.pspriteState = "atk";
        }
        if ("atk".equals(p.pspriteState)) {
            if (firing) {
                p.attackdown = true;
            }
            if (p.pspriteTics > 0) {
                p.pspriteTics--;
            }
            if (p.pspriteTics > 0) {
                return;
            }
            p.pspriteStep++;
            Player.enterAttackStep(p, game, ammo, firing, can);
            return;
        }
        if (p.pendingweapon != Defs.WP_NOCHANGE || "down".equals(p.pspriteState)) {
            Player.lowerWeapon(p, game);
            return;
        }
        if ("up".equals(p.pspriteState)) {
            Player.raiseWeapon(p, game);
            return;
        }
        if (firing && can && (!p.attackdown || (p.readyweapon != Defs.WP_MISSILE && p.readyweapon != Defs.WP_BFG))) {
            p.pspriteState = "atk";
            p.pspriteStep = 0;
            p.pspriteSy = Sprites.WEAPONTOP;
            p.attackdown = true;
            Player.enterAttackStep(p, game, ammo, firing, can);
            return;
        }
        if (!"ready".equals(p.pspriteState)) {
            Player.startReady(p, game);
            return;
        }
        Player.tickReady(p, game);
        if (!firing) {
            p.attackdown = false;
            p.refire = 0;
        }
    }

    private static Integer ammoFor(Integer[] ammoMap, int weapon)
    {
        if (weapon < 0 || weapon >= ammoMap.length) {
            return null;
        }
        return ammoMap[weapon];
    }

    private static void lowerWeapon(Player p, Game game)
    {
        p.pspriteState = "down";
        if (p.pspriteBody == null || p.pspriteBody.isEmpty()) {
            p.pspriteBody = weaponPatchName(p.readyweapon);
        }
        p.pspriteSy += Sprites.LOWERSPEED;
        if (p.pspriteSy < Sprites.WEAPONBOTTOM) {
            return;
        }
        p.pspriteSy = Sprites.WEAPONBOTTOM;
        if (p.playerstate == Defs.PST_DEAD || p.health <= 0) {
            return;
        }
        if (p.pendingweapon != Defs.WP_NOCHANGE) {
            p.readyweapon = p.pendingweapon;
            p.pendingweapon = Defs.WP_NOCHANGE;
        }
        if (p.readyweapon == Defs.WP_CHAINSAW) {
            game.startSound("sawup");
        }
        p.pspriteState = "up";
        p.pspriteBody = weaponPatchName(p.readyweapon);
    }

    private static void raiseWeapon(Player p, Game game)
    {
        p.pspriteSy -= Sprites.RAISESPEED;
        if (p.pspriteBody == null || p.pspriteBody.isEmpty()) {
            p.pspriteBody = weaponPatchName(p.readyweapon);
        }
        if (p.pspriteSy > Sprites.WEAPONTOP) {
            return;
        }
        p.pspriteSy = Sprites.WEAPONTOP;
        Player.startReady(p, game);
    }

    private static void startReady(Player p, Game game)
    {
        p.pspriteState = "ready";
        p.pspriteStep = 0;
        if (p.readyweapon != Defs.WP_CHAINSAW) {
            p.pspriteBody = weaponPatchName(p.readyweapon);
            p.pspriteTics = 0;
            return;
        }
        p.pspriteBody = "SAWGC0";
        p.pspriteTics = 4;
        game.startSound("sawidl");
    }

    private static void tickReady(Player p, Game game)
    {
        if (p.readyweapon != Defs.WP_CHAINSAW) {
            p.pspriteBody = weaponPatchName(p.readyweapon);
            return;
        }
        if (p.pspriteTics > 0) {
            p.pspriteTics--;
            if (p.pspriteTics > 0) {
                return;
            }
        }
        p.pspriteStep = (p.pspriteStep + 1) % 2;
        p.pspriteBody = p.pspriteStep != 0 ? "SAWGD0" : "SAWGC0";
        p.pspriteTics = 4;
        if (p.pspriteStep == 0) {
            game.startSound("sawidl");
        }
    }

    private static void enterAttackStep(Player p, Game game, Integer ammo, boolean firing, boolean can)
    {
        AttackStep[][] all = Player.attackSequences();
        AttackStep[] seq;
        if (p.readyweapon >= 0 && p.readyweapon < all.length && all[p.readyweapon] != null) {
            seq = all[p.readyweapon];
        } else {
            seq = all[Defs.WP_PISTOL];
        }
        while (true) {
            if (p.pspriteStep >= seq.length) {
                if (firing && can && p.pendingweapon == Defs.WP_NOCHANGE) {
                    p.pspriteStep = 0;
                    continue;
                }
                Player.startReady(p, game);
                if (!firing) {
                    p.attackdown = false;
                    p.refire = 0;
                }
                return;
            }
            AttackStep step = seq[p.pspriteStep];
            p.pspriteBody = step.body;
            p.pspriteTics = step.tics;
            if (step.ft != 0) {
                p.pspriteFlash = step.flash;
                p.flashTics = step.ft;
            }
            if (step.light != 0) {
                p.extralight = step.light;
            }
            if (step.fire != 0) {
                Player.doShot(p, game, ammo);
            }
            if (step.tics > 0) {
                return;
            }
            p.pspriteStep++;
        }
    }

    private static int ammoNeeded(int weapon)
    {
        return weapon == Defs.WP_BFG ? Deh.INSTANCE.bfgCellsPerShot : 1;
    }

    private static void doShot(Player p, Game game, Integer ammo)
    {
        int need = Player.ammoNeeded(p.readyweapon);
        if (ammo != null) {
            if (p.ammo[ammo] < need) {
                return;
            }
            p.ammo[ammo] -= need;
        }
        boolean hit = false;
        Mobj mo = p.mo;
        int weapon = p.readyweapon;
        if (mo != null && (weapon == Defs.WP_MISSILE || weapon == Defs.WP_PLASMA || weapon == Defs.WP_BFG)) {
            if (weapon == Defs.WP_PLASMA) {
                int aim = Enemy.publicRandom() & 1;
                aim = Math.abs(aim) & 1;
            }
            if (weapon == Defs.WP_MISSILE) {
                Enemy.spawnPlayerMissile(game.world, mo, "MISL", 20 * Defs.FRACUNIT, 20, "rocket");
                game.startSound("rlaunc");
            } else if (weapon == Defs.WP_PLASMA) {
                Enemy.spawnPlayerMissile(game.world, mo, "PLSS", 25 * Defs.FRACUNIT, 5, "plasma");
                game.startSound("plasma");
            } else {
                Enemy.spawnPlayerMissile(game.world, mo, "BFS1", 25 * Defs.FRACUNIT, 100, "bfg");
                game.startSound("bfg");
            }
            p.refire++;
            p.attackdown = true;
            Enemy.noiseAlert(game.world, mo, game);
            return;
        }
        if (mo != null && weapon == Defs.WP_FIST) {
            int damage = ((Enemy.publicRandom() % 10) + 1) * 2;
            if (p.powers[Defs.PW_STRENGTH] != 0) {
                damage *= 10;
            }
            int angle = Compat.asU32(mo.angle + (Enemy.publicRandom() - Enemy.publicRandom()) * 262144);
            hit = Collision.lineAttack(game.world, mo, damage, game, Defs.MELEERANGE, angle);
            if (hit) {
                game.startSound("punch");
            }
        } else if (mo != null && weapon == Defs.WP_CHAINSAW) {
            int damage = 2 * ((Enemy.publicRandom() % 10) + 1);
            int angle = Compat.asU32(mo.angle + (Enemy.publicRandom() - Enemy.publicRandom()) * 262144);
            hit = Collision.lineAttack(game.world, mo, damage, game, Defs.MELEERANGE + 1, angle);
            game.startSound(hit ? "sawhit" : "sawful");
        } else if (mo != null && weapon == Defs.WP_SHOTGUN) {
            game.startSound("shotgn");
            for (int i = 0; i < 7; i++) {
                if (Player.gunShot(p, game, false)) {
                    hit = true;
                }
            }
        } else if (mo != null && weapon == Defs.WP_SUPERSHOTGUN) {
            game.startSound("dshtgn");
            int slope = Collision.bulletSlope(game.world, mo);
            for (int i = 0; i < 20; i++) {
                int damage = 5 * ((Enemy.publicRandom() % 3) + 1);
                int angle = Compat.asU32(mo.angle + (Enemy.publicRandom() - Enemy.publicRandom()) * 524288);
                int pellet = slope + (Enemy.publicRandom() - Enemy.publicRandom()) * 32;
                if (Collision.lineAttack(game.world, mo, damage, game, Defs.MISSILERANGE, angle, pellet)) {
                    hit = true;
                }
            }
        } else if (mo != null) {
            game.startSound("pistol");
            Player.gunShot(p, game, p.refire == 0);
        }
        p.refire++;
        p.attackdown = true;
        if (mo != null) {
            Enemy.noiseAlert(game.world, mo, game);
        }
    }

    private static boolean gunShot(Player p, Game game, boolean accurate)
    {
        Mobj mo = p.mo;
        int slope = Collision.bulletSlope(game.world, mo);
        int damage = 5 * ((Enemy.publicRandom() % 3) + 1);
        int angle = mo.angle;
        if (!accurate) {
            angle = Compat.asU32(angle + (Enemy.publicRandom() - Enemy.publicRandom()) * 262144);
        }
        return Collision.lineAttack(game.world, mo, damage, game, Defs.MISSILERANGE, angle, slope);
    }

    public static String currentWeaponPatch(Player p)
    {
        if (p.pspriteBody != null && !p.pspriteBody.isEmpty()) {
            return p.pspriteBody;
        }
        if ("atk".equals(p.pspriteState) || "fire".equals(p.pspriteState)) {
            switch (p.readyweapon) {
                case Defs.WP_FIST:
                    return "PUNGC0";
                case Defs.WP_PISTOL:
                    return "PISGB0";
                case Defs.WP_SHOTGUN:
                    return "SHTGA0";
                case Defs.WP_CHAINGUN:
                    return "CHGGB0";
                case Defs.WP_MISSILE:
                    return "MISGB0";
                case Defs.WP_PLASMA:
                    return "PLSGA0";
                case Defs.WP_BFG:
                    return "BFGGB0";
                case Defs.WP_CHAINSAW:
                    return "SAWGA0";
                case Defs.WP_SUPERSHOTGUN:
                    return "SHT2A0";
                default:
                    return "PISGA0";
            }
        }
        return weaponPatchName(p.readyweapon);
    }

    private static final class AttackStep
    {
        final String body;
        final int tics;
        final int fire;
        final String flash;
        final int ft;
        final int light;

        AttackStep(String body, int tics, int fire, String flash, int ft, int light)
        {
            this.body = body;
            this.tics = tics;
            this.fire = fire;
            this.flash = flash;
            this.ft = ft;
            this.light = light;
        }
    }
}
