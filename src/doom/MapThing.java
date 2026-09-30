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

public class MapThing
{
    public int x;
    public int y;
    public int angle;
    public int type;
    public int options;

    public MapThing()
    {
    }

    public MapThing(int x, int y, int angle, int type, int options)
    {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.type = type;
        this.options = options;
    }
}
