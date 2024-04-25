package noppes.npcs.client;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiLanguage;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.ChestRenderer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import noppes.npcs.CommonProxy;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomPacketHandler;
import noppes.npcs.LogWriter;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.blocks.tiles.TileBuilder;
import noppes.npcs.client.gui.GuiNpcPather;
import noppes.npcs.client.gui.player.GuiNpcCarpentryBench;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.client.renderer.MarkRenderer;
import noppes.npcs.constants.EnumPlayerPacket;
import noppes.npcs.controllers.SchematicController;
import noppes.npcs.controllers.data.MarkData;
import noppes.npcs.controllers.data.MiniMapData;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerMiniMapData;
import noppes.npcs.items.ItemBuilder;
import noppes.npcs.schematics.ISchematic;
import noppes.npcs.schematics.Schematic;
import noppes.npcs.schematics.SchematicWrapper;
import noppes.npcs.util.AdditionalMethods;
import noppes.npcs.util.BuilderData;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.ObfuscationHelper;

public class ClientEventHandler {

	public static final Map<EntityPlayer, RenderChatMessages> chatMessages = Maps
			.<EntityPlayer, RenderChatMessages>newHashMap();
	public static GuiScreen gui;

	public static SubGuiInterface subgui;
	public static Map<ISchematic, Integer> displayMap = Maps.<ISchematic, Integer>newHashMap();
	public static BlockPos schemaPos;;
	public static Schematic schema;
	public static int rotaion;
	public static long secs;
	private boolean inGame, miniMapLoaded;

	@SubscribeEvent
	public void cnpcJoinServer(ClientConnectedToServerEvent event) {
		event.getManager().channel().pipeline().addBefore("fml:packet_handler",
				CustomNpcs.MODID + ":custom_packet_handler_client", new CustomPacketHandler());
	}

