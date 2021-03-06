package fr.thesmyler.terramap.gui.widgets.markers.markers;

import fr.thesmyler.terramap.TerramapRemote;
import fr.thesmyler.terramap.gui.widgets.map.MapWidget;
import fr.thesmyler.terramap.gui.widgets.markers.controllers.MarkerController;
import fr.thesmyler.terramap.network.playersync.TerramapLocalPlayer;
import fr.thesmyler.terramap.network.playersync.TerramapPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class OtherPlayerMarker extends AbstractPlayerMarker {
	
	protected TerramapPlayer player;

	public OtherPlayerMarker(MarkerController<?> controller, TerramapPlayer player) {
		super(controller, null);
		this.player = player;
	}
	
	@Override
	public void update(MapWidget map) {
		super.update(map);
		if(
				!TerramapRemote.getRemote().hasPlayer(this.player.getUUID())
			  || (this.player instanceof TerramapLocalPlayer && ((TerramapLocalPlayer) this.player).getPlayer().isDead)) {
			map.scheduleForNextScreenUpdate(() -> map.removeWidget(this));
		}
	}
	
	public TerramapPlayer getPlayer() {
		return this.player;
	}
	
	@Override
	protected ResourceLocation getSkin() {
		return this.player.getSkin();
	}
	
	

	@Override
	public int getDeltaX() {
		return -8;
	}

	@Override
	public int getDeltaY() {
		return -8;
	}

	@Override
	protected float getTransparency() {
		return this.player.isSpectator() ? 0.6f: 1f;
	}

	@Override
	protected boolean showName(boolean hovered) {
		return !this.player.isSpectator() || hovered;
	}
	
	@Override
	public ITextComponent getDisplayName() {
		return this.player.getDisplayName();
	}

	@Override
	public String getIdentifier() {
		return this.getControllerId() + ":" + this.player.getUUID().toString();
	}

	@Override
	protected double[] getActualCoordinates() {
		return this.player.getGeoCoordinates();
	}

}
