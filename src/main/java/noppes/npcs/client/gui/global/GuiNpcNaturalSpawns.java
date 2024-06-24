package noppes.npcs.client.gui.global;

import java.util.HashMap;
import java.util.Vector;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentTranslation;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.Client;
import noppes.npcs.client.gui.GuiNpcMobSpawnerSelector;
import noppes.npcs.client.gui.SubGuiNpcBiomes;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcSlider;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.ICustomScrollListener;
import noppes.npcs.client.gui.util.IGuiData;
import noppes.npcs.client.gui.util.IScrollData;
import noppes.npcs.client.gui.util.ISliderListener;
import noppes.npcs.client.gui.util.ITextfieldListener;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.controllers.data.SpawnData;
import noppes.npcs.entity.EntityNPCInterface;

public class GuiNpcNaturalSpawns
extends GuiNPCInterface2
implements IGuiData, IScrollData, ITextfieldListener, ICustomScrollListener, ISliderListener {

	private HashMap<String, Integer> data;
	private GuiCustomScroll scroll;
	private SpawnData spawn;
	private Entity displayNpc = null;

	public GuiNpcNaturalSpawns(EntityNPCInterface npc) {
		super(npc);
		this.data = new HashMap<String, Integer>();
		this.spawn = new SpawnData();
		Client.sendData(EnumPacketServer.NaturalSpawnGetAll, new Object[0]);
	}

	@Override
	public void buttonEvent(GuiNpcButton button) {
		switch(button.id) {
			case 1: { // add
				this.save();
				String name;
				for (name = new TextComponentTranslation("gui.new").getFormattedText(); this.data.containsKey(name); name += "_") { }
				SpawnData spawn = new SpawnData();
				spawn.name = name;
				Client.sendData(EnumPacketServer.NaturalSpawnSave, spawn.writeNBT(new NBTTagCompound()));
				break;
			}
			case 2: { // remove
				if (!this.data.containsKey(this.scroll.getSelected())) { return; }
				Client.sendData(EnumPacketServer.NaturalSpawnRemove, this.spawn.id);
				this.spawn = new SpawnData();
				this.scroll.clear();
				displayNpc = null;
				break;
			}
			case 3: { // set biome
				this.setSubGui(new SubGuiNpcBiomes(this.spawn));
				break;
			}
			case 4: { // set liquid
				this.spawn.liquid = button.getValue() == 0;
				break;
			}
			case 5: { // select npc
				this.setSubGui(new GuiNpcMobSpawnerSelector());
				break;
			}
			case 25: { // nbt
				this.spawn.compound1 = new NBTTagCompound();
				displayNpc = null;
				this.initGui();
				break;
			}
			case 27: { // type
				this.spawn.type = button.getValue();
				break;
			}
		}
	}

	@Override
	public void closeSubGui(SubGuiInterface gui) {
		super.closeSubGui(gui);
		if (gui instanceof GuiNpcMobSpawnerSelector) {
			GuiNpcMobSpawnerSelector selector = (GuiNpcMobSpawnerSelector) gui;
			NBTTagCompound compound = selector.getCompound();
			if (compound != null) {
				this.spawn.compound1 = compound;
				if (compound.hasKey("SpawnCycle", 3)) { compound.setInteger("SpawnCycle", 4); }
			}
		}
		this.initGui();
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		if (this.subgui == null) {
			int r, p = 0, x = 387, y = 196;
			GlStateManager.pushMatrix();
			if (this.displayNpc != null) {
				this.displayNpc.ticksExisted = this.player.ticksExisted;
				if (this.displayNpc instanceof EntityLivingBase) {
					r = (int) (3 * this.player.world.getTotalWorldTime() % 360);
				} else {
					r = 0;
					y -= 34;
					if (this.displayNpc instanceof EntityItem) {
						p = 30;
						y += 10;
					}
					if (this.displayNpc instanceof EntityItemFrame) {
						x += 16;
					}
				}
				this.drawNpc(this.displayNpc, x, y, 1.0f, r, p, 0);
			}
			Gui.drawRect(this.guiLeft + x - 30, this.guiTop + y - 77, this.guiLeft + x + 31, this.guiTop + y + 9, 0xFF808080);
			Gui.drawRect(this.guiLeft + x - 29, this.guiTop + y - 76, this.guiLeft + x + 30, this.guiTop + y + 8, 0xFF000000);
			GlStateManager.popMatrix();
			if (!CustomNpcs.ShowDescriptions) { return; }
			if (this.getTextField(1) != null && this.getTextField(1).isMouseOver()) {
				this.setHoverText(new TextComponentTranslation("spawning.hover.name").getFormattedText());
			} else if (this.getTextField(2) != null && this.getTextField(2).isMouseOver()) {
				this.setHoverText(new TextComponentTranslation("spawning.hover.chance").getFormattedText());
			} else if (this.getTextField(3) != null && this.getTextField(3).isMouseOver()) {
				this.setHoverText(new TextComponentTranslation("spawning.hover.group").getFormattedText());
			} else if (this.getTextField(4) != null && this.getTextField(4).isMouseOver()) {
				this.setHoverText(new TextComponentTranslation("spawning.hover.range").getFormattedText());
			} else if (this.getButton(1) != null && this.getButton(1).isMouseOver()) {
				this.setHoverText(new TextComponentTranslation("spawning.hover.add").getFormattedText());
			} else if (this.getButton(2) != null && this.getButton(2).isMouseOver()) {
				this.setHoverText(new TextComponentTranslation("spawning.hover.del").getFormattedText());
			} else if (this.getButton(3) != null && this.getButton(3).isMouseOver()) {
				this.setHoverText(new TextComponentTranslation("spawning.hover.biomes").getFormattedText());
			} else if (this.getSlider(4) != null && this.getSlider(4).isMouseOver()) {
				this.setHoverText(new TextComponentTranslation("spawning.hover.chance").getFormattedText());
			} else if (this.getButton(4) != null && this.getButton(4).isMouseOver()) {
				this.setHoverText(new TextComponentTranslation("spawning.hover.liquid."+this.getButton(4).getValue()).getFormattedText());
			} else if (this.getButton(5) != null && this.getButton(5).isMouseOver()) {
				this.setHoverText(new TextComponentTranslation("spawning.hover.sel.npc").getFormattedText());
			} else if (this.getButton(25) != null && this.getButton(25).isMouseOver()) {
				this.setHoverText(new TextComponentTranslation("spawning.hover.del.npc").getFormattedText());
			} else if (this.getButton(27) != null && this.getButton(27).isMouseOver()) {
				this.setHoverText(new TextComponentTranslation("spawning.hover.type").getFormattedText());
			}
		}
	}
	
	private String getTitle(NBTTagCompound compound) {
		displayNpc = EntityList.createEntityFromNBT(compound, this.mc.world);
		if (displayNpc != null) { return displayNpc.getName(); }
		return "gui.selectnpc";
	}

	@Override
	public void initGui() {
		super.initGui();
		if (this.scroll == null) { (this.scroll = new GuiCustomScroll(this, 0)).setSize(143, 208); }
		this.scroll.guiLeft = this.guiLeft + 214;
		this.scroll.guiTop = this.guiTop + 4;
		this.addScroll(this.scroll);
		this.addButton(new GuiNpcButton(1, this.guiLeft + 358, this.guiTop + 38, 58, 20, "gui.add"));
		this.addButton(new GuiNpcButton(2, this.guiLeft + 358, this.guiTop + 61, 58, 20, "gui.remove"));
		if (this.spawn.id >= 0) { this.showSpawn(); }
	}

	@Override
	public void keyTyped(char c, int i) {
		if (i == 1 && this.subgui == null) {
			this.save();
			CustomNpcs.proxy.openGui(this.npc, EnumGuiType.MainMenuGlobal);
			return;
		}
		super.keyTyped(c, i);
	}

	@Override
	public void mouseDragged(GuiNpcSlider guiNpcSlider) {
		this.spawn.itemWeight = (int) (guiNpcSlider.sliderValue * 100.0f);
		if (this.spawn.itemWeight < 1) { this.spawn.itemWeight = 1; }
		guiNpcSlider.displayString = new TextComponentTranslation("spawning.weightedChance").getFormattedText() + ": " + this.spawn.itemWeight + "%";
		if (this.getTextField(2) != null) { this.getTextField(2).setText("" + this.spawn.itemWeight); }
	}

	@Override
	public void mousePressed(GuiNpcSlider guiNpcSlider) {
	}

	@Override
	public void mouseReleased(GuiNpcSlider guiNpcSlider) {
		this.spawn.itemWeight = (int) (guiNpcSlider.sliderValue * 100.0f);
	}

	@Override
	public void save() {
		GuiNpcTextField.unfocus();
		if (this.spawn.id >= 0) {
			Client.sendData(EnumPacketServer.NaturalSpawnSave, this.spawn.writeNBT(new NBTTagCompound()));
		}
	}

	@Override
	public void scrollClicked(int i, int j, int k, GuiCustomScroll guiCustomScroll) {
		if (guiCustomScroll.id == 0) {
			this.save();
			String selected = this.scroll.getSelected();
			this.spawn = new SpawnData();
			Client.sendData(EnumPacketServer.NaturalSpawnGet, this.data.get(selected));
		}
	}

	@Override
	public void scrollDoubleClicked(String selection, GuiCustomScroll scroll) {
	}

	@Override
	public void setData(Vector<String> list, HashMap<String, Integer> data) {
		String name = this.scroll.getSelected();
		this.data.clear();
		this.data.putAll(data);
		this.scroll.setList(list);
		if (name != null) { this.scroll.setSelected(name); }
		this.initGui();
	}

	@Override
	public void setGuiData(NBTTagCompound compound) {
		this.spawn.readNBT(compound);
		this.setSelected(this.spawn.name);
		this.initGui();
	}

	@Override
	public void setSelected(String selected) {
	}

	private void showSpawn() {
		int lId = 0;
		int x = this.guiLeft + 5;
		int y = this.guiTop + 5;
		this.addLabel(new GuiNpcLabel(lId++, "gui.title", x, y + 5));
		this.addTextField(new GuiNpcTextField(1, this, this.fontRenderer, x + 56, y, 150, 20, this.spawn.name));
		
		this.addLabel(new GuiNpcLabel(lId++, "spawning.biomes", x, (y += 22) + 5));
		this.addButton(new GuiNpcButton(3, x + 156, y, 50, 20, "selectServer.edit"));
		if (this.spawn.biomes.isEmpty()) { this.getButton(3).layerColor = 0xFFF02020; }
		
		this.addSlider(new GuiNpcSlider(this, 4, x, y += 22, 160, 20, (float) this.spawn.itemWeight / 100.0f));
		this.addTextField(new GuiNpcTextField(2, this, this.fontRenderer, x + 163, y, 42, 20, "" + this.spawn.itemWeight));
		this.getTextField(2).setNumbersOnly();
		this.getTextField(2).setMinMaxDefault(1, 100, this.spawn.itemWeight);
		
		this.addLabel(new GuiNpcLabel(lId++, "gui.type", x, (y += 22) + 5));
		this.addButton(new GuiNpcButton(27, x + 86, y, 120, 20, new String[] { "spawner.any", "spawner.dark", "spawner.light" }, this.spawn.type));

		this.addButton(new GuiNpcButton(5, x, y += 22, 184, 20, this.getTitle(this.spawn.compound1)));
		this.addButton(new GuiNpcButton(25, x + 186, y, 20, 20, "X"));

		this.addButton(new GuiNpcButton(4, x, y += 22, 80, 20, new String[] { "spawning.liquid.0", "spawning.liquid.1" }, this.spawn.liquid ? 0 : 1));

		this.addLabel(new GuiNpcLabel(lId++, "spawning.group", x, (y += 22) + 5));
		this.addTextField(new GuiNpcTextField(3, this, this.fontRenderer, x + 164, y, 42, 20, "" + this.spawn.group));
		this.getTextField(3).setNumbersOnly();
		this.getTextField(3).setMinMaxDefault(1, 8, this.spawn.group);

		this.addLabel(new GuiNpcLabel(lId++, "spawning.range", x, (y += 22) + 5));
		this.addTextField(new GuiNpcTextField(4, this, this.fontRenderer, x + 164, y, 42, 20, "" + this.spawn.range));
		this.getTextField(4).setNumbersOnly();
		this.getTextField(4).setMinMaxDefault(1, 16, this.spawn.range);
	}

	@Override
	public void unFocused(GuiNpcTextField textField) {
		if (textField.getId() == 1) {
			String name = textField.getText();
			if (name.isEmpty() || this.data.containsKey(name)) {
				textField.setText(this.spawn.name);
			} else {
				String old = this.spawn.name;
				this.data.remove(old);
				this.spawn.name = name;
				this.data.put(this.spawn.name, this.spawn.id);
				this.scroll.replace(old, this.spawn.name);
			}
		}
		else if (textField.getId() == 2) {
			this.spawn.itemWeight = textField.getInteger();
			if (this.getSlider(4) != null) { this.getSlider(4).displayString = new TextComponentTranslation("spawning.weightedChance").getFormattedText() + ": " + this.spawn.itemWeight; }
		}
		else if (textField.getId() == 3) {
			this.spawn.group = textField.getInteger();
		}
		else if (textField.getId() == 4) {
			this.spawn.range = textField.getInteger();
		}
	}
}
