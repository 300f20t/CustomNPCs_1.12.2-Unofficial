package noppes.npcs.controllers.data;

import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextComponentTranslation;
import noppes.npcs.controllers.MarcetController;

public class MarcetSection {

	private int id = 0;
	public String name = "market.default.section";
	public List<Deal> deals = Lists.<Deal>newArrayList();
	
	public MarcetSection(int id) { this.id = id; }
	
	public int getId() { return id; }
	
	public void addDeal(int dealId) {
		if (hadDeal(dealId)) { return; }
		Deal deal = (Deal) MarcetController.getInstance().getDeal(dealId);
		if (deal == null || !deal.isValid()) { return; }
		Deal marcetDeal = deal.copy();
		marcetDeal.updateNew();
		deals.add(marcetDeal);
	}

	private boolean hadDeal(int dealId) {
		for (Deal deal : deals) {
			if (deal.getId() == dealId) { return true; }
		}
		return false;
	}

	public boolean removeDeal(int dealId) {
		for (Deal deal : deals) {
			if (deal.getId() == dealId) { return deals.remove(deal); }
		}
		return false;
	}
	
	public void removeAllDeals() { deals.clear(); }

	public static MarcetSection create(NBTTagCompound compound) {
		MarcetSection ms = new MarcetSection(compound.getInteger("ID"));
		ms.name = compound.getString("Name");
		
		NBTTagList list = compound.getTagList("Deals", 10);
		if (list != null) {
			for (NBTBase nbt : list) {
				Deal deal = new Deal();
				deal.readDataNBT((NBTTagCompound) nbt);
				ms.deals.add(deal);
			}
		}
		
		return ms;
	}

	public NBTTagCompound save() {
		NBTTagCompound compound = new NBTTagCompound();
		compound.setInteger("ID", id);
		compound.setString("Name", name);
		
		NBTTagList list = new NBTTagList();
		for (Deal deal : deals) { list.appendTag(deal.writeDataToNBT()); }
		compound.setTag("Deals", list);
		
		return compound;
	}

	public String getName() { return new TextComponentTranslation(name).getFormattedText(); }

	
}
