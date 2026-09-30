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

public final class WbStart
{
    public int epsd;
    public int last;
    public int next;
    public int maxkills;
    public int maxitems;
    public int maxsecret;
    public int partime;
    public int skills;
    public int sitems;
    public int ssecret;
    public int stime;
    public boolean didsecret;
    public boolean commercial;

    public WbStart(
        int epsd,
        int last,
        int next,
        int maxkills,
        int maxitems,
        int maxsecret,
        int partime,
        int skills,
        int sitems,
        int ssecret,
        int stime,
        boolean didsecret,
        boolean commercial
    )
    {
        this.epsd = epsd;
        this.last = last;
        this.next = next;
        this.maxkills = maxkills;
        this.maxitems = maxitems;
        this.maxsecret = maxsecret;
        this.partime = partime;
        this.skills = skills;
        this.sitems = sitems;
        this.ssecret = ssecret;
        this.stime = stime;
        this.didsecret = didsecret;
        this.commercial = commercial;
    }
}
