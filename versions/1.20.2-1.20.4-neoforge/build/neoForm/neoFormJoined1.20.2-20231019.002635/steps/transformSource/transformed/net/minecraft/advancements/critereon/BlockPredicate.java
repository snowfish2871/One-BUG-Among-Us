package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public record BlockPredicate(
   Optional<TagKey<Block>> tag, Optional<HolderSet<Block>> blocks, Optional<StatePropertiesPredicate> properties, Optional<NbtPredicate> nbt
) {
   private static final Codec<HolderSet<Block>> BLOCKS_CODEC = BuiltInRegistries.BLOCK
      .holderByNameCodec()
      .listOf()
      .xmap(HolderSet::direct, p_297883_ -> p_297883_.stream().toList());
   public static final Codec<BlockPredicate> CODEC = RecordCodecBuilder.create(
      p_297882_ -> p_297882_.group(
               ExtraCodecs.strictOptionalField(TagKey.codec(Registries.BLOCK), "tag").forGetter(BlockPredicate::tag),
               ExtraCodecs.strictOptionalField(BLOCKS_CODEC, "blocks").forGetter(BlockPredicate::blocks),
               ExtraCodecs.strictOptionalField(StatePropertiesPredicate.CODEC, "state").forGetter(BlockPredicate::properties),
               ExtraCodecs.strictOptionalField(NbtPredicate.CODEC, "nbt").forGetter(BlockPredicate::nbt)
            )
            .apply(p_297882_, BlockPredicate::new)
   );

   public boolean matches(ServerLevel p_17915_, BlockPos p_17916_) {
      if (!p_17915_.isLoaded(p_17916_)) {
         return false;
      } else {
         BlockState blockstate = p_17915_.getBlockState(p_17916_);
         if (this.tag.isPresent() && !blockstate.is(this.tag.get())) {
            return false;
         } else if (this.blocks.isPresent() && !blockstate.is(this.blocks.get())) {
            return false;
         } else if (this.properties.isPresent() && !this.properties.get().matches(blockstate)) {
            return false;
         } else {
            if (this.nbt.isPresent()) {
               BlockEntity blockentity = p_17915_.getBlockEntity(p_17916_);
               if (blockentity == null || !this.nbt.get().matches(blockentity.saveWithFullMetadata())) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   public static class Builder {
      private Optional<HolderSet<Block>> blocks = Optional.empty();
      private Optional<TagKey<Block>> tag = Optional.empty();
      private Optional<StatePropertiesPredicate> properties = Optional.empty();
      private Optional<NbtPredicate> nbt = Optional.empty();

      private Builder() {
      }

      public static BlockPredicate.Builder block() {
         return new BlockPredicate.Builder();
      }

      public BlockPredicate.Builder of(Block... p_146727_) {
         this.blocks = Optional.of(HolderSet.direct(Block::builtInRegistryHolder, p_146727_));
         return this;
      }

      public BlockPredicate.Builder of(Collection<Block> p_298407_) {
         this.blocks = Optional.of(HolderSet.direct(Block::builtInRegistryHolder, p_298407_));
         return this;
      }

      public BlockPredicate.Builder of(TagKey<Block> p_204028_) {
         this.tag = Optional.of(p_204028_);
         return this;
      }

      public BlockPredicate.Builder hasNbt(CompoundTag p_146725_) {
         this.nbt = Optional.of(new NbtPredicate(p_146725_));
         return this;
      }

      public BlockPredicate.Builder setProperties(StatePropertiesPredicate.Builder p_298989_) {
         this.properties = p_298989_.build();
         return this;
      }

      public BlockPredicate build() {
         return new BlockPredicate(this.tag, this.blocks, this.properties, this.nbt);
      }
   }
}
