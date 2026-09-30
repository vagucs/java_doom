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

public final class Plat
{
    public boolean dead;
    public Sector sector;
    public int type;
    public int status;
    public int speed;
    public int low;
    public int high;
    public int wait;
    public int count;

    public Plat(Sector sector, int type, int status, int speed, int low, int high, int wait)
    {
        this(sector, type, status, speed, low, high, wait, 0);
    }

    public Plat(Sector sector, int type, int status, int speed, int low, int high, int wait, int count)
    {
        this.sector = sector;
        this.type = type;
        this.status = status;
        this.speed = speed;
        this.low = low;
        this.high = high;
        this.wait = wait;
        this.count = count;
    }
}
