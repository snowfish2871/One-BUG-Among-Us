package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ShapelessRecipe implements CraftingRecipe {
   final String group;
   final CraftingBookCategory category;
   final ItemStack result;
   final NonNullList<Ingredient> ingredients;
   private final boolean isSimple;

   public ShapelessRecipe(String p_249640_, CraftingBookCategory p_249390_, ItemStack p_252071_, NonNullList<Ingredient> p_250689_) {
      this.group = p_249640_;
      this.category = p_249390_;
      this.result = p_252071_;
      this.ingredients = p_250689_;
      this.isSimple = p_250689_.stream().allMatch(Ingredient::isSimple);
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.SHAPELESS_RECIPE;
   }

   @Override
   public String getGroup() {
      return this.group;
   }

   @Override
   public CraftingBookCategory category() {
      return this.category;
   }

   @Override
   public ItemStack getResultItem(RegistryAccess p_267111_) {
      return this.result;
   }

   @Override
   public NonNullList<Ingredient> getIngredients() {
      return this.ingredients;
   }

   public boolean matches(CraftingContainer p_44262_, Level p_44263_) {
      StackedContents stackedcontents = new StackedContents();
      java.util.List<ItemStack> inputs = new java.util.ArrayList<>();
      int i = 0;

      for(int j = 0; j < p_44262_.getContainerSize(); ++j) {
         ItemStack itemstack = p_44262_.getItem(j);
         if (!itemstack.isEmpty()) {
            ++i;
            if (isSimple)
            stackedcontents.accountStack(itemstack, 1);
            else inputs.add(itemstack);
         }
      }

      return i == this.ingredients.size() && (isSimple ? stackedcontents.canCraft(this, null) : net.neoforged.neoforge.common.util.RecipeMatcher.findMatches(inputs,  this.ingredients) != null);
   }

   public ItemStack assemble(CraftingContainer p_44260_, RegistryAccess p_266797_) {
      return this.result.copy();
   }

   @Override
   public boolean canCraftInDimensions(int p_44252_, int p_44253_) {
      return p_44252_ * p_44253_ >= this.ingredients.size();
   }

   public static class Serializer implements RecipeSerializer<ShapelessRecipe> {
      private static final net.minecraft.resources.ResourceLocation NAME = new net.minecraft.resources.ResourceLocation("minecraft", "crafting_shapeless");
      private static final Codec<ShapelessRecipe> CODEC = RecordCodecBuilder.create(
         p_300958_ -> p_300958_.group(
                  ExtraCodecs.strictOptionalField(Codec.STRING, "group", "").forGetter(p_301127_ -> p_301127_.group),
                  CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(p_301133_ -> p_301133_.category),
                  CraftingRecipeCodecs.ITEMSTACK_OBJECT_CODEC.fieldOf("result").forGetter(p_301142_ -> p_301142_.result),
                  Ingredient.CODEC_NONEMPTY
                     .listOf()
                     .fieldOf("ingredients")
                     .flatXmap(
                        p_301021_ -> {
                           Ingredient[] aingredient = p_301021_
                                   .toArray(Ingredient[]::new); //Forge skip the empty check and immediatly create the array.
                           if (aingredient.length == 0) {
                              return DataResult.error(() -> "No ingredients for shapeless recipe");
                           } else {
                              return aingredient.length > ShapedRecipe.MAX_HEIGHT * ShapedRecipe.MAX_WIDTH
                                 ? DataResult.error(() -> "Too many ingredients for shapeless recipe. The maximum is: %s".formatted(ShapedRecipe.MAX_HEIGHT * ShapedRecipe.MAX_WIDTH))
                                 : DataResult.success(NonNullList.of(Ingredient.EMPTY, aingredient));
                           }
                        },
                        DataResult::success
                     )
                     .forGetter(p_300975_ -> p_300975_.ingredients)
               )
               .apply(p_300958_, ShapelessRecipe::new)
      );

      @Override
      public Codec<ShapelessRecipe> codec() {
         return CODEC;
      }

      public ShapelessRecipe fromNetwork(FriendlyByteBuf p_44294_) {
         String s = p_44294_.readUtf();
         CraftingBookCategory craftingbookcategory = p_44294_.readEnum(CraftingBookCategory.class);
         int i = p_44294_.readVarInt();
         NonNullList<Ingredient> nonnulllist = NonNullList.withSize(i, Ingredient.EMPTY);

         for(int j = 0; j < nonnulllist.size(); ++j) {
            nonnulllist.set(j, Ingredient.fromNetwork(p_44294_));
         }

         ItemStack itemstack = p_44294_.readItem();
         return new ShapelessRecipe(s, craftingbookcategory, itemstack, nonnulllist);
      }

      public void toNetwork(FriendlyByteBuf p_44281_, ShapelessRecipe p_44282_) {
         p_44281_.writeUtf(p_44282_.group);
         p_44281_.writeEnum(p_44282_.category);
         p_44281_.writeVarInt(p_44282_.ingredients.size());

         for(Ingredient ingredient : p_44282_.ingredients) {
            ingredient.toNetwork(p_44281_);
         }

         p_44281_.writeItem(p_44282_.result);
      }
   }
}
