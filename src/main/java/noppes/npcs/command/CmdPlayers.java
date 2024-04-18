package noppes.npcs.command;

import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import noppes.npcs.CustomNpcs;
import noppes.npcs.Server;
import noppes.npcs.api.CommandNoppesBase;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.controllers.MarcetController;
import noppes.npcs.controllers.PlayerDataController;
import noppes.npcs.controllers.data.Marcet;
import noppes.npcs.controllers.data.PlayerData;

public class CmdPlayers extends CommandNoppesBase {

	@Override
	public String getDescription() {
		return "Player mod data";
	}

	@Override
	public String getName() {
		return "player";
	}
	
	@SubCommand(desc = "Show the store window to the player", usage = "<playername> <marcetID>")
	public void openmarcet(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		EntityPlayerMP player = null;
		try { player  = CommandBase.getPlayer(server, sender, args[0]); }
		catch (Exception e) {
			throw new PlayerNotFoundException("commands.generic.player.notFound", new Object[] {args[0]});
		}
		int marcetId = -1;
		try { marcetId = Integer.parseInt(args[1]); }
		catch (NumberFormatException ex) { throw new CommandException("Must be an integer: "+args[1]); }
		Marcet marcet = (Marcet) MarcetController.getInstance().getMarcet(marcetId);
		if (marcet == null || !marcet.isValid()) {
			sender.sendMessage(new TextComponentTranslation("command.player.openmarcet.error", ""+marcetId));
			return;
		}
		Server.sendDataChecked(player, EnumPacketClient.GUI, EnumGuiType.PlayerTrader.ordinal(), marcetId, 0, 0);
	}
	
	@SubCommand(desc = "Shows the player's virtual currency balance", usage = "<playername>")
	public void getmoney(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		Object[] objs = this.getPlayerData(server, sender, args[0]);
		PlayerData playerdata = (PlayerData) objs[0];
		boolean isOnline = objs[1] != null;
		if (playerdata==null) {
			throw new PlayerNotFoundException("commands.generic.player.notFound", new Object[] {args[0]});
		}
		sender.sendMessage(new TextComponentTranslation("command.player.getmoney", playerdata.playername, ""+playerdata.game.getMoney(), ""+CustomNpcs.CharCurrencies.charAt(0)).appendSibling(new TextComponentTranslation(isOnline ? "gui.online" : "gui.offline")));
	}

	@SubCommand(desc = "Change the player's virtual currency balance", usage = "<playername> <value>")
	public void addmoney(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		Object[] objs = this.getPlayerData(server, sender, args[0]);
		PlayerData playerdata = (PlayerData) objs[0];
		boolean isOnline = objs[1] != null;
		if (playerdata==null) {
			throw new PlayerNotFoundException("commands.generic.player.notFound", args[0]);
		}
		try {
			long money = Long.parseLong(args[1]);
			playerdata.game.addMoney(money);
			sender.sendMessage(new TextComponentTranslation("command.player."+(money>=0 ? "add" : "del")+"money", playerdata.playername, ""+money, ""+playerdata.game.getMoney(), CustomNpcs.CharCurrencies).appendSibling(new TextComponentTranslation(isOnline ? "gui.online" : "gui.offline")));
		}
		catch (Exception e) { }
	}
	
	private Object[] getPlayerData(MinecraftServer server, ICommandSender sender, String playername) {
		EntityPlayerMP player = null;
		try { player = CommandBase.getPlayer(server, sender, playername); }
		catch (Exception e) { e.printStackTrace(); }
		PlayerData playerdata = null;
		if (player != null) { playerdata = PlayerData.get(player); }
		else { playerdata = PlayerDataController.instance.getDataFromUsername(server, playername); }
		return new Object[] { playerdata, player };
	}
	
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
		List<String> list = Lists.<String>newArrayList();
		if (args.length==2) {
			list = PlayerDataController.instance.getPlayerNames();
		}
		if (args.length==3) {
			if (args[0].equalsIgnoreCase("openmarcet")) {
				for (int id : MarcetController.getInstance().marcets.keySet()) { list.add("" + id); }
			}
		}
		return list;
	}
	
}
