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

import java.util.ArrayList;
import java.util.List;

public class MoveCheck
{
    public int floorz;
    public int ceilingz;
    public int dropoffz;
    public List<Line> spechit = new ArrayList<>();
    public boolean blocked;
    public Mobj hitThing;
    public Line ceilingline;
    public int[] bbox = new int[4];
}
