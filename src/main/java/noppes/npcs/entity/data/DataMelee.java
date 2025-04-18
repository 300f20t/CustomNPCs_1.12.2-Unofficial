package noppes.npcs.entity.data;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.api.entity.data.INPCMelee;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.util.ValueUtil;

public class DataMelee implements INPCMelee {

	private static final int version = 1;
	private double attackRange = 2.0d;
	private int attackSpeed = 20;
	private int attackStrength = 5;
	private int knockback = 0;
	private final EntityNPCInterface npc;
	private int potionAmp = 0;
	private int potionDuration = 5;
	private int potionType = 0;

	public DataMelee(EntityNPCInterface npc) {
		this.npc = npc;
	}

	@Override
	public int getDelay() {
		return this.attackSpeed;
	}

	public int getDelayRNG() {
		int delay = this.attackSpeed;
		if (this.attackSpeed < 120 && this.attackSpeed > 10) {
			delay += this.npc.world.rand.nextInt((int) ((double) this.attackSpeed * 0.15d));
		}
		return delay;
	}

	@Override
	public int getEffectStrength() {
		return this.potionAmp;
	}

	@Override
	public int getEffectTime() {
		return this.potionDuration;
	}

	@Override
	public int getEffectType() {
		return this.potionType;
	}

	@Override
	public int getKnockback() {
		return this.knockback;
	}

	@Override
	public double getRange() {
		return this.attackRange;
	}

	@Override
	public int getStrength() {
		return this.attackStrength;
	}

	public void readFromNBT(NBTTagCompound compound) {
		this.attackSpeed = compound.getInteger("AttackSpeed");
		this.setStrength(compound.getInteger("AttackStrenght"));
		if (compound.hasKey("AttackRange", 3)) {
			this.attackRange = (float) compound.getInteger("AttackRange");
		} else {
			this.attackRange = compound.getDouble("AttackRange");
		}
		this.knockback = compound.getInteger("KnockBack");
		if (version != compound.getInteger("version")) {
			int v = compound.getInteger("version");
			if (v < 1) { knockback++; }
		}
		this.potionType = compound.getInteger("PotionEffect");
		this.potionDuration = compound.getInteger("PotionDuration");
		this.potionAmp = compound.getInteger("PotionAmp");
	}

	@Override
	public void setDelay(int speed) {
		this.attackSpeed = speed;
	}

	@Override
	public void setEffect(int type, int strength, int time) {
		this.potionType = type;
		this.potionDuration = time;
		this.potionAmp = strength;
	}

	@Override
	public void setKnockback(int knockback) {
		this.knockback = knockback;
	}

	@Override
	public void setRange(double range) {
		this.attackRange = ValueUtil.correctDouble(range, 0.2d, 30.0d);
	}

	@Override
	public void setStrength(int strength) {
		this.attackStrength = strength;
		this.npc.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(this.attackStrength);
	}

	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setInteger("AttackStrenght", this.attackStrength);
		compound.setInteger("AttackSpeed", this.attackSpeed);
		compound.setDouble("AttackRange", this.attackRange);
		compound.setInteger("KnockBack", this.knockback);
		compound.setInteger("PotionEffect", this.potionType);
		compound.setInteger("PotionDuration", this.potionDuration);
		compound.setInteger("PotionAmp", this.potionAmp);
		compound.setInteger("version", version);
		return compound;
	}

}
