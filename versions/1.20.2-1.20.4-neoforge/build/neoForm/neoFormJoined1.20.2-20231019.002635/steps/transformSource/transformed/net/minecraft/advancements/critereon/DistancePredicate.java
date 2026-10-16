package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;

public record DistancePredicate(
   MinMaxBounds.Doubles x, MinMaxBounds.Doubles y, MinMaxBounds.Doubles z, MinMaxBounds.Doubles horizontal, MinMaxBounds.Doubles absolute
) {
   public static final Codec<DistancePredicate> CODEC = RecordCodecBuilder.create(
      p_298498_ -> p_298498_.group(
               ExtraCodecs.strictOptionalField(MinMaxBounds.Doubles.CODEC, "x", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::x),
               ExtraCodecs.strictOptionalField(MinMaxBounds.Doubles.CODEC, "y", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::y),
               ExtraCodecs.strictOptionalField(MinMaxBounds.Doubles.CODEC, "z", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::z),
               ExtraCodecs.strictOptionalField(MinMaxBounds.Doubles.CODEC, "horizontal", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::horizontal),
               ExtraCodecs.strictOptionalField(MinMaxBounds.Doubles.CODEC, "absolute", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::absolute)
            )
            .apply(p_298498_, DistancePredicate::new)
   );

   public static DistancePredicate horizontal(MinMaxBounds.Doubles p_148837_) {
      return new DistancePredicate(MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, p_148837_, MinMaxBounds.Doubles.ANY);
   }

   public static DistancePredicate vertical(MinMaxBounds.Doubles p_148839_) {
      return new DistancePredicate(MinMaxBounds.Doubles.ANY, p_148839_, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY);
   }

   public static DistancePredicate absolute(MinMaxBounds.Doubles p_148841_) {
      return new DistancePredicate(MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, p_148841_);
   }

   public boolean matches(double p_26256_, double p_26257_, double p_26258_, double p_26259_, double p_26260_, double p_26261_) {
      float f = (float)(p_26256_ - p_26259_);
      float f1 = (float)(p_26257_ - p_26260_);
      float f2 = (float)(p_26258_ - p_26261_);
      if (!this.x.matches((double)Mth.abs(f)) || !this.y.matches((double)Mth.abs(f1)) || !this.z.matches((double)Mth.abs(f2))) {
         return false;
      } else if (!this.horizontal.matchesSqr((double)(f * f + f2 * f2))) {
         return false;
      } else {
         return this.absolute.matchesSqr((double)(f * f + f1 * f1 + f2 * f2));
      }
   }

   public static Optional<DistancePredicate> fromJson(@Nullable JsonElement p_26265_) {
      return p_26265_ != null && !p_26265_.isJsonNull()
         ? Optional.of(Util.getOrThrow(CODEC.parse(JsonOps.INSTANCE, p_26265_), JsonParseException::new))
         : Optional.empty();
   }

   public JsonElement serializeToJson() {
      return Util.getOrThrow(CODEC.encodeStart(JsonOps.INSTANCE, this), IllegalStateException::new);
   }
}
