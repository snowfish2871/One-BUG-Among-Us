package net.minecraft.world.level.storage.loot.functions;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootDataId;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

public class FunctionReference extends LootItemConditionalFunction {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<FunctionReference> CODEC = RecordCodecBuilder.create(
      p_298089_ -> commonFields(p_298089_)
            .and(ResourceLocation.CODEC.fieldOf("name").forGetter(p_298088_ -> p_298088_.name))
            .apply(p_298089_, FunctionReference::new)
   );
   private final ResourceLocation name;

   private FunctionReference(List<LootItemCondition> p_298661_, ResourceLocation p_279246_) {
      super(p_298661_);
      this.name = p_279246_;
   }

   @Override
   public LootItemFunctionType getType() {
      return LootItemFunctions.REFERENCE;
   }

   @Override
   public void validate(ValidationContext p_279281_) {
      LootDataId<LootItemFunction> lootdataid = new LootDataId<>(LootDataType.MODIFIER, this.name);
      if (p_279281_.hasVisitedElement(lootdataid)) {
         p_279281_.reportProblem("Function " + this.name + " is recursively called");
      } else {
         super.validate(p_279281_);
         p_279281_.resolver()
            .getElementOptional(lootdataid)
            .ifPresentOrElse(
               p_279367_ -> p_279367_.validate(p_279281_.enterElement(".{" + this.name + "}", lootdataid)),
               () -> p_279281_.reportProblem("Unknown function table called " + this.name)
            );
      }
   }

   @Override
   protected ItemStack run(ItemStack p_279458_, LootContext p_279370_) {
      LootItemFunction lootitemfunction = p_279370_.getResolver().getElement(LootDataType.MODIFIER, this.name);
      if (lootitemfunction == null) {
         LOGGER.warn("Unknown function: {}", this.name);
         return p_279458_;
      } else {
         LootContext.VisitedEntry<?> visitedentry = LootContext.createVisitedEntry(lootitemfunction);
         if (p_279370_.pushVisitedElement(visitedentry)) {
            ItemStack itemstack;
            try {
               itemstack = lootitemfunction.apply(p_279458_, p_279370_);
            } finally {
               p_279370_.popVisitedElement(visitedentry);
            }

            return itemstack;
         } else {
            LOGGER.warn("Detected infinite loop in loot tables");
            return p_279458_;
         }
      }
   }

   public static LootItemConditionalFunction.Builder<?> functionReference(ResourceLocation p_279115_) {
      return simpleBuilder(p_298091_ -> new FunctionReference(p_298091_, p_279115_));
   }
}
