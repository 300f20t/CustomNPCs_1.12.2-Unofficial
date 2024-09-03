package noppes.npcs.items;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArrow;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomRegisters;
import noppes.npcs.api.ICustomElement;
import noppes.npcs.api.INbt;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.util.Util;

import java.util.Objects;

public class CustomBow extends ItemBow implements ICustomElement {

	protected NBTTagCompound nbtData;
	protected ItemStack repairItemStack;
	protected int enchantability = 0;
	private final Item.ToolMaterial material;

	public ItemStack itemArrow;
	protected boolean isFlame;
	private float critChance = 0.0f;
	private double attackDamage = 2.0d;

	protected float speed = 30.0f;

	public CustomBow(NBTTagCompound nbtItem) {
		super();
		this.nbtData = nbtItem;
		this.setRegistryName(CustomNpcs.MODID, "custom_" + nbtItem.getString("RegistryName"));
		this.setUnlocalizedName("custom_" + nbtItem.getString("RegistryName"));
		this.itemArrow = nbtItem.hasKey("Bullet", 10) ? new ItemStack(nbtItem.getCompoundTag("Bullet"))
				: ItemStack.EMPTY;
		this.isFlame = nbtItem.getBoolean("SetFlame");
		if (nbtItem.hasKey("CritChance", 5)) {
			this.critChance = nbtItem.getFloat("CritChance");
		}
		this.material = CustomItem.getMaterialTool(nbtItem);

		if (nbtItem.hasKey("DrawstringSpeed", 5)) {
			this.speed = nbtItem.getFloat("DrawstringSpeed");
		}
		if (nbtItem.getInteger("MaxStackDamage") > 1) {
			this.setMaxDamage(nbtItem.getInteger("MaxStackDamage"));
		}
		if (nbtItem.hasKey("EntityDamage", 6)) {
			this.attackDamage = nbtItem.getDouble("EntityDamage");
		}
		if (nbtItem.hasKey("RepairItem", 10)) {
			this.repairItemStack = new ItemStack(nbtItem.getCompoundTag("RepairItem"));
		} else {
			this.repairItemStack = this.material.getRepairItemStack();
		}
		if (nbtItem.hasKey("IsFull3D", 1) && nbtItem.getBoolean("IsFull3D")) {
			this.setFull3D();
		}
		this.setCreativeTab(CustomRegisters.tabItems);

		this.addPropertyOverride(new ResourceLocation("pull"), new IItemPropertyGetter() {
			@SideOnly(Side.CLIENT)
			public float apply(@Nonnull ItemStack stack, @Nullable World worldIn, @Nullable EntityLivingBase entityIn) {
				float f = 0.0f;
				if (entityIn != null) {
					f = (!(entityIn.getActiveItemStack().getItem() instanceof ItemBow)) ? 0.0F
							: (float) (stack.getMaxItemUseDuration() - entityIn.getItemInUseCount())
									/ ((CustomBow) stack.getItem()).speed;
				}
				return f;
			}
		});
	}

	protected @Nonnull ItemStack findAmmo(@Nonnull EntityPlayer player) {
		if (this.isArrow(player.getHeldItem(EnumHand.OFF_HAND))) {
			return player.getHeldItem(EnumHand.OFF_HAND);
		} else if (this.isArrow(player.getHeldItem(EnumHand.MAIN_HAND))) {
			return player.getHeldItem(EnumHand.MAIN_HAND);
		} else {
			for (int i = 0; i < player.inventory.getSizeInventory(); ++i) {
				ItemStack itemstack = player.inventory.getStackInSlot(i);
				if (this.isArrow(itemstack)) {
					return itemstack;
				}
			}
			return ItemStack.EMPTY;
		}
	}

	@Override
	public String getCustomName() {
		return this.nbtData.getString("RegistryName");
	}

	@Override
	public INbt getCustomNbt() {
		return Objects.requireNonNull(NpcAPI.Instance()).getINbt(this.nbtData);
	}

	public boolean getIsRepairable(@Nonnull ItemStack toRepair, @Nonnull ItemStack repair) {
		ItemStack mat = this.repairItemStack;
		if (this.repairItemStack.isEmpty()) {
			mat = this.material.getRepairItemStack();
		}
		if (!mat.isEmpty() && net.minecraftforge.oredict.OreDictionary.itemMatches(mat, repair, false)) {
			return true;
		}
		return super.getIsRepairable(toRepair, repair);
	}

	public int getItemEnchantability() {
		if (this.enchantability > 0) {
			return this.enchantability;
		}
		return super.getItemEnchantability();
	}

