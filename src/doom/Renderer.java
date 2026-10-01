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
import java.util.Arrays;
import java.util.List;

public final class Renderer
{
    private static final int HEIGHTBITS = 12;
    private static final int HEIGHTUNIT = 1 << 12;
    private static final int ANGLETOSKYSHIFT = 22;
    private static final int SHRT_MAX = 0x7fff;
    private static final int INT_MAX = 0x7fffffff;
    private static final int INT_MIN = -0x7fffffff;

    public Resources res;
    public int viewwidth;
    public int viewheight;
    public int centerx;
    public int centery;
    public int centerxfrac;
    public int centeryfrac;
    public int projection;
    public int detailshift;
    public int[] viewangletox;
    public int[] xtoviewangle;
    public int clipangle;
    public int[] yslope;
    public int[] distscale;
    public int[][] scalelight;
    public int[][] zlight;
    public int[] walllights;
    public int[] ylookup;
    public int[] columnofs;
    public int viewwindowx;
    public int viewwindowy;
    public int scaledviewwidth;
    public int screenblocks = 10;
    public int pspritescale;
    public int pspriteiscale;
    public int[] ceilingclip;
    public int[] floorclip;
    private final List<ClipRange> solidsegs = new ArrayList<>();
    private int newend;
    private final List<Visplane> visplanes = new ArrayList<>();
    private Visplane floorplane;
    private Visplane ceilingplane;
    public int viewx;
    public int viewy;
    public int viewz;
    public int viewangle;
    public int viewcos;
    public int viewsin;
    public int extralight;
    public byte[] fixedcolormap;
    private Seg curline;
    private Sector frontsector;
    private Sector backsector;
    private int rw_x;
    private int rw_stopx;
    private int rw_start;
    private int rw_centerangle;
    private int rw_offset;
    private int rw_distance;
    private int rw_scale;
    private int rw_scalestep;
    private int rw_midtexturemid;
    private int rw_toptexturemid;
    private int rw_bottomtexturemid;
    private int rw_normalangle;
    private int rw_angle1;
    private boolean segtextured;
    private boolean markfloor;
    private boolean markceiling;
    private boolean maskedtexture;
    private int[] maskedtexturecol;
    private int midtexture;
    private int toptexture;
    private int bottomtexture;
    private int pixhigh;
    private int pixlow;
    private int pixhighstep;
    private int pixlowstep;
    private int topfrac;
    private int topstep;
    private int bottomfrac;
    private int bottomstep;
    private int worldtop;
    private int worldbottom;
    private int worldhigh;
    private int worldlow;
    public int dc_x;
    public int dc_yl;
    public int dc_yh;
    public int dc_iscale;
    public int dc_texturemid;
    public byte[] dc_source;
    public byte[] dc_colormap;
    public int[] fb;
    public final List<DrawSeg> drawsegs = new ArrayList<>();
    private int basexscale;
    private int baseyscale;

    public Renderer(Resources res)
    {
        Tables.initTables();
        this.res = res;
        this.viewwidth = Defs.SCREENWIDTH;
        this.viewheight = Defs.SCREENHEIGHT - Defs.SBARHEIGHT;
        this.centerx = Compat.intdiv(this.viewwidth, 2);
        this.centery = Compat.intdiv(this.viewheight, 2);
        this.centerxfrac = this.centerx << Defs.FRACBITS;
        this.centeryfrac = this.centery << Defs.FRACBITS;
        this.projection = this.centerxfrac;
        this.viewangletox = new int[Compat.intdiv(Defs.FINEANGLES, 2)];
        this.xtoviewangle = new int[Defs.SCREENWIDTH + 1];
        this.yslope = new int[Defs.SCREENHEIGHT];
        this.distscale = new int[Defs.SCREENWIDTH];
        this.scalelight = new int[Defs.LIGHTLEVELS][Defs.MAXLIGHTSCALE];
        this.zlight = new int[Defs.LIGHTLEVELS][Defs.MAXLIGHTZ];
        this.walllights = new int[Defs.MAXLIGHTSCALE];
        this.ylookup = new int[Defs.SCREENHEIGHT];
        for (int i = 0; i < Defs.SCREENHEIGHT; ++i) {
            this.ylookup[i] = i * Defs.SCREENWIDTH;
        }
        this.columnofs = new int[Defs.SCREENWIDTH];
        for (int i = 0; i < Defs.SCREENWIDTH; ++i) {
            this.columnofs[i] = i;
        }
        this.scaledviewwidth = Defs.SCREENWIDTH;
        this.pspritescale = Defs.FRACUNIT;
        this.pspriteiscale = Defs.FRACUNIT;
        this.ceilingclip = new int[Defs.SCREENWIDTH];
        this.floorclip = new int[Defs.SCREENWIDTH];
        for (int i = 0; i < 64; ++i) {
            this.solidsegs.add(new ClipRange());
        }
        this.dc_source = new byte[128];
        this.dc_colormap = new byte[256];
        for (int i = 0; i < 256; ++i) {
            this.dc_colormap[i] = (byte) i;
        }
        this.fb = new int[Defs.SCREENWIDTH * Defs.SCREENHEIGHT];
        this.initMapping();
        this.initLights();
        this.initSlopes();
    }

