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

public final class AmMap
{
    private static final int REDS = 176;
    private static final int RED_RANGE = 16;
    private static final int GREENS = 112;
    private static final int GRAYS = 96;
    private static final int BROWNS = 64;
    private static final int YELLOWS = 231;
    private static final int WHITE = 209;
    private static final int GRID_COLOR = 104;
    private static final int INIT_SCALE = 13107;
    private static final int PAN_INC = 4;
    private static final int ZOOM_IN = 66846;
    private static final int ZOOM_OUT = 64250;
    private static final int INT_MAX = 0x7fffffff;

    public boolean active;
    public int cheating;
    public int grid;
    public int followPlayer = 1;
    private boolean stopped = true;
    private int lastLevel = -1;
    private int lastEpisode = -1;
    private int bigState;
    private int clock;
    private int fW = Defs.SCREENWIDTH;
    private int fH = Defs.SCREENHEIGHT - Defs.SBARHEIGHT;
    private int mX;
    private int mY;
    private int mX2;
    private int mY2;
    private int mW;
    private int mH;
    private int minX;
    private int minY;
    private int maxX;
    private int maxY;
    private int minScale = Defs.FRACUNIT;
    private int maxScale = Defs.FRACUNIT;
    private int scaleMtof = INIT_SCALE;
    private int scaleFtom = Defs.FRACUNIT;
    private int oldMX;
    private int oldMY;
    private int oldMW;
    private int oldMH;
    private int oldFollowX = INT_MAX;
    private int oldFollowY;
    private int panX;
    private int panY;
    private int zoomMtof = Defs.FRACUNIT;
    private int zoomFtom = Defs.FRACUNIT;
    private final int[][] playerArrow;
    private final int[][] thingTriangle;
    private byte[][] markNumbers = new byte[10][];
    private int[][] marks = new int[10][2];
    private int markNumber;

    public AmMap()
    {
        int radius = Compat.intdiv(8 * Defs.PLAYER_RADIUS, 7);
        int q = Compat.intdiv(radius, 4);
        int e = Compat.intdiv(radius, 8);
        playerArrow = new int[][] {
            { -radius + e, 0, radius, 0 },
            { radius, 0, radius - Compat.intdiv(radius, 2), q },
            { radius, 0, radius - Compat.intdiv(radius, 2), -q },
            { -radius + e, 0, -radius - e, q },
            { -radius + e, 0, -radius - e, -q },
            { -radius + 3 * e, 0, -radius + e, q },
            { -radius + 3 * e, 0, -radius + e, -q },
        };
        thingTriangle = new int[][] {
            { -Compat.intdiv(Defs.FRACUNIT, 2), -Compat.intdiv(7 * Defs.FRACUNIT, 10), Defs.FRACUNIT, 0 },
            { Defs.FRACUNIT, 0, -Compat.intdiv(Defs.FRACUNIT, 2), Compat.intdiv(7 * Defs.FRACUNIT, 10) },
            { -Compat.intdiv(Defs.FRACUNIT, 2), Compat.intdiv(7 * Defs.FRACUNIT, 10), -Compat.intdiv(Defs.FRACUNIT, 2), -Compat.intdiv(7 * Defs.FRACUNIT, 10) },
        };
        clearMarks();
    }

    public void start(Game game)
    {
        if (!stopped)
        {
            stop();
        }
        stopped = false;
        if (lastLevel != game.mapn || lastEpisode != game.episode)
        {
            levelInit(game);
            lastLevel = game.mapn;
            lastEpisode = game.episode;
        }
        initVariables(game);
        for (int i = 0; i < 10; ++i)
        {
            int n = game.wad.checkNumForName("AMMNUM" + i);
            markNumbers[i] = n >= 0 ? game.wad.cacheLumpNum(n) : null;
        }
        active = true;
    }

    public void stop()
    {
        active = false;
        stopped = true;
        panX = 0;
        panY = 0;
        zoomMtof = Defs.FRACUNIT;
        zoomFtom = Defs.FRACUNIT;
        bigState = 0;
    }

