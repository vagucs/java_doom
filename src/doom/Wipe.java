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

import java.util.concurrent.ThreadLocalRandom;

public final class Wipe
{
    private int[] start = new int[0];
    private int[] end = new int[0];
    private int[] y = new int[0];
    public boolean active;

    private static int randomInt(int min, int max)
    {
        return min + ThreadLocalRandom.current().nextInt(max - min + 1);
    }

    public void captureStart(int[] fb)
    {
        start = fb.clone();
    }

    public void captureEnd(int[] fb)
    {
        end = fb.clone();
    }

    public void begin(int[] fb)
    {
        System.arraycopy(start, 0, fb, 0, Math.min(start.length, fb.length));
        int columns = Defs.SCREENWIDTH / 2;
        y = new int[columns];
        y[0] = -randomInt(0, 15);
        for (int i = 1; i < columns; ++i) {
            int next = y[i - 1] + randomInt(-1, 1);
            y[i] = next > 0 ? 0 : next == -16 ? -15 : next;
        }
        active = true;
    }

    public boolean tick(int tics, int[] fb)
    {
        int height = Defs.SCREENHEIGHT;
        int width = Defs.SCREENWIDTH;
        boolean done = true;
        for (int tic = 0; tic < Math.max(1, tics); ++tic) {
            for (int column = 0; column < y.length; ++column) {
                int yy = y[column];
                int x = column * 2;
                if (yy < 0) {
                    for (int row = 0; row < height; ++row) {
                        int off = row * width + x;
                        fb[off] = start[off];
                        fb[off + 1] = start[off + 1];
                    }
                    y[column] = yy + 1;
                    done = false;
                    continue;
                }
                if (yy >= height) {
                    continue;
                }
                int dy = yy < 16 ? yy + 1 : 8;
                dy = Math.min(dy, height - yy);
                for (int row = 0; row < dy; ++row) {
                    int off = (yy + row) * width + x;
                    fb[off] = end[off];
                    fb[off + 1] = end[off + 1];
                }
                yy += dy;
                y[column] = yy;
                for (int row = yy; row < height; ++row) {
                    int src = (row - yy) * width + x;
                    int dst = row * width + x;
                    fb[dst] = start[src];
                    fb[dst + 1] = start[src + 1];
                }
                done = false;
            }
        }
        if (done) {
            System.arraycopy(end, 0, fb, 0, Math.min(end.length, fb.length));
            active = false;
        }
        return done;
    }
}
