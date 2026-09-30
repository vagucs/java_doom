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

public final class Visplane
{
    public int[] top = new int[0];
    public int[] bottom = new int[0];
    public int height;
    public int picnum;
    public int lightlevel;
    public int minx;
    public int maxx;

    public Visplane()
    {
        this(0, 0, 0, 0, 0);
    }

    public Visplane(int height, int picnum, int lightlevel, int minx, int maxx)
    {
        this.height = height;
        this.picnum = picnum;
        this.lightlevel = lightlevel;
        this.minx = minx;
        this.maxx = maxx;
    }
}
