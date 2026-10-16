package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;

public class EntityHorseSplitFix extends EntityRenameFix {
   public EntityHorseSplitFix(Schema p_15447_, boolean p_15448_) {
      super("EntityHorseSplitFix", p_15447_, p_15448_);
   }

   @Override
   protected Pair<String, Typed<?>> fix(String p_15451_, Typed<?> p_15452_) {
      Dynamic<?> dynamic = p_15452_.get(DSL.remainderFinder());
      if (Objects.equals("EntityHorse", p_15451_)) {
         int i = dynamic.get("Type").asInt(0);

         String $$8 = switch(i) {
            default -> "Horse";
            case 1 -> "Donkey";
            case 2 -> "Mule";
            case 3 -> "ZombieHorse";
            case 4 -> "SkeletonHorse";
         };
         dynamic.remove("Type");
         Type<?> type = (Type)this.getOutputSchema().findChoiceType(References.ENTITY).types().get($$8);
         return Pair.of(
            $$8,
            (Typed<?>)((Pair)((com.mojang.serialization.DataResult<Dynamic<?>>)p_15452_.write()).flatMap(type::readTyped).result().orElseThrow(() -> new IllegalStateException("Could not parse the new horse")))
               .getFirst()
         );
      } else {
         return Pair.of(p_15451_, p_15452_);
      }
   }
}
