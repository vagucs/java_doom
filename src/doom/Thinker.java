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
 *
 * P_SetMobjState / P_MobjThinker / P_SpawnMobj (p_mobj.prg).
 */
package doom;

public final class Thinker
{
    public static final int ONFLOORZ = 0x80000000;
    public static final int ONCEILINGZ = 0x7fffffff;

    private static int[] sargTics;
    private static int[] shotTypes = { Info.MT_BRUISERSHOT, Info.MT_HEADSHOT, Info.MT_TROOPSHOT };
    private static int[] shotSpeed;

    private Thinker() {}

    public static Mobj spawnMobj(World world, int x, int y, int z, int typ, Game game)
    {
        Subsector sub = Collision.pointInSubsector(world, x, y);
        Mobj mo = new Mobj();
        mo.x = x;
        mo.y = y;
        mo.radius = Info.miInt(typ, Info.MI_RADIUS);
        mo.height = Info.miInt(typ, Info.MI_HEIGHT);
        mo.floorz = sub.sector.floorheight;
        mo.ceilingz = sub.sector.ceilingheight;
        mo.flags = Info.miInt(typ, Info.MI_FLAGS);
        mo.health = Info.miInt(typ, Info.MI_SPAWNHEALTH);
        mo.type = typ;
        mo.doomednum = Info.miInt(typ, Info.MI_DOOMEDNUM);
        mo.damage = Info.miInt(typ, Info.MI_DAMAGE);
        mo.alive = true;
        if (game != null && game.skill != Defs.SK_NIGHTMARE)
            mo.reactiontime = Info.miInt(typ, Info.MI_REACTIONTIME);
        mo.lastlook = Enemy.publicRandom() % 4;
        int flags = mo.flags;
        if (z == ONCEILINGZ || ((flags & Defs.MF_SPAWNCEILING) != 0 && z == ONFLOORZ))
            mo.z = mo.ceilingz - mo.height;
        else if (z == ONFLOORZ)
            mo.z = mo.floorz;
        else
            mo.z = z;
        world.mobjs.add(mo);
        Collision.setThingPosition(world, mo);
        setMobjState(mo, Info.miInt(typ, Info.MI_SPAWNSTATE), world, game);
        return mo;
    }

    public static void removeMobj(World world, Mobj mo)
    {
        Collision.unsetThingPosition(world, mo);
        mo.alive = false;
        mo.istate = Info.S_NULL;
        mo.flags = 0;
        world.mobjs.remove(mo);
    }

    public static boolean setMobjState(Mobj mo, int state, World world, Game game)
    {
        int safety = 0;
        while (true)
        {
            if (state == Info.S_NULL)
            {
                removeMobj(world, mo);
                return false;
            }
            int[] st = Info.STATES[state];
            mo.istate = state;
            mo.tics = st[2];
            mo.sprite = Info.SPRNAMES[st[0]];
            mo.frame = st[1];
            String act = Info.ACTIONS[st[3]];
            if (act != null && !act.isEmpty())
            {
                Enemy.callAction(act, mo, world, game);
                if (!mo.alive)
                    return false;
            }
            state = st[4];
            if (mo.tics != 0)
                return true;
            if (++safety > 100)
                return true;
        }
    }

    public static void mobjThinker(World world, Mobj mo, Game game)
    {
        if (mo.momx != 0 || mo.momy != 0 || (mo.flags & Defs.MF_SKULLFLY) != 0)
        {
            Enemy.pXyMovement(world, mo, game);
            if (!mo.alive)
                return;
        }
        if (mo.z != mo.floorz || mo.momz != 0)
        {
            Enemy.mobjZ(mo, world, game);
            if (!mo.alive)
                return;
        }
        if (mo.tics != -1)
        {
            mo.tics--;
            if (mo.tics <= 0)
                setMobjState(mo, Info.STATES[mo.istate][4], world, game);
            return;
        }
        if ((mo.flags & Defs.MF_COUNTKILL) == 0)
            return;
        if (!game.respawnmonsters)
            return;
        mo.movecount++;
        if (mo.movecount < 12 * Defs.TICRATE)
            return;
        if ((game.leveltime & 31) != 0)
            return;
        if (Enemy.publicRandom() > 4)
            return;
        nightmareRespawn(world, mo, game);
    }

    public static void nightmareRespawn(World world, Mobj mo, Game game)
    {
        if (!(mo.spawnpoint instanceof MapThing))
            return;
        MapThing sp = (MapThing) mo.spawnpoint;
        int x = sp.x * Defs.FRACUNIT;
        int y = sp.y * Defs.FRACUNIT;
        MoveCheck chk = Collision.checkPosition(world, mo, x, y);
        if (chk.blocked)
            return;
        spawnMobj(world, mo.x, mo.y, mo.floorz, Info.MT_TFOG, game);
        game.startSound("telept");
        Subsector sub = Collision.pointInSubsector(world, x, y);
        spawnMobj(world, x, y, sub.sector.floorheight, Info.MT_TFOG, game);
        game.startSound("telept");
        int z = (Info.miInt(mo.type, Info.MI_FLAGS) & Defs.MF_SPAWNCEILING) != 0 ? ONCEILINGZ : ONFLOORZ;
        Mobj spawned = spawnMobj(world, x, y, z, mo.type, game);
        spawned.spawnpoint = sp;
        spawned.angle = Compat.asU32(Compat.intdiv(sp.angle, 45) * 0x20000000);
        if ((sp.options & Defs.MTF_AMBUSH) != 0)
            spawned.flags |= Defs.MF_AMBUSH;
        spawned.reactiontime = 18;
        removeMobj(world, mo);
    }

    public static void applyFast(Game game)
    {
        boolean want = game.fastparm || game.skill == Defs.SK_NIGHTMARE;
        game.respawnmonsters = game.skill == Defs.SK_NIGHTMARE || game.respawnparm;
        if (game.fastOn != null && game.fastOn.booleanValue() == want)
            return;
        if (sargTics == null)
        {
            sargTics = new int[Info.S_SARG_PAIN2 - Info.S_SARG_RUN1 + 1];
            for (int i = 0; i < sargTics.length; i++)
                sargTics[i] = Info.STATES[Info.S_SARG_RUN1 + i][2];
            shotSpeed = new int[shotTypes.length];
            for (int i = 0; i < shotTypes.length; i++)
                shotSpeed[i] = Info.miInt(shotTypes[i], Info.MI_SPEED);
        }
        game.fastOn = want;
        for (int i = 0; i < sargTics.length; i++)
            Info.STATES[Info.S_SARG_RUN1 + i][2] = want ? Math.max(1, sargTics[i] / 2) : sargTics[i];
        int fast = 20 * Defs.FRACUNIT;
        for (int i = 0; i < shotTypes.length; i++)
            Info.MOBJINFO[shotTypes[i]][Info.MI_SPEED] = want ? fast : shotSpeed[i];
    }
}
