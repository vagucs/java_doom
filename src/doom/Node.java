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

public class Node
{
    public int x;
    public int y;
    public int dx;
    public int dy;
    public int[][] bbox = new int[][] {
        { 0, 0, 0, 0 },
        { 0, 0, 0, 0 }
    };
    public int[] children = new int[] { 0, 0 };
}
