package net.minecraft.world.entity;

import net.minecraft.sounds.SoundSource;

@Deprecated // Forge: Use IShearable
public interface Shearable {
   @Deprecated // Forge: Use IShearable
   void shear(SoundSource p_21749_);

   @Deprecated // Forge: Use IShearable
   boolean readyForShearing();
}
