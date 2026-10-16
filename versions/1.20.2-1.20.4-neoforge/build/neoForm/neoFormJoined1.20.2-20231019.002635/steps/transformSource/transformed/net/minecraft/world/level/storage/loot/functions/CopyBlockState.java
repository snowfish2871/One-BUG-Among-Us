package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class CopyBlockState extends LootItemConditionalFunction {
   public static final Codec<CopyBlockState> CODEC = RecordCodecBuilder.create(
      p_298063_ -> commonFields(p_298063_)
            .and(
               p_298063_.group(
                  BuiltInRegistries.BLOCK.holderByNameCodec().fieldOf("block").forGetter(p_298064_ -> p_298064_.block),
                  Codec.STRING.listOf().fieldOf("properties").forGetter(p_298062_ -> p_298062_.properties.stream().map(Property::getName).toList())
               )
            )
            .apply(p_298063_, CopyBlockState::new)
   );
   private final Holder<Block> block;
   private final Set<Property<?>> properties;

   CopyBlockState(List<LootItemCondition> p_298625_, Holder<Block> p_298940_, Set<Property<?>> p_80052_) {
      super(p_298625_);
      this.block = p_298940_;
      this.properties = p_80052_;
   }

   private CopyBlockState(List<LootItemCondition> p_298279_, Holder<Block> p_299047_, List<String> p_298632_) {
      this(
         p_298279_, p_299047_, p_298632_.stream().map(p_299047_.value().getStateDefinition()::getProperty).filter(Objects::nonNull).collect(Collectors.toSet())
      );
   }

   @Override
   public LootItemFunctionType getType() {
      return LootItemFunctions.COPY_STATE;
   }

   @Override
   public Set<LootContextParam<?>> getReferencedContextParams() {
      return ImmutableSet.of(LootContextParams.BLOCK_STATE);
   }

   @Override
   protected ItemStack run(ItemStack p_80060_, LootContext p_80061_) {
      BlockState blockstate = p_80061_.getParamOrNull(LootContextParams.BLOCK_STATE);
      if (blockstate != null) {
         CompoundTag compoundtag = p_80060_.getOrCreateTag();
         CompoundTag compoundtag1;
         if (compoundtag.contains("BlockStateTag", 10)) {
            compoundtag1 = compoundtag.getCompound("BlockStateTag");
         } else {
            compoundtag1 = new CompoundTag();
            compoundtag.put("BlockStateTag", compoundtag1);
         }

         for(Property<?> property : this.properties) {
            if (blockstate.hasProperty(property)) {
               compoundtag1.putString(property.getName(), serialize(blockstate, property));
            }
         }
      }

      return p_80060_;
   }

   public static CopyBlockState.Builder copyState(Block p_80063_) {
      return new CopyBlockState.Builder(p_80063_);
   }

   private static <T extends Comparable<T>> String serialize(BlockState p_80065_, Property<T> p_80066_) {
      T t = p_80065_.getValue(p_80066_);
      return p_80066_.getName(t);
   }

   public static class Builder extends LootItemConditionalFunction.Builder<CopyBlockState.Builder> {
      private final Holder<Block> block;
      private final ImmutableSet.Builder<Property<?>> properties = ImmutableSet.builder();

      Builder(Block p_80079_) {
         this.block = p_80079_.builtInRegistryHolder();
      }

      public CopyBlockState.Builder copy(Property<?> p_80085_) {
         if (!this.block.value().getStateDefinition().getProperties().contains(p_80085_)) {
            throw new IllegalStateException("Property " + p_80085_ + " is not present on block " + this.block);
         } else {
            this.properties.add(p_80085_);
            return this;
         }
      }

      protected CopyBlockState.Builder getThis() {
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new CopyBlockState(this.getConditions(), this.block, this.properties.build());
      }
   }
}
