package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CraftingRecipeCodecs {
   private static final Codec<Item> ITEM_NONAIR_CODEC = ExtraCodecs.validate(
      BuiltInRegistries.ITEM.byNameCodec(),
      p_301330_ -> p_301330_ == Items.AIR ? DataResult.error(() -> "Crafting result must not be minecraft:air") : DataResult.success(p_301330_)
   );
   public static final Codec<ItemStack> ITEMSTACK_OBJECT_CODEC = RecordCodecBuilder.create(
      p_301292_ -> p_301292_.group(
               ITEM_NONAIR_CODEC.fieldOf("item").forGetter(ItemStack::getItem),
               ExtraCodecs.strictOptionalField(ExtraCodecs.POSITIVE_INT, "count", 1).forGetter(ItemStack::getCount),
               ExtraCodecs.strictOptionalField(ExtraCodecs.withAlternative(net.minecraft.nbt.TagParser.AS_CODEC, net.minecraft.nbt.CompoundTag.CODEC), "nbt").forGetter(stack -> java.util.Optional.ofNullable(stack.getTag()))
            )
            .apply(p_301292_, ItemStack::new)
   );
   static final Codec<ItemStack> ITEMSTACK_NONAIR_CODEC = ExtraCodecs.<Item>validate(
         BuiltInRegistries.ITEM.byNameCodec(),
         p_300898_ -> p_300898_ == Items.AIR ? DataResult.error(() -> "Empty ingredient not allowed here") : DataResult.success(p_300898_)
      )
      .xmap(ItemStack::new, ItemStack::getItem);
}
