package com.capstone.cameraex.gps;

import android.util.Log;

import com.capstone.cameraex.network.ApiService;
import com.capstone.cameraex.network.NetworkClient;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class GpsService {

    private ApiService apiService;

    public GpsService() {
        Retrofit retrofit = NetworkClient.getClient();
        apiService = retrofit.create(ApiService.class);
    }

    public void saveDetectLocation(DetectLocation detectLocation) {
        Call<DetectLocationResponse> call = apiService.postDetectLocation(detectLocation);
        call.enqueue(new Callback<DetectLocationResponse>() {
            @Override
            public void onResponse(Call<DetectLocationResponse> call, Response<DetectLocationResponse> response) {
                if (response.isSuccessful()) {
                    Log.d("DB","위치 정보 저장 완료");
                }else {
                    Log.d("DB", "위치 정보 저장 실패: " + response.code() + " " + response.message());
                    // 추가적인 응답 내용 확인
                    try {
                        Log.d("DB", "Error Body: " + response.errorBody().string());
                    } catch (IOException e) {
                        Log.e("DB", "Error reading response body", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<DetectLocationResponse> call, Throwable t) {
                Log.d("DB","위치 정보 저장 실패");
            }
        });
    }
}