    public void setViewSize(int blocks, int detail)
    {
        blocks = Math.max(3, Math.min(11, blocks));
        this.detailshift = detail != 0 ? 1 : 0;
        this.screenblocks = blocks;
        int scaled;
        int viewheight;
        if (blocks == 11) {
            scaled = Defs.SCREENWIDTH;
            viewheight = Defs.SCREENHEIGHT;
        } else {
            scaled = blocks * 32;
            viewheight = Compat.intdiv(blocks * 168, 10) & ~7;
        }
        this.scaledviewwidth = scaled;
        this.viewwidth = scaled >> this.detailshift;
        this.viewheight = viewheight;
        this.centerx = Compat.intdiv(this.viewwidth, 2);
        this.centery = Compat.intdiv(this.viewheight, 2);
        this.centerxfrac = this.centerx << Defs.FRACBITS;
        this.centeryfrac = this.centery << Defs.FRACBITS;
        this.projection = this.centerxfrac;
        this.viewwindowx = (Defs.SCREENWIDTH - scaled) >> 1;
        this.viewwindowy = scaled == Defs.SCREENWIDTH ? 0 : (Defs.SCREENHEIGHT - Defs.SBARHEIGHT - viewheight) >> 1;
        this.ylookup = new int[Defs.SCREENHEIGHT];
        for (int i = 0; i < Defs.SCREENHEIGHT; ++i) {
            this.ylookup[i] = (i + this.viewwindowy) * Defs.SCREENWIDTH;
        }
        this.columnofs = new int[Defs.SCREENWIDTH];
        for (int i = 0; i < Defs.SCREENWIDTH; ++i) {
            this.columnofs[i] = this.viewwindowx + i;
        }
        this.ceilingclip = new int[Math.max(1, this.viewwidth)];
        this.floorclip = new int[Math.max(1, this.viewwidth)];
        this.pspritescale = Compat.intdiv(Defs.FRACUNIT * this.viewwidth, Defs.SCREENWIDTH);
        this.pspriteiscale = Compat.intdiv(Defs.FRACUNIT * Defs.SCREENWIDTH, Math.max(1, this.viewwidth));
        this.initMapping();
        this.initSlopes();
        this.initLights();
    }

    private void initMapping()
    {
        int half = Compat.intdiv(Defs.FINEANGLES, 2);
        int focal = Compat.fixedDiv(
            this.centerxfrac,
            Tables.finetangent[Compat.intdiv(Defs.FINEANGLES, 4) + Compat.intdiv(Defs.FIELDOFVIEW, 2)]
        );
        for (int i = 0; i < half; ++i) {
            int ft = Tables.finetangent[i];
            int t;
            if (ft > Defs.FRACUNIT * 2) {
                t = -1;
            } else if (ft < -Defs.FRACUNIT * 2) {
                t = this.viewwidth + 1;
            } else {
                t = Compat.asI32(this.centerxfrac - Compat.fixedMul(ft, focal) + Defs.FRACUNIT - 1) >> Defs.FRACBITS;
                t = Math.max(-1, Math.min(this.viewwidth + 1, t));
            }
            this.viewangletox[i] = t;
        }
        for (int x = 0; x <= this.viewwidth; ++x) {
            int i = 0;
            while (i < half && this.viewangletox[i] > x) {
                ++i;
            }
            this.xtoviewangle[x] = Compat.asU32((i << Defs.ANGLETOFINESHIFT) - Defs.ANG90);
        }
        for (int i = 0; i < half; ++i) {
            if (this.viewangletox[i] == -1) {
                this.viewangletox[i] = 0;
            } else if (this.viewangletox[i] == this.viewwidth + 1) {
                this.viewangletox[i] = this.viewwidth;
            }
        }
        this.clipangle = this.xtoviewangle[0];
    }

    private void initLights()
    {
        for (int i = 0; i < Defs.LIGHTLEVELS; ++i) {
            int startmap = Compat.intdiv((Defs.LIGHTLEVELS - 1 - i) * 2 * Defs.NUMCOLORMAPS, Defs.LIGHTLEVELS);
            for (int j = 0; j < Defs.MAXLIGHTZ; ++j) {
                int scale = Compat.fixedDiv(Compat.intdiv(Defs.SCREENWIDTH, 2) * Defs.FRACUNIT, (j + 1) << Defs.LIGHTZSHIFT)
                    >> Defs.LIGHTSCALESHIFT;
                this.zlight[i][j] = Math.max(0, Math.min(Defs.NUMCOLORMAPS - 1, startmap - Compat.intdiv(scale, 2)));
            }
            int vw = Math.max(1, this.viewwidth << this.detailshift);
            for (int j = 0; j < Defs.MAXLIGHTSCALE; ++j) {
                int level = startmap - (j * Defs.SCREENWIDTH / vw / 2);
                this.scalelight[i][j] = Math.max(0, Math.min(Defs.NUMCOLORMAPS - 1, level));
            }
        }
    }

    private void initSlopes()
    {
        for (int i = 0; i < this.viewheight; ++i) {
            int dy = Math.abs(((i - this.centery) << Defs.FRACBITS) + Compat.intdiv(Defs.FRACUNIT, 2));
            this.yslope[i] = Compat.fixedDiv(
                Compat.intdiv(this.viewwidth << this.detailshift, 2) * Defs.FRACUNIT,
                Math.max(1, dy)
            );
        }
        for (int i = 0; i < this.viewwidth; ++i) {
            int cosadj = Compat.absFixed(Tables.fineCos(this.xtoviewangle[i]));
            this.distscale[i] = Compat.fixedDiv(Defs.FRACUNIT, Math.max(1, cosadj));
        }
    }

    public int pointToAngle(int x, int y)
    {
        x = Compat.asI32(x - this.viewx);
        y = Compat.asI32(y - this.viewy);
        if (x == 0 && y == 0) {
            return 0;
        }
        if (x >= 0) {
            if (y >= 0) {
                return x > y
                    ? Tables.tantoangle[Tables.slopeDiv(y, x)]
                    : Compat.asU32(Defs.ANG90 - 1 - Tables.tantoangle[Tables.slopeDiv(x, y)]);
            }
            y = -y;
            return x > y
                ? Compat.asU32(-Tables.tantoangle[Tables.slopeDiv(y, x)])
                : Compat.asU32(0xc0000000 + Tables.tantoangle[Tables.slopeDiv(x, y)]);
        }
        x = -x;
        if (y >= 0) {
            return x > y
                ? Compat.asU32(Defs.ANG180 - 1 - Tables.tantoangle[Tables.slopeDiv(y, x)])
                : Compat.asU32(Defs.ANG90 + Tables.tantoangle[Tables.slopeDiv(x, y)]);
        }
        y = -y;
        return x > y
            ? Compat.asU32(Defs.ANG180 + Tables.tantoangle[Tables.slopeDiv(y, x)])
            : Compat.asU32(0xc0000000 - 1 - Tables.tantoangle[Tables.slopeDiv(x, y)]);
    }

