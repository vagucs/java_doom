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

public class Mobj
{
    public int x;
    public int y;
    public int z;
    public int angle;
    public int momx;
    public int momy;
    public int momz;
    public int radius = 16 * Defs.FRACUNIT;
    public int height = 56 * Defs.FRACUNIT;
    public int floorz;
    public int ceilingz;
    public int flags = Defs.MF_SOLID | Defs.MF_SHOOTABLE | Defs.MF_PICKUP | Defs.MF_DROPOFF;
    public int health = 100;
    public Player player;
    public int type = 1;
    public String sprite = "";
    public Object[] info;
    public boolean alive = true;
    public int reactiontime;
    public Mobj target;
    public int movedir = 8;
    public int movecount;
    public String aiState = "";
    public int frame;
    public int tics;
    public int chaseTics;
    public boolean justAttacked;
    public int damage;
    public int tmx;
    public int tmy;
    public Mobj pickup;
    public String attackKind = "hitscan";
    public boolean didFire;

    public Mobj()
    {
        this.tmx = this.x;
        this.tmy = this.y;
    }

    public static Map<Integer, Object[]> infoTable()
    {
        int enemy = Defs.MF_SOLID | Defs.MF_SHOOTABLE;
        int special = Defs.MF_SPECIAL;
        int solid = Defs.MF_SOLID;
        Map<Integer, Object[]> table = new HashMap<>();
        table.put(3004, new Object[] { "POSSA1", 20, 56, 20, enemy, "enemy", "posit1" });
        table.put(9, new Object[] { "SPOSA1", 20, 56, 30, enemy, "enemy", "posit1" });
        table.put(3001, new Object[] { "TROOA1", 20, 56, 60, enemy, "enemy", "bgsit1" });
        table.put(3002, new Object[] { "SARGA1", 30, 56, 150, enemy, "enemy", "sgtsit" });
        table.put(3003, new Object[] { "BOSSA1", 24, 64, 1000, enemy, "enemy", "brssit" });
        table.put(3005, new Object[] { "HEADA1", 31, 56, 400, enemy, "enemy", "cacsit" });
        table.put(3006, new Object[] { "SKULA1", 16, 56, 100, enemy, "enemy", "sklatk" });
        table.put(16, new Object[] { "CYBRA1", 40, 110, 4000, enemy, "enemy", "cybsit" });
        table.put(7, new Object[] { "SPIDA1", 128, 100, 3000, enemy, "enemy", "spisit" });
        table.put(68, new Object[] { "BSPIA1", 64, 64, 500, enemy, "enemy", "bspsit" });
        table.put(69, new Object[] { "BOS2A1", 24, 64, 500, enemy, "enemy", "kntsit" });
        table.put(64, new Object[] { "VILEA1", 20, 56, 700, enemy, "enemy", "vilsit" });
        table.put(66, new Object[] { "SKELA1", 20, 56, 500, enemy, "enemy", "skesit" });
        table.put(67, new Object[] { "FATTA1", 48, 64, 600, enemy, "enemy", "mansit" });
        table.put(71, new Object[] { "PAINA1", 31, 56, 400, enemy, "enemy", "pesit" });
        table.put(84, new Object[] { "SSWVA1", 20, 56, 50, enemy, "enemy", "posit1" });
        table.put(72, new Object[] { "KEENA1", 16, 72, 100, enemy, "enemy", "keenpn" });
        table.put(2035, new Object[] { "BAR1A0", 10, 42, 20, enemy, "enemy", null });
        table.put(2011, new Object[] { "STIMA0", 20, 16, 0, special, "health", 10 });
        table.put(2012, new Object[] { "MEDIA0", 20, 16, 0, special, "health", 25 });
        table.put(2014, new Object[] { "BON1A0", 20, 16, 0, special, "bonus_h", 1 });
        table.put(2015, new Object[] { "BON2A0", 20, 16, 0, special, "bonus_a", 1 });
        table.put(2018, new Object[] { "ARM1A0", 20, 16, 0, special, "armor", 1 });
        table.put(2019, new Object[] { "ARM2A0", 20, 16, 0, special, "armor", 2 });
        table.put(83, new Object[] { "MEGAA0", 20, 16, 0, special, "mega", 0 });
        table.put(2013, new Object[] { "SOULA0", 20, 16, 0, special, "soul", 0 });
        table.put(2022, new Object[] { "PINVA0", 20, 16, 0, special, "item", "Invulnerability" });
        table.put(2023, new Object[] { "PSTRA0", 20, 16, 0, special, "berserk", 0 });
        table.put(2024, new Object[] { "PINSA0", 20, 16, 0, special, "item", "Partial invisibility" });
        table.put(2025, new Object[] { "SUITA0", 20, 16, 0, special, "item", "Radiation shielding" });
        table.put(2026, new Object[] { "PMAPA0", 20, 16, 0, special, "item", "Computer area map" });
        table.put(2045, new Object[] { "PVISA0", 20, 16, 0, special, "item", "Light amplification visor" });
        table.put(5, new Object[] { "BKEYA0", 20, 16, 0, special, "key", Defs.IT_BLUECARD });
        table.put(6, new Object[] { "YKEYA0", 20, 16, 0, special, "key", Defs.IT_YELLOWCARD });
        table.put(13, new Object[] { "RKEYA0", 20, 16, 0, special, "key", Defs.IT_REDCARD });
        table.put(40, new Object[] { "BSKUA0", 20, 16, 0, special, "key", Defs.IT_BLUESKULL });
        table.put(39, new Object[] { "YSKUA0", 20, 16, 0, special, "key", Defs.IT_YELLOWSKULL });
        table.put(38, new Object[] { "RSKUA0", 20, 16, 0, special, "key", Defs.IT_REDSKULL });
        table.put(2001, new Object[] { "SHOTA0", 20, 16, 0, special, "weapon", Defs.WP_SHOTGUN });
        table.put(82, new Object[] { "SGN2A0", 20, 16, 0, special, "weapon", Defs.WP_SUPERSHOTGUN });
        table.put(2002, new Object[] { "MGUNA0", 20, 16, 0, special, "weapon", Defs.WP_CHAINGUN });
        table.put(2003, new Object[] { "LAUNA0", 20, 16, 0, special, "weapon", Defs.WP_MISSILE });
        table.put(2004, new Object[] { "PLASA0", 20, 16, 0, special, "weapon", Defs.WP_PLASMA });
        table.put(2005, new Object[] { "CSAWA0", 20, 16, 0, special, "weapon", Defs.WP_CHAINSAW });
        table.put(2006, new Object[] { "BFUGA0", 20, 16, 0, special, "weapon", Defs.WP_BFG });
        table.put(2007, new Object[] { "CLIPA0", 20, 16, 0, special, "ammo", new int[] { Defs.AM_CLIP, 1 } });
        table.put(2048, new Object[] { "AMMOA0", 20, 16, 0, special, "ammo", new int[] { Defs.AM_CLIP, 5 } });
        table.put(2008, new Object[] { "SHELA0", 20, 16, 0, special, "ammo", new int[] { Defs.AM_SHELL, 1 } });
        table.put(2049, new Object[] { "SBOXA0", 20, 16, 0, special, "ammo", new int[] { Defs.AM_SHELL, 5 } });
        table.put(2047, new Object[] { "CELLA0", 20, 16, 0, special, "ammo", new int[] { Defs.AM_CELL, 1 } });
        table.put(17, new Object[] { "CELPA0", 20, 16, 0, special, "ammo", new int[] { Defs.AM_CELL, 5 } });
        table.put(2010, new Object[] { "ROCKA0", 20, 16, 0, special, "ammo", new int[] { Defs.AM_MISL, 1 } });
        table.put(2046, new Object[] { "BROKA0", 20, 16, 0, special, "ammo", new int[] { Defs.AM_MISL, 5 } });
        table.put(8, new Object[] { "BPAKA0", 20, 16, 0, special, "backpack", 0 });
        table.put(2028, new Object[] { "COLUA0", 16, 16, 0, solid, "deco", null });
        table.put(85, new Object[] { "TLMPA0", 16, 16, 0, solid, "deco", null });
        table.put(86, new Object[] { "TLP2A0", 16, 16, 0, solid, "deco", null });
        table.put(48, new Object[] { "ELECA0", 16, 16, 0, solid, "deco", null });
        table.put(30, new Object[] { "COL1A0", 16, 16, 0, solid, "deco", null });
        table.put(31, new Object[] { "COL2A0", 16, 16, 0, solid, "deco", null });
        table.put(32, new Object[] { "COL3A0", 16, 16, 0, solid, "deco", null });
        table.put(33, new Object[] { "COL4A0", 16, 16, 0, solid, "deco", null });
        table.put(35, new Object[] { "CANDA0", 16, 16, 0, 0, "deco", null });
        table.put(37, new Object[] { "CBRAA0", 16, 16, 0, solid, "deco", null });
        table.put(41, new Object[] { "CEYEA0", 16, 16, 0, solid, "deco", null });
        table.put(42, new Object[] { "FSKUA0", 16, 16, 0, solid, "deco", null });
        table.put(43, new Object[] { "TRE1A0", 16, 16, 0, solid, "deco", null });
        table.put(47, new Object[] { "SMITA0", 16, 16, 0, solid, "deco", null });
        table.put(54, new Object[] { "TRE2A0", 32, 16, 0, solid, "deco", null });
        table.put(10, new Object[] { "PLAYW0", 16, 16, 0, 0, "deco", null });
        table.put(12, new Object[] { "PLAYW0", 16, 16, 0, 0, "deco", null });
        table.put(15, new Object[] { "PLAYN0", 16, 16, 0, 0, "deco", null });
        table.put(24, new Object[] { "POL5A0", 16, 16, 0, 0, "deco", null });
        table.put(25, new Object[] { "POL1A0", 16, 16, 0, solid, "deco", null });
        table.put(26, new Object[] { "POL6A0", 16, 16, 0, solid, "deco", null });
        table.put(27, new Object[] { "POL4A0", 16, 16, 0, solid, "deco", null });
        table.put(28, new Object[] { "POL2A0", 16, 16, 0, solid, "deco", null });
        table.put(34, new Object[] { "CANDA0", 16, 16, 0, 0, "deco", null });
        table.put(14, new Object[] { "TFOGA0", 20, 16, 0, 0, "teleport", null });
        table.put(36, new Object[] { "CBRAA0", 16, 16, 0, solid, "deco", null });
        table.put(46, new Object[] { "TREDA0", 16, 16, 0, 0, "deco", null });
        table.put(55, new Object[] { "GOR1A0", 16, 16, 0, 0, "deco", null });
        table.put(56, new Object[] { "GOR2A0", 16, 16, 0, 0, "deco", null });
        table.put(57, new Object[] { "GOR3A0", 16, 16, 0, 0, "deco", null });
        table.put(58, new Object[] { "GOR4A0", 16, 16, 0, 0, "deco", null });
        table.put(59, new Object[] { "GOR5A0", 16, 16, 0, 0, "deco", null });
        return table;
    }

