package noppes.npcs.mixin.item;

import net.minecraft.item.ItemTool;
import noppes.npcs.mixin.api.item.ItemToolAPIMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ItemTool.class, remap = false)
public class ItemToolMixin implements ItemToolAPIMixin {

    @Mutable
    @Shadow(aliases = "toolClass")
    private String toolClass;

    @Override
    public void npcs$setToolClass(String newToolClass) {
        if (toolClass == null || toolClass.isEmpty()) { return; }
        toolClass = newToolClass;
    }

}