    public void resetLevel()
    {
        stop();
        lastLevel = -1;
        lastEpisode = -1;
        cheating = 0;
    }

    public void ticker(Game game)
    {
        if (!active)
        {
            return;
        }
        ++clock;
        if (followPlayer != 0)
        {
            follow(game);
        }
        if (zoomFtom != Defs.FRACUNIT)
        {
            changeScale();
        }
        if (panX != 0 || panY != 0)
        {
            changeLocation();
        }
    }

    public void draw(int[] fb, Game game)
    {
        if (!active)
        {
            return;
        }
        int limit = fW * fH;
        for (int i = 0; i < limit; ++i)
        {
            fb[i] = 0;
        }
        if (grid != 0)
        {
            drawGrid(fb, game);
        }
        drawWalls(fb, game);
        Mobj mo = game.player.mo;
        drawCharacter(fb, playerArrow, 0, mo.angle, WHITE, mo.x, mo.y);
        if (cheating == 2)
        {
            for (Mobj thing : game.world.mobjs)
            {
                drawCharacter(fb, thingTriangle, 16 * Defs.FRACUNIT, thing.angle, GREENS, thing.x, thing.y);
            }
        }
        put(fb, Compat.intdiv(fW, 2), Compat.intdiv(fH, 2), GRAYS);
        for (int i = 0; i < marks.length; ++i)
        {
            int mx = marks[i][0];
            int my = marks[i][1];
            if (mx == -1 || markNumbers[i] == null)
            {
                continue;
            }
            int x = cx(mx);
            int y = cy(my);
            if (x >= 0 && x <= fW - 5 && y >= 0 && y <= fH - 6)
            {
                VVideo.drawPatch(fb, x, y, markNumbers[i]);
            }
        }
    }

    public boolean responder(String type, int key, Game game)
    {
        if (game.gamestate != Defs.GS_LEVEL || game.player == null || game.world == null)
        {
            return false;
        }
        if ("keydown".equals(type))
        {
            if (!active)
            {
                if (key == Keys.TAB)
                {
                    start(game);
                    return true;
                }
                return false;
            }
            if ((key == Keys.LEFT || key == Keys.RIGHT) && followPlayer == 0)
            {
                panX = (key == Keys.RIGHT ? 1 : -1) * ftom(PAN_INC);
                return true;
            }
            if ((key == Keys.UP || key == Keys.DOWN) && followPlayer == 0)
            {
                panY = (key == Keys.UP ? 1 : -1) * ftom(PAN_INC);
                return true;
            }
            if (Keys.isMinus(key))
            {
                zoomMtof = ZOOM_OUT;
                zoomFtom = ZOOM_IN;
                return true;
            }
            if (Keys.isPlus(key))
            {
                zoomMtof = ZOOM_IN;
                zoomFtom = ZOOM_OUT;
                return true;
            }
            if (key == Keys.TAB)
            {
                stop();
                return true;
            }
            if (key == '0')
            {
                bigState ^= 1;
                if (bigState != 0)
                {
                    saveScale();
                    minOut();
                }
                else
                {
                    restoreScale(game);
                }
                return true;
            }
            if (key == 'f')
            {
                followPlayer ^= 1;
                oldFollowX = INT_MAX;
                game.player.setMessage(followPlayer != 0 ? "Follow Mode ON" : "Follow Mode OFF");
                return true;
            }
            if (key == 'g')
            {
                grid ^= 1;
                game.player.setMessage(grid != 0 ? "Grid ON" : "Grid OFF");
                return true;
            }
            if (key == 'm')
            {
                game.player.setMessage("Marked Spot " + markNumber);
                marks[markNumber][0] = mX + Compat.intdiv(mW, 2);
                marks[markNumber][1] = mY + Compat.intdiv(mH, 2);
                markNumber = (markNumber + 1) % 10;
                return true;
            }
            if (key == 'c')
            {
                clearMarks();
                game.player.setMessage("All Marks Cleared");
                return true;
            }
        }
        else if ("keyup".equals(type) && active)
        {
            if ((key == Keys.LEFT || key == Keys.RIGHT) && followPlayer == 0)
            {
                panX = 0;
            }
            else if ((key == Keys.UP || key == Keys.DOWN) && followPlayer == 0)
            {
                panY = 0;
            }
            else if (Keys.isMinus(key) || Keys.isPlus(key))
            {
                zoomMtof = Defs.FRACUNIT;
                zoomFtom = Defs.FRACUNIT;
            }
        }
        return false;
    }

