package noppes.npcs.api.handler.data;

import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IPlayer;

public interface IFaction {
	
	void addHostile(int id);

	boolean getAttackedByMobs();

	int getColor();

	int getDefaultPoints();

	int[] getHostileList();

	int getId();

	boolean getIsHidden();

	String getName();

	boolean hasHostile(int id);

	boolean hostileToFaction(int factionId);

	boolean hostileToNpc(ICustomNpc<?> npc);

	int playerStatus(IPlayer<?> player);

	void removeHostile(int id);

	void save();

	void setAttackedByMobs(boolean bo);

	void setDefaultPoints(int points);

	void setIsHidden(boolean bo);
	
	String getFlag();
	
	void setFlag(String flagPath);
	
	String getDescription();
	
	void setDescription(String descr);
	
}
