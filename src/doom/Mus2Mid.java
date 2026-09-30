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

/** MUS lump -> Standard MIDI File (Chocolate Doom mus2mid.c). */
public final class Mus2Mid
{
    public final List<Integer> body = new ArrayList<>();
    public int queued;
    public int tracksize;
    public final int[] velocities = new int[16];
    public final int[] channelMap = new int[16];

    public Mus2Mid()
    {
        java.util.Arrays.fill(velocities, 127);
        java.util.Arrays.fill(channelMap, -1);
    }

    public void writeTime(int time)
    {
        int buffer = time & 0x7f;
        int working = time;
        while ((working >>= 7) != 0) {
            buffer <<= 8;
            buffer |= (working & 0x7f) | 0x80;
        }
        while (true) {
            body.add(buffer & 0xff);
            ++tracksize;
            if ((buffer & 0x80) != 0) {
                buffer >>= 8;
            } else {
                queued = 0;
                return;
            }
        }
    }

    public void write(int... data)
    {
        writeTime(queued);
        for (int b : data) {
            body.add(b & 0xff);
        }
        tracksize += data.length;
    }

    private int allocateChannel()
    {
        int result = -1;
        for (int mapped : channelMap) {
            if (mapped > result) {
                result = mapped;
            }
        }
        result += 1;
        if (result == 9) {
            ++result;
        }
        return result;
    }

    public int midiChannel(int musChannel)
    {
        if (musChannel == 15) {
            return 9;
        }
        if (channelMap[musChannel] == -1) {
            channelMap[musChannel] = allocateChannel();
            int channel = channelMap[musChannel];
            write(0xb0 | channel, 0x7b, 0);
        }
        return channelMap[musChannel];
    }

    public static byte[] convert(byte[] mus)
    {
        if (mus.length >= 4 && Bin.latin1(mus, 0, 4).equals("MThd")) {
            return mus;
        }
        if (mus.length < 16 || !Bin.latin1(mus, 0, 4).equals("MUS\u001a")) {
            return null;
        }

        int[] controllerMap = {
            0x00, 0x20, 0x01, 0x07, 0x0a, 0x0b, 0x5b, 0x5d, 0x40, 0x43, 0x78, 0x7b, 0x7e, 0x7f, 0x79,
        };
        byte[] midiHeader = {
            0x4d, 0x54, 0x68, 0x64, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, 0x01, 0x00, 0x46, 0x4d, 0x54,
            0x72, 0x6b, 0x00, 0x00, 0x00, 0x00,
        };
        int[] position = { Bin.u16(mus, 6) };
        int length = mus.length;
        Mus2Mid output = new Mus2Mid();
        boolean[] hitScoreEnd = { false };

        java.util.function.Supplier<Integer> readU8 = () -> {
            if (position[0] >= length) {
                return null;
            }
            return Bin.u8(mus, position[0]++);
        };

        while (!hitScoreEnd[0]) {
            inner:
            while (!hitScoreEnd[0]) {
                Integer descriptorObj = readU8.get();
                if (descriptorObj == null) {
                    return null;
                }
                int descriptor = descriptorObj;
                int channel = output.midiChannel(descriptor & 0x0f);
                int event = descriptor & 0x70;
                switch (event) {
                    case 0x00: {
                        Integer key = readU8.get();
                        if (key == null) {
                            return null;
                        }
                        output.write(0x80 | channel, key & 0x7f, 0);
                        break;
                    }
                    case 0x10: {
                        Integer key = readU8.get();
                        if (key == null) {
                            return null;
                        }
                        if ((key & 0x80) != 0) {
                            Integer velocity = readU8.get();
                            if (velocity == null) {
                                return null;
                            }
                            output.velocities[channel] = velocity & 0x7f;
                        }
                        output.write(0x90 | channel, key & 0x7f, output.velocities[channel]);
                        break;
                    }
                    case 0x20: {
                        Integer key = readU8.get();
                        if (key == null) {
                            break inner;
                        }
                        int wheel = key * 64;
                        output.write(0xe0 | channel, wheel & 0x7f, (wheel >> 7) & 0x7f);
                        break;
                    }
                    case 0x30: {
                        Integer controller = readU8.get();
                        if (controller == null || controller < 10 || controller > 14) {
                            return null;
                        }
                        output.write(0xb0 | channel, controllerMap[controller], 0);
                        break;
                    }
                    case 0x40: {
                        Integer controller = readU8.get();
                        Integer value = readU8.get();
                        if (controller == null || value == null) {
                            return null;
                        }
                        if (controller == 0) {
                            output.write(0xc0 | channel, value & 0x7f);
                        } else {
                            if (controller < 1 || controller > 9) {
                                return null;
                            }
                            int working = (value & 0x80) != 0 ? 0x7f : value;
                            output.write(0xb0 | channel, controllerMap[controller], working);
                        }
                        break;
                    }
                    case 0x60:
                        hitScoreEnd[0] = true;
                        break;
                    default:
                        return null;
                }
                if ((descriptor & 0x80) != 0) {
                    break inner;
                }
            }

            if (!hitScoreEnd[0]) {
                int timeDelay = 0;
                Integer working;
                do {
                    working = readU8.get();
                    if (working == null) {
                        return null;
                    }
                    timeDelay = timeDelay * 128 + (working & 0x7f);
                } while ((working & 0x80) != 0);
                output.queued += timeDelay;
            }
        }

        output.writeTime(output.queued);
        output.body.add(0xff);
        output.body.add(0x2f);
        output.body.add(0x00);
        output.tracksize += 3;
        midiHeader[18] = (byte) ((output.tracksize >> 24) & 0xff);
        midiHeader[19] = (byte) ((output.tracksize >> 16) & 0xff);
        midiHeader[20] = (byte) ((output.tracksize >> 8) & 0xff);
        midiHeader[21] = (byte) (output.tracksize & 0xff);
        byte[] out = new byte[midiHeader.length + output.body.size()];
        System.arraycopy(midiHeader, 0, out, 0, midiHeader.length);
        for (int i = 0; i < output.body.size(); i++) {
            out[midiHeader.length + i] = (byte) (output.body.get(i) & 0xff);
        }
        return out;
    }
}