    public void cycleIddt()
    {
        cheating = (cheating + 1) % 3;
    }

    private void levelInit(Game game)
    {
        clearMarks();
        minX = INT_MAX;
        minY = INT_MAX;
        maxX = -INT_MAX;
        maxY = -INT_MAX;
        for (Vertex vertex : game.world.vertexes)
        {
            minX = Math.min(minX, vertex.x);
            maxX = Math.max(maxX, vertex.x);
            minY = Math.min(minY, vertex.y);
            maxY = Math.max(maxY, vertex.y);
        }
        int width = Math.max(Defs.FRACUNIT, maxX - minX);
        int height = Math.max(Defs.FRACUNIT, maxY - minY);
        minScale = Math.min(Compat.fixedDiv(fW * Defs.FRACUNIT, width), Compat.fixedDiv(fH * Defs.FRACUNIT, height));
        maxScale = Compat.fixedDiv(fH * Defs.FRACUNIT, 2 * Defs.PLAYER_RADIUS);
        scaleMtof = Compat.fixedDiv(minScale, (int) (0.7 * Defs.FRACUNIT));
        if (scaleMtof > maxScale)
        {
            scaleMtof = minScale;
        }
        scaleFtom = Compat.fixedDiv(Defs.FRACUNIT, scaleMtof);
    }

    private void initVariables(Game game)
    {
        oldFollowX = INT_MAX;
        clock = 0;
        panX = 0;
        panY = 0;
        zoomMtof = Defs.FRACUNIT;
        zoomFtom = Defs.FRACUNIT;
        mW = ftom(fW);
        mH = ftom(fH);
        Mobj mo = game.player.mo;
        mX = mo.x - Compat.intdiv(mW, 2);
        mY = mo.y - Compat.intdiv(mH, 2);
        changeLocation();
        saveScale();
    }

    private void clearMarks()
    {
        marks = new int[10][2];
        for (int i = 0; i < 10; ++i)
        {
            marks[i][0] = -1;
            marks[i][1] = -1;
        }
        markNumbers = new byte[10][];
        markNumber = 0;
    }

    private int ftom(int pixels)
    {
        return Compat.fixedMul(pixels * Defs.FRACUNIT, scaleFtom);
    }

    private int mtof(int map)
    {
        return Compat.shar(Compat.fixedMul(map, scaleMtof), 16);
    }

    private int cx(int x)
    {
        return mtof(x - mX);
    }

    private int cy(int y)
    {
        return fH - mtof(y - mY);
    }

    private void saveScale()
    {
        oldMX = mX;
        oldMY = mY;
        oldMW = mW;
        oldMH = mH;
    }

    private void activateScale()
    {
        int cx = mX + Compat.intdiv(mW, 2);
        int cy = mY + Compat.intdiv(mH, 2);
        mW = ftom(fW);
        mH = ftom(fH);
        mX = cx - Compat.intdiv(mW, 2);
        mY = cy - Compat.intdiv(mH, 2);
        mX2 = mX + mW;
        mY2 = mY + mH;
    }

    private void minOut()
    {
        scaleMtof = minScale;
        scaleFtom = Compat.fixedDiv(Defs.FRACUNIT, scaleMtof);
        activateScale();
    }

