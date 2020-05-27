package fr.smyler.terramap;

import java.util.ArrayList;
import java.util.List;

import fr.smyler.terramap.config.TerramapConfiguration;
import fr.smyler.terramap.network.S2CPlayerSyncPacket;
import fr.smyler.terramap.network.S2CTerramapHelloPacket;
import fr.smyler.terramap.network.TerramapLocalPlayer;
import fr.smyler.terramap.network.TerramapPacketHandler;
import io.github.terra121.EarthGeneratorSettings;
import io.github.terra121.EarthWorldType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * The event subscriber for generic server events
 * 
 * @author Smyler
 *
 */
@Mod.EventBusSubscriber(modid=TerramapMod.MODID)
public final class TerramapEventHandler {

	private static long tickCounter = 0;

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerLoggedInEvent event){
		//Send world data to the client
		TerramapMod.proxy.onPlayerLoggedIn(event);
		EntityPlayerMP player = (EntityPlayerMP)event.player;
		World world = player.getEntityWorld();
		if(!(world.getWorldType() instanceof EarthWorldType)) return;
		EarthGeneratorSettings settings = S2CTerramapHelloPacket.getEarthGeneratorSettingsFromWorld(world);
		if(settings == null) return;
		IMessage data = new S2CTerramapHelloPacket(settings);
		TerramapPacketHandler.INSTANCE.sendTo(data, player);

	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) { //FIXME Never called on Client
		TerramapMod.proxy.onPlayerLoggedOut(event);
	}
	
	@SubscribeEvent
	public static void onClientDisconnect(ClientDisconnectionFromServerEvent event) {
		TerramapServer.resetServer();
	}
	
	@SubscribeEvent
	public static void onClientConnected(ClientConnectedToServerEvent event) {
	}
	

	@SubscribeEvent
	public static void onWorldTick(WorldTickEvent event) {
		//TODO Only sync players which changed
		if(!TerramapConfiguration.synchronizePlayers) return;
		if(!event.phase.equals(TickEvent.Phase.END)) return;
		World world = event.world.getMinecraftServer().worlds[0];
		if(!(world.getWorldType() instanceof EarthWorldType)) return;
		if(tickCounter == 0) {
			List<TerramapLocalPlayer> players = new ArrayList<TerramapLocalPlayer>();
			for(EntityPlayer player: world.playerEntities) {
				TerramapLocalPlayer terraPlayer = new TerramapLocalPlayer(player);
				if(terraPlayer.isSpectator() && !TerramapConfiguration.syncSpectators) continue;
				players.add(terraPlayer);
			}
			IMessage pkt = new S2CPlayerSyncPacket(players.toArray(new TerramapLocalPlayer[players.size()]));
			for(EntityPlayer player: world.playerEntities) TerramapPacketHandler.INSTANCE.sendTo(pkt, (EntityPlayerMP)player);
		}
		tickCounter = (tickCounter+1) % TerramapConfiguration.syncInterval;
	}

}
