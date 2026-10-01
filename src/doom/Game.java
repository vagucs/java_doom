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

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Game
{
    private static final String[] IWADS = {
        "DOOM1.WAD",
        "doom1.wad",
        "DOOM.WAD",
        "doom.wad",
        "DOOM2.WAD",
        "doom2.wad",
        "PLUTONIA.WAD",
        "TNT.WAD",
        "freedoom1.wad",
        "freedoom2.wad",
    };
    private static final String[] WEAPON_PATCH = {
        "PUNGA0", "PISGA0", "SHTGA0", "CHGGA0", "MISGA0", "PLSGA0", "BFGGA0", "SAWGA0", "SHT2A0",
    };
    private static final String[] WEAPON_FIRE_BODY = {
        "PUNGB0", "PISGB0", "SHTGB0", "CHGGB0", "MISGB0", "PLSGB0", "BFGGB0", "SAWGB0", "SHT2B0",
    };
    private static final String[] WEAPON_FIRE_PATCH = {
        null, "PISFA0", "SHTFA0", "CHGFA0", "MISFA0", "PLSFA0", "BFGFA0", null, "SHT2F0",
    };

    public Wad wad;
    public Video video;
    public Sound sound;
    public Resources res;
    public Renderer renderer;
    public World world;
    public Specials specials;
    public Status status;
    public Player player;
    public int gamestate = Defs.GS_TITLE;
    public int episode = 1;
    public int mapn = 1;
    public int skill = Defs.SK_MEDIUM;
    public int leveltime;
    public final Map<Integer, Boolean> keys = new HashMap<>();
    public boolean running = true;
    public boolean showFps;
    public boolean nomonsters;
    public boolean fastparm;
    public boolean respawnparm;
    public boolean respawnmonsters;
    public Boolean fastOn;
    public boolean fullscreen;
    public boolean crt;
    public String iwadPath = "";
    public Menu menu;
    public boolean showMessages = true;
    public int detailLevel;
    public int screenSize = 7;
    public int mouseSensitivity = 5;
    public boolean useMouse = true;
    public int mouseX;
    public int mouseY;
    public boolean mouseFire;
    public boolean nosound;
    public boolean nomusic;
    public boolean demoRecording;
    public boolean demoPlayback;
    public boolean singledemo;
    public boolean timingdemo;
    public int timedemoStart;
    public int gametic;
    public String demoName = "";
    public String recordName;
    public String playdemoName;
    public String timedemoName;
    public final List<String> pwadFiles = new ArrayList<>();
    public int totalkills;
    public int totalitems;
    public int totalsecret;
    public Intermission wi;
    public Finale finale;
    public Wipe wipe;
    public boolean wiping;
    public boolean forceWipe;
    public AmMap automap;
    private int turnheld;
    private byte[] titlePatch;
    private byte[] creditPatch;
    private byte[] pagePatch;
    private int pageTic;
    private int demoSequence = -1;
    private boolean advancedemo;
    private byte[] demoBuffer;
    private int demoP;
    private ByteArrayOutputStream demoRecord;
    private static final int DEMOMARKER = 0x80;
    private int palette = -1;
    private byte[] playpal;
    private Integer wipeState = Defs.GS_TITLE;
    private int nextMapNum = 1;

    public Game()
    {
        wad = new Wad();
        video = new Video();
        sound = new Sound();
        wipe = new Wipe();
        automap = new AmMap();
    }

    public void startSound(String name)
    {
        sound.play(name);
    }

    public void touchSpecial(Mobj special, Mobj toucher)
    {
        Mobj.touchSpecial(this, special, toucher);
    }

    public void useSpecial(Line line, Mobj thing, int side)
    {
        if (specials != null)
        {
            specials.useSpecial(line, thing, side);
        }
    }

    public void crossSpecial(Line line, int side, Mobj thing)
    {
        if (specials != null)
        {
            specials.crossSpecial(line, side, thing);
        }
    }

    public void shootSpecial(Line line, Mobj thing)
    {
        if (specials != null)
        {
            specials.shootSpecial(line, thing);
        }
    }

    public void damageMobj(Mobj target, Mobj source, int damage)
    {
        damageMobj(target, source, damage, null);
    }

    public void damageMobj(Mobj target, Mobj source, int damage, Mobj inflictor)
    {
        if (!target.alive || (target.flags & Defs.MF_SHOOTABLE) == 0)
        {
            return;
        }
        if (target.player != null && skill == Defs.SK_BABY)
        {
            damage >>= 1;
        }
        Mobj origin = inflictor != null ? inflictor : source;
        boolean skipSaw = source != null && source.player != null && source.player.readyweapon == Defs.WP_CHAINSAW;
        if (origin != null && (target.flags & Defs.MF_NOCLIP) == 0 && !skipSaw)
        {
            int angle = Collision.angleTo(origin.x, origin.y, target.x, target.y);
            int mass = Info.miInt(target.type, Info.MI_MASS);
            if (mass == 0) {
                mass = 100;
            }
            int thrust = (int) (((long) damage * Compat.intdiv(Defs.FRACUNIT, 8) * 100L) / mass);
            if (damage < 40
                    && damage > target.health
                    && target.z - origin.z > 64 * Defs.FRACUNIT
                    && (Enemy.publicRandom() & 1) != 0) {
                angle = Compat.asU32(angle + Defs.ANG180);
                thrust *= 4;
            }
            target.momx += Compat.fixedMul(thrust, Tables.fineCos(angle));
            target.momy += Compat.fixedMul(thrust, Tables.fineSin(angle));
        }
        if (target.player != null)
        {
            Player p = target.player;
            if (((p.cheats & Defs.CF_GODMODE) != 0 || p.powers[Defs.PW_INVULNERABILITY] != 0) && damage < 1000)
            {
                return;
            }
            if (p.armortype != 0)
            {
                int saved = Compat.intdiv(damage, p.armortype == 1 ? 3 : 2);
                if (p.armorpoints <= saved)
                {
                    saved = p.armorpoints;
                    p.armortype = 0;
                }
                p.armorpoints -= saved;
                damage -= saved;
            }
            p.health -= damage;
            target.health = p.health;
            p.damagecount = Math.min(100, p.damagecount + damage);
            p.attacker = source;
            if (p.health <= 0)
            {
                p.health = 0;
                p.playerstate = Defs.PST_DEAD;
                target.alive = false;
                target.flags &= ~(Defs.MF_SOLID | Defs.MF_SHOOTABLE);
                startSound("pldeth");
            }
            else
            {
                startSound("plpain");
                painOrWake(target, source);
            }
            return;
        }
        target.health -= damage;
        if (target.health <= 0)
        {
            Enemy.killMonster(target, this, source);
            return;
        }
        painOrWake(target, source);
    }

    private void painOrWake(Mobj target, Mobj source)
    {
        int painChance = Info.miInt(target.type, Info.MI_PAINCHANCE);
        if (Enemy.publicRandom() < painChance && (target.flags & Defs.MF_SKULLFLY) == 0)
        {
            target.flags |= Defs.MF_JUSTHIT;
            int painState = Info.miInt(target.type, Info.MI_PAINSTATE);
            if (painState != Info.S_NULL)
                Thinker.setMobjState(target, painState, world, this);
        }
        target.reactiontime = 0;
        if (source != null && source != target && target.player == null)
        {
            target.target = source;
            int spawnState = Info.miInt(target.type, Info.MI_SPAWNSTATE);
            int seeState = Info.miInt(target.type, Info.MI_SEESTATE);
            if (target.istate == spawnState && seeState != Info.S_NULL)
                Thinker.setMobjState(target, seeState, world, this);
        }
    }

    public void loadLevel()
    {
        loadLevel(false);
    }

    public void loadLevel(boolean carry)
    {
        if (res == null)
        {
            throw new RuntimeException("resources not initialized");
        }
        Player previous = carry ? player : null;
        Enemy.clearRandom();
        world = new World();
        world.setupLevel(wad, res, episode, mapn);
        specials = new Specials(world, res, sound);
        player = null;
        int[] counts = Mobj.spawnMapThings(world, skill, this);
        if (player == null)
        {
            throw new RuntimeException("no player 1 start");
        }
        if (previous != null)
        {
            carryPlayer(previous);
        }
        player.killcount = 0;
        player.itemcount = 0;
        player.secretcount = 0;
        totalkills = counts[0];
        totalitems = counts[1];
        totalsecret = 0;
        for (Sector s : world.sectors)
        {
            if (s.special == 9)
            {
                ++totalsecret;
            }
        }
        Thinker.applyFast(this);
        leveltime = 0;
        gamestate = Defs.GS_LEVEL;
        specials.exitRequested = false;
        specials.secretExit = false;
        wi = null;
        finale = null;
        sound.playLevelMusic(episode, mapn);
        automap.resetLevel();
        if (status != null) {
            status.reset(player);
        }
        System.out.println("Entering E" + episode + "M" + mapn);
    }

    private void carryPlayer(Player previous)
    {
        Player p = player;
        p.health = previous.health;
        p.armorpoints = previous.armorpoints;
        p.armortype = previous.armortype;
        p.ammo = previous.ammo.clone();
        p.maxammo = previous.maxammo.clone();
        p.weaponowned = previous.weaponowned.clone();
        p.readyweapon = previous.readyweapon;
        p.cheats = previous.cheats;
        p.didsecret = previous.didsecret;
        p.mo.health = p.health;
        p.pendingweapon = Defs.WP_NOCHANGE;
        p.pspriteState = "up";
        p.pspriteSy = 128 * Defs.FRACUNIT;
        p.pspriteBody = "";
        p.cards = new boolean[] { false, false, false, false, false, false };
        p.damagecount = 0;
        p.bonuscount = 0;
        p.extralight = 0;
        p.playerstate = Defs.PST_LIVE;
    }

    private boolean commercial()
    {
        return wad.checkNumForName("MAP01") >= 0;
    }

    public void completeLevel()
    {
        automap.stop();
        Player p = player;
        if (p != null)
        {
            p.cards = new boolean[] { false, false, false, false, false, false };
            p.damagecount = 0;
            p.bonuscount = 0;
            p.extralight = 0;
        }
        boolean secret = specials != null && specials.secretExit;
        boolean commercial = commercial();
        if (!commercial && mapn == 8)
        {
            finale = new Finale(this);
            gamestate = Defs.GS_FINALE;
            return;
        }
        if (!commercial && mapn == 9 && p != null)
        {
            p.didsecret = true;
        }
        int next;
        if (commercial)
        {
            if (secret && mapn == 15)
            {
                next = 30;
            }
            else if (secret && mapn == 31)
            {
                next = 31;
            }
            else if (mapn == 31 || mapn == 32)
            {
                next = 15;
            }
            else
            {
                next = mapn;
            }
        }
        else if (secret)
        {
            next = 8;
        }
        else if (mapn == 9)
        {
            if (episode == 1)
            {
                next = 3;
            }
            else if (episode == 2)
            {
                next = 5;
            }
            else if (episode == 3)
            {
                next = 6;
            }
            else if (episode == 4)
            {
                next = 2;
            }
            else
            {
                next = 0;
            }
        }
        else
        {
            next = mapn;
        }
        nextMapNum = next + 1;
        WbStart wbs = new WbStart(
            episode - 1,
            mapn - 1,
            next,
            Math.max(1, totalkills),
            Math.max(1, totalitems),
            Math.max(1, totalsecret),
            Intermission.parTime(episode, mapn, commercial),
            p != null ? p.killcount : 0,
            p != null ? p.itemcount : 0,
            p != null ? p.secretcount : 0,
            leveltime,
            p != null && p.didsecret,
            commercial
        );
        wi = new Intermission(this, wbs);
        gamestate = Defs.GS_INTERMISSION;
    }

    public void worldDone()
    {
        worldDone(false);
    }

    public void worldDone(boolean fromFinale)
    {
        boolean secret = specials != null && specials.secretExit;
        if (secret && player != null)
        {
            player.didsecret = true;
        }
        if (!fromFinale && commercial() && Finale.commercialFinaleMap(mapn, secret))
        {
            finale = new Finale(this);
            gamestate = Defs.GS_FINALE;
            return;
        }
        mapn = nextMapNum;
        String lump = commercial() ? String.format("MAP%02d", mapn) : ("E" + episode + "M" + mapn);
        if (wad.checkNumForName(lump) < 0)
        {
            returnToTitle();
            return;
        }
        loadLevel(true);
    }

    public void nextMap()
    {
        completeLevel();
    }

    public void startNewGame(int skill, int episode, int map)
    {
        demoPlayback = false;
        advancedemo = false;
        demoBuffer = null;
        this.skill = skill;
        this.episode = episode;
        this.mapn = map;
        fastOn = null;
        Thinker.applyFast(this);
        loadLevel();
        if (demoRecording)
        {
            beginRecording();
        }
    }

    public boolean saveGame(int slot, String description)
    {
        if (gamestate != Defs.GS_LEVEL || player == null || world == null)
        {
            return false;
        }
        boolean ok = Saveg.writeSave(this, slot, description);
        if (ok)
        {
            player.setMessage("game saved.");
        }
        return ok;
    }

    public boolean loadGame(int slot)
    {
        demoPlayback = false;
        advancedemo = false;
        demoBuffer = null;
        if (!Saveg.readAndRestore(this, slot))
        {
            return false;
        }
        palette = -1;
        keys.clear();
        automap.resetLevel();
        if (status != null)
        {
            status.reset(player);
        }
        if (player != null)
        {
            player.setMessage("game loaded.");
        }
        System.out.println("Loaded E" + episode + "M" + mapn);
        return true;
    }

    public void returnToTitle()
    {
        player = null;
        world = null;
        wi = null;
        finale = null;
        automap.resetLevel();
        if (menu != null)
        {
            menu.clear();
        }
        startTitle();
    }

    public void startTitle()
    {
        demoPlayback = false;
        demoBuffer = null;
        demoP = 0;
        demoSequence = -1;
        advancedemo = true;
        doAdvanceDemo();
    }

    private void pageTicker()
    {
        pageTic--;
        if (pageTic < 0)
        {
            advancedemo = true;
        }
    }

    private void doAdvanceDemo()
    {
        advancedemo = false;
        demoPlayback = false;
        demoSequence = (demoSequence + 1) % 6;
        switch (demoSequence)
        {
            case 0:
                pageTic = commercial() ? Defs.TICRATE * 11 : 170;
                gamestate = Defs.GS_TITLE;
                pagePatch = titlePatch;
                sound.playTitleMusic();
                break;
            case 1:
                if (!playDemo("demo1"))
                {
                    advancedemo = true;
                    doAdvanceDemo();
                }
                break;
            case 2:
                pageTic = 200;
                gamestate = Defs.GS_TITLE;
                pagePatch = creditPatch != null ? creditPatch : titlePatch;
                break;
            case 3:
                if (!playDemo("demo2"))
                {
                    advancedemo = true;
                    doAdvanceDemo();
                }
                break;
            case 4:
                pageTic = commercial() ? Defs.TICRATE * 11 : 200;
                gamestate = Defs.GS_TITLE;
                pagePatch = titlePatch;
                if (commercial())
                {
                    sound.playTitleMusic();
                }
                break;
            default:
                if (!playDemo("demo3"))
                {
                    advancedemo = true;
                    doAdvanceDemo();
                }
                break;
        }
    }

    public boolean playDemo(String name)
    {
        byte[] data = loadDemoBytes(name);
        if (data == null || data.length < 13)
        {
            return false;
        }
        demoBuffer = data;
        demoP = 0;
        int demoVersion = Bin.u8(data, demoP++);
        if (demoVersion <= 4)
        {
            demoP = 0;
        }
        int demoSkill = Bin.u8(data, demoP++);
        int demoEpisode = Bin.u8(data, demoP++);
        int demoMap = Bin.u8(data, demoP++);
        demoP += 5;
        demoP += 4;
        if (demoSkill >= 0 && demoSkill <= 4)
        {
            skill = demoSkill;
        }
        if (demoEpisode >= 1)
        {
            episode = demoEpisode;
        }
        if (demoMap >= 1)
        {
            mapn = demoMap;
        }
        loadLevel(false);
        demoPlayback = true;
        if (timingdemo)
        {
            timedemoStart = video.ticksMs();
            gametic = 0;
        }
        return true;
    }

    private byte[] loadDemoBytes(String name)
    {
        String[] paths = { name, name + ".lmp" };
        for (String path : paths)
        {
            if (isFile(path))
            {
                try
                {
                    return Files.readAllBytes(Path.of(path));
                }
                catch (Exception e)
                {
                    return null;
                }
            }
        }
        if (wad.checkNumForName(name) < 0)
        {
            return null;
        }
        return wad.cacheLumpName(name);
    }

    public void beginRecording()
    {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        buf.write(109);
        buf.write(skill & 0xff);
        buf.write(episode & 0xff);
        buf.write(mapn & 0xff);
        buf.write(0);
        buf.write(respawnparm ? 1 : 0);
        buf.write(fastparm ? 1 : 0);
        buf.write(nomonsters ? 1 : 0);
        buf.write(0);
        buf.write(1);
        buf.write(0);
        buf.write(0);
        buf.write(0);
        demoRecord = buf;
        demoP = buf.size();
        demoRecording = true;
    }

    public void writeDemoTiccmd(Ticcmd cmd)
    {
        if (demoRecord == null)
        {
            return;
        }
        demoRecord.write(cmd.forwardmove & 0xff);
        demoRecord.write(cmd.sidemove & 0xff);
        demoRecord.write((cmd.angleturn >> 8) & 0xff);
        demoRecord.write(cmd.buttons & 0xff);
        demoP = demoRecord.size();
    }

    public void finishRecording()
    {
        if (!demoRecording || demoRecord == null)
        {
            return;
        }
        demoRecord.write(DEMOMARKER);
        String name = demoName == null || demoName.isEmpty() ? "demo.lmp" : demoName;
        if (!name.toLowerCase().endsWith(".lmp"))
        {
            name = name + ".lmp";
        }
        try
        {
            Files.write(Path.of(name), demoRecord.toByteArray());
            System.out.println("Demo " + name + " recorded");
        }
        catch (Exception e)
        {
            System.out.println("Demo write failed: " + e.getMessage());
        }
        demoRecording = false;
    }

    public void checkDemoStatus()
    {
        if (timingdemo)
        {
            int now = video.ticksMs();
            int real = Math.max(1, Compat.intdiv((now - timedemoStart) * Defs.TICRATE, 1000));
            double fps = (gametic * (double) Defs.TICRATE) / real;
            System.out.printf("timed %d gametics in %d realtics (%.1f fps)%n", gametic, real, fps);
            timingdemo = false;
            demoPlayback = false;
            running = false;
            return;
        }
        if (demoPlayback)
        {
            demoPlayback = false;
            if (singledemo)
            {
                running = false;
            }
            else
            {
                advancedemo = true;
            }
            return;
        }
        if (demoRecording)
        {
            finishRecording();
            running = false;
        }
    }

    private Ticcmd readDemoTiccmd()
    {
        Ticcmd cmd = new Ticcmd();
        if (demoBuffer == null || demoP + 4 > demoBuffer.length || (demoBuffer[demoP] & 0xff) == DEMOMARKER)
        {
            checkDemoStatus();
            return cmd;
        }
        cmd.forwardmove = demoBuffer[demoP++];
        cmd.sidemove = demoBuffer[demoP++];
        cmd.angleturn = (demoBuffer[demoP++] & 0xff) << 8;
        if (cmd.angleturn >= 32768)
        {
            cmd.angleturn -= 65536;
        }
        cmd.buttons = demoBuffer[demoP++] & 0xff;
        return cmd;
    }

    private void beginPlay()
    {
        demoPlayback = false;
        advancedemo = false;
        demoBuffer = null;
        loadLevel(false);
    }

    private boolean held(int key)
    {
        return Boolean.TRUE.equals(keys.get(key));
    }

    public Ticcmd buildTiccmd()
    {
        Ticcmd cmd = new Ticcmd();
        int speed = held(Keys.LSHIFT) || held(Keys.RSHIFT) ? 1 : 0;
        boolean strafe = held(Keys.LALT) || held(Keys.RALT);
        boolean turning = held(Keys.RIGHT) || held(Keys.LEFT);
        turnheld = turning ? turnheld + 1 : 0;
        int turnSpeed = turnheld < 6 ? 2 : speed;
        if (strafe)
        {
            if (held(Keys.RIGHT))
            {
                cmd.sidemove += Player.SIDEMOVE[speed];
            }
            if (held(Keys.LEFT))
            {
                cmd.sidemove -= Player.SIDEMOVE[speed];
            }
        }
        else
        {
            if (held(Keys.RIGHT))
            {
                cmd.angleturn -= Player.ANGLETURN[turnSpeed];
            }
            if (held(Keys.LEFT))
            {
                cmd.angleturn += Player.ANGLETURN[turnSpeed];
            }
        }
        if (held(Keys.UP))
        {
            cmd.forwardmove += Player.FORWARDMOVE[speed];
        }
        if (held(Keys.DOWN))
        {
            cmd.forwardmove -= Player.FORWARDMOVE[speed];
        }
        if (held(Keys.COMMA))
        {
            cmd.sidemove -= Player.SIDEMOVE[speed];
        }
        if (held(Keys.PERIOD))
        {
            cmd.sidemove += Player.SIDEMOVE[speed];
        }
        if (held(Keys.LCTRL) || held(Keys.RCTRL))
        {
            cmd.buttons |= Defs.BT_ATTACK;
        }
        if (held(Keys.SPACE) || held('e'))
        {
            cmd.buttons |= Defs.BT_USE;
        }
        if (held('1'))
        {
            int weapon;
            if (player != null && player.readyweapon == Defs.WP_CHAINSAW)
            {
                weapon = Defs.WP_FIST;
            }
            else if (player != null && player.weaponowned[Defs.WP_CHAINSAW])
            {
                weapon = Defs.WP_CHAINSAW;
            }
            else
            {
                weapon = Defs.WP_FIST;
            }
            cmd.buttons |= Defs.BT_CHANGE | (weapon << Defs.BT_WEAPONSHIFT);
        }
        else
        {
            int[] weapons = { 0, 0, Defs.WP_PISTOL, Defs.WP_SHOTGUN, Defs.WP_CHAINGUN, Defs.WP_MISSILE, Defs.WP_PLASMA, Defs.WP_BFG };
            for (int number = 2; number <= 7; ++number)
            {
                if (held('0' + number))
                {
                    cmd.buttons |= Defs.BT_CHANGE | (weapons[number] << Defs.BT_WEAPONSHIFT);
                    break;
                }
            }
        }
        if (useMouse)
        {
            double sens = (mouseSensitivity + 5) / 10.0;
            int mx = (int) (mouseX * sens);
            int my = (int) (mouseY * sens);
            cmd.forwardmove += my;
            if (cmd.forwardmove > 127)
            {
                cmd.forwardmove = 127;
            }
            if (cmd.forwardmove < -127)
            {
                cmd.forwardmove = -127;
            }
            if (strafe)
            {
                cmd.sidemove += mx * 2;
                if (cmd.sidemove > 127)
                {
                    cmd.sidemove = 127;
                }
                if (cmd.sidemove < -127)
                {
                    cmd.sidemove = -127;
                }
            }
            else
            {
                cmd.angleturn -= mx * 8;
            }
            if (mouseFire)
            {
                cmd.buttons |= Defs.BT_ATTACK;
            }
            mouseX = 0;
            mouseY = 0;
        }
        return cmd;
    }

    public void runTic()
    {
        ++gametic;
        syncMouseGrab();
        if (wiping)
        {
            if (wipe.tick(1, video.fb))
            {
                wiping = false;
            }
            return;
        }
        if (menu != null)
        {
            menu.ticker();
        }
        sound.update();
        if (advancedemo)
        {
            doAdvanceDemo();
        }
        if (gamestate == Defs.GS_TITLE)
        {
            pageTicker();
            return;
        }
        if (gamestate == Defs.GS_INTERMISSION)
        {
            if (wi != null)
            {
                wi.ticker();
                if (wi.done)
                {
                    worldDone();
                }
            }
            return;
        }
        if (gamestate == Defs.GS_FINALE)
        {
            if (finale != null)
            {
                finale.ticker();
                if (finale.done)
                {
                    if ("worlddone".equals(finale.action))
                    {
                        worldDone(true);
                    }
                    else
                    {
                        returnToTitle();
                    }
                }
            }
            return;
        }
        if (gamestate != Defs.GS_LEVEL || player == null)
        {
            return;
        }
        if (demoPlayback)
        {
            player.cmd = readDemoTiccmd();
        }
        else if (menu != null && menu.active)
        {
            player.cmd = new Ticcmd();
        }
        else
        {
            player.cmd = buildTiccmd();
            if (demoRecording)
            {
                writeDemoTiccmd(player.cmd);
            }
        }
        Player.playerThink(world, player, this, leveltime);
        if (player.playerstate == Defs.PST_REBORN)
        {
            loadLevel(false);
            return;
        }
        Enemy.tickEnemies(world, this);
        if (specials != null)
        {
            specials.tick();
        }
        if (specials != null && specials.exitRequested)
        {
            startSound("swtchx");
            completeLevel();
            return;
        }
        ++leveltime;
        automap.ticker(this);
        if (status != null) {
            status.ticker(player);
        }
    }

    public void draw()
    {
        int[] fb = video.fb;
        if (wiping)
        {
            if (menu != null)
            {
                menu.draw(fb);
            }
            applyPalette();
            video.present();
            return;
        }
        boolean need = forceWipe || wipeState == null || gamestate != wipeState;
        forceWipe = false;
        if (need)
        {
            wipe.captureStart(fb);
        }
        drawFrame(fb);
        if (need)
        {
            wipe.captureEnd(fb);
            wipe.begin(fb);
            wipeState = gamestate;
            wiping = true;
        }
        if (menu != null)
        {
            menu.draw(fb);
        }
        if (video.showFps && status != null)
        {
            status.drawText(fb, 250, 1, video.fpsValue + " FPS");
        }
        applyPalette();
        video.present();
    }

    private void drawFrame(int[] fb)
    {
        if (gamestate == Defs.GS_TITLE && pagePatch != null)
        {
            VVideo.fill(fb, 0);
            VVideo.drawPatch(fb, 0, 0, pagePatch);
            return;
        }
        if (gamestate == Defs.GS_INTERMISSION && wi != null)
        {
            wi.draw(fb);
            return;
        }
        if (gamestate == Defs.GS_FINALE && finale != null)
        {
            finale.draw(fb);
            return;
        }
        if (gamestate != Defs.GS_LEVEL || player == null)
        {
            VVideo.fill(fb, 0);
            return;
        }
        if (automap.active)
        {
            automap.draw(fb, this);
        }
        else
        {
            Mobj mo = player.mo;
            VVideo.fill(fb, 0);
            renderer.setupFrame(mo.x, mo.y, player.viewz, mo.angle, player.extralight, player.fixedcolormap);
            renderer.render(world, fb);
            Sprites.drawSprites(renderer, world, fb);
            renderer.drawMasked();
            if (player.playerstate != Defs.PST_DEAD
                || player.pspriteSy < Sprites.WEAPONBOTTOM) {
                drawWeapon(fb);
            }
        }
        if (status != null && (automap.active || renderer.screenblocks < 11))
        {
            status.draw(fb, player, showMessages);
        }
    }

    private void applyPalette()
    {
        int next = 0;
        Player p = player;
        if (p != null && gamestate == Defs.GS_LEVEL)
        {
            int cnt = p.damagecount;
            if (p.powers[Defs.PW_STRENGTH] != 0)
            {
                int bzc = 12 - Compat.intdiv(p.powers[Defs.PW_STRENGTH], 64);
                if (bzc > cnt)
                {
                    cnt = bzc;
                }
            }
            if (cnt != 0)
            {
                next = Math.min(7, (cnt + 7) >> 3) + 1;
            }
            else if (p.bonuscount != 0)
            {
                next = Math.min(3, (p.bonuscount + 7) >> 3) + 9;
            }
            else if (p.powers[Defs.PW_IRONFEET] > 4 * 32 || (p.powers[Defs.PW_IRONFEET] & 8) != 0)
            {
                next = Defs.RADIATIONPAL;
            }
        }
        if (next == palette)
        {
            return;
        }
        palette = next;
        if (playpal != null)
        {
            int start = palette * 768;
            if (start + 768 <= playpal.length)
            {
                byte[] raw = Arrays.copyOfRange(playpal, start, start + 768);
                video.setPaletteRaw(raw);
            }
        }
    }

    private void drawWeapon(int[] fb)
    {
        Player p = player;
        int[] xy = Sprites.weaponPspriteXY(p, leveltime);
        int x = xy[0];
        int y = xy[1];
        String body = p.pspriteBody;
        if (body == null || body.isEmpty())
        {
            String fireBody = null;
            if (("atk".equals(p.pspriteState) || "fire".equals(p.pspriteState))
                && p.readyweapon >= 0 && p.readyweapon < WEAPON_FIRE_BODY.length)
            {
                fireBody = WEAPON_FIRE_BODY[p.readyweapon];
            }
            if (fireBody != null)
            {
                body = fireBody;
            }
            else if (p.readyweapon >= 0 && p.readyweapon < WEAPON_PATCH.length && WEAPON_PATCH[p.readyweapon] != null)
            {
                body = WEAPON_PATCH[p.readyweapon];
            }
            else
            {
                body = "PISGA0";
            }
        }
        int n = wad.checkNumForName(body);
        if (n < 0)
        {
            n = wad.checkNumForName("PISGA0");
        }
        if (n >= 0)
        {
            Sprites.drawPsprite(renderer, fb, wad.cacheLumpNum(n), x, y);
        }
        String flash = p.flashTics > 0 ? p.pspriteFlash : "";
        if ((flash == null || flash.isEmpty()) && "fire".equals(p.pspriteState))
        {
            flash = p.readyweapon >= 0 && p.readyweapon < WEAPON_FIRE_PATCH.length ? WEAPON_FIRE_PATCH[p.readyweapon] : "";
            if (flash == null)
            {
                flash = "";
            }
        }
        if (flash != null && !flash.isEmpty())
        {
            int fn = wad.checkNumForName(flash);
            if (fn >= 0)
            {
                Sprites.drawPsprite(renderer, fb, wad.cacheLumpNum(fn), x, y);
            }
        }
    }

    public void applyViewSize()
    {
        if (renderer != null)
        {
            renderer.setViewSize(screenSize + 3, detailLevel);
        }
    }

    public void sizeDisplay(int choice)
    {
        int next = Math.max(0, Math.min(8, screenSize + (choice != 0 ? 1 : -1)));
        if (next == screenSize)
        {
            return;
        }
        screenSize = next;
        applyViewSize();
        startSound("stnmov");
    }

    private void syncMouseGrab()
    {
        boolean want = useMouse && gamestate == Defs.GS_LEVEL && !demoPlayback && !(menu != null && menu.active);
        video.setRelativeMouse(want);
    }

    public void handleEvent(GameEvent ev)
    {
        if (ev == null)
        {
            return;
        }
        if ("mousemotion".equals(ev.type))
        {
            if (useMouse)
            {
                mouseX += ev.dx;
                mouseY += -ev.dy;
            }
            return;
        }
        if ("mousedown".equals(ev.type))
        {
            if (ev.button == 1)
            {
                mouseFire = true;
                if (finale != null && gamestate == Defs.GS_FINALE)
                {
                    finale.responder();
                }
            }
            return;
        }
        if ("mouseup".equals(ev.type))
        {
            if (ev.button == 1)
            {
                mouseFire = false;
            }
            return;
        }
        handleEvent(ev.type, ev.key, ev.text != null ? ev.text : "");
    }

    public void handleEvent(String type, int key, String unicodeChar)
    {
        if (unicodeChar == null)
        {
            unicodeChar = "";
        }
        if ("quit".equals(type))
        {
            if (demoRecording)
            {
                finishRecording();
            }
            running = false;
            return;
        }
        if ("keydown".equals(type))
        {
            if (key == Keys.LALT || key == Keys.RALT || key == Keys.LSHIFT || key == Keys.RSHIFT || key == Keys.LCTRL || key == Keys.RCTRL)
            {
                keys.put(key, true);
            }
            if ((key == Keys.RETURN || key == Keys.KP_ENTER) && (held(Keys.LALT) || held(Keys.RALT)))
            {
                video.toggleFullscreen();
                fullscreen = video.fullscreen;
                return;
            }
            keys.put(key, true);
            if (finale != null && gamestate == Defs.GS_FINALE && finale.responder())
            {
                return;
            }
            if (menu != null && menu.responder(key, unicodeChar))
            {
                return;
            }
            if (automap.responder(type, key, this))
            {
                return;
            }
            if (Keys.isPlus(key) || Keys.isMinus(key) || "+".equals(unicodeChar) || "=".equals(unicodeChar) || "-".equals(unicodeChar) || "_".equals(unicodeChar))
            {
                if (!automap.active)
                {
                    boolean plus = Keys.isPlus(key) || "+".equals(unicodeChar) || "=".equals(unicodeChar);
                    sizeDisplay(plus ? 1 : 0);
                }
                return;
            }
            if (key == Keys.F11)
            {
                video.showFps = !video.showFps;
            }
            else if (demoPlayback || gamestate == Defs.GS_TITLE)
            {
                beginPlay();
            }
            else
            {
                feedCheat(unicodeChar);
            }
        }
        else if ("keyup".equals(type))
        {
            keys.remove(key);
            automap.responder(type, key, this);
        }
    }

    private void feedCheat(String input)
    {
        if (gamestate != Defs.GS_LEVEL || player == null)
        {
            return;
        }
        String ch = input.toLowerCase();
        if (ch.length() != 1)
        {
            return;
        }
        char letter = ch.charAt(0);
        if (!((letter >= 'a' && letter <= 'z') || (letter >= '0' && letter <= '9')))
        {
            return;
        }
        boolean nightmare = skill == Defs.SK_NIGHTMARE;
        for (Deh.CheatSeq cheat : Deh.INSTANCE.cheats)
        {
            String param = cheat.feed(ch);
            if (param == null)
            {
                continue;
            }
            if (nightmare && !"clev".equals(cheat.action) && !"iddt".equals(cheat.action))
            {
                continue;
            }
            doCheat(cheat.action, param);
        }
    }

    private void doCheat(String action, String param)
    {
        switch (action)
        {
            case "god":
                cheatGod();
                break;
            case "kfa":
                cheatAmmo(true);
                break;
            case "fa":
                cheatAmmo(false);
                break;
            case "noclip":
            case "noclip2":
                cheatNoclip();
                break;
            case "iddt":
                if (automap.active)
                {
                    automap.cycleIddt();
                }
                break;
            case "behold":
                player.setMessage("invin visis rad allmap lite amp");
                break;
            case "beholdv":
            case "beholds":
            case "beholdi":
            case "beholdr":
            case "beholda":
            case "beholdl":
                cheatBehold("vsiral".indexOf(action.charAt(6)));
                break;
            case "choppers":
                player.weaponowned[Defs.WP_CHAINSAW] = true;
                player.pendingweapon = Defs.WP_CHAINSAW;
                player.powers[Defs.PW_INVULNERABILITY] = 1;
                player.setMessage("... doesn't suck - GM");
                break;
            case "mypos":
                player.setMessage(String.format("ang=0x%x;x,y=(0x%x,0x%x)", player.mo.angle, player.mo.x, player.mo.y));
                break;
            case "clev":
                cheatClev(param);
                break;
            case "mus":
                cheatMus(param);
                break;
            default:
                break;
        }
    }

    private void cheatGod()
    {
        Player p = player;
        p.cheats ^= Defs.CF_GODMODE;
        if ((p.cheats & Defs.CF_GODMODE) != 0)
        {
            p.health = Deh.INSTANCE.godModeHealth;
            p.mo.health = Deh.INSTANCE.godModeHealth;
            p.setMessage("Degreelessness Mode On");
        }
        else
        {
            p.setMessage("Degreelessness Mode Off");
        }
    }

    private void cheatAmmo(boolean giveKeys)
    {
        Player p = player;
        p.armorpoints = giveKeys ? Deh.INSTANCE.idkfaArmor : Deh.INSTANCE.idfaArmor;
        p.armortype = giveKeys ? Deh.INSTANCE.idkfaArmorClass : Deh.INSTANCE.idfaArmorClass;
        Arrays.fill(p.weaponowned, true);
        p.maxammo = Deh.INSTANCE.maxammo.clone();
        for (int i = 0; i < p.ammo.length; ++i)
        {
            p.ammo[i] = p.maxammo[i];
        }
        if (giveKeys)
        {
            p.cards = new boolean[] { true, true, true, true, true, true };
        }
        p.setMessage(giveKeys ? "Very Happy Ammo Added" : "Ammo Added");
    }

    private void cheatNoclip()
    {
        Player p = player;
        p.cheats ^= Defs.CF_NOCLIP;
        if ((p.cheats & Defs.CF_NOCLIP) != 0)
        {
            p.mo.flags |= Defs.MF_NOCLIP;
        }
        else
        {
            p.mo.flags &= ~Defs.MF_NOCLIP;
        }
        p.setMessage((p.cheats & Defs.CF_NOCLIP) != 0 ? "No Clipping Mode ON" : "No Clipping Mode OFF");
    }

    private void cheatBehold(int pw)
    {
        if (pw < 0)
        {
            return;
        }
        Player p = player;
        if (p.powers[pw] == 0)
        {
            Player.givePower(p, pw);
            if (pw == Defs.PW_STRENGTH && p.readyweapon != Defs.WP_FIST)
            {
                p.pendingweapon = Defs.WP_FIST;
            }
        }
        else if (pw == Defs.PW_STRENGTH)
        {
            p.powers[pw] = 0;
        }
        else
        {
            p.powers[pw] = 1;
        }
        p.setMessage("Power-up Toggled");
    }

    private void cheatClev(String param)
    {
        if (param.length() < 2 || !Character.isDigit(param.charAt(0)) || !Character.isDigit(param.charAt(1)))
        {
            return;
        }
        int a = param.charAt(0) - '0';
        int b = param.charAt(1) - '0';
        int episode;
        int map;
        String lump;
        if (commercial())
        {
            episode = 1;
            map = a * 10 + b;
            lump = String.format("MAP%02d", map);
        }
        else
        {
            episode = a;
            map = b;
            lump = "E" + episode + "M" + map;
        }
        if (episode < 1 || map < 1 || wad.checkNumForName(lump) < 0)
        {
            return;
        }
        player.setMessage("Changing Level...");
        startNewGame(skill, episode, map);
    }

    private void cheatMus(String param)
    {
        if (param.length() < 2 || !Character.isDigit(param.charAt(0)) || !Character.isDigit(param.charAt(1)))
        {
            return;
        }
        int a = param.charAt(0) - '0';
        int b = param.charAt(1) - '0';
        String name;
        if (commercial())
        {
            int map = a * 10 + b;
            String[] tracks = Sound.doom2Music();
            if (map < 1 || map > tracks.length)
            {
                player.setMessage("IMPOSSIBLE SELECTION");
                return;
            }
            name = tracks[map - 1];
        }
        else
        {
            if (a < 1 || b < 1 || b > 9)
            {
                player.setMessage("IMPOSSIBLE SELECTION");
                return;
            }
            name = "e" + a + "m" + b;
        }
        if (!sound.hasMusic(name))
        {
            player.setMessage("IMPOSSIBLE SELECTION");
            return;
        }
        sound.changeMusic(name, true);
        player.setMessage("Music Change");
    }

    public static int main(String[] args)
    {
        try
        {
            Game game = new Game();
            Config.load(game);
            String iwad = parseArgs(args, game);
            String iwadPath = findIwad(iwad);
            game.iwadPath = iwadPath;
            System.out.println("IWAD: " + iwadPath);
            game.wad.addFile(iwadPath);
            for (String extra : game.pwadFiles)
            {
                System.out.println("PWAD: " + extra);
                game.wad.addFile(extra);
            }
            Deh.INSTANCE.loadAfterIwad(game.wad, iwadPath);
            Tables.initTables();
            game.res = new Resources(game.wad);
            game.res.init();
            game.renderer = new Renderer(game.res);
            game.applyViewSize();
            game.video.init(game.fullscreen, "DOOM (Java)");
            game.video.showFps = game.showFps;
            game.video.crt = game.crt;
            game.playpal = game.wad.cacheLumpName("PLAYPAL");
            game.video.setPalette(game.playpal);
            if (game.nosound)
            {
                game.sound.enabled = false;
                game.sound.musicEnabled = false;
            }
            if (game.nomusic)
            {
                game.sound.musicEnabled = false;
            }
            game.sound.init(game.wad);
            game.sound.output = game.video;
            game.menu = new Menu(game.wad, game.sound, game);
            if (game.wad.checkNumForName("TITLEPIC") >= 0)
            {
                game.titlePatch = game.wad.cacheLumpName("TITLEPIC");
            }
            if (game.wad.checkNumForName("CREDIT") >= 0)
            {
                game.creditPatch = game.wad.cacheLumpName("CREDIT");
            }
            game.pagePatch = game.titlePatch;
            game.status = new Status(game.wad);
            boolean warp = false;
            for (String arg : args)
            {
                if ("-warp".equals(arg))
                {
                    warp = true;
                    break;
                }
            }
            if (game.recordName != null)
            {
                game.demoName = game.recordName;
                game.demoRecording = true;
                game.startNewGame(game.skill, game.episode, game.mapn);
            }
            else if (game.timedemoName != null)
            {
                game.timingdemo = true;
                game.singledemo = true;
                if (!game.playDemo(game.timedemoName))
                {
                    System.out.println("timedemo not found: " + game.timedemoName);
                    game.video.shutdown();
                    return 1;
                }
            }
            else if (game.playdemoName != null)
            {
                game.singledemo = true;
                if (!game.playDemo(game.playdemoName))
                {
                    System.out.println("playdemo not found: " + game.playdemoName);
                    game.video.shutdown();
                    return 1;
                }
            }
            else if (warp)
            {
                game.loadLevel();
            }
            else
            {
                game.startTitle();
            }
            double tickMs = 1000.0 / Defs.TICRATE;
            double accum = 0;
            int last = game.video.ticksMs();
            while (game.running)
            {
                for (GameEvent event : game.video.pollEvents())
                {
                    game.handleEvent(event);
                }
                int now = game.video.ticksMs();
                accum += now - last;
                last = now;
                if (game.timingdemo)
                {
                    game.runTic();
                }
                else
                {
                    while (accum >= tickMs)
                    {
                        game.runTic();
                        accum -= tickMs;
                    }
                }
                game.draw();
                if (!game.timingdemo && accum < tickMs / 2)
                {
                    usleep(1);
                }
            }
            if (game.demoRecording)
            {
                game.finishRecording();
            }
            Config.save(game);
            game.sound.stopMusic();
            game.video.shutdown();
            return 0;
        }
        catch (Throwable exception)
        {
            String message = exception.getMessage();
            System.err.println(message != null ? message : String.valueOf(exception));
            exception.printStackTrace(System.err);
            return 1;
        }
    }

    private static void usleep(int ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    private static String parseArgs(String[] argv, Game game)
    {
        String iwad = null;
        for (int i = 0; i < argv.length; ++i)
        {
            String arg = argv[i];
            if ("-iwad".equals(arg) && i + 1 < argv.length)
            {
                iwad = argv[++i];
            }
            else if ("-fps".equals(arg))
            {
                game.showFps = true;
            }
            else if ("-nomonsters".equals(arg))
            {
                game.nomonsters = true;
            }
            else if ("-fast".equals(arg))
            {
                game.fastparm = true;
            }
            else if ("-respawn".equals(arg))
            {
                game.respawnparm = true;
            }
            else if ("-warp".equals(arg) && i + 2 < argv.length)
            {
                game.episode = parseIntOrZero(argv[++i]);
                game.mapn = parseIntOrZero(argv[++i]);
            }
            else if ("-skill".equals(arg) && i + 1 < argv.length)
            {
                game.skill = parseIntOrZero(argv[++i]);
            }
            else if ("-fullscreen".equals(arg))
            {
                game.fullscreen = true;
            }
            else if ("-crt".equals(arg))
            {
                game.crt = true;
            }
            else if ("-deh".equals(arg))
            {
                while (i + 1 < argv.length && !argv[i + 1].startsWith("-"))
                {
                    Deh.INSTANCE.files.add(argv[++i]);
                }
            }
            else if ("-nodeh".equals(arg))
            {
                Deh.INSTANCE.nodeh = true;
            }
            else if ("-dehlump".equals(arg))
            {
                Deh.INSTANCE.dehlump = true;
            }
            else if ("-nocheats".equals(arg))
            {
                Deh.INSTANCE.applyCheats = false;
            }
            else if ("-file".equals(arg))
            {
                while (i + 1 < argv.length && !argv[i + 1].startsWith("-"))
                {
                    game.pwadFiles.add(argv[++i]);
                }
            }
            else if ("-record".equals(arg) && i + 1 < argv.length)
            {
                game.recordName = argv[++i];
            }
            else if ("-playdemo".equals(arg) && i + 1 < argv.length)
            {
                game.playdemoName = argv[++i];
            }
            else if ("-timedemo".equals(arg) && i + 1 < argv.length)
            {
                game.timedemoName = argv[++i];
            }
            else if ("-nosound".equals(arg))
            {
                game.nosound = true;
            }
            else if ("-nomusic".equals(arg))
            {
                game.nomusic = true;
            }
            else if (!arg.startsWith("-") && arg.toLowerCase().endsWith(".wad"))
            {
                iwad = arg;
            }
        }
        return iwad;
    }

    private static int parseIntOrZero(String text)
    {
        try
        {
            return Integer.parseInt(text);
        }
        catch (NumberFormatException e)
        {
            return 0;
        }
    }

    private static String findIwad(String explicit)
    {
        if (explicit != null)
        {
            if (isFile(explicit))
            {
                return realpath(explicit);
            }
            throw new RuntimeException("IWAD not found: " + explicit);
        }
        Path cwd = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        List<Path> roots = new ArrayList<>();
        String env = System.getenv("DOOMWADDIR");
        if (env == null || env.isEmpty())
        {
            env = System.getenv("DOOMWADPATH");
        }
        if (env != null && !env.isEmpty())
        {
            for (String part : env.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator)))
            {
                if (!part.isEmpty())
                {
                    roots.add(Path.of(part));
                }
            }
        }
        roots.add(cwd);
        String folder = cwd.getFileName() != null ? cwd.getFileName().toString() : "";
        Path javaDoom = "java_doom".equals(folder) ? cwd : cwd.resolve("java_doom");
        roots.add(javaDoom);
        Path doomMinimal;
        if ("java_doom".equals(folder) && cwd.getParent() != null)
        {
            doomMinimal = cwd.getParent();
        }
        else
        {
            doomMinimal = cwd;
        }
        roots.add(doomMinimal);
        for (Path root : roots)
        {
            for (String name : IWADS)
            {
                Path full = root.resolve(name);
                if (isFile(full.toString()))
                {
                    return realpath(full.toString());
                }
            }
        }
        throw new RuntimeException("No IWAD found. Put doom1.wad in this folder or pass -iwad file.wad");
    }

    private static boolean isFile(String p)
    {
        try
        {
            return Files.isRegularFile(Path.of(p));
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private static String realpath(String p)
    {
        try
        {
            return Path.of(p).toRealPath().toString();
        }
        catch (Exception e)
        {
            return p;
        }
    }
}
