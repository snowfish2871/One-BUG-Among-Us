package net.minecraft.client.renderer.texture.atlas;

import com.mojang.serialization.Codec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record SpriteSourceType(Codec<? extends SpriteSource> codec) {
}
