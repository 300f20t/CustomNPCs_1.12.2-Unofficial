package noppes.npcs.quests;

import java.util.HashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.DimensionManager;
import noppes.npcs.NBTTags;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.Server;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.IPos;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.handler.data.IDialog;
import noppes.npcs.api.handler.data.IQuestObjective;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.QuestData;
import noppes.npcs.util.ValueUtil;

public class QuestObjective
implements IQuestObjective {

	private int id = 0;
	private boolean ignoreDamage = false;
	private boolean ignoreNBT = false;
	private ItemStack item = ItemStack.EMPTY;
	private boolean leaveItem = false;
	private int maxProgress = 1;
	private String name = "";
	// private final QuestInterface parent;
	private int parentID = 0;
	private final EntityPlayer player;
	private int range = 10;
	public int slotID = 0;
	private EnumQuestTask type = EnumQuestTask.ITEM;
	public BlockPos pos = new BlockPos(0, 0, 0);
	public int dimensionID = 0, rangeCompass = 5;
	public String entityName = "";

	public QuestObjective(int parentID, EntityPlayer player) {
		this.parentID = parentID;
		this.player = player;
	}

	public QuestObjective(int parentID, EnumQuestTask type) {
		this.parentID = parentID;
		this.type = type;
		this.player = null;
	}

	public QuestObjective copyToPlayer(EntityPlayer player) {
		QuestObjective newObj = new QuestObjective(this.parentID, player);
		newObj.type = this.type;
		newObj.maxProgress = this.maxProgress;
		newObj.id = this.id;
		newObj.range = this.range;
		newObj.name = this.name;
		newObj.item = this.item;
		newObj.leaveItem = this.leaveItem;
		newObj.ignoreDamage = this.ignoreDamage;
		newObj.ignoreNBT = this.ignoreNBT;
		newObj.pos = this.pos;
		newObj.dimensionID = this.dimensionID;
		newObj.rangeCompass = this.rangeCompass;
		newObj.entityName = this.entityName;
		return newObj;
	}

	@Override
	public int getAreaRange() {
		return this.range;
	}

	public HashMap<ItemStack, Integer> getCrafted(QuestData data) {
		if (!data.extraData.hasKey("Crafts", 9)) {
			data.extraData.setTag("Crafts", new NBTTagList());
		}
		return NBTTags.getItemStackIntegerMap(data.extraData.getTagList("Crafts", 10));
	}

	public EnumQuestTask getEnumType() {
		return this.type;
	}

	@Override
	public IItemStack getItem() {
		return NpcAPI.Instance().getIItemStack(this.item);
	}

	public ItemStack getItemStack() {
		return this.item;
	}

	public HashMap<String, Integer> getKilled(QuestData data) {
		if (!data.extraData.hasKey("Targets", 9)) {
			data.extraData.setTag("Targets", new NBTTagList());
		}
		return NBTTags.getStringIntegerMap(data.extraData.getTagList("Targets", 10));
	}

	@Override
	public int getMaxProgress() {
		if (this.type == EnumQuestTask.DIALOG || this.type == EnumQuestTask.LOCATION) {
			return 1;
		}
		return this.maxProgress;
	}

	public NBTTagCompound getNBT() {
		NBTTagCompound nbtTask = new NBTTagCompound();
		nbtTask.setInteger("Type", this.type.ordinal());
		NBTTagCompound nbtCompass = new NBTTagCompound();
		nbtCompass.setIntArray("Pos", new int[] { this.pos.getX(), this.pos.getY(), this.pos.getZ()});
		nbtCompass.setInteger("DimensionID", this.dimensionID);
		nbtCompass.setInteger("Range", this.rangeCompass);
		nbtCompass.setString("EntityName", this.entityName);
		nbtTask.setTag("CompassData", nbtCompass);
		
		if (this.maxProgress > 0) {
			nbtTask.setInteger("Progress", this.maxProgress);
		}
		if (this.id > 0) {
			nbtTask.setInteger("TargetID", this.id);
		}
		if (!this.name.isEmpty()) {
			nbtTask.setString("TargetName", this.name);
		}

		if (this.type == EnumQuestTask.AREAKILL) {
			nbtTask.setInteger("Range", this.range);
		}

		if (!this.item.isEmpty()) {
			nbtTask.setTag("Item", this.item.writeToNBT(new NBTTagCompound()));
			nbtTask.setBoolean("LeaveItem", this.leaveItem);
			nbtTask.setBoolean("IgnoreDamage", this.ignoreDamage);
			nbtTask.setBoolean("IgnoreNBT", this.ignoreNBT);
		}
		return nbtTask;
	}

	@Override
	public int getProgress() {
		if (this.type == EnumQuestTask.ITEM) {
			int count = 0;
			for (int i = 0; i < this.player.inventory.getSizeInventory(); ++i) {
				ItemStack item = this.player.inventory.getStackInSlot(i);
				if (!NoppesUtilServer.IsItemStackNull(item)) {
					if (NoppesUtilPlayer.compareItems(this.item, item, this.ignoreDamage, this.ignoreNBT)) {
						count += item.getCount();
					}
				}
			}
			return ValueUtil.correctInt(count, 0, this.maxProgress);
		}
		PlayerData data = PlayerData.get(this.player);
		QuestData questData = data.questData.activeQuests.get(this.parentID);
		if (this.type == EnumQuestTask.DIALOG) {
			return data.dialogData.dialogsRead.contains(this.id) ? 1 : 0;
		}
		if (this.type == EnumQuestTask.LOCATION) {

			return 0;
		}
		if (questData == null) {
			return 0;
		}
		if (this.type == EnumQuestTask.KILL || this.type == EnumQuestTask.AREAKILL
				|| this.type == EnumQuestTask.MANUAL) {
			HashMap<String, Integer> killed = this.getKilled(questData);
			if (!killed.containsKey(this.name)) {
				return 0;
			}
			return killed.get(this.name);
		}
		if (this.type == EnumQuestTask.CRAFT) {
			HashMap<ItemStack, Integer> crafted = this.getCrafted(questData);
			for (ItemStack item : crafted.keySet()) {
				if (NoppesUtilPlayer.compareItems(this.item, item, this.ignoreDamage, this.ignoreNBT)) {
					return crafted.get(item);
				}
			}
		}
		return 0;
	}

	@Override
	public int getTargetID() {
		return this.id;
	}

	@Override
	public String getTargetName() {
		return this.name;
	}

	@Override
	public String getText() {
		String done = "";
		if (this.type == EnumQuestTask.ITEM) { // Collect Item
			done = new TextComponentTranslation("quest.task.item." + (!this.isCompleted() ? "1" : "0")).getFormattedText();
			return this.item.getDisplayName() + ": " + this.getProgress() + "/" + this.getMaxProgress() + done
					+ (this.leaveItem ? new TextComponentTranslation("quest.take.log").getFormattedText() : "");
		}
		if (this.type == EnumQuestTask.CRAFT) { // Craft Item
			done = new TextComponentTranslation("quest.task.craft." + (!this.isCompleted() ? "1" : "0"))
					.getFormattedText();
			return this.item.getDisplayName() + ": " + this.getProgress() + "/" + this.getMaxProgress() + done
					+ (this.leaveItem ? new TextComponentTranslation("quest.take.log").getFormattedText() : "");
		}
		if (this.type == EnumQuestTask.DIALOG) { // Dialog
			done = new TextComponentTranslation("quest.task.dialog." + (!this.isCompleted() ? "1" : "0"))
					.getFormattedText();
			String name = "null";
			Dialog dialog = DialogController.instance.dialogs.get(this.id);
			if (dialog != null) {
				name = new TextComponentTranslation(dialog.title).getFormattedText();
			}
			return name + done;
		}
		if (this.type == EnumQuestTask.KILL || this.type == EnumQuestTask.AREAKILL) { // Kill
			String name = "entity." + this.name + ".name";
			String transName = new TextComponentTranslation(name).getFormattedText();
			if (transName.indexOf("entity.") >= 0 && transName.indexOf(".name") > 0) {
				transName = this.name;
			}
			done = new TextComponentTranslation("quest.task.kill." + (!this.isCompleted() ? "1" : "0"))
					.getFormattedText();
			return transName + ": " + this.getProgress() + "/" + this.getMaxProgress() + done;
		}
		if (this.type == EnumQuestTask.LOCATION) { // Location
			done = new TextComponentTranslation("quest.task.location." + (!this.isCompleted() ? "1" : "0"))
					.getFormattedText();
			return this.name + ":" + done;
		}
		if (this.type == EnumQuestTask.MANUAL) { // Manual
			done = new TextComponentTranslation("quest.task.manual." + (!this.isCompleted() ? "1" : "0"))
					.getFormattedText();
			return this.name + ": " + this.getProgress() + "/" + this.getMaxProgress() + done;
		}
		return "null type: " + this.type + " #" + this.toString().substring(this.toString().indexOf("@") + 1);
	}

	@Override
	public int getType() {
		return this.type.ordinal();
	}

	@Override
	public boolean isCompleted() {
		if (this.type == EnumQuestTask.ITEM) {
			return NoppesUtilPlayer.compareItems(this.player, this.item, this.ignoreDamage, this.ignoreNBT, this.maxProgress);
		}
		try {
			PlayerData data = PlayerData.get(this.player);
			QuestData questData = data.questData.activeQuests.get(this.parentID);
			if (this.type == EnumQuestTask.DIALOG) {
				return data.dialogData.dialogsRead.contains(this.id);
			} else if (this.type == EnumQuestTask.LOCATION) {
				for (NBTBase dataNBT : questData.extraData.getTagList("Locations", 10)) {
					if (this.name.equalsIgnoreCase(((NBTTagCompound) dataNBT).getString("Location"))) {
						return ((NBTTagCompound) dataNBT).getBoolean("Found");
					}
				}
			}
		} catch (Exception e) { return false; }
		return this.getProgress() >= this.maxProgress;
	}

	@Override
	public boolean isIgnoreDamage() {
		return this.ignoreDamage;
	}

	@Override
	public boolean isItemIgnoreNBT() {
		return this.ignoreNBT;
	}

	@Override
	public boolean isItemLeave() {
		return this.leaveItem;
	}

	public void load(NBTTagCompound nbtTask) {
		this.type = EnumQuestTask.values()[nbtTask.getInteger("Type")];
		if (nbtTask.hasKey("CompassData", 10)) {
			NBTTagCompound nbtCompass = nbtTask.getCompoundTag("CompassData");
			int[] bp = nbtCompass.getIntArray("Pos");
			this.pos = new BlockPos(bp[0], bp[1], bp[2]);
			this.dimensionID = nbtCompass.getInteger("DimensionID");
			this.rangeCompass = nbtCompass.getInteger("Range");
			this.entityName = nbtCompass.getString("EntityName");
		}
		if (nbtTask.hasKey("Progress", 3)) {
			this.setMaxProgress(nbtTask.getInteger("Progress"));
		}
		if (nbtTask.hasKey("TargetID", 3)) {
			this.setTargetID(nbtTask.getInteger("TargetID"));
		}
		if (nbtTask.hasKey("TargetName", 8)) {
			this.setTargetName(nbtTask.getString("TargetName"));
		}

		if (nbtTask.hasKey("Range", 3)) {
			this.setAreaRange(nbtTask.getInteger("Range"));
		}

		if (nbtTask.hasKey("Item", 10)) {
			this.setItem(new ItemStack(nbtTask.getCompoundTag("Item")));
			// this.setMaxProgress(this.item.getCount());
			this.leaveItem = nbtTask.getBoolean("LeaveItem");
			this.ignoreDamage = nbtTask.getBoolean("IgnoreDamage");
			this.ignoreNBT = nbtTask.getBoolean("IgnoreNBT");
		}
	}

	@Override
	public void setAreaRange(int range) {
		if (range < 3 || range > 24) {
			throw new CustomNPCsException("Range must be between 3 and 24");
		}
		this.range = range;
	}

	public void setCrafted(QuestData data, HashMap<ItemStack, Integer> crafted) {
		data.extraData.setTag("Crafts", NBTTags.nbtItemStackIntegerMap(crafted));
	}

	@Override
	public void setItem(IItemStack item) {
		this.item = item.getMCItemStack();
	}

	public void setItem(ItemStack item) {
		this.item = item;
	}

	@Override
	public void setItemIgnoreDamage(boolean bo) {
		this.ignoreDamage = bo;
	}

	@Override
	public void setItemIgnoreNBT(boolean bo) {
		this.ignoreNBT = bo;
	}

	@Override
	public void setItemLeave(boolean bo) {
		this.leaveItem = bo;
	}

	public void setKilled(QuestData data, HashMap<String, Integer> killed) {
		data.extraData.setTag("Targets", (NBTBase) NBTTags.nbtStringIntegerMap(killed));
	}

	@Override
	public void setMaxProgress(int value) {
		if (value < 1 || value > 10000000) {
			throw new CustomNPCsException("Progress must be between 1 and 10000000");
		} else if ((this.type == EnumQuestTask.DIALOG || this.type == EnumQuestTask.LOCATION) && value > 1) {
			throw new CustomNPCsException("Progress has to be 0 or 1");
		}
		this.maxProgress = value;
	}

	@Override
	public void setProgress(int progress) {
		if (this.type == EnumQuestTask.ITEM) {
			throw new CustomNPCsException("Cant set the progress of ItemTask");
		}
		PlayerData data = PlayerData.get(this.player);
		QuestData questData = data.questData.activeQuests.get(this.parentID);
		if (this.type == EnumQuestTask.DIALOG) {
			if (progress < 0 || progress > 1) {
				throw new CustomNPCsException("Progress has to be 0 or 1");
			}
			boolean completed = data.dialogData.dialogsRead.contains(this.id);
			if (progress == 0 && completed) {
				data.dialogData.dialogsRead.remove(this.id);
			}
			if (progress == 1 && !completed) {
				data.dialogData.dialogsRead.add(this.id);
			}
			// Message
			String dialog = "N/A";
			IDialog d = DialogController.instance.get(progress);
			if (d != null) {
				dialog = d.getName();
			}
			NBTTagCompound compound = new NBTTagCompound();
			compound.setInteger("QuestID", questData.quest.id);
			compound.setString("Type", "dialog");
			compound.setIntArray("Progress", new int[] { progress, 1 });
			compound.setString("TargetName", dialog);
			compound.setInteger("MessageType", 0);
			Server.sendData((EntityPlayerMP) this.player, EnumPacketClient.MESSAGE_DATA, compound);
			this.player.sendMessage(new TextComponentTranslation("quest.message.dialog." + progress,
					new TextComponentTranslation(dialog).getFormattedText(), questData.quest.getTitle()));

			data.updateClient = true;
		} else if (this.type == EnumQuestTask.LOCATION) {
			if (progress < 0 || progress > 1) {
				throw new CustomNPCsException("Progress has to be 0 or 1");
			}
			for (NBTBase dataNBT : questData.extraData.getTagList("Locations", 10)) {
				if (this.name.equalsIgnoreCase(((NBTTagCompound) dataNBT).getString("Location"))) {
					boolean completed = ((NBTTagCompound) dataNBT).getBoolean("Found");
					if ((completed && progress == 1) || (!completed && progress == 0)) {
						return;
					}
					((NBTTagCompound) dataNBT).setBoolean("Found", progress == 1);
					break;
				}
			}
			// Message
			NBTTagCompound compound = new NBTTagCompound();
			compound.setInteger("QuestID", questData.quest.id);
			compound.setString("Type", "location");
			compound.setIntArray("Progress", new int[] { progress, 1 });
			compound.setString("TargetName", this.name);
			compound.setInteger("MessageType", 0);
			Server.sendData((EntityPlayerMP) this.player, EnumPacketClient.MESSAGE_DATA, compound);
			this.player.sendMessage(new TextComponentTranslation("quest.message.location." + progress,
					new TextComponentTranslation(this.name).getFormattedText(), questData.quest.getTitle()));

			data.updateClient = true;
		} else if (this.type == EnumQuestTask.KILL || this.type == EnumQuestTask.AREAKILL
				|| this.type == EnumQuestTask.MANUAL) {
			if (progress < 0 || progress > this.maxProgress) {
				throw new CustomNPCsException("Progress has to be between 0 and " + this.maxProgress);
			}
			HashMap<String, Integer> killed = this.getKilled(questData);
			if (killed.containsKey(this.name) && killed.get(this.name) == progress) {
				return;
			}
			killed.put(this.name, progress);
			this.setKilled(questData, killed);
			// Message
			String key = this.type == EnumQuestTask.MANUAL ? "manual" : "kill";
			NBTTagCompound compound = new NBTTagCompound();
			compound.setInteger("QuestID", questData.quest.id);
			compound.setString("Type", key);
			compound.setIntArray("Progress", new int[] { progress, this.maxProgress });
			compound.setString("TargetName", this.name);
			compound.setInteger("MessageType", 0);
			Server.sendData((EntityPlayerMP) this.player, EnumPacketClient.MESSAGE_DATA, compound);
			this.player.sendMessage(new TextComponentTranslation("quest.message." + key + ".0",
					new TextComponentTranslation(this.name).getFormattedText(), "" + progress, "" + this.maxProgress,
					questData.quest.getTitle()));
			if (progress >= this.maxProgress) {
				this.player.sendMessage(new TextComponentTranslation("quest.message." + key + ".1",
						new TextComponentTranslation(this.name).getFormattedText(), questData.quest.getTitle()));
			}

			data.updateClient = true;
		} else if (this.type == EnumQuestTask.CRAFT) {
			if (progress < 0 || progress > this.maxProgress) {
				throw new CustomNPCsException("Progress has to be between 0 and " + this.maxProgress);
			}
			HashMap<ItemStack, Integer> crafted = this.getCrafted(questData);
			for (ItemStack item : crafted.keySet()) {
				if (NoppesUtilPlayer.compareItems(this.item, item, this.ignoreDamage, this.ignoreNBT)) {
					if (crafted.get(item) == progress) {
						continue;
					}
					crafted.put(item, progress);
					break;
				}
			}
			this.setCrafted(questData, crafted);
			// Message
			NBTTagCompound compound = new NBTTagCompound();
			compound.setInteger("QuestID", questData.quest.id);
			compound.setString("Type", "craft");
			compound.setIntArray("Progress", new int[] { progress, this.maxProgress });
			compound.setString("TargetName", this.item.getDisplayName());
			compound.setInteger("MessageType", 0);
			Server.sendData((EntityPlayerMP) this.player, EnumPacketClient.MESSAGE_DATA, compound);
			if (progress >= this.maxProgress) {
				this.player.sendMessage(new TextComponentTranslation("quest.message.craft.1",
						this.item.getDisplayName(), questData.quest.getTitle()));
			} else {
				this.player.sendMessage(new TextComponentTranslation("quest.message.craft.0",
						this.item.getDisplayName(), "" + progress, "" + this.maxProgress, questData.quest.getTitle()));
			}

			data.updateClient = true;
		}
		for (IQuestObjective obj : questData.quest.getObjectives((IPlayer<?>) NpcAPI.Instance().getIEntity(player))) {
			if (((QuestObjective) obj).getEnumType() != this.type) { continue; }
			data.questData.checkQuestCompletion(this.player, questData);
		}
	}

	@Override
	public void setTargetID(int id) {
		if (id < 0) {
			throw new CustomNPCsException("Task ID must be greater than 0");
		}
		this.id = id;
	}

	@Override
	public void setTargetName(String name) {
		if (name == null) {
			name = "";
		}
		this.name = name;
	}

	public void setType(EnumQuestTask type) {
		this.type = type;
	}

	@Override
	public void setType(int type) {
		if (type < 0 || type >= EnumQuestTask.values().length) {
			throw new CustomNPCsException("Type must be between 0 and " + (EnumQuestTask.values().length - 1));
		}
		this.type = EnumQuestTask.values()[type];
	}

	@Override
	public IPos getCompassPos() { return NpcAPI.Instance().getIPos(this.pos.getX(), this.pos.getY(), this.pos.getZ()); }

	@Override
	public void setCompassPos(IPos pos) { this.pos = pos.getMCBlockPos(); }

	@Override
	public void setCompassPos(int x, int y, int z) { this.pos = new BlockPos(x, y, z); }

	@Override
	public int getCompassDimension() { return this.dimensionID; }

	@Override
	public void setCompassDimension(int dimensionID) {
		if (DimensionManager.isDimensionRegistered(dimensionID)) {
			throw new CustomNPCsException("Dimension ID:"+dimensionID+" not found");
		}
		this.dimensionID = dimensionID;
	}

	@Override
	public int getCompassRange() { return this.rangeCompass; }

	@Override
	public void setCompassRange(int range) {
		if (range < 0 || range > 64) {
			throw new CustomNPCsException("Compass Range must be between 3 and 64");
		}
		this.rangeCompass = range;
	}

	@Override
	public String getOrientationEntityName() { return this.entityName; }
	
	@Override
	public void setOrientationEntityName(String name) { this.entityName = name; }

}
