package fr.thesmyler.terramap.eventhandlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import fr.thesmyler.smylibgui.SmyLibGui;
import fr.thesmyler.smylibgui.screen.HudScreen;
import fr.thesmyler.smylibgui.screen.Screen;
import fr.thesmyler.smylibgui.screen.TestScreen;
import fr.thesmyler.terramap.GeoServices;
import fr.thesmyler.terramap.MapContext;
import fr.thesmyler.terramap.TerramapRemote;
import fr.thesmyler.terramap.config.TerramapConfig;
import fr.thesmyler.terramap.gui.widgets.map.MapWidget;
import fr.thesmyler.terramap.gui.widgets.markers.controllers.AnimalMarkerController;
import fr.thesmyler.terramap.gui.widgets.markers.controllers.MobMarkerController;
import fr.thesmyler.terramap.gui.widgets.markers.controllers.OtherPlayerMarkerController;
import fr.thesmyler.terramap.input.KeyBindings;
import fr.thesmyler.terramap.maps.TiledMap;
import io.github.terra121.projection.GeographicProjection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Event handler for the physical client
 *
 */
@SideOnly(Side.CLIENT)
public class ClientTerramapEventHandler {
	
	private boolean testScreenWasShown = false;
	private boolean configWasFixed = false;
	
	@SubscribeEvent
	public void onRenderHUD(RenderGameOverlayEvent.Text e) {
		if(Minecraft.getMinecraft().gameSettings.showDebugInfo) {
			GeographicProjection proj = TerramapRemote.getRemote().getProjection();
			if(proj != null) {
				e.getLeft().add("");
				double x = Minecraft.getMinecraft().player.posX;
				double z = Minecraft.getMinecraft().player.posZ;
				double[] coords = proj.toGeo(x, z);
				if(Double.isFinite(coords[0]) && Double.isFinite(coords[1])) {
					String lon = GeoServices.formatGeoCoordForDisplay(coords[0]);
					String lat = GeoServices.formatGeoCoordForDisplay(coords[1]);
					e.getLeft().add("Position: " + lat + "° " + lon + "°");
				}
				
			}
		}
	}

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
    	KeyBindings.checkBindings();
    }
    
	@SubscribeEvent
	public void onClientDisconnect(ClientDisconnectionFromServerEvent event) {
		Minecraft.getMinecraft().addScheduledTask(TerramapRemote::resetRemote); // This event is called from the network thread
	}

	@SubscribeEvent
	public void onClientConnected(ClientConnectedToServerEvent event) {
		Minecraft.getMinecraft().addScheduledTask(() -> TerramapRemote.getRemote().guessRemoteIdentifier()); // This event is called from the network thread
	}
	
	@SubscribeEvent
	public void onChangeDimension(PlayerChangedDimensionEvent event) {
		if(event.player.world.isRemote)
			TerramapRemote.getRemote().resetServerMapStyles();
	}
	
	@SubscribeEvent
	public void onGuiScreenInit(InitGuiEvent event) {
		if(event.getGui() instanceof GuiMainMenu && !configWasFixed) {
			/* 
			 * Unfortunately, Forge's ConfigManager does not let us modify our config when the game is still loading and 
			 * and calling ConfigManager::sync only injects the file's value into the fields instead of saving them to disk,
			 * which is why we have to do it once the game is fully loaded.
			 * 
			 * This is called on the physical server by TerramapServerProxy::onServerStarting .
			 * 
			 */
		    TerramapConfig.update(); // Update if invalid values were left by old versions
		    configWasFixed = true;
		}
		if(SmyLibGui.debug && !testScreenWasShown && !(event.getGui() instanceof Screen)) {
			Minecraft.getMinecraft().displayGuiScreen(new TestScreen(event.getGui()));
			this.testScreenWasShown = true;
		} else if(event.getGui() instanceof HudScreen) {
			
			//TODO Only show the minimap on overworld earth worlds
			
			HudScreen screen = (HudScreen) event.getGui();
			screen.removeAllWidgets();
			screen.cancellAllScheduled();
			
			if(TerramapConfig.minimapEnable) {
				MapWidget map = new MapWidget(10, TerramapRemote.getRemote().getMapStyles().values().toArray(new TiledMap[0])[0], MapContext.MINIMAP, TerramapConfig.getEffectiveTileScaling());
				map.setInteractive(false);
				map.setX((int) (TerramapConfig.minimapPosX * 0.01 * screen.getWidth()));
				map.setY((int) (TerramapConfig.minimapPosX * 0.01 * screen.getWidth()));
				map.setWidth((int) (TerramapConfig.minimapWidth * 0.01 * screen.getWidth()));
				map.setHeight((int) (TerramapConfig.minimapHeight * 0.01 * screen.getWidth()));
				Map<String, Boolean> markerVisibility = new HashMap<String, Boolean>();
				markerVisibility.put(AnimalMarkerController.ID, TerramapConfig.showEntities);
				markerVisibility.put(MobMarkerController.ID, TerramapConfig.showEntities);
				markerVisibility.put(OtherPlayerMarkerController.ID, TerramapConfig.minimapShowOtherPlayers);
				map.setMarkersVisibility(markerVisibility);
				Map<String, TiledMap> styles = TerramapRemote.getRemote().getMapStyles();
				TiledMap bg = styles.get(TerramapConfig.minimapStyle);
				if(bg == null || ! bg.isAllowedOnMinimap()) {
					ArrayList<TiledMap> maps = new ArrayList<TiledMap>(styles.values());
					Collections.sort(maps, Collections.reverseOrder());
					bg = maps.get(0);
				}
				map.setBackground(bg);
				int zoomLevel = Math.max(bg.getMinZoom(), TerramapConfig.minimapZoomLevel);
				zoomLevel = Math.min(bg.getMaxZoom(), TerramapConfig.minimapZoomLevel);
				map.setZoom(zoomLevel);
				map.setZoom(TerramapConfig.minimapZoomLevel);
				map.setCopyrightVisibility(false);
				map.setScaleVisibility(false);
				screen.addWidget(map);
				screen.scheduleAtUpdate(() -> {
					if(TerramapRemote.getRemote().getProjection() != null) {
						map.track(map.getMainPlayerMarker());
					}
				});
			}
			
		}
	}
	
}
