package noppes.npcs.ai.movement;

import java.util.Random;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import noppes.npcs.CustomNpcs;
import noppes.npcs.constants.AiMutex;

public class EntityAIMoveIndoors extends EntityAIBase {

	private double shelterX;
	private double shelterY;
	private double shelterZ;
	private final EntityCreature theCreature;
	private final World world;

	public EntityAIMoveIndoors(EntityCreature par1EntityCreature) {
		this.theCreature = par1EntityCreature;
		this.world = par1EntityCreature.world;
		this.setMutexBits(AiMutex.PASSIVE);
	}

	private Vec3d findPossibleShelter() {
		Random random = this.theCreature.getRNG();
		BlockPos blockpos = new BlockPos(this.theCreature.posX, this.theCreature.getEntityBoundingBox().minY,
				this.theCreature.posZ);
		for (int i = 0; i < 10; ++i) {
			BlockPos blockpos2 = blockpos.add(random.nextInt(20) - 10, random.nextInt(6) - 3, random.nextInt(20) - 10);
			if (!this.world.canSeeSky(blockpos2) && this.theCreature.getBlockPathWeight(blockpos2) < 0.0f) {
				return new Vec3d(blockpos2.getX(), blockpos2.getY(), blockpos2.getZ());
			}
		}
		return null;
	}

	public boolean shouldContinueExecuting() {
		return !this.theCreature.getNavigator().noPath();
	}

	public boolean shouldExecute() {
		CustomNpcs.debugData.start(theCreature, this, "shouldExecute");
		if ((this.theCreature.world.isDaytime() && !this.theCreature.world.isRaining())
				|| this.theCreature.world.provider.hasSkyLight()) {
			CustomNpcs.debugData.end(theCreature, this, "shouldExecute");
			return false;
		}
		BlockPos pos = new BlockPos(this.theCreature.posX, this.theCreature.getEntityBoundingBox().minY,
				this.theCreature.posZ);
		if (!this.world.canSeeSky(pos) && this.world.getLight(pos) > 8) {
			CustomNpcs.debugData.end(theCreature, this, "shouldExecute");
			return false;
		}
		Vec3d var1 = this.findPossibleShelter();
		if (var1 == null) {
			CustomNpcs.debugData.end(theCreature, this, "shouldExecute");
			return false;
		}
		this.shelterX = var1.x;
		this.shelterY = var1.y;
		this.shelterZ = var1.z;
		CustomNpcs.debugData.end(theCreature, this, "shouldExecute");
		return true;
	}

	public void startExecuting() {
		CustomNpcs.debugData.start(theCreature, this, "startExecuting");
		this.theCreature.getNavigator().tryMoveToXYZ(this.shelterX, this.shelterY, this.shelterZ, 1.0);
		CustomNpcs.debugData.end(theCreature, this, "startExecuting");
	}
}
