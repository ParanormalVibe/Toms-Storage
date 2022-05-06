package com.tom.storagemod;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.tom.storagemod.NetworkHandler.IDataReceiver;
import com.tom.storagemod.block.BlockInventoryCable;
import com.tom.storagemod.block.BlockInventoryCableConnector;
import com.tom.storagemod.block.BlockInventoryCableConnectorFiltered;
import com.tom.storagemod.block.BlockInventoryCableConnectorFramed;
import com.tom.storagemod.block.BlockInventoryCableFramed;
import com.tom.storagemod.block.BlockInventoryCableFramedPainted;
import com.tom.storagemod.block.BlockInventoryHopperBasic;
import com.tom.storagemod.block.BlockInventoryProxy;
import com.tom.storagemod.block.BlockLevelEmitter;
import com.tom.storagemod.block.BlockOpenCrate;
import com.tom.storagemod.block.BlockPaintedTrim;
import com.tom.storagemod.block.BlockTrim;
import com.tom.storagemod.block.CraftingTerminal;
import com.tom.storagemod.block.InventoryConnector;
import com.tom.storagemod.block.StorageTerminal;
import com.tom.storagemod.gui.ContainerCraftingTerminal;
import com.tom.storagemod.gui.ContainerFiltered;
import com.tom.storagemod.gui.ContainerInventoryLink;
import com.tom.storagemod.gui.ContainerLevelEmitter;
import com.tom.storagemod.gui.ContainerStorageTerminal;
import com.tom.storagemod.item.ItemAdvWirelessTerminal;
import com.tom.storagemod.item.ItemBlockPainted;
import com.tom.storagemod.item.ItemPaintKit;
import com.tom.storagemod.item.ItemWirelessTerminal;
import com.tom.storagemod.tile.TileEntityCraftingTerminal;
import com.tom.storagemod.tile.TileEntityInventoryCableConnector;
import com.tom.storagemod.tile.TileEntityInventoryCableConnectorFiltered;
import com.tom.storagemod.tile.TileEntityInventoryConnector;
import com.tom.storagemod.tile.TileEntityInventoryHopperBasic;
import com.tom.storagemod.tile.TileEntityInventoryProxy;
import com.tom.storagemod.tile.TileEntityLevelEmitter;
import com.tom.storagemod.tile.TileEntityOpenCrate;
import com.tom.storagemod.tile.TileEntityPainted;
import com.tom.storagemod.tile.TileEntityStorageTerminal;

import io.netty.buffer.ByteBufOutputStream;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

public class StorageMod implements ModInitializer {
	// Directly reference a log4j logger.
	public static final Logger LOGGER = LogManager.getLogger();

	public static final String modid = "toms_storage";
	public static InventoryConnector connector;
	public static StorageTerminal terminal;
	public static BlockTrim inventoryTrim;
	public static BlockOpenCrate openCrate;
	public static BlockPaintedTrim paintedTrim;
	public static BlockInventoryCable invCable;
	public static BlockInventoryCableFramed invCableFramed;
	public static BlockInventoryCableFramedPainted invCablePainted;
	public static BlockInventoryCableConnector invCableConnector;
	public static BlockInventoryCableConnectorFiltered invCableConnectorFiltered;
	public static BlockInventoryCableConnectorFramed invCableConnectorFramed, invCableConnectorPainted;
	public static BlockInventoryProxy invProxy, invProxyPainted;
	public static CraftingTerminal craftingTerminal;
	public static BlockInventoryHopperBasic invHopperBasic;
	public static BlockLevelEmitter levelEmitter;

	public static ItemPaintKit paintingKit;
	public static ItemWirelessTerminal wirelessTerminal;
	public static ItemAdvWirelessTerminal advWirelessTerminal;

	public static BlockEntityType<TileEntityInventoryConnector> connectorTile;
	public static BlockEntityType<TileEntityStorageTerminal> terminalTile;
	public static BlockEntityType<TileEntityOpenCrate> openCrateTile;
	public static BlockEntityType<TileEntityPainted> paintedTile;
	public static BlockEntityType<TileEntityInventoryCableConnector> invCableConnectorTile;
	public static BlockEntityType<TileEntityInventoryCableConnectorFiltered> invCableConnectorFilteredTile;
	public static BlockEntityType<TileEntityInventoryProxy> invProxyTile;
	public static BlockEntityType<TileEntityCraftingTerminal> craftingTerminalTile;
	public static BlockEntityType<TileEntityInventoryHopperBasic> invHopperBasicTile;
	public static BlockEntityType<TileEntityLevelEmitter> levelEmitterTile;

	public static ScreenHandlerType<ContainerStorageTerminal> storageTerminal;
	public static ScreenHandlerType<ContainerCraftingTerminal> craftingTerminalCont;
	public static ScreenHandlerType<ContainerFiltered> filteredConatiner;
	public static ScreenHandlerType<ContainerLevelEmitter> levelEmitterConatiner;
	public static ScreenHandlerType<ContainerInventoryLink> inventoryLink;

