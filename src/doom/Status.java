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

/** Status bar (st_stuff / st_lib), including vanilla HUD face widget. */
public final class Status
{
    private static final int AMMO_X = 44;
    private static final int AMMO_Y = 171;
    private static final int HEALTH_X = 90;
    private static final int HEALTH_Y = 171;
    private static final int ARMOR_X = 221;
    private static final int ARMOR_Y = 171;
    private static final int FACE_X = 143;
    private static final int FACE_Y = 168;
    private static final int ARMS_X = 111;
    private static final int ARMS_Y = 172;
    private static final int KEY_X = 239;

    private static final int ST_NUMPAINFACES = 5;
    private static final int ST_NUMSTRAIGHTFACES = 3;
    private static final int ST_NUMTURNFACES = 2;
    private static final int ST_NUMSPECIALFACES = 3;
    private static final int ST_FACESTRIDE =
        ST_NUMSTRAIGHTFACES + ST_NUMTURNFACES + ST_NUMSPECIALFACES;
    private static final int ST_TURNOFFSET = ST_NUMSTRAIGHTFACES;
    private static final int ST_OUCHOFFSET = ST_TURNOFFSET + ST_NUMTURNFACES;
    private static final int ST_EVILGRINOFFSET = ST_OUCHOFFSET + 1;
    private static final int ST_RAMPAGEOFFSET = ST_EVILGRINOFFSET + 1;
    private static final int ST_GODFACE = ST_NUMPAINFACES * ST_FACESTRIDE;
    private static final int ST_DEADFACE = ST_GODFACE + 1;
    private static final int ST_EVILGRINCOUNT = 2 * Defs.TICRATE;
    private static final int ST_STRAIGHTFACECOUNT = Defs.TICRATE / 2;
    private static final int ST_TURNCOUNT = Defs.TICRATE;
    private static final int ST_RAMPAGEDELAY = 2 * Defs.TICRATE;
    private static final int ST_MUCHPAIN = 20;

    private final Wad wad;
    private final byte[] sbar;
    private final List<byte[]> tallNum = new ArrayList<>();
    private final List<byte[]> shortNum = new ArrayList<>();
    private final byte[] percent;
    private final List<byte[]> keys = new ArrayList<>();
    private final byte[] armsBg;
    private final List<byte[]> armsOff = new ArrayList<>();
    private final List<byte[]> faces = new ArrayList<>();
    private final byte[] fallbackFace;
    private final List<byte[]> font = new ArrayList<>();

    private int faceIndex;
    private int faceCount;
    private int facePriority;
    private int oldHealth = -1;
    private int painOldHealth = -1;
    private int lastCalc;
    private int lastAttackDown = -1;
    private boolean[] oldWeaponsOwned = new boolean[9];
    private int rnd = 1;

    public Status(Wad wad)
    {
        this.wad = wad;
        this.sbar = wad.cacheLumpName("STBAR");
        for (int i = 0; i < 10; ++i) {
            tallNum.add(wad.cacheLumpName("STTNUM" + i));
            shortNum.add(wad.cacheLumpName("STYSNUM" + i));
        }
        this.percent = wad.cacheLumpName("STTPRCNT");
        for (int i = 0; i < 6; ++i) {
            keys.add(optional("STKEYS" + i));
        }
        this.armsBg = optional("STARMS");
        for (int i = 2; i < 8; ++i) {
            armsOff.add(wad.cacheLumpName("STGNUM" + i));
        }
        this.fallbackFace = wad.cacheLumpName("STFST00");
        for (int pain = 0; pain < ST_NUMPAINFACES; ++pain) {
            for (int look = 0; look < ST_NUMSTRAIGHTFACES; ++look) {
                faces.add(optional("STFST" + pain + look));
            }
            faces.add(optional("STFTR" + pain + "0"));
            faces.add(optional("STFTL" + pain + "0"));
            faces.add(optional("STFOUCH" + pain));
            faces.add(optional("STFEVL" + pain));
            faces.add(optional("STFKILL" + pain));
        }
        faces.add(optional("STFGOD0"));
        faces.add(optional("STFDEAD0"));
        for (int ch = Defs.HU_FONTSTART; ch <= Defs.HU_FONTEND; ++ch) {
            font.add(optional(String.format("STCFN%03d", ch)));
        }
        reset(null);
    }

    public void reset(Player player)
    {
        faceIndex = 0;
        faceCount = 0;
        facePriority = 0;
        oldHealth = -1;
        painOldHealth = -1;
        lastCalc = 0;
        lastAttackDown = -1;
        if (player != null) {
            oldWeaponsOwned = player.weaponowned.clone();
        } else {
            oldWeaponsOwned = new boolean[9];
        }
    }