    public static int skillBit(int skill)
    {
        if (skill <= Defs.SK_EASY) {
            return 1;
        }
        return skill == Defs.SK_NIGHTMARE || skill >= Defs.SK_HARD ? 4 : 2;
    }

    public static int[] spawnMapThings(World world, int skill)
    {
        int bit = skillBit(skill);
        int kills = 0;
        int items = 0;
        Map<Integer, Object[]> table = infoTable();
        for (MapThing mt : world.things) {
            if (mt.type == 14) {
                int x = mt.x * Defs.FRACUNIT;
                int y = mt.y * Defs.FRACUNIT;
                Sector sec = Collision.pointInSubsector(world, x, y).sector;
                Mobj mo = new Mobj();
                mo.x = x;
                mo.y = y;
                mo.z = sec.floorheight;
                mo.angle = Compat.asU32(Compat.intdiv(mt.angle, 45) * 0x20000000);
                mo.radius = 20 * Defs.FRACUNIT;
                mo.height = 16 * Defs.FRACUNIT;
                mo.floorz = sec.floorheight;
                mo.ceilingz = sec.ceilingheight;
                mo.flags = 0;
                mo.health = 1000;
                mo.type = 14;
                mo.sprite = "";
                mo.info = new Object[] { "teleport", null };
                mo.tmx = mo.x;
                mo.tmy = mo.y;
                world.mobjs.add(mo);
                continue;
            }
            if (mt.type == 1 || mt.type == 2 || mt.type == 3 || mt.type == 4
                || mt.type == 11 || mt.type == 87 || mt.type == 89 || mt.type == 88
                || (mt.options & bit) == 0
                || (mt.options & 16) != 0
                || !table.containsKey(mt.type)) {
                continue;
            }
            Object[] entry = table.get(mt.type);
            String sprite = (String) entry[0];
            int rad = (Integer) entry[1];
            int h = (Integer) entry[2];
            int health = (Integer) entry[3];
            int flags = (Integer) entry[4];
            String kind = (String) entry[5];
            Object extra = entry[6];
            if ("enemy".equals(kind) && mt.type != 2035) {
                flags |= Defs.MF_COUNTKILL;
                kills++;
            } else if ("bonus_h".equals(kind) || "bonus_a".equals(kind) || "soul".equals(kind)
                || "mega".equals(kind) || "berserk".equals(kind)
                || ("item".equals(kind) && !"Radiation shielding".equals(extra))) {
                flags |= Defs.MF_COUNTITEM;
                items++;
            }
            if ((mt.options & Defs.MTF_AMBUSH) != 0) {
                flags |= Defs.MF_AMBUSH;
            }
            int frame = 0;
            if (sprite.length() >= 5) {
                char ch = sprite.charAt(4);
                if (ch >= 'A' && ch <= ']') {
                    frame = ch - 65;
                }
            }
            int x = mt.x * Defs.FRACUNIT;
            int y = mt.y * Defs.FRACUNIT;
            Sector sec = Collision.pointInSubsector(world, x, y).sector;
            Mobj mo = new Mobj();
            mo.x = x;
            mo.y = y;
            mo.z = sec.floorheight;
            mo.angle = Compat.asU32(Compat.intdiv(mt.angle, 45) * 0x20000000);
            mo.radius = rad * Defs.FRACUNIT;
            mo.height = h * Defs.FRACUNIT;
            mo.floorz = sec.floorheight;
            mo.ceilingz = sec.ceilingheight;
            mo.flags = flags;
            mo.health = health != 0 ? health : 1000;
            mo.type = mt.type;
            String upper = sprite.toUpperCase();
            mo.sprite = upper.length() <= 4 ? upper : upper.substring(0, 4);
            mo.info = new Object[] { kind, extra };
            mo.aiState = "enemy".equals(kind) ? "look" : "";
            mo.frame = frame;
            mo.tics = "enemy".equals(kind) ? 10 : 0;
            mo.reactiontime = "enemy".equals(kind) ? 8 : 0;
            mo.tmx = mo.x;
            mo.tmy = mo.y;
            world.mobjs.add(mo);
        }
        return new int[] { kills, items };
    }

