package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootDataId;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetContainerLootTable extends LootItemConditionalFunction {
   public static final Codec<SetContainerLootTable> CODEC = RecordCodecBuilder.create(
      p_298104_ -> commonFields(p_298104_)
            .and(
               p_298104_.group(
                  ResourceLocation.CODEC.fieldOf("name").forGetter(p_298106_ -> p_298106_.name),
                  ExtraCodecs.strictOptionalField(Codec.LONG, "seed", 0L).forGetter(p_298105_ -> p_298105_.seed),
                  BuiltInRegistries.BLOCK_ENTITY_TYPE.holderByNameCodec().fieldOf("type").forGetter(p_298107_ -> p_298107_.type)
               )
            )
            .apply(p_298104_, SetContainerLootTable::new)
   );
   private final ResourceLocation name;
   private final long seed;
   private final Holder<BlockEntityType<?>> type;

   private SetContainerLootTable(List<LootItemCondition> p_298290_, ResourceLocation p_193046_, long p_193047_, Holder<BlockEntityType<?>> p_298416_) {
      super(p_298290_);
      this.name = p_193046_;
      this.seed = p_193047_;
      this.type = p_298416_;
   }

   @Override
   public LootItemFunctionType getType() {
      return LootItemFunctions.SET_LOOT_TABLE;
   }

   @Override
   public ItemStack run(ItemStack p_80967_, LootContext p_80968_) {
      if (p_80967_.isEmpty()) {
         return p_80967_;
      } else {
         CompoundTag compoundtag = BlockItem.getBlockEntityData(p_80967_);
         if (compoundtag == null) {
            compoundtag = new CompoundTag();
         }

         compoundtag.putString("LootTable", this.name.toString());
         if (this.seed != 0L) {
            compoundtag.putLong("LootTableSeed", this.seed);
         }

         BlockItem.setBlockEntityData(p_80967_, this.type.value(), compoundtag);
         return p_80967_;
      }
   }

   @Override
   public void validate(ValidationContext p_80970_) {
      super.validate(p_80970_);
      LootDataId<LootTable> lootdataid = new LootDataId<>(LootDataType.TABLE, this.name);
      if (p_80970_.resolver().getElementOptional(lootdataid).isEmpty()) {
         p_80970_.reportProblem("Missing loot table used for container: " + this.name);
      }
   }

   public static LootItemConditionalFunction.Builder<?> withLootTable(BlockEntityType<?> p_193050_, ResourceLocation p_193051_) {
      return simpleBuilder(p_298114_ -> new SetContainerLootTable(p_298114_, p_193051_, 0L, p_193050_.builtInRegistryHolder()));
   }

   public static LootItemConditionalFunction.Builder<?> withLootTable(BlockEntityType<?> p_193053_, ResourceLocation p_193054_, long p_193055_) {
      return simpleBuilder(p_298111_ -> new SetContainerLootTable(p_298111_, p_193054_, p_193055_, p_193053_.builtInRegistryHolder()));
   }
}
