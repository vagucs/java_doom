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

public final class GameEvent
{
    public String type;
    public int key;
    public int sym;
    public boolean repeat;
    public int mod;
    public String text = "";

    public GameEvent(String type)
    {
        this.type = type;
    }
}
