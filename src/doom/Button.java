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

public final class Button
{
    public Line line;
    public String where;
    public int texture;
    public int timer;

    public Button(Line line, String where, int texture, int timer)
    {
        this.line = line;
        this.where = where;
        this.texture = texture;
        this.timer = timer;
    }
}