    public static boolean giveAmmo(Player p, int ammo, int num)
    {
        if (p.ammo[ammo] >= p.maxammo[ammo]) {
            return false;
        }
        p.ammo[ammo] = Math.min(p.maxammo[ammo], p.ammo[ammo] + Player.CLIPAMMO[ammo] * num);
        return true;
    }

    public static void touchSpecial(Game game, Mobj special, Mobj toucher)
    {
        Player p = toucher.player;
        if (p == null || !special.alive) {
            return;
        }
        Object[] pair = special.info != null ? special.info : new Object[] { "deco", null };
        String kind = (String) pair[0];
        Object extra = pair[1];
        boolean taken = true;
        if ("health".equals(kind)) {
            if (p.health >= Defs.MAXHEALTH) {
                taken = false;
            } else {
                int extraN = (Integer) extra;
                p.health = Math.min(Defs.MAXHEALTH, p.health + extraN);
                p.mo.health = p.health;
                p.setMessage(extraN == 10 ? "Picked up a stimpack." : "Picked up a medikit.");
            }
        } else if ("bonus_h".equals(kind)) {
            p.health = Math.min(200, p.health + 1);
            p.mo.health = p.health;
            p.setMessage("You pick up a health bonus.");
        } else if ("bonus_a".equals(kind)) {
            p.armorpoints = Math.min(200, p.armorpoints + 1);
            if (p.armortype == 0) {
                p.armortype = 1;
            }
            p.setMessage("You pick up an armor bonus.");
        } else if ("armor".equals(kind)) {
            int extraN = (Integer) extra;
            int points = extraN == 1 ? 100 : 200;
            if (p.armorpoints >= points) {
                taken = false;
            } else {
                p.armorpoints = points;
                p.armortype = extraN;
                p.setMessage(extraN == 1 ? "Picked up the armor." : "Picked up the MegaArmor!");
            }
        } else if ("soul".equals(kind)) {
            p.health = Math.min(200, p.health + 100);
            p.mo.health = p.health;
            p.setMessage("Supercharge!");
        } else if ("mega".equals(kind)) {
            p.health = 200;
            p.mo.health = 200;
            p.armorpoints = 200;
            p.armortype = 2;
            p.setMessage("MegaSphere!");
        } else if ("berserk".equals(kind)) {
            p.health = Math.max(p.health, 100);
            p.mo.health = p.health;
            p.setMessage("Berserk!");
        } else if ("key".equals(kind)) {
            int extraN = (Integer) extra;
            p.cards[extraN] = true;
            String names;
            if (extraN == Defs.IT_BLUECARD) {
                names = "You picked up a blue keycard.";
            } else if (extraN == Defs.IT_YELLOWCARD) {
                names = "You picked up a yellow keycard.";
            } else if (extraN == Defs.IT_REDCARD) {
                names = "You picked up a red keycard.";
            } else if (extraN == Defs.IT_BLUESKULL) {
                names = "You picked up a blue skull key.";
            } else if (extraN == Defs.IT_YELLOWSKULL) {
                names = "You picked up a yellow skull key.";
            } else if (extraN == Defs.IT_REDSKULL) {
                names = "You picked up a red skull key.";
            } else {
                names = "You picked up a key.";
            }
            p.setMessage(names);
        } else if ("weapon".equals(kind)) {
            int w = (Integer) extra;
            p.weaponowned[w] = true;
            if (p.readyweapon != w) {
                p.pendingweapon = w;
            }
            if (w == Defs.WP_SHOTGUN || w == Defs.WP_SUPERSHOTGUN) {
                giveAmmo(p, Defs.AM_SHELL, 1);
            } else if (w == Defs.WP_CHAINGUN) {
                giveAmmo(p, Defs.AM_CLIP, 1);
            } else if (w == Defs.WP_MISSILE) {
                giveAmmo(p, Defs.AM_MISL, 1);
            } else if (w == Defs.WP_PLASMA || w == Defs.WP_BFG) {
                giveAmmo(p, Defs.AM_CELL, 1);
            }
            String names;
            if (w == Defs.WP_SHOTGUN) {
                names = "You got the shotgun!";
            } else if (w == Defs.WP_SUPERSHOTGUN) {
                names = "You got the super shotgun!";
            } else if (w == Defs.WP_CHAINGUN) {
                names = "You got the chaingun!";
            } else if (w == Defs.WP_MISSILE) {
                names = "You got the rocket launcher!";
            } else if (w == Defs.WP_PLASMA) {
                names = "You got the plasma gun!";
            } else if (w == Defs.WP_BFG) {
                names = "You got the BFG9000!";
            } else if (w == Defs.WP_CHAINSAW) {
                names = "A chainsaw!  Find some meat!";
            } else {
                names = "You got a weapon!";
            }
            p.setMessage(names);
            game.startSound("wpnup");
        } else if ("ammo".equals(kind)) {
            int[] ammoExtra = (int[]) extra;
            taken = giveAmmo(p, ammoExtra[0], ammoExtra[1]);
            if (taken) {
                p.setMessage("Picked up some ammo.");
            }
        } else if ("backpack".equals(kind)) {
            for (int i = 0; i < 4; i++) {
                if (p.maxammo[i] < 400) {
                    p.maxammo[i] *= 2;
                }
                giveAmmo(p, i, 1);
            }
            p.setMessage("You picked up a backpack full of ammo!");
        } else if ("item".equals(kind)) {
            p.setMessage(String.valueOf(extra));
        } else {
            taken = false;
        }
        if (taken) {
            if (!"weapon".equals(kind)) {
                game.startSound("itemup");
            }
            p.bonuscount += 6;
            if ((special.flags & Defs.MF_COUNTITEM) != 0) {
                p.itemcount++;
            }
            special.alive = false;
            special.flags = 0;
            int i = game.world.mobjs.indexOf(special);
            if (i != -1) {
                game.world.mobjs.remove(i);
            }
        }
    }
}
