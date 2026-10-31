package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.loot.LootContext;

public record WeatherCheck(Optional<Boolean> isRaining, Optional<Boolean> isThundering) implements LootItemCondition {
   public static final Codec<WeatherCheck> CODEC = RecordCodecBuilder.create(
      p_299078_ -> p_299078_.group(
               ExtraCodecs.strictOptionalField(Codec.BOOL, "raining").forGetter(WeatherCheck::isRaining),
               ExtraCodecs.strictOptionalField(Codec.BOOL, "thundering").forGetter(WeatherCheck::isThundering)
            )
            .apply(p_299078_, WeatherCheck::new)
   );

   @Override
   public LootItemConditionType getType() {
      return LootItemConditions.WEATHER_CHECK;
   }

   public boolean test(LootContext p_82066_) {
      ServerLevel serverlevel = p_82066_.getLevel();
      if (this.isRaining.isPresent() && this.isRaining.get() != serverlevel.isRaining()) {
         return false;
      } else {
         return !this.isThundering.isPresent() || this.isThundering.get() == serverlevel.isThundering();
      }
   }

   public static WeatherCheck.Builder weather() {
      return new WeatherCheck.Builder();
   }

   public static class Builder implements LootItemCondition.Builder {
      private Optional<Boolean> isRaining = Optional.empty();
      private Optional<Boolean> isThundering = Optional.empty();

      public WeatherCheck.Builder setRaining(boolean p_299240_) {
         this.isRaining = Optional.of(p_299240_);
         return this;
      }

      public WeatherCheck.Builder setThundering(boolean p_298912_) {
         this.isThundering = Optional.of(p_298912_);
         return this;
      }

      public WeatherCheck build() {
         return new WeatherCheck(this.isRaining, this.isThundering);
      }
   }
}
