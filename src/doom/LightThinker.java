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

public final class LightThinker
{
    public Sector sector;
    public String kind;
    public int count;
    public int minlight;
    public int maxlight;
    public int darktime;
    public int brighttime;
    public int maxtime = 64;
    public int mintime = 7;
    public int direction = -1;

    public LightThinker(Sector sector, String kind)
    {
        this.sector = sector;
        this.kind = kind;
    }
}
