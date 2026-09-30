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

public class Sector
{
    public int floorheight;
    public int ceilingheight;
    public int floorpic;
    public int ceilingpic;
    public int lightlevel;
    public int special;
    public int tag;
    public List<Line> lines = new ArrayList<>();
    public Object specialdata;
    public Object soundorg;
    public int iSector;
    public int validcount;
    public int soundtraversed;
    public Mobj soundtarget;
}
