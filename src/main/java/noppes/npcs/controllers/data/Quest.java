package noppes.npcs.controllers.data;

import java.util.Map;
import java.util.UUID;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.ICompatibilty;
import noppes.npcs.NpcMiscInventory;
import noppes.npcs.Server;
import noppes.npcs.VersionCompatibility;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.IContainer;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.handler.data.IQuest;
import noppes.npcs.api.handler.data.IQuestCategory;
import noppes.npcs.api.handler.data.IQuestObjective;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.constants.EnumQuestCompletion;
import noppes.npcs.constants.EnumQuestRepeat;
import noppes.npcs.constants.EnumQuestTask;
import noppes.npcs.constants.EnumRewardType;
import noppes.npcs.constants.EnumSync;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.quests.QuestInterface;
import noppes.npcs.quests.QuestObjective;
import noppes.npcs.util.AdditionalMethods;

public class Quest
implements ICompatibilty, IQuest, Predicate<EntityNPCInterface> {
	
	public boolean cancelable = false;
	public int id, level, nextQuestid, rewardExp, rewardMoney, step, version;
	public int[] forgetDialogues, forgetQuests, completerPos;
	public String command, completeText, logText, nextQuestTitle, rewardText, title;
	public QuestCategory category;
	public FactionOptions factionOptions;
	public ResourceLocation icon, texture;
	public PlayerMail mail;
	public QuestInterface questInterface;
	public NpcMiscInventory rewardItems = new NpcMiscInventory(9);
	public EnumQuestRepeat repeat;
	public EnumQuestCompletion completion;
	public EnumRewardType rewardType;
	public EntityNPCInterface completer;
	private UUID completerUUID;

	public Quest(QuestCategory category) {
		this.version = VersionCompatibility.ModRev;
		this.id = -1;
		this.repeat = EnumQuestRepeat.NONE;
		this.completion = EnumQuestCompletion.Npc;
		this.title = "default";
		this.logText = "";
		this.completeText = "";
		this.nextQuestid = -1;
		this.nextQuestTitle = "";
		this.mail = new PlayerMail();
		this.command = "";
		this.icon = new ResourceLocation(CustomNpcs.MODID, "textures/quest icon/q_0.png");
		this.texture = null;
		this.questInterface = new QuestInterface();
		this.rewardExp = 0;
		this.rewardMoney = 0;
		this.rewardItems = new NpcMiscInventory(9);
		this.rewardType = EnumRewardType.RANDOM_ONE;
		this.factionOptions = new FactionOptions();
		this.category = category;
		this.level = 0;
		this.cancelable = false;
		this.rewardText = "";
		this.step = 0;
		this.forgetDialogues = new int[0];
		this.forgetQuests = new int[0];
		this.completerPos = new int[] { 0, 0, 0, 0 };
		this.completer = null;
		this.completerUUID = null;
	}

	@Override
	public IQuestObjective addTask() {
		return this.questInterface.addTask(EnumQuestTask.ITEM);
	}

	public boolean complete(EntityPlayer player, QuestData data) {
		if (this.completion == EnumQuestCompletion.Instant) {
			Server.sendData((EntityPlayerMP) player, EnumPacketClient.QUEST_COMPLETION, data.quest.id);
			return true;
		}
		return false;
	}

	public Quest copy() {
		Quest quest = new Quest(this.category);
		quest.readNBT(this.writeToNBT(new NBTTagCompound()));
		return quest;
	}

	@Override
	public IQuestCategory getCategory() {
		return this.category;
	}

	@Override
	public String getCompleteText() {
		return this.completeText;
	}

	@Override
	public int[] getForgetDialogues() {
		return this.forgetDialogues;
	}

	@Override
	public int[] getForgetQuests() {
		return this.forgetQuests;
	}

	@Override
	public int getId() {
		return this.id;
	}

	@Override
	public boolean getIsRepeatable() {
		return this.repeat != EnumQuestRepeat.NONE;
	}

	@Override
	public int getLevel() {
		return this.level;
	}

	@Override
	public String getLogText() {
		String allTextLogs = "";
		String ent = ""+((char) 10);
		Map<ItemStack, Integer> rewardist = Maps.<ItemStack, Integer>newHashMap();
		for (int i = 0; i < this.rewardItems.getSizeInventory(); i++) {
			ItemStack item = this.rewardItems.getStackInSlot(i);
			if (item == null || item.isEmpty()) { continue; }
			boolean has = false;
			if (this.rewardType == EnumRewardType.ALL) {
				for (ItemStack it : rewardist.keySet()) {
					if (item.isItemEqual(it) && ItemStack.areItemStackShareTagsEqual(item, it)) {
						int c = rewardist.get(it);
						rewardist.put(it, c + item.getCount());
						has = true;
						break;
					}
				}
			}
			if (!has) { rewardist.put(item, item.getCount()); }
		}
		if (rewardist.size() > 0 || this.rewardExp > 0 || this.rewardMoney > 0 || !this.rewardText.isEmpty()) {
			allTextLogs += ent + ent + new TextComponentTranslation("questlog.reward").getFormattedText();
		}
		if (rewardist.size() > 0) {
			allTextLogs += ent + new TextComponentTranslation("questlog." + (this.rewardType == EnumRewardType.ONE_SELECT ? "one" : this.rewardType == EnumRewardType.RANDOM_ONE ? "rnd" : "all") + ".reward").getFormattedText();
			int j = 1;
			for (ItemStack item : rewardist.keySet()) {
				int c = rewardist.get(item);
				allTextLogs += ent + (rewardist.size() > 1 ? "" + j + " - " : "") + item.getDisplayName()+(c>1 ? " x"+c : "");
				j++;
			}
		}
		if (this.rewardMoney > 0) {
			allTextLogs += ent + new TextComponentTranslation("questlog.rewardmoney", AdditionalMethods.getTextReducedNumber(this.rewardMoney, true, true, false), CustomNpcs.charCurrencies).getFormattedText();
		}
		if (this.rewardExp > 0) {
			allTextLogs += ent + new TextComponentTranslation("questlog.rewardexp", "" + this.rewardExp).getFormattedText();
		}
		if (!this.rewardText.isEmpty()) {
			allTextLogs += ent + (this.rewardText.indexOf("%")!=-1 ? this.rewardText : new TextComponentTranslation(this.rewardText).getFormattedText());
		}
		if (!this.logText.isEmpty()) {
			allTextLogs += ent + ent + ((char) 167) + "l" + new TextComponentTranslation("gui.description").getFormattedText() + ent + (this.logText.indexOf("%")!=-1 ? this.logText : new TextComponentTranslation(this.logText).getFormattedText());
		}
		return allTextLogs;
	}

	@Override
	public String getName() { return this.title; }

	@Override
	public Quest getNextQuest() {
		return (QuestController.instance == null) ? null : QuestController.instance.quests.get(this.nextQuestid);
	}

	@Override
	public IQuestObjective[] getObjectives(IPlayer<?> player) {
		if (!player.hasActiveQuest(this.id)) {
			throw new CustomNPCsException("Player doesnt have this quest active.");
		}
		return this.questInterface.getObjectives(player.getMCEntity());
	}
	
	public IQuestObjective[] getObjectives(EntityPlayer player) {
		if (player==null) { return new IQuestObjective[0]; }
		PlayerData data = PlayerData.get(player);
		if (data==null || !data.questData.activeQuests.containsKey(this.id)) { return new IQuestObjective[0]; }
		return this.questInterface.getObjectives(player);
	}

	@Override
	public IContainer getRewards() {
		return NpcAPI.Instance().getIContainer((IInventory) this.rewardItems);
	}

	@Override
	public int getRewardType() {
		return this.rewardType.ordinal();
	}

	@Override
	public String getTitle() {
		String title = "";
		if (this.level > 0) {
			String chr = new String(Character.toChars(0x00A7));
			title = chr + (this.level <= CustomNpcs.maxLv / 3 ? "2"
					: (float) this.level <= (float) CustomNpcs.maxLv / 1.5f ? "e" : "c");
			title += this.level + chr + "7 Lv.: " + chr + "r";
		}
		title += new TextComponentTranslation(this.title).getFormattedText();
		return title;
	}

	@Override
	public int getVersion() {
		return this.version;
	}

	public boolean hasNewQuest() {
		return this.getNextQuest() != null;
	}

	@Override
	public boolean isCancelable() {
		return this.cancelable;
	}

	@Override
	public boolean isSetUp() {
		if (this.questInterface.tasks.length == 0) {
			return false;
		}
		for (QuestObjective task : this.questInterface.tasks) {
			if ((task.getEnumType() == EnumQuestTask.ITEM || task.getEnumType() == EnumQuestTask.CRAFT)) {
				if (task.getItemStack().isEmpty()) {
					return false;
				}
			} else if (task.getEnumType() == EnumQuestTask.DIALOG) {
				if (DialogController.instance.dialogs.get(task.getTargetID()) == null) {
					return false;
				}
			} else if (task.getEnumType() == EnumQuestTask.KILL || task.getEnumType() == EnumQuestTask.AREAKILL
					|| task.getEnumType() == EnumQuestTask.MANUAL || task.getEnumType() == EnumQuestTask.LOCATION) {
				if (task.getTargetName().isEmpty()) {
					continue;
				}
			}
		}
		return true;
	}

	public void readNBT(NBTTagCompound compound) {
		this.id = compound.getInteger("Id");
		this.readNBTPartial(compound);
	}

	public void readNBTPartial(NBTTagCompound compound) {
		this.version = compound.getInteger("ModRev");
		VersionCompatibility.CheckAvailabilityCompatibility(this, compound);
		this.title = compound.getString("Title");
		this.logText = compound.getString("Text");
		this.completeText = compound.getString("CompleteText");
		this.command = compound.getString("QuestCommand");
		if (compound.hasKey("QuestIcon", 8)) { this.icon = new ResourceLocation(compound.getString("QuestIcon")); }
		else { this.icon = new ResourceLocation(CustomNpcs.MODID, "textures/quest icon/q_0.png"); }
		if (compound.hasKey("QuestTexture", 8)) { this.texture = new ResourceLocation(compound.getString("QuestTexture")); }
		else { this.texture = null; }
		this.nextQuestid = compound.getInteger("NextQuestId");
		this.nextQuestTitle = compound.getString("NextQuestTitle");
		if (this.hasNewQuest()) {
			this.nextQuestTitle = this.getNextQuest().title;
		} else {
			this.nextQuestTitle = "";
		}
		this.rewardType = EnumRewardType.values()[compound.getInteger("RewardType")];
		this.rewardExp = compound.getInteger("RewardExp");
		this.rewardMoney = compound.getInteger("RewardMoney");
		this.rewardItems.setFromNBT(compound.getCompoundTag("Rewards"));
		this.completion = EnumQuestCompletion.values()[compound.getInteger("QuestCompletion")];
		this.repeat = EnumQuestRepeat.values()[compound.getInteger("QuestRepeat")];
		this.questInterface.readEntityFromNBT(compound, this.id);
		this.factionOptions.readFromNBT(compound.getCompoundTag("QuestFactionPoints"));
		this.mail.readNBT(compound.getCompoundTag("QuestMail"));
		this.level = compound.getInteger("QuestLevel");
		this.cancelable = compound.getBoolean("Cancelable");
		this.rewardText = compound.getString("AddRewardText");
		this.step = compound.getInteger("Step") % 3;
		if (this.step<0) { this.step *= -1; }
		this.forgetDialogues = compound.getIntArray("ForgetDialogues");
		this.forgetQuests = compound.getIntArray("ForgetQuests");
		this.completer = null;
		this.completerUUID = null;
		if (compound.hasKey("CompleterPos", 11)) {
			this.completerPos = compound.getIntArray("CompleterPos");
		}
		try {
			// New
			if (compound.hasKey("CompleterNpc", 10)) {
				if (compound.getCompoundTag("CompleterNpc").hasKey("UUIDMost", 4) && compound.getCompoundTag("CompleterNpc").hasKey("UUIDLeast", 4)) {
					this.completerUUID = compound.getCompoundTag("CompleterNpc").getUniqueId("UUID");
				}
				String name = compound.getCompoundTag("CompleterNpc").getString("Name");
				if (CustomNpcs.Server != null) {
					for (WorldServer w : CustomNpcs.Server.worlds) {
						for (EntityNPCInterface entity : w.getEntities(EntityNPCInterface.class, this)) {
							if (entity.getName().equals(name)) {
								this.completer = entity;
								if (this.completerUUID==null) { this.completerUUID = entity.getUniqueID(); }
								break;
							}
							break;
						}
						if (this.completer !=null) { break; }
					}
				}
				else if (CustomNpcs.proxy.getPlayer()!=null) {
					for (EntityNPCInterface entity : CustomNpcs.proxy.getPlayer().world.getEntities(EntityNPCInterface.class, this)) {
						if (entity.getName().equals(name)) {
							this.completer = entity;
							if (this.completerUUID==null) { this.completerUUID = entity.getUniqueID(); }
							break;
						}
						break;
					}
				}
				if (this.completer == null) {
					World world = null;
					if (CustomNpcs.Server!=null) { world = CustomNpcs.Server.getEntityWorld(); }
					else if (CustomNpcs.proxy.getPlayer()!=null) { world = CustomNpcs.proxy.getPlayer().world; }
					Entity e = EntityList.createEntityFromNBT(compound.getCompoundTag("CompleterNpc"), world);
					if (e instanceof EntityNPCInterface) {
						this.completer = (EntityNPCInterface) e;
						this.completerUUID = e.getUniqueID();
					}
				}
			}
			else if (compound.hasKey("CompleterNpc", 8)) { // OLD
				String name = compound.getString("CompleterNpc");
				if (CustomNpcs.Server != null) {
					for (WorldServer w : CustomNpcs.Server.worlds) {
						for (EntityNPCInterface entity : w.getEntities(EntityNPCInterface.class, this)) {
							if (entity.getName().equals(name)) {
								this.completer = entity;
								this.completerUUID = entity.getUniqueID();
								break;
							}
						}
						if (this.completer !=null) { break; }
					}
				}
				else if (CustomNpcs.proxy.getPlayer()!=null) {
					for (EntityNPCInterface entity : CustomNpcs.proxy.getPlayer().world.getEntities(EntityNPCInterface.class, this)) {
						if (entity.getName().equals(name)) {
							this.completer = entity;
							this.completerUUID = entity.getUniqueID();
							break;
						}
					}
				}
				if (this.completer == null) {
					World world = null;
					if (CustomNpcs.Server!=null) { world = CustomNpcs.Server.getEntityWorld(); }
					else if (CustomNpcs.proxy.getPlayer()!=null) { world = CustomNpcs.proxy.getPlayer().world; }
					this.completer = (EntityNPCInterface) EntityList.createEntityByIDFromName(new ResourceLocation(CustomNpcs.MODID, "customnpc"), world);
					if (this.completer != null) {
						this.completer.display.setName(name);
						this.completerUUID = this.completer.getUniqueID();
					}
				}
			}
		}
		catch (Exception e) { e.printStackTrace(); }
	}

	@Override
	public boolean removeTask(IQuestObjective task) {
		return this.questInterface.removeTask((QuestObjective) task);
	}

	@Override
	public void save() {
		QuestController.instance.saveQuest(this.category, this);
	}

	@SideOnly(Side.SERVER)
	@Override
	public void sendChangeToAll() {
		Server.sendToAll(CustomNpcs.Server, EnumPacketClient.SYNC_UPDATE, EnumSync.QuestData, this.writeToNBT(new NBTTagCompound()), this.category.id);
	}

	@Override
	public void setCancelable(boolean cancelable) {
		this.cancelable = cancelable;
	}

	@Override
	public void setCompleteText(String text) {
		this.completeText = text;
	}

	@Override
	public void setForgetDialogues(int[] forget) {
		this.forgetDialogues = forget;
	}

	@Override
	public void setForgetQuests(int[] forget) {
		this.forgetQuests = forget;
	}

	@Override
	public void setLevel(int level) {
		this.level = level;
	}

	@Override
	public void setLogText(String text) {
		this.logText = text;
	}

	@Override
	public void setName(String name) {
		this.title = name;
	}

	@Override
	public void setNextQuest(IQuest quest) {
		if (quest == null) {
			this.nextQuestid = -1;
			this.nextQuestTitle = "";
		} else {
			if (quest.getId() < 0) {
				throw new CustomNPCsException("Quest id is lower than 0");
			}
			this.nextQuestid = quest.getId();
			this.nextQuestTitle = quest.getTitle();
		}
	}
	
	@Override
	public void setCompleterNpc(ICustomNpc<?> npc) {
		this.completer = (EntityNPCInterface) npc.getMCEntity();
	}

	@Override
	public void setRewardText(String text) {
		this.rewardText = text;
	}

	@Override
	public void setRewardType(int type) {
		if (type < 0 || type >= EnumRewardType.values().length) {
			return;
		}
		this.rewardType = EnumRewardType.values()[type];
	}

	@Override
	public void setVersion(int version) {
		this.version = version;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setInteger("Id", this.id);
		return this.writeToNBTPartial(compound);
	}

	public NBTTagCompound writeToNBTPartial(NBTTagCompound compound) {
		compound.setInteger("ModRev", this.version);
		compound.setString("Title", this.title);
		compound.setString("Text", this.logText);
		compound.setString("CompleteText", this.completeText);
		compound.setInteger("NextQuestId", this.nextQuestid);
		compound.setString("NextQuestTitle", this.nextQuestTitle);
		compound.setInteger("RewardExp", this.rewardExp);
		compound.setInteger("RewardMoney", this.rewardMoney);
		compound.setTag("Rewards", this.rewardItems.getToNBT());
		compound.setString("QuestCommand", this.command);
		compound.setString("QuestIcon", this.icon.toString());
		if (this.texture!=null) { compound.setString("QuestTexture", this.texture.toString()); }
		compound.setInteger("RewardType", this.rewardType.ordinal());
		compound.setInteger("QuestCompletion", this.completion.ordinal());
		compound.setInteger("QuestRepeat", this.repeat.ordinal());
		this.questInterface.writeEntityToNBT(compound);
		compound.setTag("QuestFactionPoints", this.factionOptions.writeToNBT(new NBTTagCompound()));
		compound.setTag("QuestMail", this.mail.writeNBT());
		compound.setInteger("QuestLevel", this.level);
		compound.setBoolean("Cancelable", this.cancelable);
		compound.setString("AddRewardText", this.rewardText);
		compound.setInteger("Step", this.step);
		compound.setIntArray("ForgetDialogues", this.forgetDialogues);
		compound.setIntArray("ForgetQuests", this.forgetQuests);
		
		if (this.completer!=null) {
			NBTTagCompound npcNbt = new NBTTagCompound();
			this.completer.writeToNBTOptional(npcNbt);
			compound.setTag("CompleterNpc", npcNbt);
			if (this.completerPos[0]==0 && this.completerPos[1]==0 && this.completerPos[2]==0 && this.completerPos[3]==0) {
				 this.completerPos[0] = (int) this.completer.posX;
				 this.completerPos[1] = (int) (this.completer.posY + 0.5d);
				 this.completerPos[2] = (int) this.completer.posZ;
				 this.completerPos[3] = this.completer.world.provider.getDimension();
			}
		}
		compound.setIntArray("CompleterPos", this.completerPos);
		
		return compound;
	}

	public String getKey() {
		char c = ((char) 167);
		return c + "7ID:" + this.id + c + "8" + this.category.title+"/" + c + "7 \"" + c + "5" + this.getTitle() + c + "7\"";
	}

	public boolean hasCompassSettings() {
		for (QuestObjective task : this.questInterface.tasks) {
			if (task.rangeCompass>3 && task.pos.getX()!=0 && task.pos.getY()!=0 && task.pos.getZ()!=0) {
				return true;
			}
		}
		return false;
	}

	@Override
	public ICustomNpc<?> getCompleterNpc() {
		if (this.completer==null) { return null; }
		return (ICustomNpc<?>) NpcAPI.Instance().getIEntity(this.completer);
	}

	@Override
	public boolean apply(EntityNPCInterface entity) {
		return this.completerUUID == null || entity.getUniqueID().equals(this.completerUUID);
	}

}