	@SubscribeEvent
	public void cnpcOpenGUIEvent(GuiOpenEvent event) {
		Minecraft mc = Minecraft.getMinecraft();
		CustomNpcs.debugData.startDebug("Client", "Players", "ClientEventHandler_onOpenGUIEvent");
		ClientEventHandler.gui = event.getGui();
		ClientEventHandler.subgui = null;
		LogWriter.debug(((event.getGui() == null ? "Cloce GUI " : "Open GUI - " + event.getGui().getClass())
				+ "; OLD - " + (mc.currentScreen == null ? "null" : mc.currentScreen.getClass().getSimpleName())));
		NoppesUtilPlayer.sendData(EnumPlayerPacket.OpenGui,
				event.getGui() == null ? "GuiIngame" : event.getGui().getClass().getSimpleName(),
				mc.currentScreen == null ? "GuiIngame" : mc.currentScreen.getClass().getSimpleName());

		if (mc.currentScreen instanceof GuiNpcPather) {
			ClientGuiEventHandler.movingPath.clear();
		} else if (event.getGui() instanceof GuiNpcCarpentryBench || event.getGui() instanceof GuiCrafting) {
			AdditionalMethods.resetRecipes(mc.player, (GuiContainer) event.getGui());
			event.getGui().mc = mc;
		} else if (event.getGui() instanceof GuiInventory && !mc.player.capabilities.isCreativeMode) {
			AdditionalMethods.resetRecipes(mc.player, (GuiContainer) event.getGui());
			event.getGui().mc = mc;
		}
		if (event.getGui() instanceof GuiOptions && mc.currentScreen instanceof GuiLanguage) {
			ClientProxy.checkLocalization();
		}
		ScaledResolution scaleW = new ScaledResolution(mc);
		double[] d = ClientProxy.playerData.hud.getWindowSize();
		if (d[0] != scaleW.getScaledWidth_double() || d[1] != scaleW.getScaledHeight_double()) {
			d[0] = scaleW.getScaledWidth_double();
			d[1] = scaleW.getScaledHeight_double();
			if (mc.player != null) {
				NBTTagCompound compound = new NBTTagCompound();
				NBTTagList list = new NBTTagList();
				list.appendTag(new NBTTagDouble(d[0]));
				list.appendTag(new NBTTagDouble(d[1]));
				compound.setTag("WindowSize", list);
				NoppesUtilPlayer.sendData(EnumPlayerPacket.WindowSize, compound);
			}
			ClientProxy.playerData.hud.clearGuiComponents();
		}
		if (event.getGui() instanceof GuiIngameMenu) {
			if (!this.inGame) {
				this.inGame = true;
				this.miniMapLoaded = false;
				this.updateMiniMaps(true);
				LogWriter.debug("Login: Start game");
			}
		} else if (mc.player == null
				&& (event.getGui() instanceof GuiMainMenu || mc.currentScreen instanceof GuiIngameMenu)) {
			/*
			 * if (mc.currentScreen == null && !this.postLoad) { this.postLoad = true;
			 * List<String> list = Lists.<String>newArrayList(); Option<JSONType> mainSounds
			 * = null; scala.Option<JSONType> customSounds = null; try { customSounds =
			 * JSON.parseRaw(IOUtils.toString(new FileInputStream( new File(CustomNpcs.Dir,
			 * "assets/" + CustomNpcs.MODID+"/sounds.json")), StandardCharsets.UTF_8)); }
			 * catch (IOException e1) {} for (ModContainer mod :
			 * Loader.instance().getModList()) { if (mainSounds!=null) { break; } if
			 * (mod.getSource().exists()) { try { if (!mod.getSource().isDirectory() &&
			 * (mod.getSource().getName().endsWith(".jar") ||
			 * mod.getSource().getName().endsWith(".zip"))) { ZipFile zip = new
			 * ZipFile(mod.getSource()); ZipEntry entry =
			 * zip.getEntry("assets/customnpcs/sounds.json"); if (entry == null) { entry =
			 * zip.getEntry("assets\\customnpcs\\sounds.json"); } try (InputStream is =
			 * zip.getInputStream(entry)) { try (@SuppressWarnings("resource") Scanner s =
			 * new Scanner(is, "UTF-8").useDelimiter("\\A")) { Object fileAsString =
			 * s.hasNext() ? s.next() : ""; mainSounds =
			 * JSON.parseRaw(fileAsString.toString()); } } zip.close(); } else if
			 * (mod.getSource().isDirectory()) { File file = new File(mod.getSource(),
			 * "assets/customnpcs/sounds.json"); if (file.exists()) { try { mainSounds =
			 * JSON.parseRaw(IOUtils.toString(new FileInputStream(file),
			 * StandardCharsets.UTF_8)); } catch (IOException e1) {} } } } catch (Exception
			 * e) { } } } if (mainSounds!=null) { String keys = ((JSONObject)
			 * mainSounds.get()).obj().keySet().toString().replace("Set(", "").replace(")",
			 * ""); for (String key : keys.split(", ")) { list.add(key); } } if
			 * (customSounds!=null) { String keys = ((JSONObject)
			 * customSounds.get()).obj().keySet().toString().replace("Set(",
			 * "").replace(")", ""); for (String key : keys.split(", ")) { list.add(key); }
			 * } SoundHandler sh = Minecraft.getMinecraft().getSoundHandler(); for (String
			 * name : list) { LogWriter.debug("Load Sound: \"" + CustomNpcs.MODID + ":" +
			 * name + "\""); ResourceLocation res = new ResourceLocation(CustomNpcs.MODID,
			 * name); sh.playSound(new PositionedSoundRecord(res, SoundCategory.MUSIC,
			 * 0.00001f, 1.0f, false, 0, ISound.AttenuationType.NONE, 0.0f, 0.0f, 0.0f)); }
			 * try { sh.stop("", SoundCategory.MUSIC); } catch (Exception e) {} }
			 */
			if (this.inGame) {
				LogWriter.debug("Logout: Exit game");
				this.inGame = false;
				if (CustomNpcs.VerboseDebug) {
					CustomNpcs.showDebugs();
				}
			}
		}
		CustomNpcs.debugData.endDebug("Client", "Players", "ClientEventHandler_onOpenGUIEvent");
	}