    private void restoreScale(Game game)
    {
        mW = oldMW;
        mH = oldMH;
        if (followPlayer != 0)
        {
            mX = game.player.mo.x - Compat.intdiv(mW, 2);
            mY = game.player.mo.y - Compat.intdiv(mH, 2);
        }
        else
        {
            mX = oldMX;
            mY = oldMY;
        }
        mX2 = mX + mW;
        mY2 = mY + mH;
        scaleMtof = Compat.fixedDiv(fW * Defs.FRACUNIT, mW);
        scaleFtom = Compat.fixedDiv(Defs.FRACUNIT, scaleMtof);
    }

    private void changeScale()
    {
        scaleMtof = Compat.fixedMul(scaleMtof, zoomMtof);
        scaleFtom = Compat.fixedDiv(Defs.FRACUNIT, scaleMtof);
        if (scaleMtof < minScale)
        {
            minOut();
        }
        else if (scaleMtof > maxScale)
        {
            scaleMtof = maxScale;
            scaleFtom = Compat.fixedDiv(Defs.FRACUNIT, scaleMtof);
            activateScale();
        }
        else
        {
            activateScale();
        }
    }

    private void changeLocation()
    {
        if (panX != 0 || panY != 0)
        {
            followPlayer = 0;
            oldFollowX = INT_MAX;
        }
        mX += panX;
        mY += panY;
        mX = Math.min(maxX - Compat.intdiv(mW, 2), Math.max(minX - Compat.intdiv(mW, 2), mX));
        mY = Math.min(maxY - Compat.intdiv(mH, 2), Math.max(minY - Compat.intdiv(mH, 2), mY));
        mX2 = mX + mW;
        mY2 = mY + mH;
    }

    private void follow(Game game)
    {
        Mobj mo = game.player.mo;
        if (oldFollowX != mo.x || oldFollowY != mo.y)
        {
            mX = ftom(mtof(mo.x)) - Compat.intdiv(mW, 2);
            mY = ftom(mtof(mo.y)) - Compat.intdiv(mH, 2);
            mX2 = mX + mW;
            mY2 = mY + mH;
            oldFollowX = mo.x;
            oldFollowY = mo.y;
        }
    }

    private void drawGrid(int[] fb, Game game)
    {
        int block = 128 * Defs.FRACUNIT;
        int start = mX + ((block - ((mX - game.world.bmaporgx) % block)) % block);
        for (int x = start; x < mX + mW; x += block)
        {
            line(fb, x, mY, x, mY + mH, GRID_COLOR);
        }
        start = mY + ((block - ((mY - game.world.bmaporgy) % block)) % block);
        for (int y = start; y < mY + mH; y += block)
        {
            line(fb, mX, y, mX + mW, y, GRID_COLOR);
        }
    }

    private void drawWalls(int[] fb, Game game)
    {
        for (Line linedef : game.world.lines)
        {
            if (cheating == 0 && ((linedef.flags & Defs.ML_MAPPED) == 0 || (linedef.flags & Defs.ML_DONTDRAW) != 0))
            {
                continue;
            }
            Integer color = null;
            if (linedef.backsector == null)
            {
                color = REDS;
            }
            else if (linedef.frontsector != null)
            {
                if (linedef.special == 39)
                {
                    color = REDS + Compat.intdiv(RED_RANGE, 2);
                }
                else if ((linedef.flags & Defs.ML_SECRET) != 0)
                {
                    color = REDS;
                }
                else if (linedef.backsector.floorheight != linedef.frontsector.floorheight)
                {
                    color = BROWNS;
                }
                else if (linedef.backsector.ceilingheight != linedef.frontsector.ceilingheight)
                {
                    color = YELLOWS;
                }
                else if (cheating != 0)
                {
                    color = GRAYS;
                }
            }
            if (color != null)
            {
                line(fb, linedef.v1.x, linedef.v1.y, linedef.v2.x, linedef.v2.y, color);
            }
        }
    }

