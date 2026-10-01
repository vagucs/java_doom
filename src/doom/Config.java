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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** default.cfg subset (m_misc): mouse, volumes, messages, screenblocks. */
public final class Config
{
    private Config()
    {
    }

    public static Path configPath()
    {
        return Path.of(System.getProperty("user.dir", "")).resolve("default.cfg");
    }

    public static void load(Game game)
    {
        Path path = configPath();
        if (!Files.isRegularFile(path))
        {
            return;
        }
        List<String> lines;
        try
        {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            return;
        }
        for (String raw : lines)
        {
            String line = raw;
            int hash = line.indexOf('#');
            if (hash >= 0)
            {
                line = line.substring(0, hash);
            }
            line = line.trim();
            if (line.isEmpty())
            {
                continue;
            }
            String[] parts = line.split("\\s+");
            if (parts.length < 2)
            {
                continue;
            }
            String key = parts[0];
            int n;
            try
            {
                n = Integer.parseInt(parts[1]);
            }
            catch (NumberFormatException e)
            {
                continue;
            }
            if ("mouse_sensitivity".equals(key))
            {
                game.mouseSensitivity = Math.max(0, Math.min(9, n));
            }
            else if ("sfx_volume".equals(key))
            {
                game.sound.sfxVolume = Math.max(0, Math.min(15, n));
            }
            else if ("music_volume".equals(key))
            {
                game.sound.musicVolume = Math.max(0, Math.min(15, n));
            }
            else if ("show_messages".equals(key))
            {
                game.showMessages = n != 0;
            }
            else if ("use_mouse".equals(key))
            {
                game.useMouse = n != 0;
            }
            else if ("screenblocks".equals(key))
            {
                game.screenSize = Math.max(0, Math.min(8, n - 3));
            }
        }
    }

    public static void save(Game game)
    {
        Path path = configPath();
        String body =
            "mouse_sensitivity\t\t" + game.mouseSensitivity + "\n"
                + "sfx_volume\t\t" + game.sound.sfxVolume + "\n"
                + "music_volume\t\t" + game.sound.musicVolume + "\n"
                + "show_messages\t\t" + (game.showMessages ? 1 : 0) + "\n"
                + "use_mouse\t\t" + (game.useMouse ? 1 : 0) + "\n"
                + "screenblocks\t\t" + (game.screenSize + 3) + "\n";
        try
        {
            Files.writeString(path, body, StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            // ignore, same as vanilla m_misc write failure
        }
    }
}
