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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Textures, flats, colormaps (r_data.c). */
public final class Resources
{
    public final List<Texture> textures = new ArrayList<>();
    public final Map<String, Integer> texIndex = new HashMap<>();
    public int flatsFirst;
    public int flatsLast;
    public int[] flattranslation = new int[0];
    public int[] texturetranslation = new int[0];
    public byte[] colormaps = new byte[0];
    public int skytexture;
    public int skyflatnum;
    public Map<String, SpriteFrame[]> sprites = new HashMap<>();
    public final Wad wad;

    public Resources(Wad wad)
    {
        this.wad = wad;
    }

    public void init()
    {
        this.initTextures();
        this.initFlats();
        this.colormaps = this.wad.cacheLumpName("COLORMAP");
        this.skyflatnum = this.flatNumForName("F_SKY1");
        this.skytexture = this.textureNumForName("SKY1");
        this.sprites = Sprites.initSpriteDefs(this.wad);
    }

    public byte[] colormap(int level)
    {
        level = Math.max(0, Math.min(31, level));
        byte[] out = new byte[256];
        int src = level * 256;
        if (this.colormaps.length >= src + 256) {
            System.arraycopy(this.colormaps, src, out, 0, 256);
        } else if (this.colormaps.length > src) {
            System.arraycopy(this.colormaps, src, out, 0, this.colormaps.length - src);
        }
        return out;
    }

    private void initFlats()
    {
        this.flatsFirst = this.wad.getNumForName("F_START") + 1;
        this.flatsLast = this.wad.getNumForName("F_END") - 1;
        int count = this.flatsLast - this.flatsFirst + 1;
        this.flattranslation = count > 0 ? range0(count) : new int[0];
    }

    public int flatNumForName(String name)
    {
        int index = this.wad.checkNumForName(name);
        return index < 0 ? 0 : index - this.flatsFirst;
    }

    public int flatLump(int flatnum)
    {
        if (flatnum < 0) {
            flatnum = 0;
        }
        int count = this.flatsLast - this.flatsFirst + 1;
        if (flatnum >= count) {
            flatnum = 0;
        }
        return this.flatsFirst + this.flattranslation[flatnum];
    }

    private void initTextures()
    {
        byte[] pnames = this.wad.cacheLumpName("PNAMES");
        int numMapPatches = Bin.i32(pnames, 0);
        int[] patchLookup = new int[numMapPatches];
        for (int i = 0; i < numMapPatches; ++i) {
            patchLookup[i] = this.wad.checkNumForName(Bin.name8(pnames, 4 + i * 8));
        }

        byte[] maptex1 = this.wad.cacheLumpName("TEXTURE1");
        int numTextures1 = Bin.i32(maptex1, 0);
        byte[] maptex2 = new byte[0];
        int numTextures2 = 0;
        if (this.wad.checkNumForName("TEXTURE2") >= 0) {
            maptex2 = this.wad.cacheLumpName("TEXTURE2");
            numTextures2 = Bin.i32(maptex2, 0);
        }

        for (int i = 0; i < numTextures1 + numTextures2; ++i) {
            int offset;
            byte[] src;
            if (i < numTextures1) {
                offset = Bin.i32(maptex1, 4 + i * 4);
                src = maptex1;
            } else {
                offset = Bin.i32(maptex2, 4 + (i - numTextures1) * 4);
                src = maptex2;
            }
            String name = Bin.name8(src, offset);
            int width = Bin.i16(src, offset + 12);
            int height = Bin.i16(src, offset + 14);
            int patchCount = Bin.i16(src, offset + 20);
            Texture texture = new Texture(name, width, height);
            int patchOffset = offset + 22;
            for (int p = 0; p < patchCount; ++p) {
                int originx = Bin.i16(src, patchOffset);
                int originy = Bin.i16(src, patchOffset + 2);
                int patchIndex = Bin.i16(src, patchOffset + 4);
                patchOffset += 10;
                int lump = patchIndex >= 0 && patchIndex < patchLookup.length ? patchLookup[patchIndex] : -1;
                texture.patches.add(new TexPatch(originx, originy, lump));
            }
            int maskWidth = 1;
            while (maskWidth * 2 <= width) {
                maskWidth *= 2;
            }
            texture.widthmask = maskWidth - 1;
            texture.colLump = new int[width];
            Arrays.fill(texture.colLump, -1);
            texture.colOfs = new int[width];
            this.texIndex.put(name, this.textures.size());
            this.textures.add(texture);
        }

        int count = this.textures.size();
        this.texturetranslation = count > 0 ? range0(count) : new int[0];
        for (Texture texture : this.textures) {
            this.generateLookup(texture);
        }
    }