    public int pointOnSide(int x, int y, Node node)
    {
        return Collision.pointOnSide(x, y, node);
    }

    public int scaleFromGlobalAngle(int visangle)
    {
        int anglea = Compat.asU32(Defs.ANG90 + Compat.asU32(visangle - this.viewangle));
        int angleb = Compat.asU32(Defs.ANG90 + Compat.asU32(visangle - this.rw_normalangle));
        int sinea = Tables.finesine[Compat.ushr(anglea, Defs.ANGLETOFINESHIFT) & Defs.FINEMASK];
        int sineb = Tables.finesine[Compat.ushr(angleb, Defs.ANGLETOFINESHIFT) & Defs.FINEMASK];
        int num = Compat.fixedMul(this.projection, sineb) << this.detailshift;
        int den = Compat.fixedMul(this.rw_distance, sinea);
        if (den != 0 && den > (num >> 16)) {
            return Math.max(256, Math.min(64 * Defs.FRACUNIT, Compat.fixedDiv(num, den)));
        }
        return 64 * Defs.FRACUNIT;
    }

    public void setupFrame(int x, int y, int z, int angle)
    {
        this.setupFrame(x, y, z, angle, 0);
    }

    public void setupFrame(int x, int y, int z, int angle, int extraLight)
    {
        this.setupFrame(x, y, z, angle, extraLight, 0);
    }

    public void setupFrame(int x, int y, int z, int angle, int extraLight, int fixedcolormap)
    {
        this.viewx = x;
        this.viewy = y;
        this.viewz = z;
        this.viewangle = Compat.asU32(angle);
        this.viewsin = Tables.fineSin(this.viewangle);
        this.viewcos = Tables.fineCos(this.viewangle);
        this.extralight = extraLight;
        this.fixedcolormap = fixedcolormap != 0 ? this.res.colormap(fixedcolormap) : null;
        int ang = Compat.ushr(Compat.asU32(this.viewangle - Defs.ANG90), Defs.ANGLETOFINESHIFT) & Defs.FINEMASK;
        this.basexscale = Compat.fixedDiv(
            Tables.finesine[(ang + Compat.intdiv(Defs.FINEANGLES, 4)) & Defs.FINEMASK],
            this.centerxfrac != 0 ? this.centerxfrac : 1
        );
        this.baseyscale = -Compat.fixedDiv(Tables.finesine[ang], this.centerxfrac != 0 ? this.centerxfrac : 1);
    }

    public void render(World world, int[] fb)
    {
        this.fb = fb;
        this.visplanes.clear();
        this.drawsegs.clear();
        this.clearClip();
        if (world.nodes != null && !world.nodes.isEmpty()) {
            this.renderBspNode(world, world.numnodes - 1);
        } else {
            this.subsector(world, 0);
        }
        this.drawPlanes();
    }

    private void clearClip()
    {
        this.solidsegs.get(0).first = -0x7fffffff;
        this.solidsegs.get(0).last = -1;
        this.solidsegs.get(1).first = this.viewwidth;
        this.solidsegs.get(1).last = 0x7fffffff;
        this.newend = 2;
        for (int i = 0; i < this.viewwidth; ++i) {
            this.floorclip[i] = this.viewheight;
            this.ceilingclip[i] = -1;
        }
    }

    private Visplane findPlane(int height, int picnum, int lightlevel)
    {
        if (picnum == this.res.skyflatnum) {
            height = 0;
            lightlevel = 0;
        }
        for (Visplane plane : this.visplanes) {
            if (plane.height == height && plane.picnum == picnum && plane.lightlevel == lightlevel) {
                return plane;
            }
        }
        Visplane plane = new Visplane(height, picnum, lightlevel, this.viewwidth, -1);
        plane.top = new int[Defs.SCREENWIDTH];
        Arrays.fill(plane.top, 0xff);
        plane.bottom = new int[Defs.SCREENWIDTH];
        this.visplanes.add(plane);
        return plane;
    }

    private Visplane checkPlane(Visplane plane, int start, int stop)
    {
        if (plane == null) {
            return this.findPlane(0, 0, 0);
        }
        int intrl;
        int unionl;
        int intrh;
        int unionh;
        if (start < plane.minx) {
            intrl = plane.minx;
            unionl = start;
        } else {
            unionl = plane.minx;
            intrl = start;
        }
        if (stop > plane.maxx) {
            intrh = plane.maxx;
            unionh = stop;
        } else {
            unionh = plane.maxx;
            intrh = stop;
        }
        int x = intrl;
        while (x <= intrh) {
            if (x >= 0 && x < Defs.SCREENWIDTH && plane.top[x] != 0xff) {
                break;
            }
            ++x;
        }
        if (x > intrh) {
            plane.minx = unionl;
            plane.maxx = unionh;
            return plane;
        }
        Visplane copy = new Visplane(plane.height, plane.picnum, plane.lightlevel, start, stop);
        copy.top = new int[Defs.SCREENWIDTH];
        Arrays.fill(copy.top, 0xff);
        copy.bottom = new int[Defs.SCREENWIDTH];
        this.visplanes.add(copy);
        return copy;
    }

    private void renderBspNode(World world, int bspnum)
    {
        if ((bspnum & Defs.NF_SUBSECTOR) != 0 || bspnum < 0) {
            this.subsector(world, bspnum == -1 ? 0 : bspnum & ~Defs.NF_SUBSECTOR);
            return;
        }
        Node node = world.nodes.get(bspnum);
        int side = this.pointOnSide(this.viewx, this.viewy, node);
        this.renderBspNode(world, node.children[side]);
        this.renderBspNode(world, node.children[side ^ 1]);
    }

