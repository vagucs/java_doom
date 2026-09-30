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

public class Subsector
{
    public int numlines;
    public int firstline;
    public Sector sector;

    public Subsector()
    {
    }

    public Subsector(int numlines, int firstline)
    {
        this.numlines = numlines;
        this.firstline = firstline;
    }

    public Subsector(int numlines, int firstline, Sector sector)
    {
        this.numlines = numlines;
        this.firstline = firstline;
        this.sector = sector;
    }
}
