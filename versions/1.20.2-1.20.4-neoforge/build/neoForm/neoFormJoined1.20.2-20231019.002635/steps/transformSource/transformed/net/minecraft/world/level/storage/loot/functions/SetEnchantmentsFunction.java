package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class SetEnchantmentsFunction extends LootItemConditionalFunction {
   public static final Codec<SetEnchantmentsFunction> CODEC = RecordCodecBuilder.create(
      p_298119_ -> commonFields(p_298119_)
            .and(
               p_298119_.group(
                  ExtraCodecs.strictOptionalField(
                        Codec.unboundedMap(BuiltInRegistries.ENCHANTMENT.holderByNameCodec(), NumberProviders.CODEC), "enchantments", Map.of()
                     )
                     .forGetter(p_298120_ -> p_298120_.enchantments),
                  Codec.BOOL.fieldOf("add").orElse(false).forGetter(p_298121_ -> p_298121_.add)
               )
            )
            .apply(p_298119_, SetEnchantmentsFunction::new)
   );
   private final Map<Holder<Enchantment>, NumberProvider> enchantments;
   private final boolean add;

   SetEnchantmentsFunction(List<LootItemCondition> p_299192_, Map<Holder<Enchantment>, NumberProvider> p_165338_, boolean p_165339_) {
      super(p_299192_);
      this.enchantments = Map.copyOf(p_165338_);
      this.add = p_165339_;
   }

   @Override
   public LootItemFunctionType getType() {
      return LootItemFunctions.SET_ENCHANTMENTS;
   }

   @Override
   public Set<LootContextParam<?>> getReferencedContextParams() {
      return this.enchantments.values().stream().flatMap(p_279081_ -> p_279081_.getReferencedContextParams().stream()).collect(ImmutableSet.toImmutableSet());
   }

   @Override
   public ItemStack run(ItemStack p_165346_, LootContext p_165347_) {
      Object2IntMap<Enchantment> object2intmap = new Object2IntOpenHashMap<>();
      this.enchantments.forEach((p_298117_, p_298118_) -> object2intmap.put(p_298117_.value(), p_298118_.getInt(p_165347_)));
      if (p_165346_.getItem() == Items.BOOK) {
         ItemStack itemstack = new ItemStack(Items.ENCHANTED_BOOK);
         object2intmap.forEach((p_165343_, p_165344_) -> EnchantedBookItem.addEnchantment(itemstack, new EnchantmentInstance(p_165343_, p_165344_)));
         return itemstack;
      } else {
         Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(p_165346_);
         if (this.add) {
            object2intmap.forEach((p_165366_, p_165367_) -> updateEnchantment(map, p_165366_, Math.max(map.getOrDefault(p_165366_, 0) + p_165367_, 0)));
         } else {
            object2intmap.forEach((p_165361_, p_165362_) -> updateEnchantment(map, p_165361_, Math.max(p_165362_, 0)));
         }

         EnchantmentHelper.setEnchantments(map, p_165346_);
         return p_165346_;
      }
   }

   private static void updateEnchantment(Map<Enchantment, Integer> p_165356_, Enchantment p_165357_, int p_165358_) {
      if (p_165358_ == 0) {
         p_165356_.remove(p_165357_);
      } else {
         p_165356_.put(p_165357_, p_165358_);
      }
   }

   public static class Builder extends LootItemConditionalFunction.Builder<SetEnchantmentsFunction.Builder> {
      private final ImmutableMap.Builder<Holder<Enchantment>, NumberProvider> enchantments = ImmutableMap.builder();
      private final boolean add;

      public Builder() {
         this(false);
      }

      public Builder(boolean p_165372_) {
         this.add = p_165372_;
      }

      protected SetEnchantmentsFunction.Builder getThis() {
         return this;
      }

      public SetEnchantmentsFunction.Builder withEnchantment(Enchantment p_165375_, NumberProvider p_165376_) {
         this.enchantments.put(p_165375_.builtInRegistryHolder(), p_165376_);
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new SetEnchantmentsFunction(this.getConditions(), this.enchantments.build(), this.add);
      }
   }
}
