package noppes.npcs.mixin.client.resources;

import net.minecraft.client.resources.LanguageManager;
import net.minecraft.client.resources.Locale;
import noppes.npcs.mixin.api.client.resources.LanguageManagerAPIMixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = LanguageManager.class)
public class LanguageManagerMixin implements LanguageManagerAPIMixin {

    @Final
    @Shadow(aliases = "CURRENT_LOCALE")
    protected static Locale CURRENT_LOCALE;

    @Shadow(aliases = "currentLanguage")
    private String currentLanguage;

    @Override
    public Locale npcs$getCurrentLocate() { return CURRENT_LOCALE; }

    @Override
    public String npcs$getCurrentLanguage() { return currentLanguage; }

}