    private void generateLookup(Texture texture)
    {
        int[] patchCount = new int[texture.width];
        for (TexPatch mapPatch : texture.patches) {
            if (mapPatch.patch < 0) {
                continue;
            }
            byte[] patchData = this.wad.cacheLumpNum(mapPatch.patch);
            int patchWidth = Bin.i16(patchData, 0);
            int x1 = mapPatch.originx;
            int x2 = Math.min(x1 + patchWidth, texture.width);
            int x = Math.max(x1, 0);
            while (x < x2) {
                patchCount[x]++;
                texture.colLump[x] = mapPatch.patch;
                texture.colOfs[x] = Bin.u32(patchData, 8 + (x - mapPatch.originx) * 4);
                x++;
            }
        }
        for (int x = 0; x < texture.width; ++x) {
            if (patchCount[x] > 1) {
                texture.colLump[x] = -1;
            }
        }
    }

    private void generateComposite(Texture texture)
    {
        if (texture.composite != null) {
            return;
        }
        int[] buffer = new int[texture.width * texture.height];
        for (TexPatch mapPatch : texture.patches) {
            if (mapPatch.patch < 0) {
                continue;
            }
            byte[] patchData = this.wad.cacheLumpNum(mapPatch.patch);
            int patchWidth = Bin.i16(patchData, 0);
            int x1 = mapPatch.originx;
            int x2 = Math.min(x1 + patchWidth, texture.width);
            int x = Math.max(x1, 0);
            while (x < x2) {
                int columnOffset = Bin.u32(patchData, 8 + (x - mapPatch.originx) * 4);
                this.drawColumnInCache(patchData, columnOffset, buffer, x, mapPatch.originy, texture);
                x++;
            }
        }
        texture.composite = buffer;
        for (int x = 0; x < texture.width; ++x) {
            if (texture.colLump[x] < 0) {
                texture.colOfs[x] = x * texture.height;
            }
        }
    }

    private void drawColumnInCache(byte[] patch, int column, int[] cache, int x, int originy, Texture texture)
    {
        int patchLength = patch.length;
        while (column < patchLength) {
            int topDelta = Bin.u8(patch, column);
            if (topDelta == 0xff) {
                break;
            }
            int length = Bin.u8(patch, column + 1);
            int source = column + 3;
            int position = originy + topDelta;
            int count = length;
            if (position < 0) {
                count += position;
                source -= position;
                position = 0;
            }
            if (position + count > texture.height) {
                count = texture.height - position;
            }
            int dest = x * texture.height + position;
            for (int i = 0; i < count; ++i) {
                cache[dest + i] = Bin.u8(patch, source + i);
            }
            column += length + 4;
        }
    }

    public int textureNumForName(String name)
    {
        String key = name.toUpperCase().replaceAll("[\\s\\u0000]+$", "");
        if (key.length() > 8) {
            key = key.substring(0, 8);
        }
        if (key.equals("-") || key.equals("")) {
            return 0;
        }
        Integer found = this.texIndex.get(key);
        return found == null ? 0 : found;
    }