	@SubscribeEvent
	public void cnpcPostLivingEvent(RenderLivingEvent.Post<EntityLivingBase> event) {
		CustomNpcs.debugData.startDebug("Client", event.getEntity(), "ClientEventHandler_postRenderLivingEvent");
		MarkData data = MarkData.get(event.getEntity());
		for (MarkData.Mark m : data.marks) {
			if (m.getType() != 0 && m.availability.isAvailable(Minecraft.getMinecraft().player)) {
				MarkRenderer.render(event.getEntity(), event.getX(), event.getY(), event.getZ(), m);
				break;
			}
		}
		if (event.getEntity() instanceof EntityPlayer
				&& ClientEventHandler.chatMessages.containsKey((EntityPlayer) event.getEntity())) {
			float height = event.getEntity().height + 0.9f;
			ClientEventHandler.chatMessages.get(event.getEntity()).renderPlayerMessages(event.getX(),
					event.getY() + height, event.getZ(), 0.666667f * height, this.isInRange(
							Minecraft.getMinecraft().player, event.getX(), event.getY() + 1.2d, event.getZ(), 16.0f));
		}
		CustomNpcs.debugData.endDebug("Client", event.getEntity(), "ClientEventHandler_postRenderLivingEvent");
	}

	@SubscribeEvent
	public void cnpcRenderTick(RenderWorldLastEvent event) {
		EntityPlayer player = (EntityPlayer) Minecraft.getMinecraft().player;
		CustomNpcs.debugData.startDebug("Client", player, "ClientEventHandler_onRenderTick");
		ClientEventHandler.schema = null;
		ClientEventHandler.schemaPos = null;
		if (!player.getHeldItemMainhand().isEmpty() && player.getHeldItemMainhand().getItem() instanceof ItemBuilder
				&& player.getHeldItemMainhand().hasTagCompound()
				&& CommonProxy.dataBuilder.containsKey(player.getHeldItemMainhand().getTagCompound().getInteger("ID"))
				&& ClientGuiEventHandler.result != null && ClientGuiEventHandler.result.getBlockPos() != null) {
			BuilderData bd = CommonProxy.dataBuilder
					.get(player.getHeldItemMainhand().getTagCompound().getInteger("ID"));
			if (bd.type == 3 && !bd.schematicaName.isEmpty()) {
				SchematicWrapper schema = SchematicController.Instance.getSchema(bd.schematicaName + ".schematic");
				if (schema != null) {
					Schematic sc = (Schematic) schema.schema;
					ClientEventHandler.schemaPos = ClientGuiEventHandler.result.getBlockPos();
					ClientEventHandler.rotaion = MathHelper.floor(player.rotationYaw / 90.0f + 0.5f) & 0x3;
					int x = -1, y = 0, z = -1;
					boolean has = sc.offset != null
							&& (sc.offset.getX() != 0 || sc.offset.getY() != 0 || sc.offset.getZ() != 0);
					switch (ClientEventHandler.rotaion) {
					case 1: {
						if (has) {
							x = -1 * sc.offset.getZ() - sc.getLength();
							y = sc.offset.getY();
							z = sc.offset.getX() - 1;
						} else {
							x = -1 * sc.getLength();
							z = (int) (-1 * Math.ceil((double) sc.getWidth() / 2.0d));
						}
						break;
					}
					case 2: {
						if (has) {
							x = -1 * sc.offset.getX() - sc.getWidth();
							y = sc.offset.getY();
							z = -1 * sc.offset.getZ() - sc.getLength();
						} else {
							x = (int) (-1 * Math.ceil((double) sc.getWidth() / 2.0d));
							z = -1 * sc.getLength();
						}
						break;
					}
					case 3: {
						if (has) {
							x = sc.offset.getZ() - 1;
							y = sc.offset.getY();
							z = -1 * sc.offset.getX() - sc.getWidth();
						} else {
							x = -1;
							z = (int) (-1 * Math.ceil((double) sc.getWidth() / 2.0d));
						}
						break;
					}
					default: {
						if (has) {
							x = sc.offset.getX() - 1;
							y = sc.offset.getY();
							z = sc.offset.getZ() - 1;
						} else {
							x = (int) (-1 * Math.ceil((double) sc.getWidth() / 2.0d));
						}
						break;
					}
					}
					ClientEventHandler.schema = sc;
					ClientEventHandler.schemaPos = ClientEventHandler.schemaPos.add(x, y, z);
					this.drawSchematic(ClientEventHandler.schemaPos, schema, 0, ClientEventHandler.rotaion);
				}
			}
		}
		if (player.world.getTotalWorldTime() % 100 == 0 && secs != System.currentTimeMillis() / 1000) {
			secs = System.currentTimeMillis() / 1000;
			this.updateMiniMaps(false);
		}
		if (TileBuilder.DrawPoses.isEmpty()) {
			CustomNpcs.debugData.endDebug("Client", player, "ClientEventHandler_onRenderTick");
			return;
		}
		for (BlockPos pos : TileBuilder.DrawPoses) {
			if (pos == null || player == null || pos.distanceSq((Vec3i) player.getPosition()) > 10000.0) {
				continue;
			}
			TileEntity te = player.world.getTileEntity(pos);
			if (!(te instanceof TileBuilder)) {
				continue;
			}
			this.drawSchematic(pos, ((TileBuilder) te).getSchematic(), ((TileBuilder) te).yOffest,
					((TileBuilder) te).rotation);
		}
		CustomNpcs.debugData.endDebug("Client", player, "ClientEventHandler_onRenderTick");
	}

