package net.minecraft.advancements;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum FrameType {
   TASK("task", ChatFormatting.GREEN),
   CHALLENGE("challenge", ChatFormatting.DARK_PURPLE),
   GOAL("goal", ChatFormatting.GREEN);

   private final String name;
   private final ChatFormatting chatColor;
   private final Component displayName;

   private FrameType(String p_15545_, ChatFormatting p_15547_) {
      this.name = p_15545_;
      this.chatColor = p_15547_;
      this.displayName = Component.translatable("advancements.toast." + p_15545_);
   }

   public String getName() {
      return this.name;
   }

   public static FrameType byName(String p_15550_) {
      for(FrameType frametype : values()) {
         if (frametype.name.equals(p_15550_)) {
            return frametype;
         }
      }

      throw new IllegalArgumentException("Unknown frame type '" + p_15550_ + "'");
   }

   public ChatFormatting getChatColor() {
      return this.chatColor;
   }

   public Component getDisplayName() {
      return this.displayName;
   }
}
