package noppes.npcs.ai.attack;

import net.minecraft.entity.IRangedAttackMob;
import noppes.npcs.util.AdditionalMethods;

public class EntityAIOnslaught
extends EntityAICustom {

	public EntityAIOnslaught(IRangedAttackMob npc) {
		super(npc);
	}

	@Override
	public void updateTask() {
		super.updateTask();
		if (this.isFrend || this.npc.ticksExisted % (this.tickRate * 2) > 3) { return; }
		if (this.isRanged) { this.canSeeToAttack = AdditionalMethods.npcCanSeeTarget(this.npc, this.target, true, true); }
		else { this.canSeeToAttack = this.npc.canSee(this.target); }
		
		if (this.canSeeToAttack&& this.distance <= this.range) {
			if (this.inMove) { this.npc.getNavigator().clearPath(); }
		}
		else { this.tryMoveToTarget(); }
		
		this.tryToCauseDamage();
	}
	
}