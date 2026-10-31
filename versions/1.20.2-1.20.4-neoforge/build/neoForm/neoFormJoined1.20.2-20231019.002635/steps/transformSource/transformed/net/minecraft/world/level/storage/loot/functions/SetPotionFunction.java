package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetPotionFunction extends LootItemConditionalFunction {
   public static final Codec<SetPotionFunction> CODEC = RecordCodecBuilder.create(
      p_298159_ -> commonFields(p_298159_)
            .and(BuiltInRegistries.POTION.holderByNameCodec().fieldOf("id").forGetter(p_298158_ -> p_298158_.potion))
            .apply(p_298159_, SetPotionFunction::new)
   );
   private final Holder<Potion> potion;

   private SetPotionFunction(List<LootItemCondition> p_299010_, Holder<Potion> p_298587_) {
      super(p_299010_);
      this.potion = p_298587_;
   }

   @Override
   public LootItemFunctionType getType() {
      return LootItemFunctions.SET_POTION;
   }

   @Override
   public ItemStack run(ItemStack p_193073_, LootContext p_193074_) {
      PotionUtils.setPotion(p_193073_, this.potion.value());
      return p_193073_;
   }

   public static LootItemConditionalFunction.Builder<?> setPotion(Potion p_193076_) {
      return simpleBuilder(p_298161_ -> new SetPotionFunction(p_298161_, p_193076_.builtInRegistryHolder()));
   }
}
