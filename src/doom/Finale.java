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

import java.nio.file.Path;

/** End-of-episode / Doom 2 texts + MAP30 cast (f_finale). */
public final class Finale
{
    private static final int TEXT_SPEED = 3;
    private static final int TEXT_WAIT = 250;
    private static final int TEXT = 0;
    private static final int ART = 1;
    private static final int CAST = 2;

    private static final String E1TEXT =
        "Once you beat the big badasses and\n"
            + "clean out the moon base you're supposed\n"
            + "to win, aren't you? Aren't you? Where's\n"
            + "your fat reward and ticket home? What\n"
            + "the hell is this? It's not supposed to\n"
            + "end this way!\n"
            + "\n"
            + "It stinks like rotten meat, but looks\n"
            + "like the lost Deimos base.  Looks like\n"
            + "you're stuck on The Shores of Hell.\n"
            + "The only way out is through.\n"
            + "\n"
            + "To continue the DOOM experience, play\n"
            + "The Shores of Hell and its amazing\n"
            + "sequel, Inferno!\n";
    private static final String E2TEXT =
        "You've done it! The hideous cyber-\n"
            + "demon lord that ruled the lost Deimos\n"
            + "moon base has been slain and you\n"
            + "are triumphant! But ... where are\n"
            + "you? You clamber to the edge of the\n"
            + "moon and look down to see the awful\n"
            + "truth.\n"
            + "\n"
            + "Deimos floats above Hell itself!\n"
            + "You've never heard of anyone escaping\n"
            + "from Hell, but you'll make the bastards\n"
            + "sorry they ever heard of you! Quickly,\n"
            + "you rappel down to  the surface of\n"
            + "Hell.\n"
            + "\n"
            + "Now, it's on to the final chapter of\n"
            + "DOOM! -- Inferno.\n";
    private static final String E3TEXT =
        "The loathsome spiderdemon that\n"
            + "masterminded the invasion of the moon\n"
            + "bases and caused so much death has had\n"
            + "its ass kicked for all time.\n"
            + "\n"
            + "A hidden doorway opens and you enter.\n"
            + "You've proven too tough for Hell to\n"
            + "contain, and now Hell at last plays\n"
            + "fair -- for you emerge from the door\n"
            + "to see the green fields of Earth!\n"
            + "Home at last.\n"
            + "\n"
            + "You wonder what's been happening on\n"
            + "Earth while you were battling evil\n"
            + "unleashed. It's good that no Hell-\n"
            + "spawn could have come through that\n"
            + "door with you ...\n";
    private static final String E4TEXT =
        "the spider mastermind must have sent forth\n"
            + "its legions of hellspawn before your\n"
            + "final confrontation with that terrible\n"
            + "beast from hell.  but you stepped forward\n"
            + "and brought forth eternal damnation and\n"
            + "suffering upon the horde as a true hero\n"
            + "would in the face of something so evil.\n"
            + "\n"
            + "besides, someone was gonna pay for what\n"
            + "happened to daisy, your pet rabbit.\n"
            + "\n"
            + "but now, you see spread before you more\n"
            + "potential pain and gibbitude as a nation\n"
            + "of demons run amok among our cities.\n"
            + "\n"
            + "next stop, hell on earth!";
    private static final String C1TEXT =
        "YOU HAVE ENTERED DEEPLY INTO THE INFESTED\n"
            + "STARPORT. BUT SOMETHING IS WRONG. THE\n"
            + "MONSTERS HAVE BROUGHT THEIR OWN REALITY\n"
            + "WITH THEM, AND THE STARPORT'S TECHNOLOGY\n"
            + "IS BEING SUBVERTED BY THEIR PRESENCE.\n"
            + "\n"
            + "AHEAD, YOU SEE AN OUTPOST OF HELL, A\n"
            + "FORTIFIED ZONE. IF YOU CAN GET PAST IT,\n"
            + "YOU CAN PENETRATE INTO THE HAUNTED HEART\n"
            + "OF THE STARBASE AND FIND THE CONTROLLING\n"
            + "SWITCH WHICH HOLDS EARTH'S POPULATION\n"
            + "HOSTAGE.";
    private static final String C2TEXT =
        "YOU HAVE WON! YOUR VICTORY HAS ENABLED\n"
            + "HUMANKIND TO EVACUATE EARTH AND ESCAPE\n"
            + "THE NIGHTMARE.  NOW YOU ARE THE ONLY\n"
            + "HUMAN LEFT ON THE FACE OF THE PLANET.\n"
            + "CANNIBAL MUTATIONS, CARNIVOROUS ALIENS,\n"
            + "AND EVIL SPIRITS ARE YOUR ONLY NEIGHBORS.\n"
            + "YOU SIT BACK AND WAIT FOR DEATH, CONTENT\n"
            + "THAT YOU HAVE SAVED YOUR SPECIES.\n"
            + "\n"
            + "BUT THEN, EARTH CONTROL BEAMS DOWN A\n"
            + "MESSAGE FROM SPACE: \"SENSORS HAVE LOCATED\n"
            + "THE SOURCE OF THE ALIEN INVASION. IF YOU\n"
            + "GO THERE, YOU MAY BE ABLE TO BLOCK THEIR\n"
            + "ENTRY.  THE ALIEN BASE IS IN THE HEART OF\n"
            + "YOUR OWN HOME CITY, NOT FAR FROM THE\n"
            + "STARPORT.\" SLOWLY AND PAINFULLY YOU GET\n"
            + "UP AND RETURN TO THE FRAY.";
    private static final String C3TEXT =
        "YOU ARE AT THE CORRUPT HEART OF THE CITY,\n"
            + "SURROUNDED BY THE CORPSES OF YOUR ENEMIES.\n"
            + "YOU SEE NO WAY TO DESTROY THE CREATURES'\n"
            + "ENTRYWAY ON THIS SIDE, SO YOU CLENCH YOUR\n"
            + "TEETH AND PLUNGE THROUGH IT.\n"
            + "\n"
            + "THERE MUST BE A WAY TO CLOSE IT ON THE\n"
            + "OTHER SIDE. WHAT DO YOU CARE IF YOU'VE\n"
            + "GOT TO GO THROUGH HELL TO GET TO IT?";
    private static final String C4TEXT =
        "THE HORRENDOUS VISAGE OF THE BIGGEST\n"
            + "DEMON YOU'VE EVER SEEN CRUMBLES BEFORE\n"
            + "YOU, AFTER YOU PUMP YOUR ROCKETS INTO\n"
            + "HIS EXPOSED BRAIN. THE MONSTER SHRIVELS\n"
            + "UP AND DIES, ITS THRASHING LIMBS\n"
            + "DEVASTATING UNTOLD MILES OF HELL'S\n"
            + "SURFACE.\n"
            + "\n"
            + "YOU'VE DONE IT. THE INVASION IS OVER.\n"
            + "EARTH IS SAVED. HELL IS A WRECK. YOU\n"
            + "WONDER WHERE BAD FOLKS WILL GO WHEN THEY\n"
            + "DIE, NOW. WIPING THE SWEAT FROM YOUR\n"
            + "FOREHEAD YOU BEGIN THE LONG TREK BACK\n"
            + "HOME. REBUILDING EARTH OUGHT TO BE A\n"
            + "LOT MORE FUN THAN RUINING IT WAS.\n";
    private static final String C5TEXT =
        "CONGRATULATIONS, YOU'VE FOUND THE SECRET\n"
            + "LEVEL! LOOKS LIKE IT'S BEEN BUILT BY\n"
            + "HUMANS, RATHER THAN DEMONS. YOU WONDER\n"
            + "WHO THE INMATES OF THIS CORNER OF HELL\n"
            + "WILL BE.";
    private static final String C6TEXT =
        "CONGRATULATIONS, YOU'VE FOUND THE\n"
            + "SUPER SECRET LEVEL!  YOU'D BETTER\n"
            + "BLAZE THROUGH THIS ONE!\n";
    private static final String P1TEXT =
        "You gloat over the steaming carcass of the\n"
            + "Guardian.  With its death, you've wrested\n"
            + "the Accelerator from the stinking claws\n"
            + "of Hell.  You relax and glance around the\n"
            + "room.  Damn!  There was supposed to be at\n"
            + "least one working prototype, but you can't\n"
            + "see it. The demons must have taken it.\n"
            + "\n"
            + "You must find the prototype, or all your\n"
            + "struggles will have been wasted. Keep\n"
            + "moving, keep fighting, keep killing.\n"
            + "Oh yes, keep living, too.";
    private static final String P2TEXT =
        "Even the deadly Arch-Vile labyrinth could\n"
            + "not stop you, and you've gotten to the\n"
            + "prototype Accelerator which is soon\n"
            + "efficiently and permanently deactivated.\n"
            + "\n"
            + "You're good at that kind of thing.";
    private static final String P3TEXT =
        "You've bashed and battered your way into\n"
            + "the heart of the devil-hive.  Time for a\n"
            + "Search-and-Destroy mission, aimed at the\n"
            + "Gatekeeper, whose foul offspring is\n"
            + "cascading to Earth.  Yeah, he's bad. But\n"
            + "you know who's worse!\n"
            + "\n"
            + "Grinning evilly, you check your gear, and\n"
            + "get ready to give the bastard a little Hell\n"
            + "of your own making!";
    private static final String P4TEXT =
        "The Gatekeeper's evil face is splattered\n"
            + "all over the place.  As its tattered corpse\n"
            + "collapses, an inverted Gate forms and\n"
            + "sucks down the shards of the last\n"
            + "prototype Accelerator, not to mention the\n"
            + "few remaining demons.  You're done. Hell\n"
            + "has gone back to pounding bad dead folks \n"
            + "instead of good live ones.  Remember to\n"
            + "tell your grandkids to put a rocket\n"
            + "launcher in your coffin. If you go to Hell\n"
            + "when you die, you'll need it for some\n"
            + "final cleaning-up ...";
    private static final String P5TEXT =
        "You've found the second-hardest level we\n"
            + "got. Hope you have a saved game a level or\n"
            + "two previous.  If not, be prepared to die\n"
            + "aplenty. For master marines only.";
    private static final String P6TEXT =
        "Betcha wondered just what WAS the hardest\n"
            + "level we had ready for ya?  Now you know.\n"
            + "No one gets out alive.";
    private static final String T1TEXT =
        "You've fought your way out of the infested\n"
            + "experimental labs.   It seems that UAC has\n"
            + "once again gulped it down.  With their\n"
            + "high turnover, it must be hard for poor\n"
            + "old UAC to buy corporate health insurance\n"
            + "nowadays..\n"
            + "\n"
            + "Ahead lies the military complex, now\n"
            + "swarming with diseased horrors hot to get\n"
            + "their teeth into you. With luck, the\n"
            + "complex still has some warlike ordnance\n"
            + "laying around.";
    private static final String T2TEXT =
        "You hear the grinding of heavy machinery\n"
            + "ahead.  You sure hope they're not stamping\n"
            + "out new hellspawn, but you're ready to\n"
            + "ream out a whole herd if you have to.\n"
            + "They might be planning a blood feast, but\n"
            + "you feel about as mean as two thousand\n"
            + "maniacs packed into one mad killer.\n"
            + "\n"
            + "You don't plan to go down easy.";
    private static final String T3TEXT =
        "The vista opening ahead looks real damn\n"
            + "familiar. Smells familiar, too -- like\n"
            + "fried excrement. You didn't like this\n"
            + "place before, and you sure as hell ain't\n"
            + "planning to like it now. The more you\n"
            + "brood on it, the madder you get.\n"
            + "Hefting your gun, an evil grin trickles\n"
            + "onto your face. Time to take some names.";
    private static final String T4TEXT =
        "Suddenly, all is silent, from one horizon\n"
            + "to the other. The agonizing echo of Hell\n"
            + "fades away, the nightmare sky turns to\n"
            + "blue, the heaps of monster corpses start \n"
            + "to evaporate along with the evil stench \n"
            + "that filled the air. Jeeze, maybe you've\n"
            + "done it. Have you really won?\n"
            + "\n"
            + "Something rumbles in the distance.\n"
            + "A blue light begins to glow inside the\n"
            + "ruined skull of the demon-spitter.";
    private static final String T5TEXT =
        "What now? Looks totally different. Kind\n"
            + "of like King Tut's condo. Well,\n"
            + "whatever's here can't be any worse\n"
            + "than usual. Can it?  Or maybe it's best\n"
            + "to let sleeping gods lie..";
    private static final String T6TEXT =
        "Time for a vacation. You've burst the\n"
            + "bowels of hell and by golly you're ready\n"
            + "for a break. You mutter to yourself,\n"
            + "Maybe someone else can kick Hell's ass\n"
            + "next time around. Ahead lies a quiet town,\n"
            + "with peaceful flowing water, quaint\n"
            + "buildings, and presumably no Hellspawn.\n"
            + "\n"
            + "As you step off the transport, you hear\n"
            + "the stomp of a cyberdemon's iron shoe.";