    private void subsector(World world, int num)
    {
        Subsector sub = world.subsectors.get(num);
        this.frontsector = sub.sector;
        int light = Math.max(
            0,
            Math.min(Defs.LIGHTLEVELS - 1, (this.frontsector.lightlevel >> 4) + this.extralight)
        );
        this.walllights = this.scalelight[light];
        this.floorplane = this.findPlane(
            this.frontsector.floorheight,
            this.frontsector.floorpic,
            this.frontsector.lightlevel
        );
        this.ceilingplane = this.findPlane(
            this.frontsector.ceilingheight,
            this.frontsector.ceilingpic,
            this.frontsector.lightlevel
        );
        int line = sub.firstline;
        for (int i = 0; i < sub.numlines; ++i, ++line) {
            this.addLine(world.segs.get(line));
        }
    }

    private void addLine(Seg line)
    {
        this.curline = line;
        int angle1 = this.pointToAngle(line.v1.x, line.v1.y);
        int angle2 = this.pointToAngle(line.v2.x, line.v2.y);
        int span = Compat.asU32(angle1 - angle2);
        if (Integer.compareUnsigned(span, Defs.ANG180) >= 0) {
            return;
        }
        this.rw_angle1 = angle1;
        angle1 = Compat.asU32(angle1 - this.viewangle);
        angle2 = Compat.asU32(angle2 - this.viewangle);
        int tspan = Compat.asU32(angle1 + this.clipangle);
        if (Integer.compareUnsigned(tspan, Compat.asU32(2 * this.clipangle)) > 0) {
            tspan = Compat.asU32(tspan - 2 * this.clipangle);
            if (Integer.compareUnsigned(tspan, span) >= 0) {
                return;
            }
            angle1 = this.clipangle;
        }
        tspan = Compat.asU32(this.clipangle - angle2);
        if (Integer.compareUnsigned(tspan, Compat.asU32(2 * this.clipangle)) > 0) {
            tspan = Compat.asU32(tspan - 2 * this.clipangle);
            if (Integer.compareUnsigned(tspan, span) >= 0) {
                return;
            }
            angle2 = Compat.asU32(-this.clipangle);
        }
        int mask = Compat.intdiv(Defs.FINEANGLES, 2) - 1;
        int x1 = this.viewangletox[Compat.ushr(Compat.asU32(angle1 + Defs.ANG90), Defs.ANGLETOFINESHIFT) & mask];
        int x2 = this.viewangletox[Compat.ushr(Compat.asU32(angle2 + Defs.ANG90), Defs.ANGLETOFINESHIFT) & mask];
        if (x1 == x2) {
            return;
        }
        this.backsector = line.backsector;
        if (
            this.backsector == null
                || this.backsector.ceilingheight <= this.frontsector.floorheight
                || this.backsector.floorheight >= this.frontsector.ceilingheight
        ) {
            this.clipSolid(x1, x2 - 1);
        } else {
            this.clipPass(x1, x2 - 1);
        }
    }

    private void clipSolid(int first, int last)
    {
        if (first > last) {
            return;
        }
        int start = 0;
        while (start < this.newend && this.solidsegs.get(start).last < first - 1) {
            ++start;
        }
        if (start >= this.newend) {
            this.storeWallRange(first, last);
            return;
        }
        if (first < this.solidsegs.get(start).first) {
            if (last < this.solidsegs.get(start).first - 1) {
                this.storeWallRange(first, last);
                this.solidsegs.add(start, new ClipRange(first, last));
                ++this.newend;
                return;
            }
            this.storeWallRange(first, this.solidsegs.get(start).first - 1);
            this.solidsegs.get(start).first = first;
        }
        if (last <= this.solidsegs.get(start).last) {
            return;
        }
        int next = start;
        while (next + 1 < this.newend && last >= this.solidsegs.get(next + 1).first - 1) {
            this.storeWallRange(this.solidsegs.get(next).last + 1, this.solidsegs.get(next + 1).first - 1);
            ++next;
            if (last <= this.solidsegs.get(next).last) {
                this.solidsegs.get(start).last = this.solidsegs.get(next).last;
                this.crunchSolid(start, next);
                return;
            }
        }
        this.storeWallRange(this.solidsegs.get(next).last + 1, last);
        this.solidsegs.get(start).last = last;
        this.crunchSolid(start, next);
    }

    private void crunchSolid(int start, int next)
    {
        if (next == start) {
            return;
        }
        this.solidsegs.subList(start + 1, next + 1).clear();
        this.newend -= next - start;
        while (this.solidsegs.size() < 64) {
            this.solidsegs.add(new ClipRange());
        }
    }

    private void clipPass(int first, int last)
    {
        if (first > last) {
            return;
        }
        int start = 0;
        while (start < this.newend && this.solidsegs.get(start).last < first - 1) {
            ++start;
        }
        if (start >= this.newend) {
            this.storeWallRange(first, last);
            return;
        }
        if (first < this.solidsegs.get(start).first) {
            if (last < this.solidsegs.get(start).first - 1) {
                this.storeWallRange(first, last);
                return;
            }
            this.storeWallRange(first, this.solidsegs.get(start).first - 1);
        }
        if (last <= this.solidsegs.get(start).last) {
            return;
        }
        int next = start;
        while (next + 1 < this.newend && last >= this.solidsegs.get(next + 1).first - 1) {
            this.storeWallRange(this.solidsegs.get(next).last + 1, this.solidsegs.get(next + 1).first - 1);
            ++next;
            if (last <= this.solidsegs.get(next).last) {
                return;
            }
        }
        this.storeWallRange(this.solidsegs.get(next).last + 1, last);
    }