	private void drawSchematic(BlockPos pos, SchematicWrapper schem, int yOffest, int rotation) {
		if (pos == null || schem == null) {
			return;
		}
		GlStateManager.pushMatrix();
		RenderHelper.enableStandardItemLighting();
		GlStateManager.translate(pos.getX() - TileEntityRendererDispatcher.staticPlayerX,
				pos.getY() - TileEntityRendererDispatcher.staticPlayerY + 0.01,
				pos.getZ() - TileEntityRendererDispatcher.staticPlayerZ);
		GlStateManager.translate(1.0f, yOffest, 1.0f);
		// Bound
		if (rotation % 2 == 0) {
			this.drawSelectionBox(
					new BlockPos(schem.schema.getWidth(), schem.schema.getHeight(), schem.schema.getLength()));
		} else {
			this.drawSelectionBox(
					new BlockPos(schem.schema.getLength(), schem.schema.getHeight(), schem.schema.getWidth()));
		}
		if (!ClientEventHandler.displayMap.containsKey(schem.schema)) {
			ClientEventHandler.displayMap.put(schem.schema, GLAllocation.generateDisplayLists(1));
			GL11.glNewList(ClientEventHandler.displayMap.get(schem.schema), 4864);
			try {
				for (int i = 0; i < schem.size && i < 25000; ++i) {
					int posX = i % schem.schema.getWidth();
					int posZ = (i - posX) / schem.schema.getWidth() % schem.schema.getLength();
					int posY = ((i - posX) / schem.schema.getWidth() - posZ) / schem.schema.getLength();
					IBlockState state = schem.schema.getBlockState(posX, posY, posZ);
					if (state.getRenderType() != EnumBlockRenderType.INVISIBLE) {
						BlockPos p = schem.rotatePos(posX, posY, posZ, 0);
						GlStateManager.pushMatrix();
						GlStateManager.pushAttrib();
						GlStateManager.enableRescaleNormal();
						GlStateManager.translate(p.getX(), p.getY(), p.getZ());
						Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
						GlStateManager.rotate(-90.0f, 0.0f, 1.0f, 0.0f);
						state = SchematicWrapper.rotationState(state, 0);
						try {
							this.renderBlock(state);
							if (GL11.glGetError() != 0) {
								break;
							}
						} catch (Exception ex) {
						} finally {
							GlStateManager.popAttrib();
							GlStateManager.disableRescaleNormal();
							GlStateManager.popMatrix();
						}
					}
				}
			} catch (Exception e) {
				LogWriter.error("Error preview builder block", e);
			} finally {
				GL11.glEndList();
				if (GL11.glGetError() != 0) {
					ClientEventHandler.displayMap.remove(schem.schema);
				}
			}
		}
		if (ClientEventHandler.displayMap.containsKey(schem.schema)) {
			GlStateManager.rotate(rotation * -90.0f, 0.0f, 1.0f, 0.0f);
			switch (rotation) {
			case 1: {
				GlStateManager.translate(0, 0, -1 * schem.schema.getLength());
				break;
			}
			case 2: {
				GlStateManager.translate(-1 * schem.schema.getWidth(), 0, -1 * schem.schema.getLength());
				break;
			}
			case 3: {
				GlStateManager.translate(-1 * schem.schema.getWidth(), 0, 0);
				break;
			}
			}
			GlStateManager.callList(ClientEventHandler.displayMap.get(schem.schema));
		}
		RenderHelper.disableStandardItemLighting();
		GlStateManager.translate(-1.0f, 0.0f, -1.0f);
		GlStateManager.popMatrix();
	}

