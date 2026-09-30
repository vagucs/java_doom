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

import java.util.Arrays;

public final class SpriteFrame
{
    public int rotate = -1;
    public int[] lump;
    public int[] flip;

    public SpriteFrame()
    {
        this.lump = new int[8];
        Arrays.fill(this.lump, -1);
        this.flip = new int[8];
    }
}
