package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.stream.Stream;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SmithingTransformRecipe implements SmithingRecipe {
   final Ingredient template;
   final Ingredient base;
   final Ingredient addition;
   final ItemStack result;

   public SmithingTransformRecipe(Ingredient p_266750_, Ingredient p_266787_, Ingredient p_267292_, ItemStack p_267031_) {
      this.template = p_266750_;
      this.base = p_266787_;
      this.addition = p_267292_;
      this.result = p_267031_;
   }

   @Override
   public boolean matches(Container p_266855_, Level p_266781_) {
      return this.template.test(p_266855_.getItem(0)) && this.base.test(p_266855_.getItem(1)) && this.addition.test(p_266855_.getItem(2));
   }

   @Override
   public ItemStack assemble(Container p_267036_, RegistryAccess p_266699_) {
      ItemStack itemstack = this.result.copy();
      CompoundTag compoundtag = p_267036_.getItem(1).getTag();
      if (compoundtag != null) {
         itemstack.setTag(compoundtag.copy());
      }

      return itemstack;
   }

   @Override
   public ItemStack getResultItem(RegistryAccess p_267209_) {
      return this.result;
   }

   @Override
   public boolean isTemplateIngredient(ItemStack p_267113_) {
      return this.template.test(p_267113_);
   }

   @Override
   public boolean isBaseIngredient(ItemStack p_267276_) {
      return this.base.test(p_267276_);
   }

   @Override
   public boolean isAdditionIngredient(ItemStack p_267260_) {
      return this.addition.test(p_267260_);
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.SMITHING_TRANSFORM;
   }

   @Override
   public boolean isIncomplete() {
      return Stream.of(this.template, this.base, this.addition).anyMatch(net.neoforged.neoforge.common.CommonHooks::hasNoElements);
   }

   public static class Serializer implements RecipeSerializer<SmithingTransformRecipe> {
      private static final Codec<SmithingTransformRecipe> CODEC = RecordCodecBuilder.create(
         p_301062_ -> p_301062_.group(
                  Ingredient.CODEC.fieldOf("template").forGetter(p_301310_ -> p_301310_.template),
                  Ingredient.CODEC.fieldOf("base").forGetter(p_300938_ -> p_300938_.base),
                  Ingredient.CODEC.fieldOf("addition").forGetter(p_301153_ -> p_301153_.addition),
                  CraftingRecipeCodecs.ITEMSTACK_OBJECT_CODEC.fieldOf("result").forGetter(p_300935_ -> p_300935_.result)
               )
               .apply(p_301062_, SmithingTransformRecipe::new)
      );

      @Override
      public Codec<SmithingTransformRecipe> codec() {
         return CODEC;
      }

      public SmithingTransformRecipe fromNetwork(FriendlyByteBuf p_267139_) {
         Ingredient ingredient = Ingredient.fromNetwork(p_267139_);
         Ingredient ingredient1 = Ingredient.fromNetwork(p_267139_);
         Ingredient ingredient2 = Ingredient.fromNetwork(p_267139_);
         ItemStack itemstack = p_267139_.readItem();
         return new SmithingTransformRecipe(ingredient, ingredient1, ingredient2, itemstack);
      }

      public void toNetwork(FriendlyByteBuf p_266746_, SmithingTransformRecipe p_266927_) {
         p_266927_.template.toNetwork(p_266746_);
         p_266927_.base.toNetwork(p_266746_);
         p_266927_.addition.toNetwork(p_266746_);
         p_266746_.writeItem(p_266927_.result);
      }
   }
}
