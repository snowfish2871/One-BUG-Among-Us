package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.Date;
import java.util.UUID;
import net.minecraft.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class PendingInvite extends ValueObject {
   private static final Logger LOGGER = LogUtils.getLogger();
   public String invitationId;
   public String worldName;
   public String worldOwnerName;
   public UUID worldOwnerUuid;
   public Date date;

   public static PendingInvite parse(JsonObject p_87431_) {
      PendingInvite pendinginvite = new PendingInvite();

      try {
         pendinginvite.invitationId = JsonUtils.getStringOr("invitationId", p_87431_, "");
         pendinginvite.worldName = JsonUtils.getStringOr("worldName", p_87431_, "");
         pendinginvite.worldOwnerName = JsonUtils.getStringOr("worldOwnerName", p_87431_, "");
         pendinginvite.worldOwnerUuid = JsonUtils.getUuidOr("worldOwnerUuid", p_87431_, Util.NIL_UUID);
         pendinginvite.date = JsonUtils.getDateOr("date", p_87431_);
      } catch (Exception exception) {
         LOGGER.error("Could not parse PendingInvite: {}", exception.getMessage());
      }

      return pendinginvite;
   }
}
