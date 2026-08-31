package tvs.mcsb.client;

import net.fabricmc.api.ClientModInitializer;
import tvs.mcsb.Mcsb;

public class McsbClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		
		Mcsb.LOGGER.info("MCSB | Initializing a client that does nothing :)");

	}
}