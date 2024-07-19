package petstore;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;

import io.petstore.PetResource;
import io.petstore.beans.ApiResponse;
import io.petstore.beans.Pet;
import io.smallrye.mutiny.Uni;

public class PetStoreImpl implements PetResource {

    private static final Map<Long, Pet> PETS = new HashMap<>();

    @Override
    public Uni<RestResponse<Pet>> updatePet(Pet data) {
        return Uni.createFrom().item(RestResponse.ok(PETS.put(data.getId(), data)));
    }

    @Override
    public Uni<RestResponse<Pet>> addPet(Pet data) {
        PETS.put(data.getId(), data);
        return Uni.createFrom().item(RestResponse.noContent());
    }

    @Override
    public Uni<RestResponse<List<Pet>>> findPetsByStatus(String status) {
        return null;
    }

    @Override
    public Uni<RestResponse<List<Pet>>> findPetsByTags(List<String> tags) {
        return null;
    }

    @Override
    public Uni<RestResponse<Pet>> getPetById(long petId) {
        return Uni.createFrom().item(RestResponse.ok(PETS.get(petId)));
    }

    @Override
    public Uni<Response> updatePetWithForm(long petId, String name, String status) {
        return Uni.createFrom().nullItem();
    }

    @Override
    public Uni<Response> deletePet(String apiKey, long petId) {
        PETS.remove(petId);
        return Uni.createFrom().nullItem();
    }

    @Override
    public Uni<RestResponse<ApiResponse>> uploadFile(long petId, String additionalMetadata, InputStream data) {
        return Uni.createFrom().nullItem();
    }
}
