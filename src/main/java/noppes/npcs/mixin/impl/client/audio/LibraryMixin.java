package noppes.npcs.mixin.impl.client.audio;

import noppes.npcs.mixin.client.audio.ILibraryMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import paulscode.sound.Library;
import paulscode.sound.Source;

import java.util.HashMap;

@Mixin(value = Library.class, remap = false)
public class LibraryMixin implements ILibraryMixin {

    @Shadow
    protected HashMap<String, Source> sourceMap;

    @Override
    public HashMap<String, Source> npcs$getSourceMap() { return sourceMap; }

}
