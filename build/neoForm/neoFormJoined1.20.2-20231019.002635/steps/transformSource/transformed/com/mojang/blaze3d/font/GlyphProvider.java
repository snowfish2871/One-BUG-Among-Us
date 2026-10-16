package com.mojang.blaze3d.font;

import it.unimi.dsi.fastutil.ints.IntSet;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface GlyphProvider extends AutoCloseable {
   @Override
   default void close() {
   }

   @Nullable
   default GlyphInfo getGlyph(int p_231091_) {
      return null;
   }

   IntSet getSupportedGlyphs();
}
