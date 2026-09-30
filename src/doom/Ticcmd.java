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

public final class Ticcmd
{
    public int forwardmove;
    public int sidemove;
    public int angleturn;
    public int buttons;

    public Ticcmd()
    {
    }

    public Ticcmd(int forwardmove, int sidemove, int angleturn, int buttons)
    {
        this.forwardmove = forwardmove;
        this.sidemove = sidemove;
        this.angleturn = angleturn;
        this.buttons = buttons;
    }
}
