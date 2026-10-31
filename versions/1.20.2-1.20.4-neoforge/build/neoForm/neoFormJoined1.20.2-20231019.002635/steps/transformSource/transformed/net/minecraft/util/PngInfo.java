package net.minecraft.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public record PngInfo(int width, int height) {
   private static final long PNG_HEADER = -8552249625308161526L;
   private static final int IHDR_TYPE = 1229472850;
   private static final int IHDR_SIZE = 13;

   public static PngInfo fromStream(InputStream p_302371_) throws IOException {
      DataInputStream datainputstream = new DataInputStream(p_302371_);
      if (datainputstream.readLong() != -8552249625308161526L) {
         throw new IOException("Bad PNG Signature");
      } else if (datainputstream.readInt() != 13) {
         throw new IOException("Bad length for IHDR chunk!");
      } else if (datainputstream.readInt() != 1229472850) {
         throw new IOException("Bad type for IHDR chunk!");
      } else {
         int i = datainputstream.readInt();
         int j = datainputstream.readInt();
         return new PngInfo(i, j);
      }
   }

   public static PngInfo fromBytes(byte[] p_302374_) throws IOException {
      return fromStream(new ByteArrayInputStream(p_302374_));
   }
}