    private static final String[] CAST_NAMES = {
        "ZOMBIEMAN",
        "SHOTGUN GUY",
        "HEAVY WEAPON DUDE",
        "IMP",
        "DEMON",
        "LOST SOUL",
        "CACODEMON",
        "HELL KNIGHT",
        "BARON OF HELL",
        "ARACHNOTRON",
        "PAIN ELEMENTAL",
        "REVENANT",
        "MANCUBUS",
        "ARCH-VILE",
        "THE SPIDER MASTERMIND",
        "THE CYBERDEMON",
        "OUR HERO",
    };
    private static final int[] CAST_TYPES = {
        Info.MT_POSSESSED,
        Info.MT_SHOTGUY,
        Info.MT_CHAINGUY,
        Info.MT_TROOP,
        Info.MT_SERGEANT,
        Info.MT_SKULL,
        Info.MT_HEAD,
        Info.MT_KNIGHT,
        Info.MT_BRUISER,
        Info.MT_BABY,
        Info.MT_PAIN,
        Info.MT_UNDEAD,
        Info.MT_FATSO,
        Info.MT_VILE,
        Info.MT_SPIDER,
        Info.MT_CYBORG,
        Info.MT_PLAYER,
    };

    private final Game game;
    private int stage = TEXT;
    private int count;
    public boolean done;
    public String action = "";
    private final boolean commercial;
    private final String text;
    private final String flat;
    private byte[] flatLump;
    private byte[] art;
    private byte[] pfub1;
    private byte[] pfub2;
    private byte[] bossback;
    private int lastBunnyStage = -1;
    private int castnum;
    private int caststate = Info.S_NULL;
    private int casttics;
    private boolean castdeath;
    private int castframes;
    private int castonmelee;
    private boolean castattacking;

