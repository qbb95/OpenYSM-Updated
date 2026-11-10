import io.netty.buffer.Unpooled;
import java.util.Arrays;
import rip.ysm.security.YSMByteBuf;

public class WireBoundsSmoke {
    public static void main(String[] args) {
        try (var buffer = new YSMByteBuf(Unpooled.buffer())) {
            buffer.writeString("modèle UTF-8");
            buffer.writeByteArray(new byte[] {1,2,3});
            if (!buffer.readString().equals("modèle UTF-8")) throw new AssertionError();
            if (!Arrays.equals(buffer.readByteArray(),new byte[] {1,2,3})) throw new AssertionError();
        }
        int rejected = 0;
        for (int length : new int[] {-1, Integer.MAX_VALUE, 3}) {
            for (boolean string : new boolean[] {false,true}) {
                try (var buffer = new YSMByteBuf(Unpooled.buffer())) {
                    buffer.writeVarInt(length);
                    buffer.writeByte((byte) 1);
                    try {
                        if (string) buffer.readString(); else buffer.readByteArray();
                        throw new AssertionError("Accepted invalid length " + length);
                    } catch (IllegalArgumentException expected) {
                        rejected++;
                    }
                }
            }
        }
        System.out.println("PASS: UTF-8/bytes roundtrip and six invalid lengths rejected before allocation");
    }
}
