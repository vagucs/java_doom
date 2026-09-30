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

public final class ColumnPost
{
    public int topDelta;
    public byte[] pixels;

    public ColumnPost(int topDelta, byte[] pixels)
    {
        this.topDelta = topDelta;
        this.pixels = pixels;
    }
}
