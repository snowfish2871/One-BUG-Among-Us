package net.minecraft.core;

import com.mojang.serialization.Lifecycle;
import net.minecraft.resources.ResourceKey;

public interface WritableRegistry<T> extends Registry<T> {
   Holder.Reference<T> register(ResourceKey<T> p_256320_, T p_255978_, Lifecycle p_256625_);

   boolean isEmpty();

   HolderGetter<T> createRegistrationLookup();
}