    public Finale(Game game)
    {
        this.game = game;
        commercial = game.wad.checkNumForName("MAP01") >= 0;
        if (commercial)
        {
            String[] screen = missionScreen(game.iwadPath, game.mapn);
            flat = screen[0];
            text = screen[1];
            game.sound.changeMusic("read_m", true);
        }
        else
        {
            if (game.episode == 2)
            {
                text = E2TEXT;
            }
            else if (game.episode == 3)
            {
                text = E3TEXT;
            }
            else if (game.episode == 4)
            {
                text = E4TEXT;
            }
            else
            {
                text = E1TEXT;
            }
            if (game.episode == 2)
            {
                flat = "SFLR6_1";
            }
            else if (game.episode == 3)
            {
                flat = "MFLR8_4";
            }
            else if (game.episode == 4)
            {
                flat = "MFLR8_3";
            }
            else
            {
                flat = "FLOOR4_8";
            }
            game.sound.changeMusic("victor", true);
        }
        flatLump = lump(this.flat);
        String artName;
        if (commercial)
        {
            artName = has("CREDIT") ? "CREDIT" : "HELP2";
        }
        else if (game.episode == 2)
        {
            artName = "VICTORY2";
        }
        else if (game.episode == 4)
        {
            artName = "ENDPIC";
        }
        else
        {
            artName = has("CREDIT") ? "CREDIT" : "HELP2";
        }
        art = lump(artName);
        if (art == null)
        {
            art = lump("HELP1");
        }
        if (game.episode == 3 && !commercial)
        {
            pfub1 = lump("PFUB1");
            pfub2 = lump("PFUB2");
        }
        bossback = lump("BOSSBACK");
    }

