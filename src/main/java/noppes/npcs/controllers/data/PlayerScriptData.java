package noppes.npcs.controllers.data;

import java.util.*;

import com.google.common.base.MoreObjects;

import com.google.common.collect.Lists;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.Event;
import noppes.npcs.EventHooks;
import noppes.npcs.NBTTags;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.util.Util;

public class PlayerScriptData
extends BaseScriptData {

	private static TreeMap<Long, String> console = new TreeMap<>();
	private static List<Integer> errored = new ArrayList<>();
	
	private long lastPlayerUpdate = 0L;
	private final EntityPlayer player;
	private IPlayer<?> playerAPI;

	public PlayerScriptData(EntityPlayer player) {
		super();
		this.player = player;
		if (player != null) {
			this.enabled = ScriptController.Instance.playerScripts.enabled;
			this.hadInteract = ScriptController.Instance.playerScripts.hadInteract;
			this.lastInited = ScriptController.Instance.playerScripts.lastInited;
			this.scriptLanguage = ScriptController.Instance.playerScripts.scriptLanguage;
			this.scripts.clear();
			for (ScriptContainer sCon : ScriptController.Instance.playerScripts.scripts) {
				this.scripts.add(sCon.copyTo(this));
			}
		}
	}

	@Override
	public void clear() {
		PlayerScriptData.console = new TreeMap<>();
		PlayerScriptData.errored = new ArrayList<>();
		this.scripts = new ArrayList<>();
	}

	@Override
	public void clearConsole() {
		PlayerScriptData.console.clear();
	}

	@Override
	public TreeMap<Long, String> getConsoleText() {
		return PlayerScriptData.console;
	}

	@Override
	public void clearConsoleText(Long key) {
		PlayerScriptData.console.remove(key);
	}

	public IPlayer<?> getPlayer() {
		if (this.playerAPI == null) {
			this.playerAPI = (IPlayer<?>) Objects.requireNonNull(NpcAPI.Instance()).getIEntity(this.player);
		}
		return this.playerAPI;
	}

	@Override
	public String noticeString(String type, Object event) {
		EntityPlayer p = player;
		if (event instanceof PlayerEvent && p == null) { p = ((PlayerEvent) event).player.getMCEntity(); }
		String notice = p == null ? "Global players script" : "Player:";
		if (type != null) { notice += " hook \""+type+"\""; }
		if (p == null) { return notice + "; Side: " + (isClient() ? "Client" : "Server"); }
		notice += " \"" + p.getName() + "\"; UUID: \"" + p.getUniqueID() + "\"" +
				" in dimension ID:" + (p.world == null ? 0 : p.world.provider.getDimension()) +
				"; X:" + (Math.round(p.posX * 100.0d) / 100.0d) +
				"; Y:" + (Math.round(p.posY * 100.0d) / 100.0d) +
				"; Z:" + (Math.round(p.posZ * 100.0d) / 100.0d) +
				"; Side: " + (isClient() ? "Client" : "Server");
		return notice;
	}

	public void readFromNBT(NBTTagCompound compound) {
		this.scripts = NBTTags.GetScript(compound.getTagList("Scripts", 10), this, false);
		this.scriptLanguage = Util.instance.deleteColor(compound.getString("ScriptLanguage"));
		this.enabled = compound.getBoolean("ScriptEnabled");
		PlayerScriptData.console = NBTTags.GetLongStringMap(compound.getTagList("ScriptConsole", 10));
	}

	@Override
	public void runScript(String type, Event event) {
		super.runScript(type, event);
		if (!this.isEnabled()) {
			return;
		}
		if (ScriptController.Instance.lastLoaded > this.lastInited
				|| ScriptController.Instance.lastPlayerUpdate > this.lastPlayerUpdate) {
			this.lastInited = ScriptController.Instance.lastLoaded;
			PlayerScriptData.errored.clear();
			if (this.player != null) {
				this.scripts.clear();
				for (ScriptContainer script : ScriptController.Instance.playerScripts.scripts) {
					ScriptContainer s = new ScriptContainer(this, isClient());
					s.readFromNBT(script.writeToNBT(new NBTTagCompound()), this.isClient());
					this.scripts.add(s);
				}
			}
			this.lastPlayerUpdate = ScriptController.Instance.lastPlayerUpdate;
			if (!type.equalsIgnoreCase(EnumScriptType.INIT.function)) {
				EventHooks.onPlayerInit(this);
			}
		}
		for (int i = 0; i < this.scripts.size(); ++i) {
			ScriptContainer script = this.scripts.get(i);
			if (!PlayerScriptData.errored.contains(i)) {
				script.run(type, event, !this.isClient());
				if (script.errored) {
					PlayerScriptData.errored.add(i);
				}
				for (Map.Entry<Long, String> entry : script.console.entrySet()) {
					if (!PlayerScriptData.console.containsKey(entry.getKey())) {
						PlayerScriptData.console.put(entry.getKey(), " tab " + (i + 1) + ":\n" + entry.getValue());
					}
				}
				script.console.clear();
			}
		}
	}
	
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setTag("Scripts", NBTTags.NBTScript(this.scripts));
		compound.setString("ScriptLanguage", this.scriptLanguage);
		compound.setBoolean("ScriptEnabled", this.enabled);
		compound.setTag("ScriptConsole", NBTTags.NBTLongStringMap(PlayerScriptData.console));
		return compound;
	}

}
