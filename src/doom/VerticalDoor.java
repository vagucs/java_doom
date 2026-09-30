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

public final class VerticalDoor
{
    public boolean dead;
    public Sector sector;
    public int type;
    public int direction;
    public int topheight;
    public int speed;
    public int topwait;
    public int topcountdown;

    public VerticalDoor(Sector sector, int type, int direction, int topheight, int speed, int topwait)
    {
        this(sector, type, direction, topheight, speed, topwait, 0);
    }

    public VerticalDoor(Sector sector, int type, int direction, int topheight, int speed, int topwait, int topcountdown)
    {
        this.sector = sector;
        this.type = type;
        this.direction = direction;
        this.topheight = topheight;
        this.speed = speed;
        this.topwait = topwait;
        this.topcountdown = topcountdown;
    }
}
