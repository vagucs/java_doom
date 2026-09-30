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

public final class VVideo
{
    private VVideo()
    {
    }

    public static int[] patchSize(byte[] patch)
    {
        return new int[] { Bin.i16(patch, 0), Bin.i16(patch, 2), Bin.i16(patch, 4), Bin.i16(patch, 6) };
    }

    public static void drawPatch(int[] fb, int x, int y, byte[] patch)
    {
        drawPatch(fb, x, y, patch, false);
    }

    public static void drawPatch(int[] fb, int x, int y, byte[] patch, boolean flipped)
    {
        int[] size = patchSize(patch);
        int width = size[0];
        int left = size[2];
        int top = size[3];
        x -= left;
        y -= top;
        int destTop = y * Defs.SCREENWIDTH + x;
        int patchLength = patch.length;
        int screenSize = Defs.SCREENWIDTH * Defs.SCREENHEIGHT;
        for (int col = 0; col < width; col++) {
            int srcCol = flipped ? width - 1 - col : col;
            int column = Bin.u32(patch, 8 + srcCol * 4);
            while (column < patchLength) {
                int topDelta = Bin.u8(patch, column);
                if (topDelta == 0xff) {
                    break;
                }
                int length = Bin.u8(patch, column + 1);
                int source = column + 3;
                int dest = destTop + topDelta * Defs.SCREENWIDTH;
                for (int i = 0; i < length; i++) {
                    if (dest >= 0 && dest < screenSize) {
                        fb[dest] = Bin.u8(patch, source);
                    }
                    source++;
                    dest += Defs.SCREENWIDTH;
                }
                column += length + 4;
            }
            destTop++;
        }
    }

    public static void drawPatchDirect(int[] fb, int x, int y, byte[] patch)
    {
        drawPatch(fb, x, y, patch);
    }

    public static void copyRect(int[] dest, int[] src, int srcx, int srcy, int width, int height, int destx, int desty)
    {
        for (int row = 0; row < height; row++) {
            int source = (srcy + row) * Defs.SCREENWIDTH + srcx;
            int target = (desty + row) * Defs.SCREENWIDTH + destx;
            System.arraycopy(src, source, dest, target, width);
        }
    }

    public static void fill(int[] fb, int color)
    {
        java.util.Arrays.fill(fb, color & 0xff);
    }
}
