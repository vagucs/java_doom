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

public final class ClipRange
{
    public int first;
    public int last;

    public ClipRange()
    {
        this(0, 0);
    }

    public ClipRange(int first, int last)
    {
        this.first = first;
        this.last = last;
    }
}
