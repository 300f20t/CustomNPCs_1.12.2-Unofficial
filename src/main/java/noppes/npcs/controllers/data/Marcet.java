package noppes.npcs.controllers.data;

import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.Server;
import noppes.npcs.api.handler.data.IDeal;
import noppes.npcs.api.handler.data.IMarcet;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.controllers.MarcetController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleTrader;
import noppes.npcs.util.CustomNPCsScheduler;

public class Marcet
implements IMarcet, Predicate<EntityNPCInterface> {

	public final Map<Integer, MarkupData> markup;
	public final Map<ItemStack, Integer> inventory;
	public final Map<Integer, String> sections;
	private int id;
	public boolean isLimited, showXP;
	public long lastTime;
	public List<EntityPlayer> listeners = Lists.<EntityPlayer>newArrayList();
	public String name;
	public long nextTime;
	public int updateTime;
	public Lines lines;
	public int limitedType;
	public long money;
	public double coefficient = 5.0d;

	public Marcet(int id) {
		this.id = id;
		this.name = "Market";
		this.updateTime = 0;
		this.markup = Maps.<Integer, MarkupData>newTreeMap();
		this.markup.put(0, new MarkupData(0, 0.15f, 0.80f, 1000));
		this.markup.put(1, new MarkupData(1, 0.0f, 0.45f, 2200));
		this.markup.put(2, new MarkupData(2, -0.05f, 0.0f, 5000));
		this.inventory = Maps.<ItemStack, Integer>newHashMap();
		this.sections = Maps.<Integer, String>newTreeMap();
		this.sections.put(0, "market.default.section");
		this.lines = new Lines();
		this.isLimited = false;
		this.showXP = true;
		this.limitedType = 0;
		this.money = 0;
		this.updateNew();
	}

	@Override
	public boolean isLimited() { return this.isLimited; }

	@Override
	public void setIsLimited(boolean limited) {
		if (this.isLimited == limited) { return; }
		this.isLimited = limited;
		if (limited) { this.updateNew(); }
	}

	public void addListener(EntityPlayer listener, boolean isServer) {
		for (EntityPlayer pl : this.listeners) {
			if (listener == pl || pl.equals(listener)) {
				return;
			}
		}
		this.listeners.add(listener);
		if (isServer && listener instanceof EntityPlayerMP) {
			this.sendTo((EntityPlayerMP) listener);
			this.detectAndSendChanges();
		}
	}

	public void detectAndSendChanges() {
		for (EntityPlayer listener : this.listeners) {
			if (listener instanceof EntityPlayerMP) {
				this.sendTo((EntityPlayerMP) listener);
			}
		}
	}

	@Override
	public String getName() {
		return this.name;
	}

	public String getSettingName() {
		String str = ""+((char) 167);
		return "ID:" + this.id + " " + str + (this.isEmpty() ? "4" : this.hasEmptyDeal() ? "6" : "a") + new TextComponentTranslation(this.name).getFormattedText();
	}

	public boolean isEmpty() {
		MarcetController mData = MarcetController.getInstance();
		List<IDeal> list = Lists.<IDeal>newArrayList();
		for (Deal deal : mData.deals.values()) {
			if (deal.getMarcetID() == this.id && deal.isValid()) { list.add(deal); }
		}
		return list.isEmpty();
	}

	public String getShowName() {
		return new TextComponentTranslation(this.name).getFormattedText();
	}

	public boolean isValid() { return !this.isEmpty(); }
	
	public boolean hasEmptyDeal() {
		MarcetController mData = MarcetController.getInstance();
		for (Deal deal : mData.deals.values()) {
			if (deal.getMarcetID() != this.id) { continue; }
			if (!deal.isValid()) { return true; }
		}
		return false;
	}

	public boolean hasListener(EntityPlayer player) {
		for (EntityPlayer listener : this.listeners) {
			if (listener.equals(player)) { return true; }
		}
		return false;
	}

	public void readFromNBT(NBTTagCompound compound) {
		this.id = compound.getInteger("MarcetID");
		this.name = compound.getString("Name");
		this.isLimited = compound.getBoolean("IsLimited");
		this.showXP = compound.getBoolean("ShowXP");
		this.money = compound.getLong("Money");
		
		this.markup.clear();
		for (int i = 0; i < compound.getTagList("Markup", 10).tagCount(); i++) {
			MarkupData md = new MarkupData(compound.getTagList("Markup", 10).getCompoundTagAt(i));
			this.markup.put(md.level, md);
		}
		if (this.markup.isEmpty()) {
			this.markup.put(0, new MarkupData(0, 0.15f, 0.80f, 1000));
			this.markup.put(1, new MarkupData(1, 0.0f, 0.45f, 2200));
			this.markup.put(2, new MarkupData(2, -0.05f, 0.0f, 5000));
		}
		this.inventory.clear();
		for (int i = 0; i < compound.getTagList("Inventory", 10).tagCount(); i++) {
			NBTTagCompound nbt = compound.getTagList("Inventory", 10).getCompoundTagAt(i);
			this.inventory.put(new ItemStack(nbt), nbt.getInteger("TotalCount"));
		}
		this.sections.clear();
		Map<Integer, String> newsec = Maps.<Integer, String>newTreeMap();
		if (!compound.hasKey("Sections", 9) || compound.getTagList("Sections", 10).tagCount()==0) {
			newsec.put(0, "market.default.section");
		} else {
			for (int i = 0; i < compound.getTagList("Sections", 10).tagCount(); i++) {
				NBTTagCompound nbt = compound.getTagList("Sections", 10).getCompoundTagAt(i);
				newsec.put(nbt.getInteger("ID"), nbt.getString("Name"));
			}
			Map<Integer, String> sec = Maps.<Integer, String>newTreeMap();
			int i = 0;
			for (String name : newsec.values()) { sec.put(i, name); i++; }
			newsec = sec;
		}
		this.sections.putAll(newsec);
		this.limitedType = compound.getInteger("LimitedType");
		this.updateTime = compound.getInteger("UpdateTime");
		this.lastTime = compound.getLong("LastTime");
		this.nextTime = compound.getLong("NextTime");
		if (compound.hasKey("NpcLines", 10)) { this.lines.readNBT(compound.getCompoundTag("NpcLines")); }
	}

	public void removeListener(EntityPlayer player, boolean isServer) {
		for (EntityPlayer listener : this.listeners) {
			if (listener == player || listener.equals(player)) {
				if (isServer && listener instanceof EntityPlayerMP) {
					Server.sendData((EntityPlayerMP) listener, EnumPacketClient.MARCET_CLOSE, this.id);
				}
				this.listeners.remove(listener);
				this.detectAndSendChanges();
				return;
			}
		}
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	public void update() { // any 1.0 sec -> (MarcetController.update) ServerTickHandler / ServerTickEvent
		if (this.updateTime < 5L) { return; }
		if (this.lastTime <= System.currentTimeMillis() - 7200000L || this.lastTime + this.updateTime * 60000L < System.currentTimeMillis()) {
			this.updateNew();
		}
	}

	@SideOnly(Side.CLIENT)
	public void updateTime() { // any 0.5 sec -> (MarcetController.updateTime) ClientTickHandler / ClientTickEvent
		if (this.nextTime < 0L) {
			this.nextTime = 0L;
		} else if (this.nextTime > 0L) {
			this.nextTime -= 500L;
			if (this.nextTime<0) { this.nextTime = 0; }
		}
	}

	public NBTTagCompound writeToNBT() {
		NBTTagCompound compound = new NBTTagCompound();
		compound.setInteger("MarcetID", this.id);
		compound.setString("Name", this.name);
		compound.setBoolean("IsLimited", this.isLimited);
		compound.setBoolean("ShowXP", this.showXP);
		compound.setLong("Money", this.money);

		NBTTagList markup = new NBTTagList();
		for (int level : this.markup.keySet()) {
			MarkupData mp = this.markup.get(level);
			mp.level = level;
			markup.appendTag(mp.getNBT());
		}
		compound.setTag("Markup", markup);

		NBTTagList items = new NBTTagList();
		for (ItemStack stack : this.inventory.keySet()) {
			NBTTagCompound nbt = new NBTTagCompound();
			stack.writeToNBT(nbt);
			nbt.setInteger("TotalCount", this.inventory.get(stack));
			items.appendTag(nbt);
		}
		compound.setTag("Inventory", items);
		
		NBTTagList secs = new NBTTagList();
		for (int id : this.sections.keySet()) {
			NBTTagCompound nbt = new NBTTagCompound();
			nbt.setInteger("ID", id);
			nbt.setString("Name", this.sections.get(id));
			secs.appendTag(nbt);
		}
		compound.setTag("Sections", secs);

		compound.setInteger("LimitedType", this.limitedType);
		compound.setInteger("UpdateTime", this.updateTime);
		compound.setLong("LastTime", this.lastTime);
		compound.setLong("NextTime", this.lastTime + this.updateTime * 60000L - System.currentTimeMillis());
		compound.setTag("NpcLines", this.lines.writeToNBT());
		return compound;
	}

	@Override
	public boolean apply(EntityNPCInterface npc) {
		if (!(npc.advanced.roleInterface instanceof RoleTrader)) { return false; }
		return ((RoleTrader) npc.advanced.roleInterface).getMarket().equals(this);
	}

	@Override
	public int getId() { return this.id; }

	public Marcet copy(int newID) {
		Marcet marcet = new Marcet(newID > -1 ? newID : this.id);
		marcet.readFromNBT(this.writeToNBT());
		marcet.updateNew();
		return marcet;
	}

	@Override
	public void updateNew() {
		this.inventory.clear();
		this.lastTime = System.currentTimeMillis();
		if (this.lines!=null && !this.lines.isEmpty() && CustomNpcs.Server!=null) {
			for (WorldServer world : CustomNpcs.Server.worlds) {
				List<EntityNPCInterface> npcs = world.getEntities(EntityNPCInterface.class, this);
				for (EntityNPCInterface npc : npcs) { npc.saySurrounding(this.lines.getLine(true)); }
			}
		}
		MarcetController mData = MarcetController.getInstance();
		this.money = (long) (Math.random() * 7500.0d);
		for (Deal deal : mData.deals.values()) {
			if (deal.getMarcetID() != this.id || !deal.isValid()) { continue; }
			deal.updateNew();
			this.money += (long) ((double) (deal.getMoney()) * (this.coefficient + Math.random() * this.coefficient));
			for (IItemStack istack : deal.getCurrency().getItems()) {
				ItemStack stack = istack.getMCItemStack();
				if (NoppesUtilServer.IsItemStackNull(stack)) { continue; }
				int count = (int) (((double) stack.getCount()) * (this.coefficient + Math.random() * this.coefficient));
				boolean added = false;
				for (ItemStack st : this.inventory.keySet()) {
					if (NoppesUtilServer.IsItemStackNull(st)) { continue; }
					if (NoppesUtilPlayer.compareItems(stack, st, false, false)) {
						this.inventory.put(st, this.inventory.get(st) + count);
						added = true;
						break;
					}
				}
				if (!added) { this.inventory.put(stack, count); }
			}
		}
		this.detectAndSendChanges();
	}

	public void closeForAllPlayers() { // server only
		if (this.listeners==null) { return; }
		for (EntityPlayer player : this.listeners) {
			if (!(player instanceof EntityPlayerMP)) { return; }
			CustomNPCsScheduler.runTack(() -> {
				((EntityPlayerMP) player).closeScreen();
			}, 250);
		}
	}

	public void addInventoryItems(Map<ItemStack, Integer> items) {
		for (ItemStack stack : items.keySet()) {
			if (NoppesUtilServer.IsItemStackNull(stack)) { continue; }
			boolean added = false;
			List<ItemStack> del = Lists.<ItemStack>newArrayList();
			for (ItemStack st : this.inventory.keySet()) {
				if (NoppesUtilServer.IsItemStackNull(st)) {
					del.add(st);
					continue;
				}
				if (NoppesUtilPlayer.compareItems(stack, st, false, false)) {
					this.inventory.put(st, this.inventory.get(st) + items.get(stack));
					added = true;
					break;
				}
			}
			for (ItemStack st : del) { this.inventory.remove(st); }
			if (!added) { this.inventory.put(stack, items.get(stack)); }
		}
	}

	public void removeInventoryItems(Map<ItemStack, Integer> items) {
		for (ItemStack stack : items.keySet()) {
			if (NoppesUtilServer.IsItemStackNull(stack)) { continue; }
			List<ItemStack> del = Lists.<ItemStack>newArrayList();
			for (ItemStack st : this.inventory.keySet()) {
				if (NoppesUtilServer.IsItemStackNull(st)) {
					del.add(st);
					continue;
				}
				if (NoppesUtilPlayer.compareItems(stack, st, false, false)) {
					this.inventory.put(st, this.inventory.get(st) - items.get(stack));
					if (this.inventory.get(st)<=0) { del.add(st); }
					break;
				}
			}
			for (ItemStack st : del) { this.inventory.remove(st); }
		}
	}

	public void sendTo(EntityPlayerMP player) {
		Server.sendData(player, EnumPacketClient.MARCET_DATA, 1, this.writeToNBT());
		MarcetController mData = MarcetController.getInstance();
		for (Deal deal : mData.deals.values()) {
			if (deal.getMarcetID() != this.id) { continue; }
			Server.sendData(player, EnumPacketClient.MARCET_DATA, 3, deal.writeToNBT());
		}
		Server.sendData(player, EnumPacketClient.MARCET_DATA, 2);
	}

	@Override
	public int[] getDealIDs() {
		MarcetController mData = MarcetController.getInstance();
		List<Integer> list = Lists.<Integer>newArrayList();
		for (Deal deal : mData.deals.values()) {
			if (deal.getMarcetID() != this.id) { continue; }
			list.add(deal.getId());
		}
		int[] ids = new int[list.size()];
		int i = 0;
		for (int id : list) { ids[i] = id; i++; }
		return ids;
	}

	@Override
	public IDeal[] getDeals() {
		MarcetController mData = MarcetController.getInstance();
		List<IDeal> list = Lists.<IDeal>newArrayList();
		for (Deal deal : mData.deals.values()) {
			if (deal.getMarcetID() != this.id) { continue; }
			list.add(deal);
		}
		return list.toArray(new IDeal[list.size()]);
	}

	public boolean hasDeal(int dealID) {
		MarcetController mData = MarcetController.getInstance();
		for (Deal deal : mData.deals.values()) {
			if (deal.getMarcetID() != this.id) { continue; }
			if (deal.getId() == dealID) { return true; }
		}
		return false;
	}

}