	public void drawSelectionBox(BlockPos pos) {
		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.disableCull();
		GlStateManager.disableBlend();
		AxisAlignedBB bb = new AxisAlignedBB(BlockPos.ORIGIN, pos);
		RenderGlobal.drawSelectionBoundingBox(bb, 1.0f, 0.0f, 0.0f, 1.0f);
		GlStateManager.enableTexture2D();
		GlStateManager.enableLighting();
		GlStateManager.enableCull();
		GlStateManager.disableBlend();
	}

	private boolean isInRange(EntityPlayer player, double posX, double posY, double posZ, double range) {
		double y = Math.abs(player.posY - posY);
		if (posY >= 0.0 && y > range) {
			return false;
		}
		double x = Math.abs(player.posX - posX);
		double z = Math.abs(player.posZ - posZ);
		return x <= range && z <= range;
	}

	@SubscribeEvent
	public void npcPlayerLoginEvent(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
		if (!event.player.world.isRemote) {
			return;
		}
		ClientProxy.playerData.hud.clear();
	}

	private void renderBlock(IBlockState state) {
		BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
		// dispatcher.renderBlockBrightness(state, 0.1f);
		switch (state.getRenderType()) {
		case MODEL:
			IBakedModel ibakedmodel = dispatcher.getModelForState(state);
			GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
			BlockModelRenderer bmr = ObfuscationHelper.getValue(BlockRendererDispatcher.class, dispatcher,
					BlockModelRenderer.class);
			BlockColors bc = ObfuscationHelper.getValue(BlockModelRenderer.class, bmr, BlockColors.class);
			int color = bc.colorMultiplier(state, (IBlockAccess) null, (BlockPos) null, 0);
			if (EntityRenderer.anaglyphEnable) {
				color = TextureUtil.anaglyphColor(color);
			}
			float r = (float) (color >> 16 & 255) / 255.0F;
			float g = (float) (color >> 8 & 255) / 255.0F;
			float b = (float) (color & 255) / 255.0F;
			for (EnumFacing enumfacing : EnumFacing.values()) {
				this.renderModelBlockQuads(ibakedmodel.getQuads(state, enumfacing, 0L), r, g, b);
			}
			this.renderModelBlockQuads(ibakedmodel.getQuads(state, (EnumFacing) null, 0L), r, g, b);
			break;
		case ENTITYBLOCK_ANIMATED:
			ChestRenderer chestRenderer = ObfuscationHelper.getValue(BlockRendererDispatcher.class, dispatcher,
					ChestRenderer.class);
			chestRenderer.renderChestBrightness(state.getBlock(), 1.0f);
		case LIQUID:
			break;
		default:
			break;
		}
	}

