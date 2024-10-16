package com.capstone.cameraex.network;

import com.capstone.cameraex.gps.DetectLocation;
import com.capstone.cameraex.gps.DetectLocationResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {
    @POST("location")
    Call<DetectLocationResponse> postDetectLocation(@Body DetectLocation detectLocation);
    @GET("location/{detectLocation-id}")
    Call<DetectLocation> getDetectLocation(@Path("detectLocation-id") Long id);
    @DELETE("location/{detectLocation-id}")
    Call<DetectLocationResponse> deleteDetectLocation(@Path("detectLocation-id") Long id);
}


