package noppes.npcs.mixin.client.resources;

import net.minecraft.client.resources.IResourcePack;

import java.util.List;

public interface IFallbackResourceManagerMixin {

    List<IResourcePack> npcs$getResourcePacks();

}