    private void storeWallRange(int start, int stop)
    {
        if (start > stop) {
            return;
        }
        Seg line = this.curline;
        Line linedef = line.linedef;
        Side sidedef = line.sidedef;
        linedef.flags |= Defs.ML_MAPPED;
        this.rw_normalangle = Compat.asU32(line.angle + Defs.ANG90);
        int offsetangle = Compat.asU32(this.rw_normalangle - this.rw_angle1);
        if (Integer.compareUnsigned(offsetangle, Defs.ANG180) > 0) {
            offsetangle = Compat.asU32(-offsetangle);
        }
        offsetangle = unsignedMin(offsetangle, Defs.ANG90);
        int distangle = Compat.asU32(Defs.ANG90 - offsetangle);
        int hyp = this.pointToDist(line.v1.x, line.v1.y);
        this.rw_distance = Compat.fixedMul(
            hyp,
            Tables.finesine[Compat.ushr(distangle, Defs.ANGLETOFINESHIFT) & Defs.FINEMASK]
        );
        this.rw_x = start;
        this.rw_start = start;
        this.rw_stopx = stop + 1;
        this.rw_scale = this.scaleFromGlobalAngle(Compat.asU32(this.viewangle + this.xtoviewangle[start]));
        this.rw_scalestep = stop > start
            ? floorDiv(
                this.scaleFromGlobalAngle(Compat.asU32(this.viewangle + this.xtoviewangle[stop])) - this.rw_scale,
                stop - start
            )
            : 0;
        this.worldtop = this.frontsector.ceilingheight - this.viewz;
        this.worldbottom = this.frontsector.floorheight - this.viewz;
        this.midtexture = 0;
        this.toptexture = 0;
        this.bottomtexture = 0;
        this.maskedtexture = false;
        this.maskedtexturecol = null;
        this.segtextured = false;
        if (this.backsector == null) {
            this.midtexture = sidedef.midtexture;
            this.markfloor = true;
            this.markceiling = true;
            this.rw_midtexturemid = (linedef.flags & Defs.ML_DONTPEGBOTTOM) != 0
                ? this.frontsector.floorheight + this.res.textureHeight(this.midtexture) - this.viewz
                : this.worldtop;
            this.rw_midtexturemid += sidedef.rowoffset;
        } else {
            this.worldhigh = this.backsector.ceilingheight - this.viewz;
            this.worldlow = this.backsector.floorheight - this.viewz;
            if (
                this.frontsector.ceilingpic == this.res.skyflatnum
                    && this.backsector.ceilingpic == this.res.skyflatnum
            ) {
                this.worldtop = this.worldhigh;
            }
            this.markfloor = this.worldlow != this.worldbottom
                || this.backsector.floorpic != this.frontsector.floorpic
                || this.backsector.lightlevel != this.frontsector.lightlevel;
            this.markceiling = this.worldhigh != this.worldtop
                || this.backsector.ceilingpic != this.frontsector.ceilingpic
                || this.backsector.lightlevel != this.frontsector.lightlevel;
            if (
                this.backsector.ceilingheight <= this.frontsector.floorheight
                    || this.backsector.floorheight >= this.frontsector.ceilingheight
            ) {
                this.markfloor = true;
                this.markceiling = true;
            }
            if (this.worldhigh < this.worldtop) {
                this.toptexture = sidedef.toptexture;
                this.rw_toptexturemid = (linedef.flags & Defs.ML_DONTPEGTOP) != 0
                    ? this.worldtop
                    : this.backsector.ceilingheight + this.res.textureHeight(this.toptexture) - this.viewz;
            }
            if (this.worldlow > this.worldbottom) {
                this.bottomtexture = sidedef.bottomtexture;
                this.rw_bottomtexturemid = (linedef.flags & Defs.ML_DONTPEGBOTTOM) != 0 ? this.worldtop : this.worldlow;
            }
            this.rw_toptexturemid += sidedef.rowoffset;
            this.rw_bottomtexturemid += sidedef.rowoffset;
            if (sidedef.midtexture != 0) {
                this.maskedtexture = true;
                this.maskedtexturecol = new int[stop - start + 1];
                Arrays.fill(this.maskedtexturecol, SHRT_MAX);
            }
        }
        this.segtextured = this.midtexture != 0
            || this.toptexture != 0
            || this.bottomtexture != 0
            || this.maskedtexture;
        if (this.segtextured) {
            offsetangle = Compat.asU32(this.rw_normalangle - this.rw_angle1);
            if (Integer.compareUnsigned(offsetangle, Defs.ANG180) > 0) {
                offsetangle = Compat.asU32(-offsetangle);
            }
            this.rw_offset = Compat.fixedMul(
                hyp,
                Tables.finesine[Compat.ushr(offsetangle, Defs.ANGLETOFINESHIFT) & Defs.FINEMASK]
            );
            if (Integer.compareUnsigned(Compat.asU32(this.rw_normalangle - this.rw_angle1), Defs.ANG180) < 0) {
                this.rw_offset = -this.rw_offset;
            }
            this.rw_offset += sidedef.textureoffset + line.offset;
            this.rw_centerangle = Compat.asU32(Defs.ANG90 + this.viewangle - this.rw_normalangle);
        }
        if (this.frontsector.floorheight >= this.viewz) {
            this.markfloor = false;
        }
        if (
            this.frontsector.ceilingheight <= this.viewz
                && this.frontsector.ceilingpic != this.res.skyflatnum
        ) {
            this.markceiling = false;
        }
        if (this.markceiling) {
            this.ceilingplane = this.checkPlane(this.ceilingplane, start, stop);
        }
        if (this.markfloor) {
            this.floorplane = this.checkPlane(this.floorplane, start, stop);
        }
        this.worldtop >>= 4;
        this.worldbottom >>= 4;
        this.topstep = -Compat.fixedMul(this.rw_scalestep, this.worldtop);
        this.topfrac = (this.centeryfrac >> 4) - Compat.fixedMul(this.worldtop, this.rw_scale);
        this.bottomstep = -Compat.fixedMul(this.rw_scalestep, this.worldbottom);
        this.bottomfrac = (this.centeryfrac >> 4) - Compat.fixedMul(this.worldbottom, this.rw_scale);
        if (this.backsector != null) {
            this.worldhigh >>= 4;
            this.worldlow >>= 4;
            if (this.worldhigh < this.worldtop) {
                this.pixhigh = (this.centeryfrac >> 4) - Compat.fixedMul(this.worldhigh, this.rw_scale);
                this.pixhighstep = -Compat.fixedMul(this.rw_scalestep, this.worldhigh);
            }
            if (this.worldlow > this.worldbottom) {
                this.pixlow = (this.centeryfrac >> 4) - Compat.fixedMul(this.worldlow, this.rw_scale);
                this.pixlowstep = -Compat.fixedMul(this.rw_scalestep, this.worldlow);
            }
        }
        int scale1 = this.rw_scale;
        this.renderSegLoop();
        this.pushDrawseg(start, stop, scale1);
    }