    public static boolean commercialFinaleMap(int mapn, boolean secret)
    {
        if (mapn == 6 || mapn == 11 || mapn == 20 || mapn == 30)
        {
            return true;
        }
        return secret && (mapn == 15 || mapn == 31);
    }

    public void ticker()
    {
        if (commercial && stage == TEXT && count > 50 && wantSkip())
        {
            if (game.mapn == 30)
            {
                startCast();
            }
            else
            {
                action = "worlddone";
                done = true;
                return;
            }
        }
        ++count;
        if (stage == CAST)
        {
            castTicker();
            return;
        }
        if (commercial)
        {
            return;
        }
        if (stage == TEXT)
        {
            if (count > text.length() * TEXT_SPEED + TEXT_WAIT)
            {
                stage = ART;
                count = 0;
                game.forceWipe = true;
                if (game.episode == 3)
                {
                    game.sound.changeMusic("bunny", true);
                }
            }
        }
        else if (stage == ART)
        {
            int skipAfter = pfub1 != null && pfub2 != null ? 1130 : 10;
            if (wantSkip() && count > skipAfter)
            {
                done = true;
                action = "title";
            }
        }
    }

    public boolean responder()
    {
        if (stage != CAST || castdeath)
        {
            return false;
        }
        if (!wantSkip())
        {
            return false;
        }
        castdeath = true;
        caststate = Info.miInt(CAST_TYPES[castnum], Info.MI_DEATHSTATE);
        casttics = Info.STATES[caststate][2];
        if (casttics == -1)
        {
            casttics = 15;
        }
        castframes = 0;
        castattacking = false;
        return true;
    }