	public void getSubItems(@Nonnull CreativeTabs tab, @Nonnull NonNullList<ItemStack> items) {
		if (tab != CustomRegisters.tabItems && tab != CreativeTabs.SEARCH) { return; }
		if (this.nbtData != null && this.nbtData.hasKey("ShowInCreative", 1) && !this.nbtData.getBoolean("ShowInCreative")) { return; }
		items.add(new ItemStack(this));
		if (tab == CustomRegisters.tabItems) { Util.instance.sort(items); }
	}

	protected boolean isArrow(@Nonnull ItemStack stack) {
		if (this.itemArrow != null && !this.itemArrow.isEmpty()) {
			return stack.isItemEqualIgnoreDurability(this.itemArrow);
		}
		return stack.getItem() instanceof ItemArrow;
	}

	public void onPlayerStoppedUsing(@Nonnull ItemStack stack, @Nonnull World worldIn, @Nonnull EntityLivingBase entityLiving, int timeLeft) {
		if (!(entityLiving instanceof EntityPlayer)) {
			return;
		}
		EntityPlayer entityplayer = (EntityPlayer) entityLiving;
		boolean flag = entityplayer.capabilities.isCreativeMode
				|| EnchantmentHelper.getEnchantmentLevel(Enchantments.INFINITY, stack) > 0;
		ItemStack itemstack = this.findAmmo(entityplayer);
		int i = this.getMaxItemUseDuration(stack) - timeLeft;
		i = net.minecraftforge.event.ForgeEventFactory.onArrowLoose(stack, worldIn, entityplayer, i,
				!itemstack.isEmpty() || flag);
		if (i < 0 || (itemstack.isEmpty() && !flag)) {
			return;
		}
		if (itemstack.isEmpty()) {
			itemstack = new ItemStack(Items.ARROW);
		}
		float f = getArrowVelocity(i);
		if ((double) f < 0.1D) {
			return;
		}
		boolean flag1 = entityplayer.capabilities.isCreativeMode || (itemstack.getItem() instanceof ItemArrow
				&& ((ItemArrow) itemstack.getItem()).isInfinite(itemstack, stack, entityplayer));
		if (!worldIn.isRemote) {
			ItemArrow itemarrow = (ItemArrow) (itemstack.getItem() instanceof ItemArrow ? itemstack.getItem()
					: Items.ARROW);
			EntityArrow entityarrow = itemarrow.createArrow(worldIn, itemstack, entityplayer);
			entityarrow.shoot(entityplayer, entityplayer.rotationPitch, entityplayer.rotationYaw, 0.0F, f * 3.0F, 1.0F);
			if (f == 1.0F) {
				entityarrow.setIsCritical(!(this.critChance > 0.0f) || !(this.critChance <= 1.0f) || Item.itemRand.nextFloat() < this.critChance);
			}
			int j = EnchantmentHelper.getEnchantmentLevel(Enchantments.POWER, stack);
			double damage = (this.attackDamage > 0.0d ? this.attackDamage : entityarrow.getDamage())
					* (i > 40 ? 1.0d : (double) i / 40.0d);
			entityarrow.setDamage(damage);
			if (j > 0) {
				entityarrow.setDamage(damage + (double) j * 0.5D + 0.5D);
			}

			int k = EnchantmentHelper.getEnchantmentLevel(Enchantments.PUNCH, stack);
			if (k > 0) {
				entityarrow.setKnockbackStrength(k);
			}

			if (this.isFlame || EnchantmentHelper.getEnchantmentLevel(Enchantments.FLAME, stack) > 0) {
				entityarrow.setFire(100);
			}
			stack.damageItem(1, entityplayer);

			if (flag1 || entityplayer.capabilities.isCreativeMode
					&& (itemstack.getItem() == Items.SPECTRAL_ARROW || itemstack.getItem() == Items.TIPPED_ARROW)) {
				entityarrow.pickupStatus = EntityArrow.PickupStatus.CREATIVE_ONLY;
			}
			worldIn.spawnEntity(entityarrow);
		}
		worldIn.playSound(null, entityplayer.posX, entityplayer.posY, entityplayer.posZ,
				SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.0F,
				1.0F / (itemRand.nextFloat() * 0.4F + 1.2F) + f * 0.5F);
		if (!flag1 && !entityplayer.capabilities.isCreativeMode) {
			itemstack.shrink(1);
			if (itemstack.isEmpty()) {
				entityplayer.inventory.deleteStack(itemstack);
			}
		}
		entityplayer.addStat(Objects.requireNonNull(StatList.getObjectUseStats(this)));
	}

	@Override
	public int getType() {
		if (this.nbtData != null && this.nbtData.hasKey("ItemType", 1)) { return this.nbtData.getByte("ItemType"); }
		return 5;
	}

}
