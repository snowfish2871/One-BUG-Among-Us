package net.minecraft.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.TreeNodePosition;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.loot.LootDataManager;
import org.slf4j.Logger;

public class ServerAdvancementManager extends SimpleJsonResourceReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = new GsonBuilder().create();
   private Map<ResourceLocation, AdvancementHolder> advancements = Map.of();
   private AdvancementTree tree = new AdvancementTree();
   private final LootDataManager lootData;

   public ServerAdvancementManager(LootDataManager p_279237_) {
      super(GSON, "advancements");
      this.lootData = p_279237_;
   }

   protected void apply(Map<ResourceLocation, JsonElement> p_136034_, ResourceManager p_136035_, ProfilerFiller p_136036_) {
      Builder<ResourceLocation, AdvancementHolder> builder = ImmutableMap.builder();
      p_136034_.forEach((p_300735_, p_300736_) -> {
         try {
            JsonObject jsonobject = GsonHelper.convertToJsonObject(p_300736_, "advancement");
            Advancement advancement = Advancement.fromJson(jsonobject, new DeserializationContext(p_300735_, this.lootData), registryAccess == null ? com.mojang.serialization.JsonOps.INSTANCE : net.neoforged.neoforge.common.conditions.ConditionalOps.create(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, registryAccess), conditionContext));
            if (advancement == null) {
               LOGGER.debug("Skipping loading advancement {} as its conditions were not met", p_300735_);
               return;
            }
            builder.put(p_300735_, new AdvancementHolder(p_300735_, advancement));
         } catch (Exception exception) {
            LOGGER.error("Parsing error loading custom advancement {}: {}", p_300735_, exception.getMessage());
         }
      });
      this.advancements = builder.buildOrThrow();
      AdvancementTree advancementtree = new AdvancementTree();
      advancementtree.addAll(this.advancements.values());

      for(AdvancementNode advancementnode : advancementtree.roots()) {
         if (advancementnode.holder().value().display().isPresent()) {
            TreeNodePosition.run(advancementnode);
         }
      }

      this.tree = advancementtree;
   }

   @Nullable
   public AdvancementHolder get(ResourceLocation p_301079_) {
      return this.advancements.get(p_301079_);
   }

   public AdvancementTree tree() {
      return this.tree;
   }

   public Collection<AdvancementHolder> getAllAdvancements() {
      return this.advancements.values();
   }
}
