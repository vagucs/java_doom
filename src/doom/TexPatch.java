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

public final class TexPatch
{
    public int originx;
    public int originy;
    public int patch;

    public TexPatch(int originx, int originy, int patch)
    {
        this.originx = originx;
        this.originy = originy;
        this.patch = patch;
    }
}
