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

public final class DrawSeg
{
    public int x1;
    public int x2;
    public int scale1;
    public int scale2;
    public int silhouette;
    public int[] sprtopclip = new int[0];
    public int[] sprbottomclip = new int[0];
    public int bsilheight;
    public int tsilheight;
    public Seg curline;
    public int scalestep;
    public int[] maskedtexturecol;
}
