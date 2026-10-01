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

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;

public final class Sound
{
    private static final String[] DOOM2_MUSIC = {
        "runnin", "stalks", "countd", "betwee", "doom", "the_da", "shawn", "ddtblu",
        "in_cit", "dead", "stlks2", "theda2", "doom2", "ddtbl2", "runni2", "dead2",
        "stlks3", "romero", "shawn2", "messag", "count2", "ddtbl3", "ampie", "theda3",
        "adrian", "messg2", "romer2", "tense", "shawn3", "openin", "evil", "ultima",
    };

    private Wad wad;
    private final Map<String, byte[]> cache = new HashMap<>();
    private String musicName = "";
    private boolean musicLoop;
    private Sequencer sequencer;
    public Video output;
    public boolean enabled = true;
    public boolean musicEnabled = true;
    public int sfxVolume = 8;
    public int musicVolume = 8;

    public void init(Wad wad)
    {
        this.wad = wad;
    }

    public void play(String name)
    {
        if (!enabled) {
            return;
        }
        byte[] pcm = getPcm(name);
        if (pcm != null && output != null) {
            output.playSfx(pcm, sfxVolume);
        }
    }

    public byte[] getPcm(String name)
    {
        String key = name.toLowerCase();
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        if (wad == null) {
            cache.put(key, null);
            return null;
        }
        String lump = "DS" + (key.length() > 6 ? key.substring(0, 6) : key).toUpperCase();
        int number = wad.checkNumForName(lump);
        if (number < 0) {
            cache.put(key, null);
            return null;
        }
        byte[] pcm = decodeDs(wad.cacheLumpNum(number));
        cache.put(key, pcm);
        return pcm;
    }

    public void playTitleMusic()
    {
        if (wad == null) {
            return;
        }
        if (wad.checkNumForName("MAP01") >= 0) {
            changeMusic("dm2ttl", false);
        } else if (wad.checkNumForName("D_INTROA") >= 0) {
            changeMusic("introa", false);
        } else {
            changeMusic("intro", false);
        }
    }

    public void playLevelMusic(int episode, int map)
    {
        if (wad == null) {
            return;
        }
        String name = wad.checkNumForName("MAP01") >= 0
            ? DOOM2_MUSIC[(Math.max(1, map) - 1) % DOOM2_MUSIC.length]
            : "e" + episode + "m" + map;
        changeMusic(name, true);
    }

    public static String[] doom2Music()
    {
        return DOOM2_MUSIC;
    }

    public boolean hasMusic(String name)
    {
        if (wad == null || name == null || name.isEmpty()) {
            return false;
        }
        String lump = name.length() > 6 ? name.substring(0, 6) : name;
        return wad.checkNumForName("D_" + lump.toUpperCase()) >= 0;
    }

    public void changeMusic(String name, boolean looping)
    {
        if (!musicEnabled || wad == null || name.isEmpty() || name.toLowerCase().equals(musicName)) {
            return;
        }
        String lump = name.length() > 6 ? name.substring(0, 6) : name;
        int number = wad.checkNumForName("D_" + lump.toUpperCase());
        if (number < 0) {
            return;
        }
        byte[] midi = Mus2Mid.convert(wad.cacheLumpNum(number));
        if (midi == null || midi.length == 0) {
            return;
        }
        stopMusic();
        try {
            sequencer = MidiSystem.getSequencer(true);
            sequencer.open();
            sequencer.setSequence(MidiSystem.getSequence(new ByteArrayInputStream(midi)));
            sequencer.setLoopCount(looping ? Sequencer.LOOP_CONTINUOUSLY : 0);
            sequencer.start();
            musicName = name.toLowerCase();
            musicLoop = looping;
            setMusicVolume(musicVolume);
        } catch (Exception e) {
            sequencer = null;
            musicName = "";
        }
    }

    public void setSfxVolume(int volume)
    {
        sfxVolume = Math.max(0, Math.min(15, volume));
    }

    public void setMusicVolume(int volume)
    {
        musicVolume = Math.max(0, Math.min(15, volume));
        try {
            Synthesizer synth = MidiSystem.getSynthesizer();
            if (!synth.isOpen()) {
                synth.open();
            }
            int level = Compat.intdiv(musicVolume * 127, 15);
            for (MidiChannel channel : synth.getChannels()) {
                if (channel != null) {
                    channel.controlChange(7, level);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void stopMusic()
    {
        if (sequencer != null) {
            try {
                sequencer.stop();
                sequencer.close();
            } catch (Exception ignored) {
            }
            sequencer = null;
        }
        musicName = "";
        musicLoop = false;
    }

    public void update()
    {
        if (sequencer == null || !musicLoop) {
            return;
        }
        try {
            if (!sequencer.isRunning() && sequencer.getTickPosition() >= sequencer.getTickLength()) {
                sequencer.setTickPosition(0);
                sequencer.start();
            }
        } catch (Exception ignored) {
        }
    }

    private static byte[] decodeDs(byte[] data)
    {
        int size = data.length;
        if (size < 8 || Bin.u8(data, 0) != 3 || Bin.u8(data, 1) != 0) {
            return null;
        }
        int rate = Bin.u16(data, 2);
        int length = Bin.u32(data, 4);
        if (rate <= 0 || length > size - 8 || length <= 48) {
            return null;
        }
        int sampleCount = length - 32;
        if (sampleCount <= 0) {
            return null;
        }
        int sourceLength = Math.min(sampleCount, size - 16);
        if (sourceLength <= 0) {
            return null;
        }
        int count = rate == 11025 ? sourceLength : Math.max(1, Compat.intdiv(sourceLength * 11025, rate));
        byte[] pcm = new byte[count * 2];
        for (int i = 0; i < count; ++i) {
            int source = Math.min(sourceLength - 1, Compat.intdiv(i * sourceLength, count));
            int sample = (Bin.u8(data, 16 + source) - 128) << 8;
            pcm[i * 2] = (byte) sample;
            pcm[i * 2 + 1] = (byte) (sample >> 8);
        }
        return pcm;
    }
}