    public void draw(int[] fb)
    {
        if (stage == CAST)
        {
            drawCast(fb);
            return;
        }
        if (stage == ART)
        {
            if (game.episode == 3 && pfub1 != null && pfub2 != null)
            {
                drawBunny(fb);
            }
            else if (art != null)
            {
                VVideo.fill(fb, 0);
                VVideo.drawPatch(fb, 0, 0, art);
            }
            return;
        }
        drawText(fb);
    }

    private void startCast()
    {
        game.forceWipe = true;
        castnum = 0;
        caststate = Info.miInt(CAST_TYPES[0], Info.MI_SEESTATE);
        casttics = Info.STATES[caststate][2];
        castdeath = false;
        stage = CAST;
        castframes = 0;
        castonmelee = 0;
        castattacking = false;
        game.sound.changeMusic("evil", true);
    }

    private int castType()
    {
        return CAST_TYPES[castnum];
    }

    private void stopAttack()
    {
        castattacking = false;
        castframes = 0;
        caststate = Info.miInt(castType(), Info.MI_SEESTATE);
    }

    private void castTicker()
    {
        --casttics;
        if (casttics > 0)
        {
            return;
        }
        int[] st = Info.STATES[caststate];
        if (st[2] == -1 || st[4] == Info.S_NULL)
        {
            ++castnum;
            castdeath = false;
            if (castnum >= CAST_NAMES.length)
            {
                castnum = 0;
            }
            caststate = Info.miInt(castType(), Info.MI_SEESTATE);
            castframes = 0;
        }
        else
        {
            if (caststate == Info.S_PLAY_ATK1)
            {
                stopAttack();
            }
            else
            {
                int nxt = st[4];
                caststate = nxt;
                ++castframes;
                String sfx = castSfx(nxt);
                if (sfx != null)
                {
                    game.sound.play(sfx);
                }
            }
        }
        if (castframes == 12)
        {
            castattacking = true;
            int type = castType();
            caststate = castonmelee != 0
                ? Info.miInt(type, Info.MI_MELEESTATE)
                : Info.miInt(type, Info.MI_MISSILESTATE);
            castonmelee ^= 1;
            if (caststate == Info.S_NULL)
            {
                caststate = castonmelee != 0
                    ? Info.miInt(type, Info.MI_MELEESTATE)
                    : Info.miInt(type, Info.MI_MISSILESTATE);
            }
        }
        if (castattacking)
        {
            if (castframes == 24 || caststate == Info.miInt(castType(), Info.MI_SEESTATE))
            {
                stopAttack();
            }
        }
        casttics = Info.STATES[caststate][2];
        if (casttics == -1)
        {
            casttics = 15;
        }
    }

    private void drawCast(int[] fb)
    {
        VVideo.fill(fb, 0);
        if (bossback != null)
        {
            VVideo.drawPatch(fb, 0, 0, bossback);
        }
        castPrint(fb, CAST_NAMES[castnum]);
        int[] st = Info.STATES[caststate];
        String spr = st[0] >= 0 && st[0] < Info.SPRNAMES.length ? Info.SPRNAMES[st[0]] : "";
        if (game.res == null)
        {
            return;
        }
        int[] found = Sprites.lookupSprite(game.res, spr, 0, 0, st[1] & Info.FF_FRAMEMASK);
        if (found == null)
        {
            return;
        }
        byte[] patch = game.wad.cacheLumpNum(found[0]);
        VVideo.drawPatch(fb, 160, 170, patch, found[1] != 0);
    }

