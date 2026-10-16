package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

public record FluidPredicate(Optional<TagKey<Fluid>> tag, Optional<Holder<Fluid>> fluid, Optional<StatePropertiesPredicate> properties) {
   public static final Codec<FluidPredicate> CODEC = RecordCodecBuilder.create(
      p_299184_ -> p_299184_.group(
               ExtraCodecs.strictOptionalField(TagKey.codec(Registries.FLUID), "tag").forGetter(FluidPredicate::tag),
               ExtraCodecs.strictOptionalField(BuiltInRegistries.FLUID.holderByNameCodec(), "fluid").forGetter(FluidPredicate::fluid),
               ExtraCodecs.strictOptionalField(StatePropertiesPredicate.CODEC, "state").forGetter(FluidPredicate::properties)
            )
            .apply(p_299184_, FluidPredicate::new)
   );

   public boolean matches(ServerLevel p_41105_, BlockPos p_41106_) {
      if (!p_41105_.isLoaded(p_41106_)) {
         return false;
      } else {
         FluidState fluidstate = p_41105_.getFluidState(p_41106_);
         if (this.tag.isPresent() && !fluidstate.is(this.tag.get())) {
            return false;
         } else if (this.fluid.isPresent() && !fluidstate.is(this.fluid.get().value())) {
            return false;
         } else {
            return !this.properties.isPresent() || this.properties.get().matches(fluidstate);
         }
      }
   }

   public static class Builder {
      private Optional<Holder<Fluid>> fluid = Optional.empty();
      private Optional<TagKey<Fluid>> fluids = Optional.empty();
      private Optional<StatePropertiesPredicate> properties = Optional.empty();

      private Builder() {
      }

      public static FluidPredicate.Builder fluid() {
         return new FluidPredicate.Builder();
      }

      public FluidPredicate.Builder of(Fluid p_151172_) {
         this.fluid = Optional.of(p_151172_.builtInRegistryHolder());
         return this;
      }

      public FluidPredicate.Builder of(TagKey<Fluid> p_204106_) {
         this.fluids = Optional.of(p_204106_);
         return this;
      }

      public FluidPredicate.Builder setProperties(StatePropertiesPredicate p_151170_) {
         this.properties = Optional.of(p_151170_);
         return this;
      }

      public FluidPredicate build() {
         return new FluidPredicate(this.fluids, this.fluid, this.properties);
      }
   }
}
