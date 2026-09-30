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

public class Line
{
    public Vertex v1;
    public Vertex v2;
    public int dx;
    public int dy;
    public int flags;
    public int special;
    public int tag;
    public int[] sidenum = new int[] { -1, -1 };
    public int[] bbox = new int[] { 0, 0, 0, 0 };
    public int slopetype;
    public Sector frontsector;
    public Sector backsector;
    public Side[] sides = new Side[] { null, null };
    public int iLine;
    public int validcount;
}
