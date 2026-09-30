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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** WAD loader (w_wad / w_file_stdc). Lump names are 8-byte, case-insensitive. */
public final class Wad
{
    public static final class Lump
    {
        public final String name;
        public final int position;
        public final int size;
        public final String wadPath;
        public byte[] cache;

        public Lump(String name, int position, int size, String wadPath)
        {
            this.name = name;
            this.position = position;
            this.size = size;
            this.wadPath = wadPath;
        }
    }

    public final List<Lump> lumps = new ArrayList<>();
    private final Map<String, Integer> index = new HashMap<>();

    public void addFile(String filePath)
    {
        Path absolute = Path.of(filePath).toAbsolutePath().normalize();
        if (!absolute.toFile().isFile()) {
            throw new RuntimeException("WAD not found: " + filePath);
        }
        String path = absolute.toString();
        try (RandomAccessFile file = new RandomAccessFile(path, "r")) {
            byte[] header = new byte[12];
            if (file.read(header) != 12) {
                throw new RuntimeException("invalid WAD header: " + path);
            }
            String ident = Bin.latin1(header, 0, 4);
            if (!ident.equals("IWAD") && !ident.equals("PWAD")) {
                throw new RuntimeException("not a WAD: " + path);
            }
            int numLumps = Bin.u32(header, 4);
            int infoTableOfs = Bin.u32(header, 8);
            byte[] directory = new byte[numLumps * 16];
            if (numLumps > 0) {
                file.seek(infoTableOfs & 0xffffffffL);
                if (file.read(directory) != directory.length) {
                    throw new RuntimeException("truncated WAD directory: " + path);
                }
            }
            int start = lumps.size();
            for (int i = 0; i < numLumps; i++) {
                int off = i * 16;
                lumps.add(new Lump(Bin.name8(directory, off + 8), Bin.u32(directory, off), Bin.u32(directory, off + 4), path));
            }
            for (int i = start; i < lumps.size(); i++) {
                index.put(lumps.get(i).name, i);
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public int numLumps()
    {
        return lumps.size();
    }

    public int checkNumForName(String name)
    {
        int nul = name.indexOf('\0');
        if (nul >= 0) {
            name = name.substring(0, nul);
        }
        String key = name.replaceAll(" +$", "").toUpperCase();
        if (key.length() > 8) {
            key = key.substring(0, 8);
        }
        Integer n = index.get(key);
        return n == null ? -1 : n;
    }

    public int getNumForName(String name)
    {
        int n = checkNumForName(name);
        if (n < 0) {
            throw new RuntimeException("lump not found: " + name);
        }
        return n;
    }

    public int lumpLength(int num)
    {
        return lumps.get(num).size;
    }

    public byte[] cacheLumpNum(int num)
    {
        Lump lump = lumps.get(num);
        if (lump.cache == null) {
            byte[] buf = new byte[lump.size];
            if (lump.size > 0) {
                try (RandomAccessFile file = new RandomAccessFile(lump.wadPath, "r")) {
                    file.seek(lump.position & 0xffffffffL);
                    if (file.read(buf) != lump.size) {
                        throw new RuntimeException("truncated lump: " + lump.name);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
            lump.cache = buf;
        }
        return lump.cache;
    }

    public byte[] cacheLumpName(String name)
    {
        return cacheLumpNum(getNumForName(name));
    }

    public String lumpName(int num)
    {
        return lumps.get(num).name;
    }
}