    private void castPrint(int[] fb, String name)
    {
        int width = 0;
        for (int i = 0; i < name.length(); ++i)
        {
            char ch = name.charAt(i);
            int code = Character.toUpperCase(ch);
            if (ch == ' ' || code < Defs.HU_FONTSTART || code > Defs.HU_FONTEND)
            {
                width += 4;
                continue;
            }
            byte[] patch = font(code);
            if (patch == null)
            {
                width += 4;
                continue;
            }
            width += VVideo.patchSize(patch)[0];
        }
        int x = 160 - Compat.intdiv(width, 2);
        for (int i = 0; i < name.length(); ++i)
        {
            char ch = name.charAt(i);
            int code = Character.toUpperCase(ch);
            if (ch == ' ' || code < Defs.HU_FONTSTART || code > Defs.HU_FONTEND)
            {
                x += 4;
                continue;
            }
            byte[] patch = font(code);
            if (patch == null)
            {
                x += 4;
                continue;
            }
            int w = VVideo.patchSize(patch)[0];
            VVideo.drawPatch(fb, x, 180, patch);
            x += w;
        }
    }

    private void drawText(int[] fb)
    {
        fillFlat(fb);
        int shown = Compat.intdiv(count, TEXT_SPEED);
        int x = 10;
        int y = 10;
        for (int i = 0; i < text.length(); ++i)
        {
            if (i >= shown)
            {
                break;
            }
            char ch = text.charAt(i);
            if (ch == '\n')
            {
                x = 10;
                y += 11;
                continue;
            }
            int code = Character.toUpperCase(ch);
            if (ch == ' ' || code < Defs.HU_FONTSTART || code > Defs.HU_FONTEND)
            {
                x += 4;
                continue;
            }
            byte[] patch = font(code);
            if (patch == null)
            {
                x += 4;
                continue;
            }
            int width = VVideo.patchSize(patch)[0];
            if (x + width > Defs.SCREENWIDTH)
            {
                break;
            }
            VVideo.drawPatch(fb, x, y, patch);
            x += width;
        }
    }

    private void fillFlat(int[] fb)
    {
        if (flatLump == null || flatLump.length < 4096)
        {
            VVideo.fill(fb, 0);
            return;
        }
        for (int y = 0; y < Defs.SCREENHEIGHT; ++y)
        {
            int row = (y & 63) << 6;
            for (int x = 0; x < Defs.SCREENWIDTH; ++x)
            {
                fb[y * Defs.SCREENWIDTH + x] = flatLump[row + (x & 63)] & 0xff;
            }
        }
    }

    private void drawBunny(int[] fb)
    {
        VVideo.fill(fb, 0);
        int scroll = Math.max(0, Math.min(320, 320 - Compat.intdiv(count - 230, 2)));
        for (int x = 0; x < Defs.SCREENWIDTH; ++x)
        {
            int column = x + scroll;
            drawPatchColumn(fb, x, column < 320 ? pfub2 : pfub1, column < 320 ? column : column - 320);
        }
        if (count < 1130)
        {
            return;
        }
        int stageNow = count < 1180 ? 0 : Math.min(6, Compat.intdiv(count - 1180, 5));
        if (stageNow > lastBunnyStage)
        {
            game.sound.play("pistol");
            lastBunnyStage = stageNow;
        }
        byte[] patch = lump("END" + stageNow);
        if (patch != null)
        {
            VVideo.drawPatch(fb, Compat.intdiv(320 - 104, 2), Compat.intdiv(200 - 64, 2), patch);
        }
    }

    private void drawPatchColumn(int[] fb, int x, byte[] patch, int column)
    {
        if (column < 0)
        {
            return;
        }
        int offsetPos = 8 + column * 4;
        if (offsetPos + 4 > patch.length)
        {
            return;
        }
        int offset = Bin.u32(patch, offsetPos);
        while (offset < patch.length && (patch[offset] & 0xff) != 255)
        {
            int top = patch[offset] & 0xff;
            int length = patch[offset + 1] & 0xff;
            int source = offset + 3;
            for (int i = 0; i < length && top + i < 200; ++i)
            {
                fb[(top + i) * 320 + x] = patch[source + i] & 0xff;
            }
            offset += length + 4;
        }
    }

