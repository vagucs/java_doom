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

/** 32-bit wrap, shifts and 16.16 fixed-point (Harbour xhb_compat / m_fixed). */
public final class Compat
{
    private Compat()
    {
    }

    public static final int MASK32 = 0xffffffff;
    public static final int MASK31 = 0x7fffffff;

    public static int asU32(int n)
    {
        return n;
    }

    public static int asI32(int n)
    {
        return n;
    }

    public static int asU32(long n)
    {
        return (int) n;
    }

    public static int ushr(int n, int bits)
    {
        if (bits <= 0) {
            return n;
        }
        if (bits >= 32) {
            return 0;
        }
        return n >>> bits;
    }

    public static int shar(int n, int bits)
    {
        if (bits <= 0) {
            return n;
        }
        if (bits >= 31) {
            return n < 0 ? -1 : 0;
        }
        return n >> bits;
    }

    public static int fixedMul(int a, int b)
    {
        return (int) (((long) a * (long) b) >> Defs.FRACBITS);
    }

    public static int fixedDiv(int a, int b)
    {
        if (b == 0) {
            return a >= 0 ? 0x7fffffff : 0x80000000;
        }
        int absA = a < 0 ? -a : a;
        int absB = b < 0 ? -b : b;
        if ((absA >> 14) >= absB) {
            return ((a ^ b) < 0) ? 0x80000000 : 0x7fffffff;
        }
        return (int) (((long) a << 16) / b);
    }

    public static int absFixed(int n)
    {
        return n < 0 ? -n : n;
    }

    public static int intdiv(int a, int b)
    {
        if (b == 0) {
            return 0;
        }
        return a / b;
    }
}
