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

public class World
{
    public List<Vertex> vertexes = new ArrayList<>();
    public List<Sector> sectors = new ArrayList<>();
    public List<Side> sides = new ArrayList<>();
    public List<Line> lines = new ArrayList<>();
    public List<Seg> segs = new ArrayList<>();
    public List<Subsector> subsectors = new ArrayList<>();
    public List<Node> nodes = new ArrayList<>();
    public List<MapThing> things = new ArrayList<>();
    public int numnodes;
    public int[] blockmap = new int[0];
    public int bmaporgx;
    public int bmaporgy;
    public int bmapwidth;
    public int bmapheight;
    public byte[] blockmaplump = new byte[0];
    public int validcount;
    public List<Mobj> mobjs = new ArrayList<>();
    public byte[] rejectmatrix = new byte[0];

    public void setupLevel(Wad wad, Resources res, int episode, int mapn)
    {
        String mapName = String.format("MAP%02d", mapn);
        String lumpName = wad.checkNumForName(mapName) >= 0 ? mapName : ("E" + episode + "M" + mapn);
        int lump = wad.getNumForName(lumpName);
        loadVertexes(wad.cacheLumpNum(lump + 4));
        loadSectors(wad.cacheLumpNum(lump + 8), res);
        loadSides(wad.cacheLumpNum(lump + 3), res);
        loadLines(wad.cacheLumpNum(lump + 2));
        loadSegs(wad.cacheLumpNum(lump + 5));
        loadSubsectors(wad.cacheLumpNum(lump + 6));
        loadNodes(wad.cacheLumpNum(lump + 7));
        loadThings(wad.cacheLumpNum(lump + 1));
        loadBlockmap(wad.cacheLumpNum(lump + 10));
        byte[] reject = wad.cacheLumpNum(lump + 9);
        this.rejectmatrix = reject != null ? reject : new byte[0];
        this.numnodes = this.nodes.size();
        for (Subsector ss : this.subsectors) {
            ss.sector = this.segs.get(ss.firstline).frontsector;
        }
    }

    private void loadVertexes(byte[] data)
    {
        this.vertexes = new ArrayList<>();
        for (int o = 0; o + Defs.MAPVERTEX_SIZE <= data.length; o += Defs.MAPVERTEX_SIZE) {
            this.vertexes.add(new Vertex(Bin.i16(data, o) * Defs.FRACUNIT, Bin.i16(data, o + 2) * Defs.FRACUNIT));
        }
    }

    private void loadSectors(byte[] data, Resources res)
    {
        this.sectors = new ArrayList<>();
        int i = 0;
        for (int o = 0; o + Defs.MAPSECTOR_SIZE <= data.length; o += Defs.MAPSECTOR_SIZE) {
            Sector s = new Sector();
            s.floorheight = Bin.i16(data, o) * Defs.FRACUNIT;
            s.ceilingheight = Bin.i16(data, o + 2) * Defs.FRACUNIT;
            s.floorpic = res.flatNumForName(Bin.name8(data, o + 4));
            s.ceilingpic = res.flatNumForName(Bin.name8(data, o + 12));
            s.lightlevel = Bin.i16(data, o + 20);
            s.special = Bin.i16(data, o + 22);
            s.tag = Bin.i16(data, o + 24);
            s.iSector = i;
            this.sectors.add(s);
            i++;
        }
    }

    private void loadSides(byte[] data, Resources res)
    {
        this.sides = new ArrayList<>();
        for (int o = 0; o + Defs.MAPSIDEDEF_SIZE <= data.length; o += Defs.MAPSIDEDEF_SIZE) {
            Side s = new Side();
            s.textureoffset = Bin.i16(data, o) * Defs.FRACUNIT;
            s.rowoffset = Bin.i16(data, o + 2) * Defs.FRACUNIT;
            s.toptexture = res.textureNumForName(Bin.name8(data, o + 4));
            s.bottomtexture = res.textureNumForName(Bin.name8(data, o + 12));
            s.midtexture = res.textureNumForName(Bin.name8(data, o + 20));
            int sec = Bin.i16(data, o + 28);
            if (sec >= 0 && sec < this.sectors.size()) {
                s.sector = this.sectors.get(sec);
            } else {
                s.sector = this.sectors.get(0);
            }
            this.sides.add(s);
        }
    }

