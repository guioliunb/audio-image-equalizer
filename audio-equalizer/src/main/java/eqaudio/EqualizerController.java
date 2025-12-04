package eqaudio;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;

@Path("/eq")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EqualizerController {

    @Inject
    EqualizerState eqState;  // estados internos dos ganhos

    @Inject
    GainClient gainClient;   // envia os comandos ao PlayerStandalone

    // ======== GET /eq/bands ========
    @GET
    @Path("/bands")
    public Map<Integer, Double> getBandGains() {
        return eqState.getAllGainsDb();
    }

    // ======== PUT /eq/bands/{band}/gain ========
    @PUT
    @Path("/bands/{band}/gain")
    public Response setBandGain(
            @PathParam("band") int band,
            Map<String, Double> body
    ) {
        if (!body.containsKey("gainDb")) {
            return Response.status(400).entity(Map.of(
                    "error", "JSON must contain gainDb"
            )).build();
        }

        double gainDb = body.get("gainDb");

        if (!eqState.setGainDb(band, gainDb)) {
            return Response.status(400).entity(Map.of(
                    "error", "Invalid band index"
            )).build();
        }

        // Enviar ao PlayerStandalone via socket TCP
        Map<String, Object> msg = Map.of(
                "type", "setGain",
                "band", band,
                "gainDb", gainDb
        );

        gainClient.sendToPlayer(msg);

        return Response.ok(Map.of(
                "band", band,
                "gainDb", gainDb,
                "status", "updated"
        )).build();
    }

    // ======== PUT /eq/bands ========
    @PUT
    @Path("/bands")
    public Response setAllGains(Map<String, Double> body) {

        Map<String, Object> socketMsg = new HashMap<>();
        socketMsg.put("type", "setAll");

        for (int band = 0; band < 5; band++) {
            String key = "band" + band;

            if (body.containsKey(key)) {
                double db = body.get(key);
                eqState.setGainDb(band, db);
                socketMsg.put(key, db);
            }
        }

        // Envia tudo ao PlayerStandalone
        gainClient.sendToPlayer(socketMsg);

        return Response.ok(eqState.getAllGainsDb()).build();
    }

    // ======== GET /eq/status ========
    @GET
    @Path("/status")
    public Response getStatus() {
        return Response.ok(Map.of(
                "bands", eqState.getAllGainsDb(),
                "playerConnection", "socket-client"
        )).build();
    }
}
