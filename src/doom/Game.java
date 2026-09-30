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
    private static final String[] CHEATS = { "iddqd", "idkfa", "idfa", "iddt", "idclip", "idspispopd" };

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
    public boolean fullscreen;
    public boolean crt;
    public String iwadPath = "";
    public Menu menu;
    public boolean showMessages = true;
    public int detailLevel;
    public int screenSize = 7;
    public int mouseSensitivity = 5;
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
    private int palette = -1;
    private byte[] playpal;
    private Integer wipeState = Defs.GS_TITLE;
    private int nextMapNum = 1;
    private final Map<String, Integer> cheats = new HashMap<>();

    public Game()
    {
        wad = new Wad();
        video = new Video();
        sound = new Sound();
        wipe = new Wipe();
        automap = new AmMap();
        for (String sequence : CHEATS)
        {
            cheats.put(sequence, 0);
        }
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
            int thrust = damage * Compat.intdiv(Defs.FRACUNIT, 8);
            target.momx += Compat.fixedMul(thrust, Tables.fineCos(angle));
            target.momy += Compat.fixedMul(thrust, Tables.fineSin(angle));
        }
        if (target.player != null)
        {
            Player p = target.player;
            if ((p.cheats & Defs.CF_GODMODE) != 0 && damage < 1000)
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
            }
            return;
        }
        target.health -= damage;
        if (target.health <= 0)
        {
            Enemy.killMonster(target, this, source);
        }
        else
        {
            startSound("popain");
            if (source != null)
            {
                target.target = source;
                if ("".equals(target.aiState) || "look".equals(target.aiState))
                {
                    target.aiState = "chase";
                    target.reactiontime = 0;
                }
            }
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
        world = new World();
        world.setupLevel(wad, res, episode, mapn);
        specials = new Specials(world, res, sound);
        MapThing start = world.playerStart();
        if (start == null)
        {
            throw new RuntimeException("no player 1 start");
        }
        player = Player.spawnPlayer(world, start);
        if (previous != null)
        {
            carryPlayer(previous);
        }
        player.killcount = 0;
        player.itemcount = 0;
        player.secretcount = 0;
        totalkills = 0;
        totalitems = 0;
        totalsecret = 0;
        for (Sector s : world.sectors)
        {
            if (s.special == 9)
            {
                ++totalsecret;
            }
        }
        if (!nomonsters)
        {
            int[] counts = Mobj.spawnMapThings(world, skill);
            totalkills = counts[0];
            totalitems = counts[1];
        }
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
        if (specials != null && specials.secretExit && player != null)
        {
            player.didsecret = true;
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
        this.skill = skill;
        this.episode = episode;
        this.mapn = map;
        loadLevel();
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
        gamestate = Defs.GS_TITLE;
        player = null;
        world = null;
        wi = null;
        finale = null;
        automap.resetLevel();
        sound.playTitleMusic();
        if (menu != null)
        {
            menu.clear();
        }
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
        return cmd;
    }

    public void runTic()
    {
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
        if (gamestate == Defs.GS_TITLE)
        {
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
                    returnToTitle();
                }
            }
            return;
        }
        if (gamestate != Defs.GS_LEVEL || player == null)
        {
            return;
        }
        player.cmd = menu != null && menu.active ? new Ticcmd() : buildTiccmd();
        Player.playerThink(world, player, this, leveltime);
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
        if (gamestate == Defs.GS_TITLE && titlePatch != null)
        {
            VVideo.fill(fb, 0);
            VVideo.drawPatch(fb, 0, 0, titlePatch);
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
            renderer.setupFrame(mo.x, mo.y, player.viewz, mo.angle, player.extralight);
            renderer.render(world, fb);
            Sprites.drawSprites(renderer, world, fb);
            renderer.drawMasked();
            drawWeapon(fb);
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
            if (p.damagecount != 0)
            {
                next = Math.min(7, (p.damagecount + 7) >> 3) + 1;
            }
            else if (p.bonuscount != 0)
            {
                next = Math.min(3, (p.bonuscount + 7) >> 3) + 9;
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

    public void handleEvent(String type, int key, String unicodeChar)
    {
        if (unicodeChar == null)
        {
            unicodeChar = "";
        }
        if ("quit".equals(type))
        {
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
            keys.put(key, true);
            if ((key == Keys.RETURN || key == Keys.KP_ENTER) && gamestate == Defs.GS_TITLE)
            {
                loadLevel();
            }
            else if (key == Keys.F11)
            {
                video.showFps = !video.showFps;
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
        if (gamestate != Defs.GS_LEVEL || player == null || skill == Defs.SK_NIGHTMARE)
        {
            return;
        }
        String ch = input.toLowerCase();
        if (ch.length() != 1)
        {
            return;
        }
        char letter = ch.charAt(0);
        if (letter < 'a' || letter > 'z')
        {
            return;
        }
        for (String sequence : CHEATS)
        {
            int position = cheats.get(sequence);
            if (position < sequence.length() && letter == sequence.charAt(position))
            {
                ++position;
                if (position >= sequence.length())
                {
                    cheats.put(sequence, 0);
                    switch (sequence)
                    {
                        case "iddqd":
                            cheatGod();
                            break;
                        case "idkfa":
                            cheatAmmo(true);
                            break;
                        case "idfa":
                            cheatAmmo(false);
                            break;
                        case "iddt":
                            if (automap.active)
                            {
                                automap.cycleIddt();
                            }
                            break;
                        case "idclip":
                        case "idspispopd":
                            cheatNoclip();
                            break;
                        default:
                            break;
                    }
                }
                else
                {
                    cheats.put(sequence, position);
                }
            }
            else
            {
                cheats.put(sequence, letter == sequence.charAt(0) ? 1 : 0);
            }
        }
    }

    private void cheatGod()
    {
        Player p = player;
        p.cheats ^= Defs.CF_GODMODE;
        if ((p.cheats & Defs.CF_GODMODE) != 0)
        {
            p.health = 100;
            p.mo.health = 100;
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
        p.armorpoints = 200;
        p.armortype = 2;
        Arrays.fill(p.weaponowned, true);
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

    public static int main(String[] args)
    {
        try
        {
            Game game = new Game();
            String iwad = parseArgs(args, game);
            String iwadPath = findIwad(iwad);
            game.iwadPath = iwadPath;
            System.out.println("IWAD: " + iwadPath);
            game.wad.addFile(iwadPath);
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
            game.sound.init(game.wad);
            game.sound.output = game.video;
            game.menu = new Menu(game.wad, game.sound, game);
            if (game.wad.checkNumForName("TITLEPIC") >= 0)
            {
                game.titlePatch = game.wad.cacheLumpName("TITLEPIC");
            }
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
            if (warp)
            {
                game.loadLevel();
            }
            else
            {
                game.startSound("swtchn");
                game.sound.playTitleMusic();
            }
            double tickMs = 1000.0 / Defs.TICRATE;
            double accum = 0;
            int last = game.video.ticksMs();
            while (game.running)
            {
                for (GameEvent event : game.video.pollEvents())
                {
                    String text = event.text != null ? event.text : "";
                    game.handleEvent(event.type, event.key, text);
                }
                int now = game.video.ticksMs();
                accum += now - last;
                last = now;
                while (accum >= tickMs)
                {
                    game.runTic();
                    accum -= tickMs;
                }
                game.draw();
                if (accum < tickMs / 2)
                {
                    usleep(1);
                }
            }
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
