package net.minecraft.world.level.storage.loot.entries;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootDataId;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootTableReference extends LootPoolSingletonContainer {
   public static final Codec<LootTableReference> CODEC = RecordCodecBuilder.create(
      p_298029_ -> p_298029_.group(ResourceLocation.CODEC.fieldOf("name").forGetter(p_298023_ -> p_298023_.name))
            .and(singletonFields(p_298029_))
            .apply(p_298029_, LootTableReference::new)
   );
   private final ResourceLocation name;

   private LootTableReference(ResourceLocation p_79756_, int p_79757_, int p_79758_, List<LootItemCondition> p_298340_, List<LootItemFunction> p_298824_) {
      super(p_79757_, p_79758_, p_298340_, p_298824_);
      this.name = p_79756_;
   }

   @Override
   public LootPoolEntryType getType() {
      return LootPoolEntries.REFERENCE;
   }

   @Override
   public void createItemStack(Consumer<ItemStack> p_79774_, LootContext p_79775_) {
      LootTable loottable = p_79775_.getResolver().getLootTable(this.name);
      loottable.getRandomItemsRaw(p_79775_, p_79774_);
   }

   @Override
   public void validate(ValidationContext p_79770_) {
      LootDataId<LootTable> lootdataid = new LootDataId<>(LootDataType.TABLE, this.name);
      if (p_79770_.hasVisitedElement(lootdataid)) {
         p_79770_.reportProblem("Table " + this.name + " is recursively called");
      } else {
         super.validate(p_79770_);
         p_79770_.resolver()
            .getElementOptional(lootdataid)
            .ifPresentOrElse(
               p_279078_ -> p_279078_.validate(p_79770_.enterElement("->{" + this.name + "}", lootdataid)),
               () -> p_79770_.reportProblem("Unknown loot table called " + this.name)
            );
      }
   }

   public static LootPoolSingletonContainer.Builder<?> lootTableReference(ResourceLocation p_79777_) {
      return simpleBuilder((p_298025_, p_298026_, p_298027_, p_298028_) -> new LootTableReference(p_79777_, p_298025_, p_298026_, p_298027_, p_298028_));
   }
}
