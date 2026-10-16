package net.minecraft.data.advancements;

import java.util.function.Consumer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;

public interface AdvancementSubProvider {
   void generate(HolderLookup.Provider p_255901_, Consumer<AdvancementHolder> p_250888_);
}
