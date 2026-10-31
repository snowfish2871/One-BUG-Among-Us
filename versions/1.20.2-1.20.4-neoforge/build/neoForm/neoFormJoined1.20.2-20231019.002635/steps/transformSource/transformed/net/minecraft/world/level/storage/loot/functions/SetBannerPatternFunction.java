package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetBannerPatternFunction extends LootItemConditionalFunction {
   private static final Codec<Pair<Holder<BannerPattern>, DyeColor>> PATTERN_CODEC = Codec.mapPair(
         BuiltInRegistries.BANNER_PATTERN.holderByNameCodec().fieldOf("pattern"), DyeColor.CODEC.fieldOf("color")
      )
      .codec();
   public static final Codec<SetBannerPatternFunction> CODEC = RecordCodecBuilder.create(
      p_299105_ -> commonFields(p_299105_)
            .and(
               p_299105_.group(
                  PATTERN_CODEC.listOf().fieldOf("patterns").forGetter(p_298395_ -> p_298395_.patterns),
                  Codec.BOOL.fieldOf("append").forGetter(p_298522_ -> p_298522_.append)
               )
            )
            .apply(p_299105_, SetBannerPatternFunction::new)
   );
   private final List<Pair<Holder<BannerPattern>, DyeColor>> patterns;
   private final boolean append;

   SetBannerPatternFunction(List<LootItemCondition> p_165276_, List<Pair<Holder<BannerPattern>, DyeColor>> p_299284_, boolean p_165277_) {
      super(p_165276_);
      this.patterns = p_299284_;
      this.append = p_165277_;
   }

   @Override
   protected ItemStack run(ItemStack p_165280_, LootContext p_165281_) {
      CompoundTag compoundtag = BlockItem.getBlockEntityData(p_165280_);
      if (compoundtag == null) {
         compoundtag = new CompoundTag();
      }

      BannerPattern.Builder bannerpattern$builder = new BannerPattern.Builder();
      this.patterns.forEach(bannerpattern$builder::addPattern);
      ListTag listtag = bannerpattern$builder.toListTag();
      ListTag listtag1;
      if (this.append) {
         listtag1 = compoundtag.getList("Patterns", 10).copy();
         listtag1.addAll(listtag);
      } else {
         listtag1 = listtag;
      }

      compoundtag.put("Patterns", listtag1);
      BlockItem.setBlockEntityData(p_165280_, BlockEntityType.BANNER, compoundtag);
      return p_165280_;
   }

   @Override
   public LootItemFunctionType getType() {
      return LootItemFunctions.SET_BANNER_PATTERN;
   }

   public static SetBannerPatternFunction.Builder setBannerPattern(boolean p_165283_) {
      return new SetBannerPatternFunction.Builder(p_165283_);
   }

   public static class Builder extends LootItemConditionalFunction.Builder<SetBannerPatternFunction.Builder> {
      private final ImmutableList.Builder<Pair<Holder<BannerPattern>, DyeColor>> patterns = ImmutableList.builder();
      private final boolean append;

      Builder(boolean p_165287_) {
         this.append = p_165287_;
      }

      protected SetBannerPatternFunction.Builder getThis() {
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new SetBannerPatternFunction(this.getConditions(), this.patterns.build(), this.append);
      }

      public SetBannerPatternFunction.Builder addPattern(ResourceKey<BannerPattern> p_230996_, DyeColor p_230997_) {
         return this.addPattern(BuiltInRegistries.BANNER_PATTERN.getHolderOrThrow(p_230996_), p_230997_);
      }

      public SetBannerPatternFunction.Builder addPattern(Holder<BannerPattern> p_230999_, DyeColor p_231000_) {
         this.patterns.add(Pair.of(p_230999_, p_231000_));
         return this;
      }
   }
}
