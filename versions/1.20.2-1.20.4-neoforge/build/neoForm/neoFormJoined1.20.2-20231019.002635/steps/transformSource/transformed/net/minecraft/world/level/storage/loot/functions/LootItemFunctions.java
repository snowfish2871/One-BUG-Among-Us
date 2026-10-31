package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

public class LootItemFunctions {
   public static final BiFunction<ItemStack, LootContext, ItemStack> IDENTITY = (p_80760_, p_80761_) -> p_80760_;
   private static final Codec<LootItemFunction> TYPED_CODEC = BuiltInRegistries.LOOT_FUNCTION_TYPE
      .byNameCodec()
      .dispatch("function", LootItemFunction::getType, LootItemFunctionType::codec);
   public static final Codec<LootItemFunction> CODEC = ExtraCodecs.lazyInitializedCodec(
      () -> ExtraCodecs.withAlternative(TYPED_CODEC, SequenceFunction.INLINE_CODEC)
   );
   public static final LootItemFunctionType SET_COUNT = register("set_count", SetItemCountFunction.CODEC);
   public static final LootItemFunctionType ENCHANT_WITH_LEVELS = register("enchant_with_levels", EnchantWithLevelsFunction.CODEC);
   public static final LootItemFunctionType ENCHANT_RANDOMLY = register("enchant_randomly", EnchantRandomlyFunction.CODEC);
   public static final LootItemFunctionType SET_ENCHANTMENTS = register("set_enchantments", SetEnchantmentsFunction.CODEC);
   public static final LootItemFunctionType SET_NBT = register("set_nbt", SetNbtFunction.CODEC);
   public static final LootItemFunctionType FURNACE_SMELT = register("furnace_smelt", SmeltItemFunction.CODEC);
   public static final LootItemFunctionType LOOTING_ENCHANT = register("looting_enchant", LootingEnchantFunction.CODEC);
   public static final LootItemFunctionType SET_DAMAGE = register("set_damage", SetItemDamageFunction.CODEC);
   public static final LootItemFunctionType SET_ATTRIBUTES = register("set_attributes", SetAttributesFunction.CODEC);
   public static final LootItemFunctionType SET_NAME = register("set_name", SetNameFunction.CODEC);
   public static final LootItemFunctionType EXPLORATION_MAP = register("exploration_map", ExplorationMapFunction.CODEC);
   public static final LootItemFunctionType SET_STEW_EFFECT = register("set_stew_effect", SetStewEffectFunction.CODEC);
   public static final LootItemFunctionType COPY_NAME = register("copy_name", CopyNameFunction.CODEC);
   public static final LootItemFunctionType SET_CONTENTS = register("set_contents", SetContainerContents.CODEC);
   public static final LootItemFunctionType LIMIT_COUNT = register("limit_count", LimitCount.CODEC);
   public static final LootItemFunctionType APPLY_BONUS = register("apply_bonus", ApplyBonusCount.CODEC);
   public static final LootItemFunctionType SET_LOOT_TABLE = register("set_loot_table", SetContainerLootTable.CODEC);
   public static final LootItemFunctionType EXPLOSION_DECAY = register("explosion_decay", ApplyExplosionDecay.CODEC);
   public static final LootItemFunctionType SET_LORE = register("set_lore", SetLoreFunction.CODEC);
   public static final LootItemFunctionType FILL_PLAYER_HEAD = register("fill_player_head", FillPlayerHead.CODEC);
   public static final LootItemFunctionType COPY_NBT = register("copy_nbt", CopyNbtFunction.CODEC);
   public static final LootItemFunctionType COPY_STATE = register("copy_state", CopyBlockState.CODEC);
   public static final LootItemFunctionType SET_BANNER_PATTERN = register("set_banner_pattern", SetBannerPatternFunction.CODEC);
   public static final LootItemFunctionType SET_POTION = register("set_potion", SetPotionFunction.CODEC);
   public static final LootItemFunctionType SET_INSTRUMENT = register("set_instrument", SetInstrumentFunction.CODEC);
   public static final LootItemFunctionType REFERENCE = register("reference", FunctionReference.CODEC);
   public static final LootItemFunctionType SEQUENCE = register("sequence", SequenceFunction.CODEC);

   private static LootItemFunctionType register(String p_80763_, Codec<? extends LootItemFunction> p_298583_) {
      return Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, new ResourceLocation(p_80763_), new LootItemFunctionType(p_298583_));
   }

   public static BiFunction<ItemStack, LootContext, ItemStack> compose(List<? extends BiFunction<ItemStack, LootContext, ItemStack>> p_298851_) {
      List<BiFunction<ItemStack, LootContext, ItemStack>> list = List.copyOf(p_298851_);

      return switch(list.size()) {
         case 0 -> IDENTITY;
         case 1 -> (BiFunction)list.get(0);
         case 2 -> {
            BiFunction<ItemStack, LootContext, ItemStack> bifunction = list.get(0);
            BiFunction<ItemStack, LootContext, ItemStack> bifunction1 = list.get(1);
            yield (p_80768_, p_80769_) -> bifunction1.apply(bifunction.apply(p_80768_, p_80769_), p_80769_);
         }
         default -> (p_298097_, p_298098_) -> {
         for(BiFunction<ItemStack, LootContext, ItemStack> bifunction2 : list) {
            p_298097_ = bifunction2.apply(p_298097_, p_298098_);
         }

         return p_298097_;
      };
      };
   }
}
