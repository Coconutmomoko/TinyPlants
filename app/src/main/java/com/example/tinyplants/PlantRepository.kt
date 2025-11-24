package com.example.tinyplants

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

private interface PlantApi {
    @GET("plants.json")
    suspend fun getPlants(): List<Plant>
}

class PlantRepository {

    private val api: PlantApi = Retrofit.Builder()

        .baseUrl("https://raw.githubusercontent.com/Coconutmomoko/TinyPlants/main/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(PlantApi::class.java)

    suspend fun fetchPlants(): List<Plant> = api.getPlants()


}
