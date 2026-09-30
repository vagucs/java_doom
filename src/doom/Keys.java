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

public final class Keys
{
    private Keys()
    {
    }

    public static final int TAB = 9;
    public static final int ESC = 27;
    public static final int RETURN = 13;
    public static final int SPACE = 32;
    public static final int BACKSPACE = 8;

    public static final int F2 = 0x4000003d;
    public static final int F3 = 0x4000003e;
    public static final int F11 = 0x40000044;
    public static final int LEFT = 0x40000050;
    public static final int RIGHT = 0x4000004f;
    public static final int UP = 0x40000052;
    public static final int DOWN = 0x40000051;
    public static final int LSHIFT = 0x400000e1;
    public static final int RSHIFT = 0x400000e5;
    public static final int LCTRL = 0x400000e0;
    public static final int RCTRL = 0x400000e4;
    public static final int LALT = 0x400000e2;
    public static final int RALT = 0x400000e6;
    public static final int PLUS = 43;
    public static final int EQUALS = 61;
    public static final int MINUS = 45;
    public static final int COMMA = 44;
    public static final int PERIOD = 46;
    public static final int KP_PLUS = 0x40000057;
    public static final int KP_MINUS = 0x40000056;
    public static final int KP_ENTER = 0x40000058;

    public static boolean isMinus(int key)
    {
        return key == MINUS || key == KP_MINUS;
    }

    public static boolean isPlus(int key)
    {
        return key == PLUS || key == EQUALS || key == KP_PLUS;
    }

    public static int letter(String ch)
    {
        return Character.toLowerCase(ch.charAt(0));
    }

    public static int digit(int n)
    {
        return String.valueOf(Math.max(0, Math.min(9, n))).charAt(0);
    }
}
