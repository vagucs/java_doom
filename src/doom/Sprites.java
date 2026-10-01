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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Sprites
{
    public static final int BASEYCENTER = 100;
    public static final int WEAPONTOP = 32 * Defs.FRACUNIT;
    public static final int WEAPONBOTTOM = 128 * Defs.FRACUNIT;
    public static final int LOWERSPEED = 6 * Defs.FRACUNIT;
    public static final int RAISESPEED = 6 * Defs.FRACUNIT;

    private static final int MINZ = 4 * Defs.FRACUNIT;
    private static final int MAX_SPRITE_FRAMES = 29;
    private static final int[] FUZZ_DIR = {
        1, -1, 1, -1, 1, 1, -1, 1, 1, -1, 1, 1, 1, -1, 1, 1, 1, -1, -1, -1, -1, 1, -1, -1, 1,
        1, 1, 1, -1, 1, -1, 1, 1, -1, -1, 1, 1, -1, -1, -1, -1, 1, 1, 1, 1, -1, 1, 1, -1, 1,
    };
    private static int fuzzPos = 0;

    private Sprites()
    {
    }

    /** R_InitSpriteDefs: parse S_START..S_END, including mirrored POSSA2A8 lumps. */
    public static Map<String, SpriteFrame[]> initSpriteDefs(Wad wad)
    {
        Tables.initTables();
        int start = wad.checkNumForName("S_START");
        int end = wad.checkNumForName("S_END");
        if (start < 0) {
            start = wad.checkNumForName("SS_START");
        }
        if (end < 0) {
            end = wad.checkNumForName("SS_END");
        }
        int first;
        int last;
        if (start >= 0 && end > start) {
            first = start + 1;
            last = end - 1;
        } else {
            first = 0;
            last = wad.numLumps() - 1;
        }

        Map<String, List<Integer>> buckets = new HashMap<>();
        for (int lump = first; lump <= last; ++lump) {
            String name = wad.lumpName(lump);
            if (name.length() < 6) {
                continue;
            }
            String base = name.substring(0, 4);
            List<Integer> list = buckets.get(base);
            if (list == null) {
                list = new ArrayList<>();
                buckets.put(base, list);
            }
            list.add(lump);
        }

        Map<String, SpriteFrame[]> result = new HashMap<>();
        for (Map.Entry<String, List<Integer>> entry : buckets.entrySet()) {
            String spriteName = entry.getKey();
            List<Integer> lumps = entry.getValue();
            SpriteFrame[] frames = new SpriteFrame[MAX_SPRITE_FRAMES];
            for (int i = 0; i < MAX_SPRITE_FRAMES; ++i) {
                frames[i] = new SpriteFrame();
            }
            int maxFrame = -1;
            for (int lump : lumps) {
                String name = wad.lumpName(lump);
                int frame = name.charAt(4) - 65;
                int rotation = name.charAt(5) - 48;
                if (installSpriteLump(frames, lump, frame, rotation, false)) {
                    maxFrame = Math.max(maxFrame, frame);
                }
                if (name.length() >= 8 && name.charAt(6) >= 'A' && name.charAt(6) <= ']') {
                    int frame2 = name.charAt(6) - 65;
                    int rotation2 = name.charAt(7) - 48;
                    if (installSpriteLump(frames, lump, frame2, rotation2, true)) {
                        maxFrame = Math.max(maxFrame, frame2);
                    }
                }
            }
            if (maxFrame >= 0) {
                for (int frameIndex = 0; frameIndex <= maxFrame; frameIndex++) {
                    SpriteFrame slot = frames[frameIndex];
                    if (slot.rotate == -1) {
                        System.err.println(
                            "R_InitSprites: No patches found for " + spriteName
                                + " frame " + (char) ('A' + frameIndex)
                        );
                        slot.rotate = 0;
                    }
                }
                result.put(spriteName, Arrays.copyOf(frames, maxFrame + 1));
            }
        }
        return result;
    }

    private static boolean installSpriteLump(SpriteFrame[] frames, int lump, int frame, int rotation, boolean flipped)
    {
        if (frame < 0 || frame >= MAX_SPRITE_FRAMES || rotation < 0 || rotation > 8) {
            return false;
        }
        SpriteFrame spriteFrame = frames[frame];
        if (rotation == 0) {
            if (spriteFrame.rotate == 1) {
                return true;
            }
            spriteFrame.rotate = 0;
            for (int r = 0; r < 8; ++r) {
                spriteFrame.lump[r] = lump;
                spriteFrame.flip[r] = flipped ? 1 : 0;
            }
            return true;
        }
        if (spriteFrame.rotate == 0) {
            return true;
        }
        spriteFrame.rotate = 1;
        int index = rotation - 1;
        if (spriteFrame.lump[index] < 0) {
            spriteFrame.lump[index] = lump;
            spriteFrame.flip[index] = flipped ? 1 : 0;
        }
        return true;
    }

    public static int[] lookupSprite(Resources resources, String name, int angleToThing, int mobjAngle, int frame)
    {
        String base = name.length() <= 4 ? name.toUpperCase() : name.substring(0, 4).toUpperCase();
        SpriteFrame[] frames = resources.sprites.get(base);
        if (frames == null) {
            return null;
        }
        int frameIndex = frame & Info.FF_FRAMEMASK;
        if (frameIndex >= frames.length) {
            return null;
        }
        SpriteFrame spriteFrame = frames[frameIndex];
        int lump;
        int flip;
        if (spriteFrame.rotate != 0) {
            int rotation = Compat.ushr(
                Compat.asU32(angleToThing - mobjAngle + Compat.intdiv(Defs.ANG45, 2) * 9),
                29
            ) & 7;
            lump = spriteFrame.lump[rotation];
            flip = spriteFrame.flip[rotation];
        } else {
            lump = spriteFrame.lump[0];
            flip = spriteFrame.flip[0];
        }
        return lump < 0 ? null : new int[] { lump, flip };
    }

    public static void drawSprites(Renderer renderer, World world, int[] fb)
    {
        List<SpriteDraw> visible = new ArrayList<>();
        for (Mobj mobj : world.mobjs) {
            String sprite = mobj.sprite == null ? "" : mobj.sprite;
            if (sprite.equals("") || mobj.player != null) {
                continue;
            }
            SpriteDraw item = project(renderer, mobj);
            if (item != null) {
                visible.add(item);
            }
        }
        visible.sort(Comparator.comparingInt(a -> a.scale));
        for (SpriteDraw sprite : visible) {
            drawSprite(renderer, fb, sprite, true);
        }
    }

    private static SpriteDraw project(Renderer renderer, Mobj mobj)
    {
        int trX = Compat.asI32(mobj.x - renderer.viewx);
        int trY = Compat.asI32(mobj.y - renderer.viewy);
        int gxt = Compat.fixedMul(trX, renderer.viewcos);
        int gyt = -Compat.fixedMul(trY, renderer.viewsin);
        int tz = gxt - gyt;
        if (tz < MINZ) {
            return null;
        }
        int xscale = Compat.fixedDiv(renderer.projection, tz);
        gxt = -Compat.fixedMul(trX, renderer.viewsin);
        gyt = Compat.fixedMul(trY, renderer.viewcos);
        int tx = -(gyt + gxt);
        if (Math.abs(tx) > tz * 4) {
            return null;
        }
        int[] found = lookupSprite(
            renderer.res,
            String.valueOf(mobj.sprite),
            renderer.pointToAngle(mobj.x, mobj.y),
            mobj.angle,
            mobj.frame
        );
        if (found == null) {
            return null;
        }
        int lump = found[0];
        int flip = found[1];
        byte[] patch = renderer.res.wad.cacheLumpNum(lump);
        int[] size = VVideo.patchSize(patch);
        int width = size[0];
        int left = size[2];
        int top = size[3];
        tx -= left * Defs.FRACUNIT;
        int x1 = (renderer.centerxfrac + Compat.fixedMul(tx, xscale)) >> Defs.FRACBITS;
        if (x1 > renderer.viewwidth) {
            return null;
        }
        tx += width * Defs.FRACUNIT;
        int x2 = ((renderer.centerxfrac + Compat.fixedMul(tx, xscale)) >> Defs.FRACBITS) - 1;
        if (x2 < 0) {
            return null;
        }
        int iscale = xscale != 0 ? Compat.fixedDiv(Defs.FRACUNIT, xscale) : Defs.FRACUNIT;
        int xiscale = flip != 0 ? -iscale : iscale;
        int startfrac = flip != 0 ? (width << Defs.FRACBITS) - 1 : 0;
        int visibleX1 = Math.max(x1, 0);
        int visibleX2 = Math.min(x2, renderer.viewwidth - 1);
        if (visibleX1 > x1) {
            startfrac += xiscale * (visibleX1 - x1);
        }
        SpriteDraw item = new SpriteDraw();
        item.mo = mobj;
        item.patch = patch;
        item.w = width;
        item.scale = xscale << renderer.detailshift;
        item.gx = mobj.x;
        item.gy = mobj.y;
        item.gz = mobj.z;
        item.gzt = mobj.z + top * Defs.FRACUNIT;
        item.texturemid = mobj.z + top * Defs.FRACUNIT - renderer.viewz;
        item.x1 = visibleX1;
        item.x2 = visibleX2;
        item.xiscale = xiscale;
        item.startfrac = startfrac;
        return item;
    }

    private static int pointOnSegSide(int x, int y, Seg line)
    {
        int lx = line.v1.x;
        int ly = line.v1.y;
        int ldx = line.v2.x - lx;
        int ldy = line.v2.y - ly;
        if (ldx == 0) {
            if (x <= lx) {
                return ldy > 0 ? 1 : 0;
            }
            return ldy < 0 ? 1 : 0;
        }
        if (ldy == 0) {
            if (y <= ly) {
                return ldx < 0 ? 1 : 0;
            }
            return ldx > 0 ? 1 : 0;
        }
        int dx = x - lx;
        int dy = y - ly;
        int left = Compat.fixedMul(ldy >> Defs.FRACBITS, dx);
        int right = Compat.fixedMul(dy, ldx >> Defs.FRACBITS);
        return right < left ? 0 : 1;
    }

    private static int[][] clipAgainstWalls(Renderer renderer, SpriteDraw sprite)
    {
        int x1 = sprite.x1;
        int x2 = sprite.x2;
        int[] clipBottom = new int[Defs.SCREENWIDTH];
        int[] clipTop = new int[Defs.SCREENWIDTH];
        Arrays.fill(clipBottom, -2);
        Arrays.fill(clipTop, -2);
        for (int d = renderer.drawsegs.size() - 1; d >= 0; --d) {
            DrawSeg drawseg = renderer.drawsegs.get(d);
            if (drawseg.x1 > x2 || drawseg.x2 < x1) {
                continue;
            }
            if (drawseg.silhouette == 0 && (drawseg.maskedtexturecol == null || drawseg.maskedtexturecol.length == 0)) {
                continue;
            }
            int range1 = Math.max(drawseg.x1, x1);
            int range2 = Math.min(drawseg.x2, x2);
            int scale = Math.max(drawseg.scale1, drawseg.scale2);
            int lowScale = Math.min(drawseg.scale1, drawseg.scale2);
            boolean inFront = scale < sprite.scale
                || (
                    lowScale < sprite.scale
                        && drawseg.curline != null
                        && pointOnSegSide(sprite.gx, sprite.gy, drawseg.curline) == 0
                );
            if (inFront) {
                if (drawseg.maskedtexturecol != null && drawseg.maskedtexturecol.length > 0) {
                    renderer.renderMaskedSegRange(drawseg, range1, range2);
                }
                continue;
            }
            int silhouette = drawseg.silhouette;
            if (sprite.gz >= drawseg.bsilheight) {
                silhouette &= ~Defs.SIL_BOTTOM;
            }
            if (sprite.gzt <= drawseg.tsilheight) {
                silhouette &= ~Defs.SIL_TOP;
            }
            for (int x = range1; x <= range2; ++x) {
                int i = x - drawseg.x1;
                if (i < 0 || i >= drawseg.sprtopclip.length) {
                    continue;
                }
                if ((silhouette & Defs.SIL_BOTTOM) != 0 && clipBottom[x] == -2) {
                    clipBottom[x] = drawseg.sprbottomclip[i];
                }
                if ((silhouette & Defs.SIL_TOP) != 0 && clipTop[x] == -2) {
                    clipTop[x] = drawseg.sprtopclip[i];
                }
            }
        }
        for (int x = x1; x <= x2; ++x) {
            if (clipBottom[x] == -2) {
                clipBottom[x] = renderer.viewheight;
            }
            if (clipTop[x] == -2) {
                clipTop[x] = -1;
            }
        }
        return new int[][] { clipTop, clipBottom };
    }

    public static void drawPsprite(Renderer renderer, int[] fb, byte[] patch, int sx, int sy)
    {
        int[] size = VVideo.patchSize(patch);
        int width = size[0];
        int left = size[2];
        int top = size[3];
        int tx = sx - 160 * Defs.FRACUNIT;
        tx -= left * Defs.FRACUNIT;
        int x1 = (renderer.centerxfrac + Compat.fixedMul(tx, renderer.pspritescale)) >> Defs.FRACBITS;
        if (x1 > renderer.viewwidth) {
            return;
        }
        tx += width * Defs.FRACUNIT;
        int x2 = ((renderer.centerxfrac + Compat.fixedMul(tx, renderer.pspritescale)) >> Defs.FRACBITS) - 1;
        if (x2 < 0) {
            return;
        }
        int visibleX1 = Math.max(x1, 0);
        int visibleX2 = Math.min(x2, renderer.viewwidth - 1);
        int startfrac = visibleX1 > x1 ? renderer.pspriteiscale * (visibleX1 - x1) : 0;
        int texturemid = BASEYCENTER * Defs.FRACUNIT + Compat.intdiv(Defs.FRACUNIT, 2) - (sy - top * Defs.FRACUNIT);

        SpriteDraw sprite = new SpriteDraw();
        sprite.patch = patch;
        sprite.w = width;
        sprite.scale = renderer.pspritescale << renderer.detailshift;
        sprite.texturemid = texturemid;
        sprite.x1 = visibleX1;
        sprite.x2 = visibleX2;
        sprite.xiscale = renderer.pspriteiscale;
        sprite.startfrac = startfrac;
        drawSprite(renderer, fb, sprite, false);
    }

    public static int[] weaponPspriteXY(Player player, int leveltime)
    {
        Tables.initTables();
        String state = player.pspriteState;
        if ("up".equals(state) || "down".equals(state)) {
            return new int[] { Defs.FRACUNIT, player.pspriteSy };
        }
        if ("atk".equals(state)) {
            return new int[] { Defs.FRACUNIT, player.pspriteSy != 0 ? player.pspriteSy : WEAPONTOP };
        }
        int bob = player.bob;
        int angle = (128 * leveltime) & Defs.FINEMASK;
        int sx = Defs.FRACUNIT
            + Compat.fixedMul(bob, Tables.finesine[(angle + Compat.intdiv(Defs.FINEANGLES, 4)) % Tables.finesine.length]);
        angle &= Compat.intdiv(Defs.FINEANGLES, 2) - 1;
        int sy = WEAPONTOP + Compat.fixedMul(bob, Tables.finesine[angle]);
        player.pspriteSy = sy;
        return new int[] { sx, sy };
    }

    private static void drawSprite(Renderer renderer, int[] fb, SpriteDraw sprite, boolean clipWalls)
    {
        byte[] patch = sprite.patch;
        int patchWidth = sprite.w;
        int iscale = sprite.xiscale;
        int spryscale = sprite.scale;
        int yIscale = Math.abs(iscale) >> renderer.detailshift;
        yIscale = Math.max(1, yIscale);
        int sprTopScreen = renderer.centeryfrac - Compat.fixedMul(sprite.texturemid, spryscale);
        int[] clipTop;
        int[] clipBottom;
        if (clipWalls) {
            int[][] clips = clipAgainstWalls(renderer, sprite);
            clipTop = clips[0];
            clipBottom = clips[1];
        } else {
            clipTop = new int[Defs.SCREENWIDTH];
            Arrays.fill(clipTop, -1);
            clipBottom = new int[Defs.SCREENWIDTH];
            Arrays.fill(clipBottom, renderer.viewheight);
        }
        int[] columnOffsets = new int[Math.max(1, patchWidth)];
        for (int column = 0; column < Math.max(1, patchWidth); ++column) {
            columnOffsets[column] = Bin.u32(patch, 8 + column * 4);
        }
        boolean fuzz = sprite.mo != null && (sprite.mo.flags & Defs.MF_SHADOW) != 0;
        byte[] colormap = fuzz ? renderer.res.colormap(6)
                : (renderer.fixedcolormap != null ? renderer.fixedcolormap : renderer.res.colormap(0));
        int patchLength = patch.length;
        int frac = sprite.startfrac;
        for (int x = sprite.x1; x <= sprite.x2; ++x) {
            int columnNumber = frac >> Defs.FRACBITS;
            if (columnNumber >= 0 && columnNumber < patchWidth) {
                int column = columnOffsets[columnNumber];
                while (column < patchLength) {
                    int topDelta = Bin.u8(patch, column);
                    if (topDelta == 0xff) {
                        break;
                    }
                    int length = Bin.u8(patch, column + 1);
                    int source = column + 3;
                    int topScreen = sprTopScreen + spryscale * topDelta;
                    int bottomScreen = topScreen + spryscale * length;
                    int yl = (topScreen + Defs.FRACUNIT - 1) >> Defs.FRACBITS;
                    int yh = (bottomScreen - 1) >> Defs.FRACBITS;
                    yl = Math.max(yl, Math.max(clipTop[x] + 1, 0));
                    yh = Math.min(yh, Math.min(clipBottom[x] - 1, renderer.viewheight - 1));
                    if (fuzz) {
                        yl = Math.max(yl, 1);
                        yh = Math.min(yh, renderer.viewheight - 2);
                    }
                    if (yl <= yh) {
                        int texfrac = Compat.fixedMul((yl << Defs.FRACBITS) - topScreen, yIscale);
                        texfrac = Math.max(0, texfrac);
                        for (int y = yl; y <= yh; ++y) {
                            if (fuzz) {
                                drawFuzzPixel(renderer, fb, x, y, colormap);
                            } else {
                                int index = texfrac >> Defs.FRACBITS;
                                if (index >= 0 && index < length) {
                                    int pixel = Bin.u8(patch, source + index);
                                    int value = pixel < colormap.length ? colormap[pixel] & 0xff : pixel;
                                    if (renderer.detailshift != 0) {
                                        int xx = x << 1;
                                        int offset = renderer.ylookup[y] + renderer.columnofs[xx];
                                        fb[offset] = value;
                                        fb[offset + 1] = value;
                                    } else {
                                        fb[renderer.ylookup[y] + renderer.columnofs[x]] = value;
                                    }
                                }
                            }
                            texfrac += yIscale;
                        }
                    }
                    column += length + 4;
                }
            }
            frac += iscale;
        }
    }

    private static void drawFuzzPixel(Renderer renderer, int[] fb, int x, int y, byte[] colormap)
    {
        int dest = renderer.ylookup[y] + renderer.columnofs[renderer.detailshift != 0 ? (x << 1) : x];
        int src = dest + FUZZ_DIR[fuzzPos] * Defs.SCREENWIDTH;
        fuzzPos = (fuzzPos + 1) % FUZZ_DIR.length;
        if (src < 0 || src >= fb.length) {
            src = dest;
        }
        int pixel = fb[src] & 255;
        int value = pixel < colormap.length ? colormap[pixel] & 0xff : pixel;
        fb[dest] = value;
        if (renderer.detailshift != 0 && dest + 1 < fb.length) {
            fb[dest + 1] = value;
        }
    }

    private static final class SpriteDraw
    {
        Mobj mo;
        byte[] patch;
        int w;
        int scale;
        int gx;
        int gy;
        int gz;
        int gzt;
        int texturemid;
        int x1;
        int x2;
        int xiscale;
        int startfrac;
    }
}
