package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

public class SimpleCookingSerializer<T extends AbstractCookingRecipe> implements RecipeSerializer<T> {
   private final SimpleCookingSerializer.CookieBaker<T> factory;
   private final Codec<T> codec;

   public SimpleCookingSerializer(SimpleCookingSerializer.CookieBaker<T> p_44330_, int p_44331_) {
      this.factory = p_44330_;
      this.codec = RecordCodecBuilder.create(
         p_300831_ -> p_300831_.group(
                  ExtraCodecs.strictOptionalField(Codec.STRING, "group", "").forGetter(p_300832_ -> p_300832_.group),
                  CookingBookCategory.CODEC.fieldOf("category").orElse(CookingBookCategory.MISC).forGetter(p_300828_ -> p_300828_.category),
                  Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(p_300833_ -> p_300833_.ingredient),
                  net.neoforged.neoforge.common.crafting.CraftingHelper.smeltingResultCodec().fieldOf("result").forGetter(p_300827_ -> p_300827_.result),
                  Codec.FLOAT.fieldOf("experience").orElse(0.0F).forGetter(p_300826_ -> p_300826_.experience),
                  Codec.INT.fieldOf("cookingtime").orElse(p_44331_).forGetter(p_300834_ -> p_300834_.cookingTime)
               )
               .apply(p_300831_, p_44330_::create)
      );
   }

   @Override
   public Codec<T> codec() {
      return this.codec;
   }

   public T fromNetwork(FriendlyByteBuf p_44345_) {
      String s = p_44345_.readUtf();
      CookingBookCategory cookingbookcategory = p_44345_.readEnum(CookingBookCategory.class);
      Ingredient ingredient = Ingredient.fromNetwork(p_44345_);
      ItemStack itemstack = p_44345_.readItem();
      float f = p_44345_.readFloat();
      int i = p_44345_.readVarInt();
      return this.factory.create(s, cookingbookcategory, ingredient, itemstack, f, i);
   }

   public void toNetwork(FriendlyByteBuf p_44335_, T p_44336_) {
      p_44335_.writeUtf(p_44336_.group);
      p_44335_.writeEnum(p_44336_.category());
      p_44336_.ingredient.toNetwork(p_44335_);
      p_44335_.writeItem(p_44336_.result);
      p_44335_.writeFloat(p_44336_.experience);
      p_44335_.writeVarInt(p_44336_.cookingTime);
   }

   interface CookieBaker<T extends AbstractCookingRecipe> {
      T create(String p_44354_, CookingBookCategory p_249487_, Ingredient p_44355_, ItemStack p_44356_, float p_44357_, int p_44358_);
   }
}
