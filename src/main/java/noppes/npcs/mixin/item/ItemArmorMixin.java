package noppes.npcs.mixin.item;

import net.minecraft.item.ItemArmor;
import noppes.npcs.mixin.api.item.ItemArmorAPIMixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ItemArmor.class)
public class ItemArmorMixin implements ItemArmorAPIMixin {

    @Mutable
    @Final
    @Shadow(aliases = "damageReduceAmount")
    public int damageReduceAmount;

    @Mutable
    @Final
    @Shadow(aliases = "toughness")
    public float toughness;

    @Override
    public void npcs$setDamageReduceAmount(int newDamageReduceAmount) {
        if (newDamageReduceAmount < 0) { newDamageReduceAmount *= -1; }
        if (newDamageReduceAmount <1) { newDamageReduceAmount = 1; }
        damageReduceAmount = newDamageReduceAmount;
    }

    @Override
    public void npcs$setToughness(float newToughness) {
        if (newToughness < 0.0f) { newToughness *= -1.0f; }
        toughness = newToughness;
    }

}