    private void pushDrawseg(int start, int stop, int scale1)
    {
        DrawSeg ds = new DrawSeg();
        ds.x1 = start;
        ds.x2 = stop;
        ds.scale1 = scale1;
        ds.scale2 = scale1 + this.rw_scalestep * Math.max(0, stop - start);
        ds.curline = this.curline;
        ds.scalestep = this.rw_scalestep;
        ds.maskedtexturecol = this.maskedtexturecol;
        if (this.backsector == null) {
            ds.silhouette = Defs.SIL_BOTH;
            ds.bsilheight = INT_MAX;
            ds.tsilheight = INT_MIN;
            int width = stop - start + 1;
            ds.sprtopclip = new int[width];
            Arrays.fill(ds.sprtopclip, this.viewheight);
            ds.sprbottomclip = new int[width];
            Arrays.fill(ds.sprbottomclip, -1);
        } else {
            ds.silhouette = Defs.SIL_NONE;
            if (this.frontsector.floorheight > this.backsector.floorheight) {
                ds.silhouette = Defs.SIL_BOTTOM;
                ds.bsilheight = this.frontsector.floorheight;
            } else if (this.backsector.floorheight > this.viewz) {
                ds.silhouette = Defs.SIL_BOTTOM;
                ds.bsilheight = INT_MAX;
            }
            if (this.frontsector.ceilingheight < this.backsector.ceilingheight) {
                ds.silhouette |= Defs.SIL_TOP;
                ds.tsilheight = this.frontsector.ceilingheight;
            } else if (this.backsector.ceilingheight < this.viewz) {
                ds.silhouette |= Defs.SIL_TOP;
                ds.tsilheight = INT_MIN;
            }
            if (this.backsector.ceilingheight <= this.frontsector.floorheight) {
                ds.silhouette |= Defs.SIL_BOTTOM;
                ds.bsilheight = INT_MAX;
            }
            if (this.backsector.floorheight >= this.frontsector.ceilingheight) {
                ds.silhouette |= Defs.SIL_TOP;
                ds.tsilheight = INT_MIN;
            }
            ds.sprtopclip = Arrays.copyOfRange(this.ceilingclip, start, stop + 1);
            ds.sprbottomclip = Arrays.copyOfRange(this.floorclip, start, stop + 1);
            if (this.maskedtexture) {
                if ((ds.silhouette & Defs.SIL_TOP) == 0) {
                    ds.silhouette |= Defs.SIL_TOP;
                    ds.tsilheight = INT_MIN;
                }
                if ((ds.silhouette & Defs.SIL_BOTTOM) == 0) {
                    ds.silhouette |= Defs.SIL_BOTTOM;
                    ds.bsilheight = INT_MAX;
                }
            }
        }
        this.drawsegs.add(ds);
    }

    private int pointToDist(int x, int y)
    {
        int dx = Compat.absFixed(x - this.viewx);
        int dy = Compat.absFixed(y - this.viewy);
        if (dy > dx) {
            int t = dx;
            dx = dy;
            dy = t;
        }
        if (dx == 0) {
            return 0;
        }
        int frac = Compat.fixedDiv(dy, dx);
        int ang = Compat.ushr(Tables.tantoangle[Math.min(frac >> Defs.DBITS, 2048)] + Defs.ANG90, Defs.ANGLETOFINESHIFT);
        return Compat.fixedDiv(dx, Tables.finesine[ang & Defs.FINEMASK]);
    }

