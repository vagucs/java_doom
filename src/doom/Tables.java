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
        int sinCount = (Defs.FINEANGLES / 4) * 5;
        for (int i = 0; i < sinCount; i++) {
            double a = (i + 0.5) * Math.PI * 2.0 / Defs.FINEANGLES;
            finesine[i] = (int) (Defs.FRACUNIT * Math.sin(a));
        }
        System.arraycopy(finesine, Defs.FINEANGLES / 4, finecosine, 0, finecosine.length);
        int tanCount = Defs.FINEANGLES / 2;
        for (int i = 0; i < tanCount; i++) {
            double a = (i - Defs.FINEANGLES / 4 + 0.5) * Math.PI * 2.0 / Defs.FINEANGLES;
            double raw = Defs.FRACUNIT * Math.tan(a);
            int value;
            if (!Double.isFinite(raw)) {
                value = a > 0 ? 0x7fffffff : -0x7fffffff;
            } else if (raw > 0x7fffffff) {
                value = 0x7fffffff;
            } else if (raw < -0x7fffffff) {
                value = -0x7fffffff;
            } else {
                value = (int) raw;
            }
            finetangent[i] = value;
        }
        for (int i = 0; i <= Defs.SLOPERANGE; i++) {
            tantoangle[i] = (int) (Math.atan(i / (double) Defs.SLOPERANGE) / (Math.PI * 2.0) * 0xffffffffL);
        }
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
        int index = ((angle >>> Defs.ANGLETOFINESHIFT) + Defs.FINEANGLES / 4) & Defs.FINEMASK;
        return finesine[index];
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
