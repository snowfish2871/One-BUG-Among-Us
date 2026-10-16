package net.minecraft.advancements;

import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonSyntaxException;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;

public record AdvancementRequirements(String[][] requirements) {
   public static final AdvancementRequirements EMPTY = new AdvancementRequirements(new String[0][]);

   public AdvancementRequirements(FriendlyByteBuf p_301089_) {
      this(read(p_301089_));
   }

   private static String[][] read(FriendlyByteBuf p_300972_) {
      String[][] astring = new String[p_300972_.readVarInt()][];

      for(int i = 0; i < astring.length; ++i) {
         astring[i] = new String[p_300972_.readVarInt()];

         for(int j = 0; j < astring[i].length; ++j) {
            astring[i][j] = p_300972_.readUtf();
         }
      }

      return astring;
   }

   public void write(FriendlyByteBuf p_301190_) {
      p_301190_.writeVarInt(this.requirements.length);

      for(String[] astring : this.requirements) {
         p_301190_.writeVarInt(astring.length);

         for(String s : astring) {
            p_301190_.writeUtf(s);
         }
      }
   }

   public static AdvancementRequirements allOf(Collection<String> p_301049_) {
      return new AdvancementRequirements(p_301049_.stream().map(p_301284_ -> new String[]{p_301284_}).toArray(p_301134_ -> new String[p_301134_][]));
   }

   public static AdvancementRequirements anyOf(Collection<String> p_301268_) {
      return new AdvancementRequirements(new String[][]{p_301268_.toArray(p_301175_ -> new String[p_301175_])});
   }

   public int size() {
      return this.requirements.length;
   }

   public boolean test(Predicate<String> p_301112_) {
      if (this.requirements.length == 0) {
         return false;
      } else {
         for(String[] astring : this.requirements) {
            if (!anyMatch(astring, p_301112_)) {
               return false;
            }
         }

         return true;
      }
   }

   public int count(Predicate<String> p_301229_) {
      int i = 0;

      for(String[] astring : this.requirements) {
         if (anyMatch(astring, p_301229_)) {
            ++i;
         }
      }

      return i;
   }

   private static boolean anyMatch(String[] p_300945_, Predicate<String> p_301321_) {
      for(String s : p_300945_) {
         if (p_301321_.test(s)) {
            return true;
         }
      }

      return false;
   }

   public static AdvancementRequirements fromJson(JsonArray p_300976_, Set<String> p_301283_) {
      String[][] astring = new String[p_300976_.size()][];
      Set<String> set = new ObjectOpenHashSet<>();

      for(int i = 0; i < p_300976_.size(); ++i) {
         JsonArray jsonarray = GsonHelper.convertToJsonArray(p_300976_.get(i), "requirements[" + i + "]");
         if (jsonarray.isEmpty() && p_301283_.isEmpty()) {
            throw new JsonSyntaxException("Requirement entry cannot be empty");
         }

         astring[i] = new String[jsonarray.size()];

         for(int j = 0; j < jsonarray.size(); ++j) {
            String s = GsonHelper.convertToString(jsonarray.get(j), "requirements[" + i + "][" + j + "]");
            astring[i][j] = s;
            set.add(s);
         }
      }

      if (!p_301283_.equals(set)) {
         Set<String> set1 = Sets.difference(p_301283_, set);
         Set<String> set2 = Sets.difference(set, p_301283_);
         throw new JsonSyntaxException("Advancement completion requirements did not exactly match specified criteria. Missing: " + set1 + ". Unknown: " + set2);
      } else {
         return new AdvancementRequirements(astring);
      }
   }

   public JsonArray toJson() {
      JsonArray jsonarray = new JsonArray();

      for(String[] astring : this.requirements) {
         JsonArray jsonarray1 = new JsonArray();
         Arrays.stream(astring).forEach(jsonarray1::add);
         jsonarray.add(jsonarray1);
      }

      return jsonarray;
   }

   public boolean isEmpty() {
      return this.requirements.length == 0;
   }

   @Override
   public String toString() {
      return Arrays.deepToString(this.requirements);
   }

   public Set<String> names() {
      Set<String> set = new ObjectOpenHashSet<>();

      for(String[] astring : this.requirements) {
         Collections.addAll(set, astring);
      }

      return set;
   }

   public interface Strategy {
      AdvancementRequirements.Strategy AND = AdvancementRequirements::allOf;
      AdvancementRequirements.Strategy OR = AdvancementRequirements::anyOf;

      AdvancementRequirements create(Collection<String> p_301043_);
   }
}
