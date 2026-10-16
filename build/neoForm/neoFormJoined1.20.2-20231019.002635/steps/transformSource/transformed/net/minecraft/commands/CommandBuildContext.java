package net.minecraft.commands;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlagSet;

public interface CommandBuildContext {
   <T> HolderLookup<T> holderLookup(ResourceKey<? extends Registry<T>> p_227134_);

   static CommandBuildContext simple(final HolderLookup.Provider p_255702_, final FeatureFlagSet p_255968_) {
      return new CommandBuildContext() {
         @Override
         public <T> HolderLookup<T> holderLookup(ResourceKey<? extends Registry<T>> p_255791_) {
            return p_255702_.<T>lookupOrThrow(p_255791_).filterFeatures(p_255968_);
         }
      };
   }

   static CommandBuildContext.Configurable configurable(final RegistryAccess p_255925_, final FeatureFlagSet p_255945_) {
      return new CommandBuildContext.Configurable() {
         CommandBuildContext.MissingTagAccessPolicy missingTagAccessPolicy = CommandBuildContext.MissingTagAccessPolicy.FAIL;

         @Override
         public void missingTagAccessPolicy(CommandBuildContext.MissingTagAccessPolicy p_256626_) {
            this.missingTagAccessPolicy = p_256626_;
         }

         @Override
         public <T> HolderLookup<T> holderLookup(ResourceKey<? extends Registry<T>> p_256616_) {
            Registry<T> registry = p_255925_.registryOrThrow(p_256616_);
            final HolderLookup.RegistryLookup<T> registrylookup = registry.asLookup();
            final HolderLookup.RegistryLookup<T> registrylookup1 = registry.asTagAddingLookup();
            HolderLookup.RegistryLookup<T> registrylookup2 = new HolderLookup.RegistryLookup.Delegate<T>() {
               @Override
               protected HolderLookup.RegistryLookup<T> parent() {
                  return switch(missingTagAccessPolicy) {
                     case FAIL -> registrylookup;
                     case CREATE_NEW -> registrylookup1;
                  };
               }
            };
            return registrylookup2.filterFeatures(p_255945_);
         }
      };
   }

   public interface Configurable extends CommandBuildContext {
      void missingTagAccessPolicy(CommandBuildContext.MissingTagAccessPolicy p_256669_);
   }

   public static enum MissingTagAccessPolicy {
      CREATE_NEW,
      FAIL;
   }
}
