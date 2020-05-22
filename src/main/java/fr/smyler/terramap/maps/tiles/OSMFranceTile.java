package fr.smyler.terramap.maps.tiles;

import java.net.MalformedURLException;
import java.net.URL;

import fr.smyler.terramap.TerramapMod;
import fr.smyler.terramap.TerramapUtils;

public class OSMFranceTile extends RasterWebTile {

	private char[] servers = {'a', 'b', 'c'};
	
	public OSMFranceTile(int zoom, long x, long y) {
		super(256, zoom, x, y);
	}

	@Override
	public URL getURL() {
		URL url = null;
		try {
			url = new URL("https://" + TerramapUtils.pickChar(servers) + ".tile.openstreetmap.fr/osmfr/"
					+ this.getZoom() + "/" + this.getX() + "/" + this.getY() + ".png");
		} catch (MalformedURLException e) {
			TerramapMod.logger.error("Failed to craft a valid URL for a tile, please report to the mod author: " + TerramapMod.AUTHOR_EMAIL);
			TerramapMod.logger.catching(e);
		}
		return url;
	}

}
