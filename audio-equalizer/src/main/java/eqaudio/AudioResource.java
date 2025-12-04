package eqaudio;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("/audio")
public class AudioResource {

    @Inject
    PlayerLauncher launcher;

    @GET
    @Path("/play")
    public Response play(@QueryParam("file") String file) {

        if (file == null || file.isBlank()) {
            return Response.status(400).entity("Parâmetro file= obrigatório").build();
        }

        try {
            launcher.play(file);
            return Response.ok("Reproduzindo (em processo externo): " + file).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError()
                    .entity("Erro ao lançar player: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/stop")
    public Response stop() {
        launcher.stop();
        return Response.ok("Stop enviado para o player externo.").build();
    }
}
