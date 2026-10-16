package net.minecraft.server;

import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleReloadInstance;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagManager;
import net.minecraft.util.Unit;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootDataManager;
import org.slf4j.Logger;

public class ReloadableServerResources {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final CompletableFuture<Unit> DATA_RELOAD_INITIAL_TASK = CompletableFuture.completedFuture(Unit.INSTANCE);
   private final CommandBuildContext.Configurable commandBuildContext;
   private final Commands commands;
   private final RecipeManager recipes = new RecipeManager();
   private final TagManager tagManager;
   private final LootDataManager lootData = new LootDataManager();
   private final ServerAdvancementManager advancements = new ServerAdvancementManager(this.lootData);
   private final ServerFunctionLibrary functionLibrary;

   public ReloadableServerResources(RegistryAccess.Frozen p_206857_, FeatureFlagSet p_250695_, Commands.CommandSelection p_206858_, int p_206859_) {
      this.tagManager = new TagManager(p_206857_);
      this.commandBuildContext = CommandBuildContext.configurable(p_206857_, p_250695_);
      this.commands = new Commands(p_206858_, this.commandBuildContext);
      this.commandBuildContext.missingTagAccessPolicy(CommandBuildContext.MissingTagAccessPolicy.CREATE_NEW);
      this.functionLibrary = new ServerFunctionLibrary(p_206859_, this.commands.getDispatcher());
      // Neo: Create context object and inject it into listeners that need it
      this.registryAccess = p_206857_;
      this.context = new net.neoforged.neoforge.common.conditions.ConditionContext(this.tagManager);
   }

   public ServerFunctionLibrary getFunctionLibrary() {
      return this.functionLibrary;
   }

   public LootDataManager getLootData() {
      return this.lootData;
   }

   public RecipeManager getRecipeManager() {
      return this.recipes;
   }

   public Commands getCommands() {
      return this.commands;
   }

   public ServerAdvancementManager getAdvancements() {
      return this.advancements;
   }

   public List<PreparableReloadListener> listeners() {
      return List.of(this.tagManager, this.lootData, this.recipes, this.functionLibrary, this.advancements);
   }

   public static CompletableFuture<ReloadableServerResources> loadResources(
      ResourceManager p_248588_,
      RegistryAccess.Frozen p_251163_,
      FeatureFlagSet p_250212_,
      Commands.CommandSelection p_249301_,
      int p_251126_,
      Executor p_249136_,
      Executor p_249601_
   ) {
      ReloadableServerResources reloadableserverresources = new ReloadableServerResources(p_251163_, p_250212_, p_249301_, p_251126_);
      List<PreparableReloadListener> listeners = new java.util.ArrayList<>(reloadableserverresources.listeners());
      listeners.addAll(net.neoforged.neoforge.event.EventHooks.onResourceReload(reloadableserverresources, p_251163_));
      listeners.forEach(rl -> {
         if (rl instanceof net.neoforged.neoforge.resource.ContextAwareReloadListener srl) srl.injectContext(reloadableserverresources.context, reloadableserverresources.registryAccess);
      });
      return SimpleReloadInstance.create(p_248588_, listeners, p_249136_, p_249601_, DATA_RELOAD_INITIAL_TASK, LOGGER.isDebugEnabled())
         .done()
         .whenComplete(
            (p_255534_, p_255535_) -> reloadableserverresources.commandBuildContext.missingTagAccessPolicy(CommandBuildContext.MissingTagAccessPolicy.FAIL)
         )
         .thenApply(p_214306_ -> reloadableserverresources);
   }

   public void updateRegistryTags(RegistryAccess p_206869_) {
      this.tagManager.getResult().forEach(p_214315_ -> updateRegistryTags(p_206869_, p_214315_));
      Blocks.rebuildCache();
      net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.TagsUpdatedEvent(p_206869_, false, false));
   }

   private static <T> void updateRegistryTags(RegistryAccess p_206871_, TagManager.LoadResult<T> p_206872_) {
      ResourceKey<? extends Registry<T>> resourcekey = p_206872_.key();
      Map<TagKey<T>, List<Holder<T>>> map = p_206872_.tags()
         .entrySet()
         .stream()
         .collect(
            Collectors.toUnmodifiableMap(
               p_214303_ -> TagKey.create(resourcekey, (ResourceLocation)p_214303_.getKey()), p_214312_ -> List.copyOf((Collection)p_214312_.getValue())
            )
         );
      p_206871_.registryOrThrow(resourcekey).bindTags(map);
   }

   private final net.neoforged.neoforge.common.conditions.ICondition.IContext context;
   private final net.minecraft.core.RegistryAccess registryAccess;

   /**
    * Exposes the current condition context for usage in other reload listeners.<br>
    * This is not useful outside the reloading stage.
    * @return The condition context for the currently active reload.
    */
   public net.neoforged.neoforge.common.conditions.ICondition.IContext getConditionContext() {
      return this.context;
   }
   /**
    * {@return the registry access for the currently active reload}
    */
   public net.minecraft.core.RegistryAccess getRegistryAccess() {
      return this.registryAccess;
   }
}
