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

import java.nio.charset.StandardCharsets;

/** Binary helpers for little-endian DOOM data structures. */
public final class Bin
{
    private Bin()
    {
    }

    public static int u8(byte[] data, int off)
    {
        return data[off] & 0xff;
    }

    public static int i16(byte[] data, int off)
    {
        int value = u16(data, off);
        return value >= 0x8000 ? value - 0x10000 : value;
    }

    public static int u16(byte[] data, int off)
    {
        return (data[off] & 0xff) | ((data[off + 1] & 0xff) << 8);
    }

    public static int u32(byte[] data, int off)
    {
        return (data[off] & 0xff)
            | ((data[off + 1] & 0xff) << 8)
            | ((data[off + 2] & 0xff) << 16)
            | ((data[off + 3] & 0xff) << 24);
    }

    public static int i32(byte[] data, int off)
    {
        return u32(data, off);
    }

    public static String name8(byte[] data, int off)
    {
        int end = Math.min(off + 8, data.length);
        int n = off;
        while (n < end && data[n] != 0) {
            n++;
        }
        String raw = new String(data, off, n - off, StandardCharsets.ISO_8859_1);
        return raw.replaceAll(" +$", "").toUpperCase();
    }

    public static String latin1(byte[] data)
    {
        return new String(data, StandardCharsets.ISO_8859_1);
    }

    public static String latin1(byte[] data, int off, int len)
    {
        return new String(data, off, len, StandardCharsets.ISO_8859_1);
    }
}
