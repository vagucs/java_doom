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
import java.util.List;

public final class Texture
{
    public final List<TexPatch> patches = new ArrayList<>();
    public int widthmask;
    public int[] columnofs = new int[0];
    public int[] composite;
    public int[] colLump = new int[0];
    public int[] colOfs = new int[0];
    public String name;
    public int width;
    public int height;

    public Texture(String name, int width, int height)
    {
        this.name = name;
        this.width = width;
        this.height = height;
    }
}