    private void loadLines(byte[] data)
    {
        this.lines = new ArrayList<>();
        int i = 0;
        for (int o = 0; o + Defs.MAPLINEDEF_SIZE <= data.length; o += Defs.MAPLINEDEF_SIZE) {
            Line ln = new Line();
            ln.v1 = this.vertexes.get(Bin.i16(data, o));
            ln.v2 = this.vertexes.get(Bin.i16(data, o + 2));
            ln.dx = ln.v2.x - ln.v1.x;
            ln.dy = ln.v2.y - ln.v1.y;
            ln.flags = Bin.i16(data, o + 4);
            ln.special = Bin.i16(data, o + 6);
            ln.tag = Bin.i16(data, o + 8);
            int s0 = Bin.i16(data, o + 10);
            int s1 = Bin.i16(data, o + 12);
            ln.sidenum = new int[] { s0, s1 };
            ln.sides = new Side[] { s0 >= 0 ? this.sides.get(s0) : null, s1 >= 0 ? this.sides.get(s1) : null };
            ln.frontsector = ln.sides[0] != null ? ln.sides[0].sector : null;
            ln.backsector = ln.sides[1] != null ? ln.sides[1].sector : null;
            ln.bbox[Defs.BOXLEFT] = Math.min(ln.v1.x, ln.v2.x);
            ln.bbox[Defs.BOXRIGHT] = Math.max(ln.v1.x, ln.v2.x);
            ln.bbox[Defs.BOXBOTTOM] = Math.min(ln.v1.y, ln.v2.y);
            ln.bbox[Defs.BOXTOP] = Math.max(ln.v1.y, ln.v2.y);
            ln.iLine = i;
            if (ln.frontsector != null) {
                ln.frontsector.lines.add(ln);
            }
            if (ln.backsector != null && ln.backsector != ln.frontsector) {
                ln.backsector.lines.add(ln);
            }
            this.lines.add(ln);
            i++;
        }
    }

    private void loadSegs(byte[] data)
    {
        this.segs = new ArrayList<>();
        for (int o = 0; o + Defs.MAPSEG_SIZE <= data.length; o += Defs.MAPSEG_SIZE) {
            Seg s = new Seg();
            s.v1 = this.vertexes.get(Bin.i16(data, o));
            s.v2 = this.vertexes.get(Bin.i16(data, o + 2));
            s.angle = Compat.asU32(Bin.i16(data, o + 4) << 16);
            s.linedef = this.lines.get(Bin.i16(data, o + 6));
            int side = Bin.i16(data, o + 8);
            s.offset = Bin.i16(data, o + 10) * Defs.FRACUNIT;
            if (side >= 0 && side < s.linedef.sides.length && s.linedef.sides[side] != null) {
                s.sidedef = s.linedef.sides[side];
            } else {
                s.sidedef = s.linedef.sides[0];
            }
            s.frontsector = s.sidedef != null ? s.sidedef.sector : null;
            if ((s.linedef.flags & Defs.ML_TWOSIDED) != 0) {
                int other = side ^ 1;
                if (other >= 0 && other < s.linedef.sides.length && s.linedef.sides[other] != null) {
                    s.backsector = s.linedef.sides[other].sector;
                } else {
                    s.backsector = null;
                }
            } else {
                s.backsector = null;
            }
            this.segs.add(s);
        }
    }

    private void loadSubsectors(byte[] data)
    {
        this.subsectors = new ArrayList<>();
        for (int o = 0; o + Defs.MAPSUBSECTOR_SIZE <= data.length; o += Defs.MAPSUBSECTOR_SIZE) {
            this.subsectors.add(new Subsector(Bin.u16(data, o), Bin.u16(data, o + 2)));
        }
    }

    private void loadNodes(byte[] data)
    {
        this.nodes = new ArrayList<>();
        for (int o = 0; o + Defs.MAPNODE_SIZE <= data.length; o += Defs.MAPNODE_SIZE) {
            Node nd = new Node();
            nd.x = Bin.i16(data, o) * Defs.FRACUNIT;
            nd.y = Bin.i16(data, o + 2) * Defs.FRACUNIT;
            nd.dx = Bin.i16(data, o + 4) * Defs.FRACUNIT;
            nd.dy = Bin.i16(data, o + 6) * Defs.FRACUNIT;
            int p = o + 8;
            nd.bbox = new int[2][4];
            for (int child = 0; child < 2; child++) {
                nd.bbox[child][0] = Bin.i16(data, p) * Defs.FRACUNIT;
                nd.bbox[child][1] = Bin.i16(data, p + 2) * Defs.FRACUNIT;
                nd.bbox[child][2] = Bin.i16(data, p + 4) * Defs.FRACUNIT;
                nd.bbox[child][3] = Bin.i16(data, p + 6) * Defs.FRACUNIT;
                p += 8;
            }
            nd.children = new int[] { Bin.u16(data, p), Bin.u16(data, p + 2) };
            this.nodes.add(nd);
        }
    }

    private void loadThings(byte[] data)
    {
        this.things = new ArrayList<>();
        for (int o = 0; o + Defs.MAPTHING_SIZE <= data.length; o += Defs.MAPTHING_SIZE) {
            this.things.add(new MapThing(
                Bin.i16(data, o),
                Bin.i16(data, o + 2),
                Bin.i16(data, o + 4),
                Bin.i16(data, o + 6),
                Bin.i16(data, o + 8)
            ));
        }
    }

    private void loadBlockmap(byte[] data)
    {
        this.blockmaplump = data;
        if (data.length < 8) {
            return;
        }
        this.bmaporgx = Bin.i16(data, 0) * Defs.FRACUNIT;
        this.bmaporgy = Bin.i16(data, 2) * Defs.FRACUNIT;
        this.bmapwidth = Bin.i16(data, 4);
        this.bmapheight = Bin.i16(data, 6);
    }

    public MapThing playerStart()
    {
        for (MapThing thing : this.things) {
            if (thing.type == 1) {
                return thing;
            }
        }
        if (this.things.isEmpty()) {
            return null;
        }
        return this.things.get(0);
    }
}