    public int textureHeight(int texnum)
    {
        return this.textures.get(texnum).height * Defs.FRACUNIT;
    }

    public int textureWidth(int texnum)
    {
        return this.textures.get(texnum).width;
    }

    public List<ColumnPost> columnPosts(int texnum, int col)
    {
        List<ColumnPost> posts = new ArrayList<>();
        if (texnum <= 0 || texnum >= this.textures.size()) {
            return posts;
        }
        Texture texture = this.textures.get(texnum);
        col &= texture.widthmask;
        int lump = texture.colLump[col];
        if (lump >= 0) {
            byte[] patch = this.wad.cacheLumpNum(lump);
            int column = texture.colOfs[col];
            int patchLength = patch.length;
            while (column < patchLength) {
                int topDelta = Bin.u8(patch, column);
                if (topDelta == 0xff) {
                    break;
                }
                int length = Bin.u8(patch, column + 1);
                int from = column + 3;
                int to = Math.min(from + length, patchLength);
                posts.add(new ColumnPost(topDelta, Arrays.copyOfRange(patch, from, to)));
                column += length + 4;
            }
            return posts;
        }
        this.generateComposite(texture);
        int offset = texture.colOfs[col];
        byte[] columnBytes = bytesFromArray(texture.composite, offset, texture.height);
        if (columnBytes.length != 0) {
            posts.add(new ColumnPost(0, columnBytes));
        }
        return posts;
    }

    public byte[] getColumn(int texnum, int col)
    {
        Texture texture = this.textures.get(texnum);
        col &= texture.widthmask;
        int lump = texture.colLump[col];
        if (lump >= 0) {
            return this.columnToSource(this.wad.cacheLumpNum(lump), texture.colOfs[col], texture.height);
        }
        this.generateComposite(texture);
        byte[] columnBytes = bytesFromArray(texture.composite, texture.colOfs[col], texture.height);
        return this.repeatColumn(columnBytes);
    }

    private byte[] columnToSource(byte[] patch, int column, int height)
    {
        byte[] buffer = new byte[128];
        int patchLength = patch.length;
        while (column < patchLength) {
            int topDelta = Bin.u8(patch, column);
            if (topDelta == 0xff) {
                break;
            }
            int length = Bin.u8(patch, column + 1);
            int source = column + 3;
            for (int i = 0; i < length; ++i) {
                int y = topDelta + i;
                if (y >= 0 && y < 128) {
                    buffer[y] = patch[source + i];
                }
            }
            column += length + 4;
        }
        return buffer;
    }

    private byte[] repeatColumn(byte[] columnBytes)
    {
        if (columnBytes.length == 0) {
            return new byte[128];
        }
        int length = columnBytes.length;
        byte[] buffer = new byte[128];
        for (int i = 0; i < 128; ++i) {
            buffer[i] = columnBytes[i % length];
        }
        return buffer;
    }

    public byte[] flatPixels(int flatnum)
    {
        byte[] data = this.wad.cacheLumpNum(this.flatLump(flatnum));
        if (data.length >= 4096) {
            return Arrays.copyOf(data, 4096);
        }
        byte[] out = new byte[4096];
        System.arraycopy(data, 0, out, 0, data.length);
        return out;
    }

    private static int[] range0(int count)
    {
        int[] out = new int[count];
        for (int i = 0; i < count; ++i) {
            out[i] = i;
        }
        return out;
    }

    private static byte[] bytesFromArray(int[] bytes, int offset, int length)
    {
        if (bytes == null) {
            return new byte[0];
        }
        int from = Math.max(0, offset);
        int to = length < 0 ? bytes.length : Math.min(bytes.length, from + length);
        if (from >= to) {
            return new byte[0];
        }
        byte[] out = new byte[to - from];
        for (int i = 0; i < out.length; ++i) {
            out[i] = (byte) (bytes[from + i] & 0xff);
        }
        return out;
    }
}
