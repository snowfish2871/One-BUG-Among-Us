package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;

public class ExplorationMapFunction extends LootItemConditionalFunction {
   public static final TagKey<Structure> DEFAULT_DESTINATION = StructureTags.ON_TREASURE_MAPS;
   public static final MapDecoration.Type DEFAULT_DECORATION = MapDecoration.Type.MANSION;
   public static final byte DEFAULT_ZOOM = 2;
   public static final int DEFAULT_SEARCH_RADIUS = 50;
   public static final boolean DEFAULT_SKIP_EXISTING = true;
   public static final Codec<ExplorationMapFunction> CODEC = RecordCodecBuilder.create(
      p_298641_ -> commonFields(p_298641_)
            .and(
               p_298641_.group(
                  ExtraCodecs.strictOptionalField(TagKey.codec(Registries.STRUCTURE), "destination", DEFAULT_DESTINATION)
                     .forGetter(p_299251_ -> p_299251_.destination),
                  MapDecoration.Type.CODEC.optionalFieldOf("decoration", DEFAULT_DECORATION).forGetter(p_299122_ -> p_299122_.mapDecoration),
                  ExtraCodecs.strictOptionalField(Codec.BYTE, "zoom", (byte)2).forGetter(p_298446_ -> p_298446_.zoom),
                  ExtraCodecs.strictOptionalField(Codec.INT, "search_radius", 50).forGetter(p_298263_ -> p_298263_.searchRadius),
                  ExtraCodecs.strictOptionalField(Codec.BOOL, "skip_existing_chunks", true).forGetter(p_299182_ -> p_299182_.skipKnownStructures)
               )
            )
            .apply(p_298641_, ExplorationMapFunction::new)
   );
   private final TagKey<Structure> destination;
   private final MapDecoration.Type mapDecoration;
   private final byte zoom;
   private final int searchRadius;
   private final boolean skipKnownStructures;

   ExplorationMapFunction(
      List<LootItemCondition> p_298451_, TagKey<Structure> p_210653_, MapDecoration.Type p_210654_, byte p_210655_, int p_210656_, boolean p_210657_
   ) {
      super(p_298451_);
      this.destination = p_210653_;
      this.mapDecoration = p_210654_;
      this.zoom = p_210655_;
      this.searchRadius = p_210656_;
      this.skipKnownStructures = p_210657_;
   }

   @Override
   public LootItemFunctionType getType() {
      return LootItemFunctions.EXPLORATION_MAP;
   }

   @Override
   public Set<LootContextParam<?>> getReferencedContextParams() {
      return ImmutableSet.of(LootContextParams.ORIGIN);
   }

   @Override
   public ItemStack run(ItemStack p_80547_, LootContext p_80548_) {
      if (!p_80547_.is(Items.MAP)) {
         return p_80547_;
      } else {
         Vec3 vec3 = p_80548_.getParamOrNull(LootContextParams.ORIGIN);
         if (vec3 != null) {
            ServerLevel serverlevel = p_80548_.getLevel();
            BlockPos blockpos = serverlevel.findNearestMapStructure(this.destination, BlockPos.containing(vec3), this.searchRadius, this.skipKnownStructures);
            if (blockpos != null) {
               ItemStack itemstack = MapItem.create(serverlevel, blockpos.getX(), blockpos.getZ(), this.zoom, true, true);
               MapItem.renderBiomePreviewMap(serverlevel, itemstack);
               MapItemSavedData.addTargetDecoration(itemstack, blockpos, "+", this.mapDecoration);
               return itemstack;
            }
         }

         return p_80547_;
      }
   }

   public static ExplorationMapFunction.Builder makeExplorationMap() {
      return new ExplorationMapFunction.Builder();
   }

   public static class Builder extends LootItemConditionalFunction.Builder<ExplorationMapFunction.Builder> {
      private TagKey<Structure> destination = ExplorationMapFunction.DEFAULT_DESTINATION;
      private MapDecoration.Type mapDecoration = ExplorationMapFunction.DEFAULT_DECORATION;
      private byte zoom = 2;
      private int searchRadius = 50;
      private boolean skipKnownStructures = true;

      protected ExplorationMapFunction.Builder getThis() {
         return this;
      }

      public ExplorationMapFunction.Builder setDestination(TagKey<Structure> p_210659_) {
         this.destination = p_210659_;
         return this;
      }

      public ExplorationMapFunction.Builder setMapDecoration(MapDecoration.Type p_80574_) {
         this.mapDecoration = p_80574_;
         return this;
      }

      public ExplorationMapFunction.Builder setZoom(byte p_80570_) {
         this.zoom = p_80570_;
         return this;
      }

      public ExplorationMapFunction.Builder setSearchRadius(int p_165206_) {
         this.searchRadius = p_165206_;
         return this;
      }

      public ExplorationMapFunction.Builder setSkipKnownStructures(boolean p_80576_) {
         this.skipKnownStructures = p_80576_;
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new ExplorationMapFunction(this.getConditions(), this.destination, this.mapDecoration, this.zoom, this.searchRadius, this.skipKnownStructures);
      }
   }
}
