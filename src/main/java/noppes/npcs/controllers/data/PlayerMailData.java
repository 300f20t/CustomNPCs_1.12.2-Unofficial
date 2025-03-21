package noppes.npcs.controllers.data;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.CustomNpcs;

public class PlayerMailData {

	public final List<PlayerMail> playerMails = new ArrayList<>();

	public void addMail(PlayerMail mail) {
		mail = mail.copy();
		mail.timeWhenReceived = System.currentTimeMillis();
		if (mail.timeWhenReceived <= 0L) {
			mail.timeWhenReceived = 100000L;
		}
		mail.timeWillCome = 1000L * ((long) CustomNpcs.MailTimeWhenLettersWillBeReceived[0]
				+ (long) (Math.random() * (double) (CustomNpcs.MailTimeWhenLettersWillBeReceived[1]
						- CustomNpcs.MailTimeWhenLettersWillBeReceived[0])));
		boolean found = true;
		while (found) {
			found = false;
			for (PlayerMail m : this.playerMails) {
				if (m.timeWhenReceived == mail.timeWhenReceived) {
					mail.timeWhenReceived--;
					found = true;
					break;
				}
			}
		}
		this.playerMails.add(mail);
	}

	public PlayerMail get(long id) {
		for (PlayerMail mail : this.playerMails) {
			if (mail.timeWhenReceived == id) {
				return mail;
			}
		}
		return null;
	}

	public PlayerMail get(PlayerMail selected) {
		for (PlayerMail mail : this.playerMails) {
			if (mail.timeWhenReceived == selected.timeWhenReceived && mail.getSubject().equals(selected.getSubject())) {
				return mail;
			}
		}
		return null;
	}

	public boolean hasMail() {
		for (PlayerMail mail : this.playerMails) {
			if (!mail.beenRead) {
				return true;
			}
		}
		return false;
	}

	public void loadNBTData(NBTTagCompound compound) {
		NBTTagList list = compound.getTagList("MailData", 10);
        this.playerMails.clear();
		for (int i = 0; i < list.tagCount(); ++i) {
			PlayerMail mail = new PlayerMail();
			mail.readNBT(list.getCompoundTagAt(i));
			this.playerMails.add(mail);
		}
	}

	public NBTTagCompound saveNBTData(NBTTagCompound compound) {
		NBTTagList list = new NBTTagList();
		for (PlayerMail mail : this.playerMails) {
			list.appendTag(mail.writeNBT());
		}
		compound.setTag("MailData", list);
		return compound;
	}
}
