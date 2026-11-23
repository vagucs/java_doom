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

public final class Tables
{
    public static final int[] finesine = new int[(Defs.FINEANGLES / 4) * 5];
    public static final int[] finecosine;
    public static final int[] finetangent = new int[Defs.FINEANGLES / 2];
    public static final int[] tantoangle = new int[Defs.SLOPERANGE + 1];
    private static boolean ready;

    static {
        finecosine = new int[finesine.length - Defs.FINEANGLES / 4];
    }

    private Tables()
    {
    }

    public static void initTables()
    {
        if (ready) {
            return;
        }
        TablesData.fillFineSine(finesine);
        System.arraycopy(finesine, Defs.FINEANGLES / 4, finecosine, 0, finecosine.length);
        TablesData.fillFineTangent(finetangent);
        TablesData.fillTantoAngle(tantoangle);
        ready = true;
    }

    public static int fineSin(int angle)
    {
        initTables();
        int index = (angle >>> Defs.ANGLETOFINESHIFT) & Defs.FINEMASK;
        return finesine[index];
    }

    public static int fineCos(int angle)
    {
        initTables();
        int index = (angle >>> Defs.ANGLETOFINESHIFT) & Defs.FINEMASK;
        return finesine[index + Defs.FINEANGLES / 4];
    }

    public static int slopeDiv(int num, int den)
    {
        if (den < 512) {
            return Defs.SLOPERANGE;
        }
        int answer = (num << 3) / (den >> 8);
        return answer > Defs.SLOPERANGE ? Defs.SLOPERANGE : answer;
    }
}