    private boolean wantSkip()
    {
        if (game.menu != null && game.menu.active)
        {
            return false;
        }
        if (game.mouseFire)
        {
            return true;
        }
        int[] keys = { Keys.LCTRL, Keys.RCTRL, Keys.SPACE, Keys.RETURN, Keys.KP_ENTER, 'e' };
        for (int key : keys)
        {
            if (Boolean.TRUE.equals(game.keys.get(key)))
            {
                return true;
            }
        }
        return false;
    }

    private byte[] font(int code)
    {
        if (code - Defs.HU_FONTSTART < 0 || code - Defs.HU_FONTSTART >= Defs.HU_FONTSIZE)
        {
            return null;
        }
        return lump(String.format("STCFN%03d", code));
    }

    private boolean has(String name)
    {
        return game.wad.checkNumForName(name) >= 0;
    }

    private byte[] lump(String name)
    {
        int n = game.wad.checkNumForName(name);
        return n < 0 ? null : game.wad.cacheLumpNum(n);
    }

    private static String[] missionScreen(String iwadPath, int mapn)
    {
        String name = "";
        if (iwadPath != null && !iwadPath.isEmpty())
        {
            Path file = Path.of(iwadPath).getFileName();
            name = file != null ? file.toString().toLowerCase() : "";
        }
        boolean tnt = name.contains("tnt");
        boolean plut = name.contains("plut");
        String[] doom2 = { "SLIME16", C1TEXT, "RROCK14", C2TEXT, "RROCK07", C3TEXT, "RROCK17", C4TEXT, "RROCK13", C5TEXT, "RROCK19", C6TEXT };
        String[] tntPack = { "SLIME16", T1TEXT, "RROCK14", T2TEXT, "RROCK07", T3TEXT, "RROCK17", T4TEXT, "RROCK13", T5TEXT, "RROCK19", T6TEXT };
        String[] plutonia = { "SLIME16", P1TEXT, "RROCK14", P2TEXT, "RROCK07", P3TEXT, "RROCK17", P4TEXT, "RROCK13", P5TEXT, "RROCK19", P6TEXT };
        String[] pack = tnt ? tntPack : plut ? plutonia : doom2;
        int idx;
        if (mapn == 6)
        {
            idx = 0;
        }
        else if (mapn == 11)
        {
            idx = 2;
        }
        else if (mapn == 20)
        {
            idx = 4;
        }
        else if (mapn == 30)
        {
            idx = 6;
        }
        else if (mapn == 15)
        {
            idx = 8;
        }
        else if (mapn == 31)
        {
            idx = 10;
        }
        else
        {
            return new String[] { "SLIME16", C1TEXT };
        }
        return new String[] { pack[idx], pack[idx + 1] };
    }

    private static String castSfx(int state)
    {
        if (state == Info.S_PLAY_ATK1)
        {
            return "dshtgn";
        }
        if (state == Info.S_POSS_ATK2)
        {
            return "pistol";
        }
        if (state == Info.S_SPOS_ATK2 || state == Info.S_CPOS_ATK2 || state == Info.S_CPOS_ATK3
            || state == Info.S_CPOS_ATK4 || state == Info.S_SPID_ATK2 || state == Info.S_SPID_ATK3)
        {
            return "shotgn";
        }
        if (state == Info.S_VILE_ATK2)
        {
            return "vilatk";
        }
        if (state == Info.S_SKEL_FIST2)
        {
            return "skeswg";
        }
        if (state == Info.S_SKEL_FIST4)
        {
            return "skepch";
        }
        if (state == Info.S_SKEL_MISS2)
        {
            return "skeatk";
        }
        if (state == Info.S_FATT_ATK8 || state == Info.S_FATT_ATK5 || state == Info.S_FATT_ATK2
            || state == Info.S_BOSS_ATK2 || state == Info.S_BOS2_ATK2 || state == Info.S_HEAD_ATK2)
        {
            return "firsht";
        }
        if (state == Info.S_TROO_ATK3)
        {
            return "claw";
        }
        if (state == Info.S_SARG_ATK2)
        {
            return "sgtatk";
        }
        if (state == Info.S_SKULL_ATK2 || state == Info.S_PAIN_ATK3)
        {
            return "sklatk";
        }
        if (state == Info.S_BSPI_ATK2)
        {
            return "plasma";
        }
        if (state == Info.S_CYBER_ATK2 || state == Info.S_CYBER_ATK4 || state == Info.S_CYBER_ATK6)
        {
            return "rlaunc";
        }
        return null;
    }
}
