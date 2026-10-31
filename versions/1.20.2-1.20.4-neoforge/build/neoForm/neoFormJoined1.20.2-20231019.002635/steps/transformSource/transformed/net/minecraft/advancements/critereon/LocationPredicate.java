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
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.levelgen.structure.Structure;

public record LocationPredicate(
   Optional<LocationPredicate.PositionPredicate> position,
   Optional<ResourceKey<Biome>> biome,
   Optional<ResourceKey<Structure>> structure,
   Optional<ResourceKey<Level>> dimension,
   Optional<Boolean> smokey,
   Optional<LightPredicate> light,
   Optional<BlockPredicate> block,
   Optional<FluidPredicate> fluid
) {
   public static final Codec<LocationPredicate> CODEC = RecordCodecBuilder.create(
      p_297907_ -> p_297907_.group(
               ExtraCodecs.strictOptionalField(LocationPredicate.PositionPredicate.CODEC, "position").forGetter(LocationPredicate::position),
               ExtraCodecs.strictOptionalField(ResourceKey.codec(Registries.BIOME), "biome").forGetter(LocationPredicate::biome),
               ExtraCodecs.strictOptionalField(ResourceKey.codec(Registries.STRUCTURE), "structure").forGetter(LocationPredicate::structure),
               ExtraCodecs.strictOptionalField(ResourceKey.codec(Registries.DIMENSION), "dimension").forGetter(LocationPredicate::dimension),
               ExtraCodecs.strictOptionalField(Codec.BOOL, "smokey").forGetter(LocationPredicate::smokey),
               ExtraCodecs.strictOptionalField(LightPredicate.CODEC, "light").forGetter(LocationPredicate::light),
               ExtraCodecs.strictOptionalField(BlockPredicate.CODEC, "block").forGetter(LocationPredicate::block),
               ExtraCodecs.strictOptionalField(FluidPredicate.CODEC, "fluid").forGetter(LocationPredicate::fluid)
            )
            .apply(p_297907_, LocationPredicate::new)
   );

   private static Optional<LocationPredicate> of(
      Optional<LocationPredicate.PositionPredicate> p_299175_,
      Optional<ResourceKey<Biome>> p_298741_,
      Optional<ResourceKey<Structure>> p_298654_,
      Optional<ResourceKey<Level>> p_298829_,
      Optional<Boolean> p_299313_,
      Optional<LightPredicate> p_299195_,
      Optional<BlockPredicate> p_299295_,
      Optional<FluidPredicate> p_298350_
   ) {
      return p_299175_.isEmpty()
            && p_298741_.isEmpty()
            && p_298654_.isEmpty()
            && p_298829_.isEmpty()
            && p_299313_.isEmpty()
            && p_299195_.isEmpty()
            && p_299295_.isEmpty()
            && p_298350_.isEmpty()
         ? Optional.empty()
         : Optional.of(new LocationPredicate(p_299175_, p_298741_, p_298654_, p_298829_, p_299313_, p_299195_, p_299295_, p_298350_));
   }

   public boolean matches(ServerLevel p_52618_, double p_52619_, double p_52620_, double p_52621_) {
      if (this.position.isPresent() && !this.position.get().matches(p_52619_, p_52620_, p_52621_)) {
         return false;
      } else if (this.dimension.isPresent() && this.dimension.get() != p_52618_.dimension()) {
         return false;
      } else {
         BlockPos blockpos = BlockPos.containing(p_52619_, p_52620_, p_52621_);
         boolean flag = p_52618_.isLoaded(blockpos);
         if (!this.biome.isPresent() || flag && p_52618_.getBiome(blockpos).is(this.biome.get())) {
            if (!this.structure.isPresent() || flag && p_52618_.structureManager().getStructureWithPieceAt(blockpos, this.structure.get()).isValid()) {
               if (!this.smokey.isPresent() || flag && this.smokey.get() == CampfireBlock.isSmokeyPos(p_52618_, blockpos)) {
                  if (this.light.isPresent() && !this.light.get().matches(p_52618_, blockpos)) {
                     return false;
                  } else if (this.block.isPresent() && !this.block.get().matches(p_52618_, blockpos)) {
                     return false;
                  } else {
                     return !this.fluid.isPresent() || this.fluid.get().matches(p_52618_, blockpos);
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   public JsonElement serializeToJson() {
      return Util.getOrThrow(CODEC.encodeStart(JsonOps.INSTANCE, this), IllegalStateException::new);
   }

   public static Optional<LocationPredicate> fromJson(@Nullable JsonElement p_52630_) {
      return p_52630_ != null && !p_52630_.isJsonNull()
         ? Optional.of(Util.getOrThrow(CODEC.parse(JsonOps.INSTANCE, p_52630_), JsonParseException::new))
         : Optional.empty();
   }

   public static class Builder {
      private MinMaxBounds.Doubles x = MinMaxBounds.Doubles.ANY;
      private MinMaxBounds.Doubles y = MinMaxBounds.Doubles.ANY;
      private MinMaxBounds.Doubles z = MinMaxBounds.Doubles.ANY;
      private Optional<ResourceKey<Biome>> biome = Optional.empty();
      private Optional<ResourceKey<Structure>> structure = Optional.empty();
      private Optional<ResourceKey<Level>> dimension = Optional.empty();
      private Optional<Boolean> smokey = Optional.empty();
      private Optional<LightPredicate> light = Optional.empty();
      private Optional<BlockPredicate> block = Optional.empty();
      private Optional<FluidPredicate> fluid = Optional.empty();

      public static LocationPredicate.Builder location() {
         return new LocationPredicate.Builder();
      }

      public static LocationPredicate.Builder inBiome(ResourceKey<Biome> p_299054_) {
         return location().setBiome(p_299054_);
      }

      public static LocationPredicate.Builder inDimension(ResourceKey<Level> p_298871_) {
         return location().setDimension(p_298871_);
      }

      public static LocationPredicate.Builder inStructure(ResourceKey<Structure> p_298413_) {
         return location().setStructure(p_298413_);
      }

      public static LocationPredicate.Builder atYLocation(MinMaxBounds.Doubles p_298783_) {
         return location().setY(p_298783_);
      }

      public LocationPredicate.Builder setX(MinMaxBounds.Doubles p_153971_) {
         this.x = p_153971_;
         return this;
      }

      public LocationPredicate.Builder setY(MinMaxBounds.Doubles p_153975_) {
         this.y = p_153975_;
         return this;
      }

      public LocationPredicate.Builder setZ(MinMaxBounds.Doubles p_153979_) {
         this.z = p_153979_;
         return this;
      }

      public LocationPredicate.Builder setBiome(ResourceKey<Biome> p_52657_) {
         this.biome = Optional.of(p_52657_);
         return this;
      }

      public LocationPredicate.Builder setStructure(ResourceKey<Structure> p_220593_) {
         this.structure = Optional.of(p_220593_);
         return this;
      }

      public LocationPredicate.Builder setDimension(ResourceKey<Level> p_153977_) {
         this.dimension = Optional.of(p_153977_);
         return this;
      }

      public LocationPredicate.Builder setLight(LightPredicate.Builder p_298888_) {
         this.light = Optional.of(p_298888_.build());
         return this;
      }

      public LocationPredicate.Builder setBlock(BlockPredicate.Builder p_298335_) {
         this.block = Optional.of(p_298335_.build());
         return this;
      }

      public LocationPredicate.Builder setFluid(FluidPredicate.Builder p_299307_) {
         this.fluid = Optional.of(p_299307_.build());
         return this;
      }

      public LocationPredicate.Builder setSmokey(boolean p_299155_) {
         this.smokey = Optional.of(p_299155_);
         return this;
      }

      public LocationPredicate build() {
         Optional<LocationPredicate.PositionPredicate> optional = LocationPredicate.PositionPredicate.of(this.x, this.y, this.z);
         return new LocationPredicate(optional, this.biome, this.structure, this.dimension, this.smokey, this.light, this.block, this.fluid);
      }
   }

   static record PositionPredicate(MinMaxBounds.Doubles x, MinMaxBounds.Doubles y, MinMaxBounds.Doubles z) {
      public static final Codec<LocationPredicate.PositionPredicate> CODEC = RecordCodecBuilder.create(
         p_299107_ -> p_299107_.group(
                  ExtraCodecs.strictOptionalField(MinMaxBounds.Doubles.CODEC, "x", MinMaxBounds.Doubles.ANY).forGetter(LocationPredicate.PositionPredicate::x),
                  ExtraCodecs.strictOptionalField(MinMaxBounds.Doubles.CODEC, "y", MinMaxBounds.Doubles.ANY).forGetter(LocationPredicate.PositionPredicate::y),
                  ExtraCodecs.strictOptionalField(MinMaxBounds.Doubles.CODEC, "z", MinMaxBounds.Doubles.ANY).forGetter(LocationPredicate.PositionPredicate::z)
               )
               .apply(p_299107_, LocationPredicate.PositionPredicate::new)
      );

      static Optional<LocationPredicate.PositionPredicate> of(MinMaxBounds.Doubles p_298771_, MinMaxBounds.Doubles p_298418_, MinMaxBounds.Doubles p_299133_) {
         return p_298771_.isAny() && p_298418_.isAny() && p_299133_.isAny()
            ? Optional.empty()
            : Optional.of(new LocationPredicate.PositionPredicate(p_298771_, p_298418_, p_299133_));
      }

      public boolean matches(double p_298782_, double p_299123_, double p_298955_) {
         return this.x.matches(p_298782_) && this.y.matches(p_299123_) && this.z.matches(p_298955_);
      }
   }
}