    private void renderSegLoop()
    {
        int texturecolumn = 0;
        while (this.rw_x < this.rw_stopx) {
            int yl = Compat.shar(this.topfrac + HEIGHTUNIT - 1, HEIGHTBITS);
            yl = Math.max(yl, this.ceilingclip[this.rw_x] + 1);
            if (this.markceiling && this.ceilingplane != null) {
                int top = this.ceilingclip[this.rw_x] + 1;
                int bottom = Math.min(yl - 1, this.floorclip[this.rw_x] - 1);
                if (top <= bottom) {
                    this.ceilingplane.top[this.rw_x] = top;
                    this.ceilingplane.bottom[this.rw_x] = bottom;
                }
            }
            int yh = Math.min(Compat.shar(this.bottomfrac, HEIGHTBITS), this.floorclip[this.rw_x] - 1);
            if (this.markfloor && this.floorplane != null) {
                int top = Math.max(yh + 1, this.ceilingclip[this.rw_x] + 1);
                int bottom = this.floorclip[this.rw_x] - 1;
                if (top <= bottom) {
                    this.floorplane.top[this.rw_x] = top;
                    this.floorplane.bottom[this.rw_x] = bottom;
                }
            }
            if (this.segtextured) {
                int angle = Compat.ushr(
                    Compat.asU32(this.rw_centerangle + this.xtoviewangle[this.rw_x]),
                    Defs.ANGLETOFINESHIFT
                );
                int tan = Tables.finetangent[angle & (Compat.intdiv(Defs.FINEANGLES, 2) - 1)];
                texturecolumn = Compat.shar(this.rw_offset - Compat.fixedMul(tan, this.rw_distance), Defs.FRACBITS);
                int index = Math.min(Defs.MAXLIGHTSCALE - 1, Compat.ushr(this.rw_scale, Defs.LIGHTSCALESHIFT));
                this.dc_colormap = this.fixedcolormap != null ? this.fixedcolormap : this.res.colormap(this.walllights[index]);
                this.dc_x = this.rw_x;
                this.dc_iscale = this.rw_scale != 0 ? unsignedDiv32(this.rw_scale) : 0;
            }
            if (this.midtexture != 0) {
                this.dc_yl = yl;
                this.dc_yh = yh;
                this.dc_texturemid = this.rw_midtexturemid;
                this.dc_source = this.res.getColumn(this.midtexture, texturecolumn);
                this.drawColumn();
                this.ceilingclip[this.rw_x] = this.viewheight;
                this.floorclip[this.rw_x] = -1;
            } else {
                if (this.toptexture != 0) {
                    int mid = Math.min(Compat.shar(this.pixhigh, HEIGHTBITS), this.floorclip[this.rw_x] - 1);
                    this.pixhigh += this.pixhighstep;
                    if (mid >= yl) {
                        this.dc_yl = yl;
                        this.dc_yh = mid;
                        this.dc_texturemid = this.rw_toptexturemid;
                        this.dc_source = this.res.getColumn(this.toptexture, texturecolumn);
                        this.drawColumn();
                        this.ceilingclip[this.rw_x] = mid;
                    } else {
                        this.ceilingclip[this.rw_x] = yl - 1;
                    }
                } else if (this.markceiling) {
                    this.ceilingclip[this.rw_x] = yl - 1;
                }
                if (this.bottomtexture != 0) {
                    int mid = Math.max(
                        Compat.shar(this.pixlow + HEIGHTUNIT - 1, HEIGHTBITS),
                        this.ceilingclip[this.rw_x] + 1
                    );
                    this.pixlow += this.pixlowstep;
                    if (mid <= yh) {
                        this.dc_yl = mid;
                        this.dc_yh = yh;
                        this.dc_texturemid = this.rw_bottomtexturemid;
                        this.dc_source = this.res.getColumn(this.bottomtexture, texturecolumn);
                        this.drawColumn();
                        this.floorclip[this.rw_x] = mid;
                    } else {
                        this.floorclip[this.rw_x] = yh + 1;
                    }
                } else if (this.markfloor) {
                    this.floorclip[this.rw_x] = yh + 1;
                }
                if (this.maskedtexture && this.maskedtexturecol != null) {
                    this.maskedtexturecol[this.rw_x - this.rw_start] = texturecolumn;
                }
            }
            this.rw_scale += this.rw_scalestep;
            this.topfrac += this.topstep;
            this.bottomfrac += this.bottomstep;
            ++this.rw_x;
        }
    }

    public void drawColumn()
    {
        int count = this.dc_yh - this.dc_yl;
        if (count < 0 || this.dc_x < 0 || this.dc_x >= this.viewwidth) {
            return;
        }
        int yl = Math.max(0, Math.min(Defs.SCREENHEIGHT - 1, this.dc_yl));
        int dest;
        int dest2 = 0;
        boolean dual = false;
        if (this.detailshift != 0) {
            int x = this.dc_x << 1;
            if (x < 0 || x + 1 >= Defs.SCREENWIDTH) {
                return;
            }
            dest = this.ylookup[yl] + this.columnofs[x];
            dest2 = dest + 1;
            dual = true;
        } else {
            dest = this.ylookup[yl] + this.columnofs[this.dc_x];
        }
        int fracstep = this.dc_iscale;
        int frac = this.dc_texturemid + (this.dc_yl - this.centery) * fracstep;
        int slen = this.dc_source.length;
        int clen = this.dc_colormap.length;
        int limit = Defs.SCREENWIDTH * Defs.SCREENHEIGHT;
        while (count >= 0 && dest < limit) {
            int idx = (frac >> Defs.FRACBITS) & 127;
            if (slen != 0) {
                int pix = this.dc_source[idx % slen] & 0xff;
                int value = pix < clen ? this.dc_colormap[pix] & 0xff : pix;
                this.fb[dest] = value;
                if (dual && dest2 < limit) {
                    this.fb[dest2] = value;
                }
            }
            dest += Defs.SCREENWIDTH;
            if (dual) {
                dest2 += Defs.SCREENWIDTH;
            }
            frac = Compat.asI32(frac + fracstep);
            --count;
        }
    }

    public void drawMasked()
    {
        for (int i = this.drawsegs.size() - 1; i >= 0; --i) {
            DrawSeg ds = this.drawsegs.get(i);
            if (ds.maskedtexturecol != null && ds.maskedtexturecol.length > 0) {
                this.renderMaskedSegRange(ds, ds.x1, ds.x2);
            }
        }
    }

    public void renderMaskedSegRange(DrawSeg ds, int x1, int x2)
    {
        if (ds.maskedtexturecol == null || ds.maskedtexturecol.length == 0 || ds.curline == null) {
            return;
        }
        Seg line = ds.curline;
        Sector front = line.frontsector;
        Sector back = line.backsector;
        Side sidedef = line.sidedef;
        int texnum = sidedef != null ? sidedef.midtexture : 0;
        if (texnum == 0 || back == null || front == null) {
            return;
        }
        int lightnum = (front.lightlevel >> 4) + this.extralight;
        if (line.v1.y == line.v2.y) {
            --lightnum;
        } else if (line.v1.x == line.v2.x) {
            ++lightnum;
        }
        int[] walllights = this.scalelight[Math.max(0, Math.min(Defs.LIGHTLEVELS - 1, lightnum))];
        int texturemid;
        if ((line.linedef.flags & Defs.ML_DONTPEGBOTTOM) != 0) {
            texturemid = Math.max(front.floorheight, back.floorheight) + this.res.textureHeight(texnum) - this.viewz;
        } else {
            texturemid = Math.min(front.ceilingheight, back.ceilingheight) - this.viewz;
        }
        texturemid += sidedef.rowoffset;
        int spryscale = ds.scale1 + (x1 - ds.x1) * ds.scalestep;
        for (int x = x1; x <= x2; ++x, spryscale += ds.scalestep) {
            int i = x - ds.x1;
            if (i < 0 || i >= ds.maskedtexturecol.length) {
                continue;
            }
            int tcol = ds.maskedtexturecol[i];
            if (tcol == SHRT_MAX) {
                continue;
            }
            int index = spryscale > 0 ? Compat.ushr(spryscale, Defs.LIGHTSCALESHIFT) : 0;
            index = Math.min(Defs.MAXLIGHTSCALE - 1, index);
            this.dc_colormap = this.fixedcolormap != null ? this.fixedcolormap : this.res.colormap(walllights[index]);
            this.dc_x = x;
            this.dc_iscale = spryscale != 0 ? unsignedDiv32(spryscale) : 0;
            this.dc_texturemid = texturemid;
            int topscreen = this.centeryfrac - Compat.fixedMul(texturemid, spryscale);
            int mceil = i < ds.sprtopclip.length ? ds.sprtopclip[i] : -1;
            int mfloor = i < ds.sprbottomclip.length ? ds.sprbottomclip[i] : this.viewheight;
            this.drawMaskedColumn(this.res.columnPosts(texnum, tcol), topscreen, spryscale, mceil, mfloor);
            ds.maskedtexturecol[i] = SHRT_MAX;
        }
    }

