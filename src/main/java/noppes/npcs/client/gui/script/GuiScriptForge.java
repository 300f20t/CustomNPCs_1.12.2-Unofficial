package noppes.npcs.client.gui.script;

import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.client.Client;
import noppes.npcs.client.gui.util.ISubGuiListener;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.constants.EnumPlayerPacket;
import noppes.npcs.controllers.data.ForgeScriptData;

public class GuiScriptForge
extends GuiScriptInterface
implements ISubGuiListener {

	private final ForgeScriptData script;

	public GuiScriptForge() {
		super();
		script = new ForgeScriptData();
		handler = script;
		Client.sendData(EnumPacketServer.ScriptForgeGet);
	}

	@Override
	public void save() {
		super.save();
		Client.sendData(EnumPacketServer.ScriptForgeSave, script.writeToNBT(new NBTTagCompound()));
	}

	@Override
	public void setGuiData(NBTTagCompound compound) {
		script.readFromNBT(compound);
		super.setGuiData(compound);
	}

	@Override
	public void subGuiClosed(SubGuiInterface subgui) {
		if (subgui instanceof GuiScriptEncrypt && ((GuiScriptEncrypt) subgui).send) {
			NBTTagCompound nbt = new NBTTagCompound();
			script.writeToNBT(nbt);
			nbt.setString("Name", subgui.getTextField(0).getFullText() + ((GuiScriptEncrypt) subgui).ext);
			nbt.setString("Path", path.replaceAll("\\\\", "/") + "/" + nbt.getString("Name"));
			nbt.setInteger("Tab", activeTab - 1);
			nbt.setByte("Type", (byte) 1);
			nbt.setBoolean("OnlyTab", ((GuiScriptEncrypt) subgui).onlyTab);
			NoppesUtilPlayer.sendData(EnumPlayerPacket.ScriptEncrypt, nbt);
			displayGuiScreen(null);
			mc.setIngameFocus();
		}
	}

}
