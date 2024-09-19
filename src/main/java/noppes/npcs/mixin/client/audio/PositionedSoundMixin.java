package noppes.npcs.mixin.client.audio;

import net.minecraft.client.audio.PositionedSound;
import noppes.npcs.mixin.api.client.audio.PositionedSoundAPIMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PositionedSound.class)
public class PositionedSoundMixin implements PositionedSoundAPIMixin {

    @Mutable
    @Shadow(aliases = "xPosF")
    protected float xPosF;

    @Mutable
    @Shadow(aliases = "yPosF")
    protected float yPosF;

    @Mutable
    @Shadow(aliases = "zPosF")
    protected float zPosF;

    @Override
    public void npcs$setXPosF(float newXPosF) { xPosF = newXPosF; }

    @Override
    public void npcs$setYPosF(float newYPosF) { yPosF = newYPosF; }

    @Override
    public void npcs$setZPosF(float newZPosF) {zPosF = newZPosF; }

}
