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

    private final Wad wad;
    private final byte[] sbar;
    private final List<byte[]> tallNum = new ArrayList<>();
    private final List<byte[]> shortNum = new ArrayList<>();
    private final byte[] percent;
    private final List<byte[]> keys = new ArrayList<>();
    private final byte[] armsBg;
    private final List<byte[]> armsOff = new ArrayList<>();
    private final List<byte[]> faces = new ArrayList<>();
    private final byte[] godFace;
    private final byte[] deadFace;
    private final List<byte[]> font = new ArrayList<>();

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
        byte[] fallback = wad.cacheLumpName("STFST00");
        for (int pain = 0; pain < 5; ++pain) {
            byte[] face = optional("STFST" + pain + "0");
            faces.add(face != null ? face : fallback);
        }
        byte[] god = optional("STFGOD0");
        this.godFace = god != null ? god : fallback;
        this.deadFace = optional("STFDEAD0");
        for (int ch = Defs.HU_FONTSTART; ch <= Defs.HU_FONTEND; ++ch) {
            font.add(optional(String.format("STCFN%03d", ch)));
        }
    }

    private byte[] optional(String name)
    {
        int n = wad.checkNumForName(name);
        return n >= 0 ? wad.cacheLumpNum(n) : null;
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

        int health = Math.min(100, Math.max(0, player.health));
        int pain = player.health <= 0 ? 4 : Math.min(4, Compat.intdiv((100 - health) * 5, 101));
        if (player.health <= 0) {
            VVideo.drawPatch(fb, FACE_X, FACE_Y, deadFace != null ? deadFace : faces.get(4));
        } else if ((player.cheats & Defs.CF_GODMODE) != 0) {
            VVideo.drawPatch(fb, FACE_X, FACE_Y, godFace);
        } else {
            VVideo.drawPatch(fb, FACE_X, FACE_Y, faces.get(pain));
        }

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
