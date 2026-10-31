package net.minecraft.world.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;

public class ReactToBell {
   public static BehaviorControl<LivingEntity> create() {
      return BehaviorBuilder.create(
         p_259349_ -> p_259349_.group(p_259349_.present(MemoryModuleType.HEARD_BELL_TIME))
               .apply(p_259349_, p_259472_ -> (p_301803_, p_301804_, p_301805_) -> {
                     Raid raid = p_301803_.getRaidAt(p_301804_.blockPosition());
                     if (raid == null) {
                        p_301804_.getBrain().setActiveActivityIfPossible(Activity.HIDE);
                     }
      
                     return true;
                  })
      );
   }
}