    public void ticker(Player player)
    {
        if (player == null) {
            return;
        }
        rnd = rnd * 1103515245 + 12345;
        int stRandom = (rnd >>> 16) & 255;
        updateFaceWidget(player, stRandom);
        oldHealth = player.health;
    }

    private byte[] optional(String name)
    {
        int n = wad.checkNumForName(name);
        return n >= 0 ? wad.cacheLumpNum(n) : null;
    }

    private byte[] facePatch(int index)
    {
        if (index < 0 || index >= faces.size()) {
            return fallbackFace;
        }
        byte[] patch = faces.get(index);
        return patch != null ? patch : fallbackFace;
    }

    private int calcPainOffset(Player player)
    {
        int health = Math.min(100, Math.max(0, player.health));
        if (health != painOldHealth) {
            lastCalc = ST_FACESTRIDE * Compat.intdiv((100 - health) * ST_NUMPAINFACES, 101);
            painOldHealth = health;
        }
        return lastCalc;
    }

    private void updateFaceWidget(Player player, int stRandom)
    {
        if (facePriority < 10 && player.health <= 0) {
            facePriority = 9;
            faceIndex = ST_DEADFACE;
            faceCount = 1;
        }

        if (facePriority < 9 && player.bonuscount != 0) {
            boolean doEvilGrin = false;
            int n = Math.min(oldWeaponsOwned.length, player.weaponowned.length);
            for (int i = 0; i < n; ++i) {
                if (oldWeaponsOwned[i] != player.weaponowned[i]) {
                    doEvilGrin = true;
                    oldWeaponsOwned[i] = player.weaponowned[i];
                }
            }
            if (doEvilGrin) {
                facePriority = 8;
                faceCount = ST_EVILGRINCOUNT;
                faceIndex = calcPainOffset(player) + ST_EVILGRINOFFSET;
            }
        }

        if (facePriority < 8 && player.damagecount != 0 && player.attacker != null
            && player.mo != null && player.attacker != player.mo) {
            facePriority = 7;
            if (player.health - oldHealth > ST_MUCHPAIN) {
                faceCount = ST_TURNCOUNT;
                faceIndex = calcPainOffset(player) + ST_OUCHOFFSET;
            } else {
                int badguyangle = Collision.angleTo(
                    player.mo.x, player.mo.y, player.attacker.x, player.attacker.y);
                int diffang;
                boolean turnRight;
                if (Integer.compareUnsigned(badguyangle, player.mo.angle) > 0) {
                    diffang = badguyangle - player.mo.angle;
                    turnRight = Integer.compareUnsigned(diffang, Defs.ANG180) > 0;
                } else {
                    diffang = player.mo.angle - badguyangle;
                    turnRight = Integer.compareUnsigned(diffang, Defs.ANG180) <= 0;
                }
                faceCount = ST_TURNCOUNT;
                faceIndex = calcPainOffset(player);
                if (Integer.compareUnsigned(diffang, Defs.ANG45) < 0) {
                    faceIndex += ST_RAMPAGEOFFSET;
                } else if (turnRight) {
                    faceIndex += ST_TURNOFFSET;
                } else {
                    faceIndex += ST_TURNOFFSET + 1;
                }
            }
        }

        if (facePriority < 7 && player.damagecount != 0) {
            if (player.health - oldHealth > ST_MUCHPAIN) {
                facePriority = 7;
                faceCount = ST_TURNCOUNT;
                faceIndex = calcPainOffset(player) + ST_OUCHOFFSET;
            } else {
                facePriority = 6;
                faceCount = ST_TURNCOUNT;
                faceIndex = calcPainOffset(player) + ST_RAMPAGEOFFSET;
            }
        }

        if (facePriority < 6) {
            if (player.attackdown) {
                if (lastAttackDown == -1) {
                    lastAttackDown = ST_RAMPAGEDELAY;
                } else {
                    lastAttackDown--;
                    if (lastAttackDown == 0) {
                        facePriority = 5;
                        faceIndex = calcPainOffset(player) + ST_RAMPAGEOFFSET;
                        faceCount = 1;
                        lastAttackDown = 1;
                    }
                }
            } else {
                lastAttackDown = -1;
            }
        }

        if (facePriority < 5 && ((player.cheats & Defs.CF_GODMODE) != 0 || player.powers[Defs.PW_INVULNERABILITY] != 0)) {
            facePriority = 4;
            faceIndex = ST_GODFACE;
            faceCount = 1;
        }

        if (faceCount == 0) {
            faceIndex = calcPainOffset(player) + (stRandom % 3);
            faceCount = ST_STRAIGHTFACECOUNT;
            facePriority = 0;
        }
        faceCount--;
    }

