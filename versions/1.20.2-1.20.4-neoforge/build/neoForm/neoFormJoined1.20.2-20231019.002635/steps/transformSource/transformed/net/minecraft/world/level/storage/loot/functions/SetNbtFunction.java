package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetNbtFunction extends LootItemConditionalFunction {
   public static final Codec<SetNbtFunction> CODEC = RecordCodecBuilder.create(
      p_298156_ -> commonFields(p_298156_).and(TagParser.AS_CODEC.fieldOf("tag").forGetter(p_298157_ -> p_298157_.tag)).apply(p_298156_, SetNbtFunction::new)
   );
   private final CompoundTag tag;

   private SetNbtFunction(List<LootItemCondition> p_298385_, CompoundTag p_81177_) {
      super(p_298385_);
      this.tag = p_81177_;
   }

   @Override
   public LootItemFunctionType getType() {
      return LootItemFunctions.SET_NBT;
   }

   @Override
   public ItemStack run(ItemStack p_81183_, LootContext p_81184_) {
      p_81183_.getOrCreateTag().merge(this.tag);
      return p_81183_;
   }

   @Deprecated
   public static LootItemConditionalFunction.Builder<?> setTag(CompoundTag p_81188_) {
      return simpleBuilder(p_298155_ -> new SetNbtFunction(p_298155_, p_81188_));
   }
}
