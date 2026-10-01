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

public final class FloorMove
{
    public boolean dead;
    public boolean crush;
    public Integer floorpic;
    public Sector sector;
    public int direction;
    public int dest;
    public int speed;

    public FloorMove(Sector sector, int direction, int dest, int speed)
    {
        this.sector = sector;
        this.direction = direction;
        this.dest = dest;
        this.speed = speed;
    }
}
