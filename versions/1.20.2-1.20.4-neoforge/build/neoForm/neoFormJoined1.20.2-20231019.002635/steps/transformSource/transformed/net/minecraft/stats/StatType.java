package net.minecraft.stats;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;

public class StatType<T> implements Iterable<Stat<T>> {
   private final Registry<T> registry;
   private final Map<T, Stat<T>> map = new IdentityHashMap<>();
   private final Component displayName;

   public StatType(Registry<T> p_12892_, Component p_298934_) {
      this.registry = p_12892_;
      this.displayName = p_298934_;
   }

   public boolean contains(T p_12898_) {
      return this.map.containsKey(p_12898_);
   }

   public Stat<T> get(T p_12900_, StatFormatter p_12901_) {
      return this.map.computeIfAbsent(p_12900_, p_12896_ -> new Stat<>(this, p_12896_, p_12901_));
   }

   public Registry<T> getRegistry() {
      return this.registry;
   }

   @Override
   public Iterator<Stat<T>> iterator() {
      return this.map.values().iterator();
   }

   public Stat<T> get(T p_12903_) {
      return this.get(p_12903_, StatFormatter.DEFAULT);
   }

   public Component getDisplayName() {
      return this.displayName;
   }
}