	public static final Gson gson = new GsonBuilder().create();
	public static ConfigHolder<Config> configHolder = AutoConfig.register(Config.class, GsonConfigSerializer::new);
	private static Config LOADED_CONFIG = configHolder.getConfig();
	public static Config CONFIG = new Config();

	public static Set<Block> multiblockInvs;

	public StorageMod() {
	}

	public static final ItemGroup STORAGE_MOD_TAB = FabricItemGroupBuilder.build(id("tab"), () -> new ItemStack(terminal));


	public static Identifier id(String id) {
		return new Identifier(modid, id);
	}

	@Override
	public void onInitialize() {
		LOGGER.info("Tom's Storage Setup starting");
		connector = new InventoryConnector();
		terminal = new StorageTerminal();
		openCrate = new BlockOpenCrate();
		inventoryTrim = new BlockTrim();
		paintedTrim = new BlockPaintedTrim();
		invCable = new BlockInventoryCable();
		invCableFramed = new BlockInventoryCableFramed();
		invCablePainted = new BlockInventoryCableFramedPainted();
		invCableConnector = new BlockInventoryCableConnector();
		invCableConnectorFiltered = new BlockInventoryCableConnectorFiltered();
		invCableConnectorFramed = new BlockInventoryCableConnectorFramed();
		invCableConnectorPainted = new BlockInventoryCableConnectorFramed();
		invProxy = new BlockInventoryProxy();
		craftingTerminal = new CraftingTerminal();
		invHopperBasic = new BlockInventoryHopperBasic();
		levelEmitter = new BlockLevelEmitter();
		invProxyPainted = new BlockInventoryProxy();

		paintingKit = new ItemPaintKit();
		wirelessTerminal = new ItemWirelessTerminal();
		advWirelessTerminal = new ItemAdvWirelessTerminal();

		connectorTile = FabricBlockEntityTypeBuilder.create(TileEntityInventoryConnector::new, connector).build(null);
		terminalTile = FabricBlockEntityTypeBuilder.create(TileEntityStorageTerminal::new, terminal).build(null);
		openCrateTile = FabricBlockEntityTypeBuilder.create(TileEntityOpenCrate::new, openCrate).build(null);
		paintedTile = FabricBlockEntityTypeBuilder.create(TileEntityPainted::new, paintedTrim, invCableFramed, invCablePainted).build(null);
		invCableConnectorTile = FabricBlockEntityTypeBuilder.create(TileEntityInventoryCableConnector::new, invCableConnector, invCableConnectorFramed, invCableConnectorPainted).build(null);
		invCableConnectorFilteredTile = FabricBlockEntityTypeBuilder.create(TileEntityInventoryCableConnectorFiltered::new, invCableConnectorFiltered).build(null);
		invProxyTile = FabricBlockEntityTypeBuilder.create(TileEntityInventoryProxy::new, invProxy, invProxyPainted).build(null);
		craftingTerminalTile = FabricBlockEntityTypeBuilder.create(TileEntityCraftingTerminal::new, craftingTerminal).build(null);
		invHopperBasicTile = FabricBlockEntityTypeBuilder.create(TileEntityInventoryHopperBasic::new, invHopperBasic).build(null);
		levelEmitterTile = FabricBlockEntityTypeBuilder.create(TileEntityLevelEmitter::new, levelEmitter).build(null);

		storageTerminal = ScreenHandlerRegistry.registerSimple(id("ts.storage_terminal.container"), ContainerStorageTerminal::new);
		craftingTerminalCont = ScreenHandlerRegistry.registerSimple(id("ts.crafting_terminal.container"), ContainerCraftingTerminal::new);
		filteredConatiner = ScreenHandlerRegistry.registerSimple(id("ts.filtered.container"), ContainerFiltered::new);
		levelEmitterConatiner = ScreenHandlerRegistry.registerSimple(id("ts.level_emitter.container"), ContainerLevelEmitter::new);
		inventoryLink = ScreenHandlerRegistry.registerSimple(id("ts.inventory_link.container"), ContainerInventoryLink::new);

		Registry.register(Registry.BLOCK, id("ts.inventory_connector"), connector);
		Registry.register(Registry.BLOCK, id("ts.storage_terminal"), terminal);
		Registry.register(Registry.BLOCK, id("ts.open_crate"), openCrate);
		Registry.register(Registry.BLOCK, id("ts.trim"), inventoryTrim);
		Registry.register(Registry.BLOCK, id("ts.painted_trim"), paintedTrim);
		Registry.register(Registry.BLOCK, id("ts.inventory_cable"), invCable);
		Registry.register(Registry.BLOCK, id("ts.inventory_cable_framed"), invCableFramed);
		Registry.register(Registry.BLOCK, id("ts.inventory_cable_painted"), invCablePainted);
		Registry.register(Registry.BLOCK, id("ts.inventory_cable_connector"), invCableConnector);
		Registry.register(Registry.BLOCK, id("ts.inventory_cable_connector_filtered"), invCableConnectorFiltered);
		Registry.register(Registry.BLOCK, id("ts.inventory_proxy"), invProxy);
		Registry.register(Registry.BLOCK, id("ts.crafting_terminal"), craftingTerminal);
		Registry.register(Registry.BLOCK, id("ts.inventory_hopper_basic"), invHopperBasic);
		Registry.register(Registry.BLOCK, id("ts.level_emitter"), levelEmitter);
		Registry.register(Registry.BLOCK, id("ts.inventory_cable_connector_framed"), invCableConnectorFramed);
		Registry.register(Registry.BLOCK, id("ts.inventory_cable_connector_painted"), invCableConnectorPainted);
		Registry.register(Registry.BLOCK, id("ts.inventory_proxy_painted"), invProxyPainted);

		Registry.register(Registry.ITEM, id("ts.paint_kit"), paintingKit);
		Registry.register(Registry.ITEM, id("ts.wireless_terminal"), wirelessTerminal);
		Registry.register(Registry.ITEM, id("ts.adv_wireless_terminal"), advWirelessTerminal);

		Registry.register(Registry.BLOCK_ENTITY_TYPE, id("ts.inventory_connector.tile"), connectorTile);
		Registry.register(Registry.BLOCK_ENTITY_TYPE, id("ts.storage_terminal.tile"), terminalTile);
		Registry.register(Registry.BLOCK_ENTITY_TYPE, id("ts.open_crate.tile"), openCrateTile);
		Registry.register(Registry.BLOCK_ENTITY_TYPE, id("ts.painted.tile"), paintedTile);
		Registry.register(Registry.BLOCK_ENTITY_TYPE, id("ts.inventory_cable_connector.tile"), invCableConnectorTile);
		Registry.register(Registry.BLOCK_ENTITY_TYPE, id("ts.inventory_cable_connector_filtered.tile"), invCableConnectorFilteredTile);
		Registry.register(Registry.BLOCK_ENTITY_TYPE, id("ts.inventory_proxy.tile"), invProxyTile);
		Registry.register(Registry.BLOCK_ENTITY_TYPE, id("ts.crafting_terminal.tile"), craftingTerminalTile);
		Registry.register(Registry.BLOCK_ENTITY_TYPE, id("ts.inventoty_hopper_basic.tile"), invHopperBasicTile);
		Registry.register(Registry.BLOCK_ENTITY_TYPE, id("ts.level_emitter.tile"), levelEmitterTile);

		registerItemForBlock(connector);
		registerItemForBlock(terminal);
		registerItemForBlock(openCrate);
		registerItemForBlock(inventoryTrim);
		Registry.register(Registry.ITEM, Registry.BLOCK.getId(paintedTrim), new ItemBlockPainted(paintedTrim));
		registerItemForBlock(invCable);
		Registry.register(Registry.ITEM, Registry.BLOCK.getId(invCableFramed), new ItemBlockPainted(invCableFramed, new Item.Settings().group(STORAGE_MOD_TAB)));
		registerItemForBlock(invCableConnector);
		registerItemForBlock(invCableConnectorFiltered);
		registerItemForBlock(invProxy);
		registerItemForBlock(craftingTerminal);
		registerItemForBlock(invHopperBasic);
		registerItemForBlock(levelEmitter);
		registerItemForBlock(invCableConnectorFramed);

		ServerPlayNetworking.registerGlobalReceiver(NetworkHandler.DATA_C2S, (s, p, h, buf, rp) -> {
			NbtCompound tag = buf.readUnlimitedNbt();
			s.submit(() -> {
				if(p.currentScreenHandler instanceof IDataReceiver) {
					((IDataReceiver)p.currentScreenHandler).receive(tag);
				}
			});
		});

		ServerLoginNetworking.registerGlobalReceiver(id("config"), (server, handler, understood, buf, sync, respSender) -> {
		});

		ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, sync) -> {
			PacketByteBuf packet = PacketByteBufs.create();
			try (OutputStreamWriter writer = new OutputStreamWriter(new ByteBufOutputStream(packet))){
				gson.toJson(LOADED_CONFIG, writer);
			} catch (IOException e) {
				LOGGER.warn("Error sending config sync", e);
			}
			sender.sendPacket(id("config"), packet);
		});

		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			CONFIG = LOADED_CONFIG;
		});

		StorageTags.init();

		configHolder.registerSaveListener((a, b) -> {
			multiblockInvs = null;
			return ActionResult.PASS;
		});
	}

	private static void registerItemForBlock(Block block) {
		Registry.register(Registry.ITEM, Registry.BLOCK.getId(block), new BlockItem(block, new Item.Settings().group(STORAGE_MOD_TAB)));
	}
}