	private void renderModelBlockQuads(List<BakedQuad> quads, float r, float g, float b) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuffer();
		for (int j = quads.size(), i = 0; i < j; ++i) {
			BakedQuad bakedquad = quads.get(i);
			bufferbuilder.begin(7, DefaultVertexFormats.ITEM);
			bufferbuilder.addVertexData(bakedquad.getVertexData());
			if (bakedquad.hasTintIndex()) {
				bufferbuilder.putColorRGB_F4(r, g, b);
			} else {
				bufferbuilder.putColorRGB_F4(1.0f, 1.0f, 1.0f);
			}
			Vec3i vec3i = bakedquad.getFace().getDirectionVec();
			bufferbuilder.putNormal((float) vec3i.getX(), (float) vec3i.getY(), (float) vec3i.getZ());

			tessellator.draw();
		}
	}

	@SuppressWarnings("unchecked")
	private void updateMiniMaps(boolean update) {
		PlayerMiniMapData mm = PlayerData.get(Minecraft.getMinecraft().player).minimap;
		// Found Mods:
		boolean hasJourneyMap = false;
		boolean hasXaeroMap = false;
		boolean hasVoxelMap = false;
		for (ModContainer mod : Loader.instance().getModList()) {
			if (mod.getModId().equals("journeymap")) {
				hasJourneyMap = true;
			}
			if (mod.getModId().equals("xaerominimap")) {
				hasXaeroMap = true;
			}
			if (mod.getModId().equals("voxelmap")) {
				hasVoxelMap = true;
			}
		}
		// If name is changed:
		if (!hasJourneyMap) {
			try {
				Class<?> jm = Class.forName("journeymap.client.model.Waypoint");
				hasJourneyMap = jm != null;
			} catch (Exception e) {
			}
		}
		if (!hasXaeroMap) {
			try {
				Class<?> xm = Class.forName("xaero.common.minimap.waypoints.Waypoint");
				hasXaeroMap = xm != null;
			} catch (Exception e) {
			}
		}
		if (!hasVoxelMap) {
			try {
				Class<?> vm = Class.forName("com.mamiyaotaru.voxelmap.VoxelMap");
				hasVoxelMap = vm != null;
			} catch (Exception e) {
			}
		}
		// Cheak save client Points:
		Map<Integer, List<MiniMapData>> ps = Maps.<Integer, List<MiniMapData>>newTreeMap();
		if (hasJourneyMap) {
			mm.addData.clear();
			if (!mm.modName.equals("journeymap")) {
				mm.modName = "journeymap";
				update = true;
			}
			try {
				Class<?> ws = Class.forName("journeymap.client.waypoint.WaypointStore");
				miniMapLoaded = (boolean) ws.getDeclaredMethod("hasLoaded").invoke(ws.getEnumConstants()[0]);
				if (!miniMapLoaded) {
					CustomNPCsScheduler.runTack(() -> {
						this.updateMiniMaps(true);
					}, 50);
					return;
				}
				Collection<Object> waypoints = (Collection<Object>) ws.getDeclaredMethod("getAll")
						.invoke(ws.getEnumConstants()[0]); // Collection<Object> waypoints =
															// WaypointStore.INSTANCE.getAll();
				for (Object waypoint : waypoints) {
					Class<?> wc = waypoint.getClass();
					MiniMapData mmd = new MiniMapData();
					mmd.name = (String) wc.getDeclaredMethod("getName").invoke(waypoint);
					mmd.type = wc.getDeclaredMethod("getType").invoke(waypoint).toString();
					mmd.icon = (String) wc.getDeclaredMethod("getIcon").invoke(waypoint);
					mmd.pos = NpcAPI.Instance()
							.getIPos((BlockPos) wc.getDeclaredMethod("getBlockPos").invoke(waypoint));
					mmd.color = new Color((int) wc.getDeclaredMethod("getR").invoke(waypoint),
							(int) wc.getDeclaredMethod("getG").invoke(waypoint),
							(int) wc.getDeclaredMethod("getB").invoke(waypoint)).getRGB();
					mmd.isEnable = (boolean) wc.getDeclaredMethod("isEnable").invoke(waypoint);
					Collection<Integer> dimensions = (Collection<Integer>) wc.getDeclaredMethod("getDimensions")
							.invoke(waypoint);
					mmd.dimIDs = new int[dimensions.size()];
					int i = 0;
					for (int dim : dimensions) {
						mmd.dimIDs[i] = dim;
						i++;
					}
					int dimId = mmd.dimIDs[0];

					if (!ps.containsKey(dimId)) {
						ps.put(dimId, Lists.<MiniMapData>newArrayList());
					}
					mmd.id = ps.get(dimId).size();
					ps.get(dimId).add(mmd);

					if (!mm.points.containsKey(dimId)) {
						update = true;
					} else if (mm.points.get(dimId).get(mmd.id) == null
							|| !mm.points.get(dimId).get(mmd.id).equals(mmd)) {
						update = true;
					} else if (mm.points.get(dimId).get(mmd.id) != null) {
						MiniMapData mmdB = mm.points.get(dimId).get(mmd.id);
						if (mmdB.name.equals(mmd.name)) {
							mmd.setQuest(mmdB);
						}
					}
				}
			} catch (Exception e) {
			}
		} else if (hasXaeroMap) {
			if (!mm.modName.equals("xaerominimap")) {
				mm.modName = "xaerominimap";
				update = true;
			}
			try {
				Class<?> xms = Class.forName("xaero.common.XaeroMinimapSession");
				Object minimapSession = xms.getDeclaredMethod("getCurrentSession").invoke(xms); // XaeroMinimapSession
				if (minimapSession != null) {
					Field fl = xms.getDeclaredField("usable");
					fl.setAccessible(true);
					miniMapLoaded = fl.getBoolean(minimapSession);
				} else {
					miniMapLoaded = false;
				}
				if (!miniMapLoaded) {
					CustomNPCsScheduler.runTack(() -> {
						this.updateMiniMaps(true);
					}, 50);
					return;
				}

				Object waypointsManager = xms.getDeclaredMethod("getWaypointsManager").invoke(minimapSession); // WaypointsManager

				Method getWaypointMap = waypointsManager.getClass().getDeclaredMethod("getWaypointMap");
				HashMap<String, Object> waypointMap = (HashMap<String, Object>) getWaypointMap.invoke(waypointsManager);

				String mainContainerID = (String) waypointsManager.getClass()
						.getDeclaredMethod("getAutoRootContainerID").invoke(waypointsManager);
				Object wwrc = waypointMap.get(mainContainerID);// WaypointWorldRootContainer
				if (wwrc == null) {
					if (!mm.points.isEmpty()) {
						mm.points.clear();
						update = true;
					}
				} else {
					mm.addData.clear();
					Gson gson = new Gson();
					Field subContainers = wwrc.getClass().getField("subContainers");
					Field worlds = wwrc.getClass().getField("worlds");
					HashMap<String, Object> dimMap = (HashMap<String, Object>) subContainers.get(wwrc);
					for (String k : dimMap.keySet()) {
						if (!mm.addData.containsKey("xaero_world_name")) {
							String world_name = (String) dimMap.get(k).getClass().getDeclaredMethod("getKey")
									.invoke(dimMap.get(k));
							while (world_name.lastIndexOf("/") != -1) {
								world_name = world_name.substring(0, world_name.lastIndexOf("/"));
							}
							mm.addData.put("xaero_world_name", world_name);
						}
						HashMap<String, Object> worldMap = (HashMap<String, Object>) worlds.get(dimMap.get(k));
						for (String k1 : worldMap.keySet()) {
							if (!k1.equals("waypoints")) {
								continue;
							}
							Object waypointWorld = worldMap.get(k1); // WaypointWorld
							int dimId = (int) waypointWorld.getClass().getDeclaredMethod("getDimId")
									.invoke(waypointWorld);
							HashMap<String, Object> sets = (HashMap<String, Object>) waypointWorld.getClass()
									.getDeclaredMethod("getSets").invoke(waypointWorld);
							for (String ks : sets.keySet()) {
								Object waypointSet = sets.get(ks); // WaypointSet
								List<Object> list = (List<Object>) waypointSet.getClass().getDeclaredMethod("getList")
										.invoke(waypointSet);
								for (Object waypoint : list) {
									Class<?> wc = waypoint.getClass();
									MiniMapData mmd = new MiniMapData();
									mmd.name = (String) wc.getDeclaredMethod("getName").invoke(waypoint);
									mmd.type = wc.getDeclaredMethod("getWaypointType").invoke(waypoint).toString();
									mmd.icon = (String) wc.getDeclaredMethod("getSymbol").invoke(waypoint);
									int x = (int) wc.getDeclaredMethod("getX").invoke(waypoint);
									int y = (int) wc.getDeclaredMethod("getY").invoke(waypoint);
									int z = (int) wc.getDeclaredMethod("getZ").invoke(waypoint);
									mmd.pos = NpcAPI.Instance().getIPos(new BlockPos(x, y, z));
									mmd.color = (int) wc.getDeclaredMethod("getColor").invoke(waypoint);
									mmd.isEnable = !((boolean) wc.getDeclaredMethod("isDisabled").invoke(waypoint));
									mmd.dimIDs = new int[] { dimId };
									mmd.gsonData.put("temporary",
											gson.toJson(wc.getDeclaredMethod("isTemporary").invoke(waypoint)));

									if (!ps.containsKey(dimId)) {
										ps.put(dimId, Lists.<MiniMapData>newArrayList());
									}
									mmd.id = ps.get(dimId).size();
									ps.get(dimId).add(mmd);

									if (!mm.points.containsKey(dimId)) {
										update = true;
									} else if (mm.points.get(dimId).get(mmd.id) == null
											|| !mm.points.get(dimId).get(mmd.id).equals(mmd)) {
										update = true;
									} else if (mm.points.get(dimId).get(mmd.id) != null) {
										MiniMapData mmdB = mm.points.get(dimId).get(mmd.id);
										if (mmdB.name.equals(mmd.name)) {
											mmd.setQuest(mmdB);
										}
									}
								}
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (hasVoxelMap) {
			mm.addData.clear();
			if (!mm.modName.equals("voxelmap")) {
				mm.modName = "voxelmap";
				update = true;
			}
			try {
				Class<?> vm = Class.forName("com.mamiyaotaru.voxelmap.VoxelMap");
				Object instance = vm.getMethod("getInstance").invoke(vm);
				Object waypointManager = vm.getMethod("getWaypointManager").invoke(instance);

				Field fl = waypointManager.getClass().getDeclaredField("loaded");
				fl.setAccessible(true);
				miniMapLoaded = fl.getBoolean(waypointManager);
				if (!miniMapLoaded) {
					CustomNPCsScheduler.runTack(() -> {
						this.updateMiniMaps(true);
					}, 50);
					return;
				}
				List<Object> waypoints = (List<Object>) waypointManager.getClass().getMethod("getWaypoints")
						.invoke(waypointManager);

				for (Object waypoint : waypoints) {
					Class<?> wc = waypoint.getClass();
					MiniMapData mmd = new MiniMapData();
					mmd.name = (String) wc.getDeclaredField("name").get(waypoint);
					mmd.gsonData.put("voxel_world_name", (String) wc.getDeclaredField("world").get(waypoint));
					mmd.icon = (String) wc.getDeclaredField("imageSuffix").get(waypoint);
					int x = (int) wc.getDeclaredField("x").get(waypoint);
					int y = (int) wc.getDeclaredField("y").get(waypoint);
					int z = (int) wc.getDeclaredField("z").get(waypoint);
					mmd.pos = NpcAPI.Instance().getIPos(x, y, z);
					mmd.color = new Color((float) wc.getDeclaredField("red").get(waypoint),
							(float) wc.getDeclaredField("green").get(waypoint),
							(float) wc.getDeclaredField("blue").get(waypoint)).getRGB();
					mmd.isEnable = (boolean) wc.getDeclaredField("enabled").get(waypoint);
					TreeSet<Integer> dimensions = (TreeSet<Integer>) wc.getDeclaredField("dimensions").get(waypoint);
					mmd.dimIDs = new int[dimensions.size()];
					int i = 0;
					for (int dim : dimensions) {
						mmd.dimIDs[i] = dim;
						i++;
					}
					int dimId = mmd.dimIDs[0];

					if (!ps.containsKey(dimId)) {
						ps.put(dimId, Lists.<MiniMapData>newArrayList());
					}
					mmd.id = ps.get(dimId).size();
					ps.get(dimId).add(mmd);

					if (!mm.points.containsKey(dimId)) {
						update = true;
					} else if (mm.points.get(dimId).get(mmd.id) == null
							|| !mm.points.get(dimId).get(mmd.id).equals(mmd)) {
						update = true;
					} else if (mm.points.get(dimId).get(mmd.id) != null) {
						MiniMapData mmdB = mm.points.get(dimId).get(mmd.id);
						if (mmdB.name.equals(mmd.name)) {
							mmd.setQuest(mmdB);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				;
			}
		} else {
			mm.addData.clear();
			if (!mm.modName.equals("non")) {
				mm.modName = "non";
				update = true;
			}
		}
		if (!update) {
			update = ps.size() != mm.points.size();
		}
		if (!update) {
			for (int id : ps.keySet()) {
				if (!mm.points.containsKey(id)) {
					update = true;
					break;
				} else {
					update = ps.get(id).size() != mm.points.get(id).size();
					if (update) {
						break;
					}
				}
			}
		}
		// Send
		if (update) {
			LogWriter.debug("MiniMap update: " + mm.modName + "; dims: " + mm.points.size());
			mm.points.clear();
			mm.points.putAll(ps);
			NoppesUtilPlayer.sendData(EnumPlayerPacket.MiniMapData, mm.saveNBTData(new NBTTagCompound()));
		}
	}
}