    private void drawMaskedColumn(List<ColumnPost> posts, int sprtopscreen, int spryscale, int mceil, int mfloor)
    {
        int basemid = this.dc_texturemid;
        for (ColumnPost post : posts) {
            byte[] pixels = post.pixels;
            int length = pixels.length;
            if (length == 0) {
                continue;
            }
            int topscreen = sprtopscreen + spryscale * post.topDelta;
            int bottomscreen = topscreen + spryscale * length;
            int yl = (topscreen + Defs.FRACUNIT - 1) >> Defs.FRACBITS;
            int yh = (bottomscreen - 1) >> Defs.FRACBITS;
            yh = Math.min(yh, Math.min(mfloor - 1, this.viewheight - 1));
            yl = Math.max(yl, Math.max(mceil + 1, 0));
            if (yl <= yh) {
                this.dc_yl = yl;
                this.dc_yh = yh;
                byte[] buf = new byte[128];
                System.arraycopy(pixels, 0, buf, 0, Math.min(length, 128));
                this.dc_source = buf;
                this.dc_texturemid = basemid - (post.topDelta << Defs.FRACBITS);
                this.drawColumn();
            }
        }
        this.dc_texturemid = basemid;
    }

    private void drawPlanes()
    {
        for (Visplane plane : this.visplanes) {
            if (plane.minx > plane.maxx) {
                continue;
            }
            if (plane.picnum == this.res.skyflatnum) {
                this.dc_iscale = Compat.intdiv(9 * Defs.FRACUNIT, 10);
                this.dc_colormap = this.res.colormap(0);
                this.dc_texturemid = 100 * Defs.FRACUNIT;
                for (int x = plane.minx; x <= plane.maxx; ++x) {
                    int yl = plane.top[x];
                    int yh = plane.bottom[x];
                    if (yl <= yh && yl < 0xff) {
                        int ang = Compat.ushr(
                            Compat.asU32(this.viewangle + this.xtoviewangle[x]),
                            ANGLETOSKYSHIFT
                        );
                        this.dc_x = x;
                        this.dc_yl = yl;
                        this.dc_yh = yh;
                        this.dc_source = this.res.getColumn(this.res.skytexture, ang);
                        this.drawColumn();
                    }
                }
                continue;
            }
            int light = Math.max(0, Math.min(Defs.LIGHTLEVELS - 1, (plane.lightlevel >> 4) + this.extralight));
            int[] planezlight = this.zlight[light];
            byte[] flat = this.res.flatPixels(plane.picnum);
            int planeheight = Compat.absFixed(plane.height - this.viewz);
            if (planeheight == 0) {
                continue;
            }
            int cachedY = -1;
            int distance = 0;
            for (int x = Math.max(0, plane.minx); x < Math.min(this.viewwidth, plane.maxx + 1); ++x) {
                int t1 = plane.top[x];
                int b1 = plane.bottom[x];
                if (t1 > b1 || t1 == 0xff) {
                    continue;
                }
                for (int y = t1; y <= Math.min(b1, this.viewheight - 1); ++y) {
                    if (y != cachedY) {
                        cachedY = y;
                        distance = Compat.fixedMul(planeheight, this.yslope[y]);
                    }
                    int length = Compat.fixedMul(distance, this.distscale[x]);
                    int ang = Compat.ushr(Compat.asU32(this.viewangle + this.xtoviewangle[x]), Defs.ANGLETOFINESHIFT)
                        & Defs.FINEMASK;
                    int xfrac = this.viewx
                        + Compat.fixedMul(Tables.finesine[(ang + Compat.intdiv(Defs.FINEANGLES, 4)) & Defs.FINEMASK], length);
                    int yfrac = -this.viewy - Compat.fixedMul(Tables.finesine[ang], length);
                    int index = Math.min(Defs.MAXLIGHTZ - 1, Compat.ushr(distance, Defs.LIGHTZSHIFT));
                    byte[] cm = this.fixedcolormap != null ? this.fixedcolormap : this.res.colormap(planezlight[index]);
                    int spot = ((xfrac >> 16) & 63) | ((yfrac >> 10) & 0x0fc0);
                    int source = spot < flat.length ? flat[spot] & 0xff : 0;
                    int pix = cm[source] & 0xff;
                    if (this.detailshift != 0) {
                        int xx = x << 1;
                        int off = this.ylookup[y] + this.columnofs[xx];
                        this.fb[off] = pix;
                        this.fb[off + 1] = pix;
                    } else {
                        this.fb[this.ylookup[y] + this.columnofs[x]] = pix;
                    }
                }
            }
        }
    }

    private static int floorDiv(int a, int b)
    {
        int q = Compat.intdiv(a, b);
        if (a % b != 0 && (a < 0) != (b < 0)) {
            --q;
        }
        return q;
    }

    private static int unsignedMin(int a, int b)
    {
        return Integer.compareUnsigned(a, b) <= 0 ? a : b;
    }

    private static int unsignedDiv32(int den)
    {
        return (int) (0xffffffffL / Integer.toUnsignedLong(den));
    }
}