    public void draw(int[] fb, Player player, boolean showMessages)
    {
        VVideo.drawPatch(fb, 0, 168, sbar);
        if (armsBg != null) {
            VVideo.drawPatch(fb, 104, 168, armsBg);
        }
        Integer weaponAmmo = Player.WEAPON_AMMO[player.readyweapon];
        int ammo = weaponAmmo == null ? 0 : player.ammo[weaponAmmo];
        number(fb, AMMO_X, AMMO_Y, ammo, 3, tallNum);
        number(fb, HEALTH_X, HEALTH_Y, player.health, 3, tallNum);
        VVideo.drawPatch(fb, HEALTH_X, HEALTH_Y, percent);
        number(fb, ARMOR_X, ARMOR_Y, player.armorpoints, 3, tallNum);
        VVideo.drawPatch(fb, ARMOR_X, ARMOR_Y, percent);

        boolean[] owned = {
            player.weaponowned[Defs.WP_SHOTGUN] || player.weaponowned[Defs.WP_SUPERSHOTGUN],
            player.weaponowned[Defs.WP_CHAINGUN],
            player.weaponowned[Defs.WP_MISSILE],
            player.weaponowned[Defs.WP_PLASMA],
            player.weaponowned[Defs.WP_BFG],
            false,
        };
        for (int i = 0; i < 6; ++i) {
            int x = ARMS_X + (i % 3) * 12;
            int y = ARMS_Y + Compat.intdiv(i, 3) * 10;
            if (owned[i]) {
                digit(fb, x, y, i + 2, shortNum);
            } else {
                VVideo.drawPatch(fb, x, y, armsOff.get(i));
            }
        }

        VVideo.drawPatch(fb, FACE_X, FACE_Y, facePatch(faceIndex));

        int[][] slots = {
            { Defs.IT_BLUECARD, Defs.IT_BLUESKULL },
            { Defs.IT_YELLOWCARD, Defs.IT_YELLOWSKULL },
            { Defs.IT_REDCARD, Defs.IT_REDSKULL },
        };
        for (int slot = 0; slot < slots.length; ++slot) {
            int card = slots[slot][0];
            int skull = slots[slot][1];
            if (player.cards[card] || player.cards[skull]) {
                int index = player.cards[skull] ? skull : card;
                if (keys.get(index) != null) {
                    VVideo.drawPatch(fb, KEY_X, 171 + slot * 10, keys.get(index));
                }
            }
        }
        int[] order = { Defs.AM_CLIP, Defs.AM_SHELL, Defs.AM_CELL, Defs.AM_MISL };
        int[][] ammoPos = { { 288, 173 }, { 288, 179 }, { 288, 191 }, { 288, 185 } };
        int[][] maxPos = { { 314, 173 }, { 314, 179 }, { 314, 191 }, { 314, 185 } };
        for (int i = 0; i < order.length; ++i) {
            int type = order[i];
            number(fb, ammoPos[i][0], ammoPos[i][1], player.ammo[type], 3, shortNum);
            number(fb, maxPos[i][0], maxPos[i][1], player.maxammo[type], 3, shortNum);
        }
        if (showMessages && player.message != null && !player.message.isEmpty()) {
            drawText(fb, 0, 0, player.message);
        }
    }

    private void digit(int[] fb, int x, int y, int n, List<byte[]> font)
    {
        VVideo.drawPatch(fb, x, y, font.get(Math.max(0, Math.min(9, n))));
    }

    private void number(int[] fb, int x, int y, int value, int digits, List<byte[]> font)
    {
        int width = VVideo.patchSize(font.get(0))[0];
        x -= width;
        value = Math.abs(value);
        for (int i = 0; i < digits; ++i) {
            VVideo.drawPatch(fb, x, y, font.get(value % 10));
            x -= width;
            value = Compat.intdiv(value, 10);
            if (value == 0) {
                break;
            }
        }
    }

    public void drawText(int[] fb, int x, int y, String text)
    {
        for (char ch : text.toUpperCase().toCharArray()) {
            int index = ch - Defs.HU_FONTSTART;
            byte[] patch = index >= 0 && index < font.size() ? font.get(index) : null;
            if (patch == null) {
                x += 4;
                continue;
            }
            VVideo.drawPatch(fb, x, y, patch);
            x += VVideo.patchSize(patch)[0];
        }
    }
}