    private void drawCharacter(int[] fb, int[][] shape, int scale, int angle, int color, int x, int y)
    {
        for (int[] seg : shape)
        {
            int ax = seg[0];
            int ay = seg[1];
            int bx = seg[2];
            int by = seg[3];
            if (scale != 0)
            {
                ax = Compat.fixedMul(scale, ax);
                ay = Compat.fixedMul(scale, ay);
                bx = Compat.fixedMul(scale, bx);
                by = Compat.fixedMul(scale, by);
            }
            if (angle != 0)
            {
                int[] a = rotate(ax, ay, angle);
                ax = a[0];
                ay = a[1];
                int[] b = rotate(bx, by, angle);
                bx = b[0];
                by = b[1];
            }
            line(fb, ax + x, ay + y, bx + x, by + y, color);
        }
    }

    private int[] rotate(int x, int y, int angle)
    {
        int cos = Tables.fineCos(angle);
        int sin = Tables.fineSin(angle);
        return new int[] { Compat.fixedMul(x, cos) - Compat.fixedMul(y, sin), Compat.fixedMul(x, sin) + Compat.fixedMul(y, cos) };
    }

    private void line(int[] fb, int ax, int ay, int bx, int by, int color)
    {
        ClipSeg seg = new ClipSeg(cx(ax), cy(ay), cx(bx), cy(by));
        if (!clip(seg))
        {
            return;
        }
        int x0 = seg.x0;
        int y0 = seg.y0;
        int x1 = seg.x1;
        int y1 = seg.y1;
        int dx = Math.abs(x1 - x0);
        int sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0);
        int sy = y0 < y1 ? 1 : -1;
        int error = dx + dy;
        while (true)
        {
            put(fb, x0, y0, color);
            if (x0 == x1 && y0 == y1)
            {
                break;
            }
            int twice = 2 * error;
            if (twice >= dy)
            {
                error += dy;
                x0 += sx;
            }
            if (twice <= dx)
            {
                error += dx;
                y0 += sy;
            }
        }
    }

    private boolean clip(ClipSeg seg)
    {
        for (int i = 0; i < 8; ++i)
        {
            int a = code(seg.x0, seg.y0);
            int b = code(seg.x1, seg.y1);
            if ((a | b) == 0)
            {
                return true;
            }
            if ((a & b) != 0)
            {
                return false;
            }
            int out = a != 0 ? a : b;
            int x;
            int y;
            if ((out & 8) != 0)
            {
                int den = seg.y1 - seg.y0;
                if (den == 0)
                {
                    den = 1;
                }
                x = seg.x0 + Compat.intdiv((seg.x1 - seg.x0) * -seg.y0, den);
                y = 0;
            }
            else if ((out & 4) != 0)
            {
                y = fH - 1;
                int den = seg.y1 - seg.y0;
                if (den == 0)
                {
                    den = 1;
                }
                x = seg.x0 + Compat.intdiv((seg.x1 - seg.x0) * (y - seg.y0), den);
            }
            else if ((out & 2) != 0)
            {
                x = fW - 1;
                int den = seg.x1 - seg.x0;
                if (den == 0)
                {
                    den = 1;
                }
                y = seg.y0 + Compat.intdiv((seg.y1 - seg.y0) * (x - seg.x0), den);
            }
            else
            {
                x = 0;
                int den = seg.x1 - seg.x0;
                if (den == 0)
                {
                    den = 1;
                }
                y = seg.y0 + Compat.intdiv((seg.y1 - seg.y0) * -seg.x0, den);
            }
            if (out == a)
            {
                seg.x0 = x;
                seg.y0 = y;
            }
            else
            {
                seg.x1 = x;
                seg.y1 = y;
            }
        }
        return false;
    }

    private static int code(int x, int y)
    {
        return (x < 0 ? 1 : x >= Defs.SCREENWIDTH ? 2 : 0) | (y < 0 ? 8 : y >= Defs.SCREENHEIGHT - Defs.SBARHEIGHT ? 4 : 0);
    }

    private void put(int[] fb, int x, int y, int color)
    {
        if (x >= 0 && x < fW && y >= 0 && y < fH)
        {
            fb[y * fW + x] = color & 0xff;
        }
    }

    private static final class ClipSeg
    {
        int x0;
        int y0;
        int x1;
        int y1;

        ClipSeg(int x0, int y0, int x1, int y1)
        {
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
        }
    }
}
